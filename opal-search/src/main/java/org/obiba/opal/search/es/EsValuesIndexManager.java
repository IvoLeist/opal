/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.search.es;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.obiba.magma.Value;
import org.obiba.magma.ValueTable;
import org.obiba.magma.Variable;
import org.obiba.magma.VariableEntity;
import org.obiba.magma.concurrent.ConcurrentValueTableReader;
import org.obiba.magma.concurrent.ConcurrentValueTableReader.ConcurrentReaderCallback;
import org.obiba.magma.type.BinaryType;
import org.obiba.magma.type.DateType;
import org.obiba.opal.core.domain.VariableNature;
import org.obiba.opal.core.service.VariableStatsService;
import org.obiba.opal.search.IndexSynchronization;
import org.obiba.opal.search.ValueTableIndex;
import org.obiba.opal.search.ValueTableValuesIndex;
import org.obiba.opal.search.ValuesIndexManager;
import org.obiba.opal.search.es.mapping.ValueTableMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@Component
public class EsValuesIndexManager extends EsIndexManager implements ValuesIndexManager {

//  private static final Logger log = LoggerFactory.getLogger(EsValuesIndexManager.class);

  @Autowired
  @Nonnull
  private ThreadFactory threadFactory;

  @Autowired
  @Nonnull
  private VariableStatsService variableStatsService;

  @Nonnull
  @Override
  public EsValueTableValuesIndex getIndex(@Nonnull ValueTable vt) {
    return (EsValueTableValuesIndex) super.getIndex(vt);
  }

  @Override
  protected ValueTableIndex createIndex(@Nonnull ValueTable vt) {
    return new EsValueTableValuesIndex(vt);
  }

  @Nonnull
  @Override
  public IndexSynchronization createSyncTask(ValueTable valueTable, ValueTableIndex index) {
    return new Indexer(valueTable, (EsValueTableValuesIndex) index);
  }

  @Nonnull
  @Override
  public String getName() {
    return esIndexName() + "-values";
  }

  private class Indexer extends EsIndexer {

    private final EsValueTableValuesIndex index;

    private Indexer(ValueTable table, EsValueTableValuesIndex index) {
      super(table, index);
      this.index = index;
    }

    @Override
    protected void index() {
      ConcurrentValueTableReader.Builder.newReader().withThreads(threadFactory).ignoreReadErrors().from(valueTable)
          .variables(index.getVariables()).to(new ValuesReaderCallback()).build().read();
    }

    private class ValuesReaderCallback implements ConcurrentReaderCallback {

      private BulkRequestBuilder bulkRequest = opalSearchService.getClient().prepareBulk();

      private final Map<Variable, VariableNature> natures = new HashMap<Variable, VariableNature>();

      @Override
      public void onBegin(List<VariableEntity> entitiesToCopy, Variable... variables) {
        for(Variable variable : variables) {
          natures.put(variable, VariableNature.getNature(variable));
        }
      }

      @Override
      public void onValues(VariableEntity entity, Variable[] variables, Value... values) {
        if(stop) {
          return;
        }

        String identifier = entity.getIdentifier();
        bulkRequest.add(opalSearchService.getClient().prepareIndex(getName(), valueTable.getEntityType(), identifier)
            .setSource("{\"identifier\":\"" + identifier + "\"}"));
        try {
          XContentBuilder builder = XContentFactory.jsonBuilder().startObject();
          for(int i = 0; i < variables.length; i++) {
            indexValue(builder, variables[i], values[i]);
          }
          builder.endObject();

          IndexRequestBuilder requestBuilder = opalSearchService.getClient()
              .prepareIndex(getName(), index.getIndexName(), identifier).setParent(identifier).setSource(builder);
          bulkRequest.add(requestBuilder);
          done++;
          if(bulkRequest.numberOfActions() >= ES_BATCH_SIZE) {
            bulkRequest = sendAndCheck(bulkRequest);
          }
        } catch(IOException e) {
          throw new RuntimeException(e);
        }
      }

      private void indexValue(XContentBuilder xcb, Variable variable, Value value) throws IOException {
        String fieldName = index.getFieldName(variable.getName());
        if(value.isSequence() && !value.isNull()) {
          List<Object> vals = Lists.newArrayList();
          //noinspection ConstantConditions
          for(Value v : value.asSequence().getValue()) {
            vals.add(esValue(variable, v));
          }
          xcb.field(fieldName, vals);
        } else {
          xcb.field(fieldName, esValue(variable, value));
        }
        variableStatsService.stackVariable(getValueTable(), variable, value);
      }

      @Override
      public void onComplete() {
        if(stop) {
          index.delete();
          variableStatsService.clearComputingSummaries(getValueTable());
        } else {
          sendAndCheck(bulkRequest);
          variableStatsService.computeSummaries(getValueTable());
          index.updateTimestamps();
        }
      }

      @Override
      public boolean isCancelled() {
        return stop;
      }

      /**
       * OPAL-1158: missing values are indexed as null for continuous variables
       *
       * @param variable the variable
       * @param value the value
       * @return an object
       */
      @Nullable
      private Object esValue(Variable variable, Value value) {
        switch(natures.get(variable)) {
          case CONTINUOUS:
            if(variable.isMissingValue(value)) {
              return null;
            }
        }
        Object obj = value.getValue();
        if(obj != null && value.getValueType() == DateType.get()) {
          return obj.toString(); // ie MagmaDate.toString()
        }
        return obj;
      }
    }

  }

  private class EsValueTableValuesIndex extends EsValueTableIndex implements ValueTableValuesIndex {

    private EsValueTableValuesIndex(ValueTable vt) {
      super(vt);
    }

    @Override
    public String getFieldName(String variable) {
      return getIndexName() + "-" + variable;
    }

    @Override
    protected XContentBuilder getMapping() {
      return new ValueTableMapping().createMapping(runtimeVersionProvider.getVersion(), getIndexName(), resolveTable());
    }

    @Override
    public Iterable<Variable> getVariables() {
      // Do not index binary values, do not even extract the binary values
      // TODO Could be configurable at table level?
      return Iterables.filter(resolveTable().getVariables(), new Predicate<Variable>() {

        @Override
        public boolean apply(Variable input) {
          return !input.getValueType().isGeo() && !input.getValueType().equals(BinaryType.get());
        }

      });
    }

  }
}
