package org.obiba.opal.datasource.onyx.elmo;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.obiba.onyx.engine.variable.Attribute;
import org.obiba.onyx.engine.variable.Category;
import org.obiba.onyx.engine.variable.Variable;
import org.obiba.onyx.engine.variable.VariableHelper;
import org.obiba.opal.datasource.onyx.variable.IVariableQNameStrategy;
import org.obiba.opal.datasource.onyx.variable.VariableVisitor;
import org.obiba.opal.elmo.OpalOntologyManager;
import org.obiba.opal.elmo.concepts.CategoricalVariable;
import org.obiba.opal.elmo.concepts.ContinuousVariable;
import org.obiba.opal.elmo.concepts.DataEntryForm;
import org.obiba.opal.elmo.concepts.DataVariable;
import org.obiba.opal.elmo.concepts.MissingCategory;
import org.obiba.opal.elmo.concepts.OccurrenceItem;
import org.obiba.opal.elmo.concepts.Opal;
import org.obiba.opal.elmo.concepts.dataValue;
import org.obiba.opal.elmo.concepts.hasCategory;
import org.obiba.opal.elmo.owl.concepts.CategoryClass;
import org.obiba.opal.elmo.owl.concepts.DataEntryFormClass;
import org.obiba.opal.elmo.owl.concepts.DataItemClass;
import org.openrdf.OpenRDFException;
import org.openrdf.concepts.owl.Class;
import org.openrdf.concepts.owl.DatatypeProperty;
import org.openrdf.concepts.owl.Ontology;
import org.openrdf.concepts.owl.Restriction;
import org.openrdf.elmo.sesame.SesameManager;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElmoVariableVisitor implements VariableVisitor {

  private static final Logger log = LoggerFactory.getLogger(ElmoVariableVisitor.class);

  final OpalOntologyManager opal;

  final List<Handler> handlers = new LinkedList<Handler>();

  final IVariableQNameStrategy qnameStrategy;

  private SesameManagerFactory managerFactory;

  private SesameManager manager;

  private DataEntryFormClass currentDEF;

  public ElmoVariableVisitor(IVariableQNameStrategy qnameStrategy, SesameManagerFactory managerFactory) throws OpenRDFException, IOException {
    this.opal = new OpalOntologyManager();
    this.managerFactory = managerFactory;
    this.qnameStrategy = qnameStrategy;

    // Order of handler is important!
    handlers.add(new CategoryHandler());
    handlers.add(new CategoricalHandler());
    handlers.add(new OccurrenceHandler());
    handlers.add(new ContinuousHandler());
    handlers.add(new DataVariableHandler());
  }

  public void forDataEntryForm(Variable def) {
    if(manager == null) {
      this.manager = managerFactory.createElmoManager();
      this.manager.getTransaction().begin();
      Ontology ontology = manager.create(QName.valueOf(qnameStrategy.getBaseUri()), Ontology.class);
      Ontology opalOntology = opal.getOpalNode(Opal.class, Ontology.class);
      ontology.getOwlImports().add(opalOntology);
    }

    QName qname = qnameStrategy.getQName(def);
    Class opalDEF = opal.getOpalClass(DataEntryForm.class);
    currentDEF = manager.create(qname, DataEntryFormClass.class);
    currentDEF.getRdfsSubClassOf().add(opalDEF);
  }

  public void visit(Variable variable) {
    QName variableQName = qnameStrategy.getQName(variable);
    if(manager.find(Class.class, variableQName) != null) {
      log.debug("Variable already exists: {}", variableQName);
      return;
    }

    for(Handler h : handlers) {
      if(h.handles(variable)) {
        DataItemClass opalOnyxVariable = h.handle(variable);
        commonHandling(variable, opalOnyxVariable);
        System.out.print('.');
        break;
      }
    }

    for(Variable v : variable.getVariables()) {
      visit(v);
    }

  }

  public void end() {
    if(manager != null) {
      this.manager.getTransaction().commit();
      manager.close();
      manager = null;
    }
  }

  protected void commonHandling(Variable variable, DataItemClass opalOnyxVariable) {
    currentDEF.getDataVariables().add(opalOnyxVariable);
    annotate(opalOnyxVariable, variable.getAttributes());
    createHierarchy(variable, opalOnyxVariable);
  }

  protected void createHierarchy(Variable childVariable, DataItemClass child) {
    QName parentQName = qnameStrategy.getQName(childVariable.getParent());
    if(parentQName.equals(currentDEF.getQName())) {
      return;
    }
    DataItemClass parent = manager.find(DataItemClass.class, parentQName);
    child.setParent(parent);
    parent.getChildren().add(child);
  }

  public interface Handler {
    public boolean handles(Variable var);

    public DataItemClass handle(Variable var);
  }

  public class CategoryHandler implements Handler {
    public DataItemClass handle(Variable onyxVariable) {
      QName qname = qnameStrategy.getQName(onyxVariable);

      Category onyxCategory = (Category) onyxVariable;

      Class opalCategory = opal.getOpalClass(org.obiba.opal.elmo.concepts.Category.class);

      if(onyxCategory.getEscape() != null && onyxCategory.getEscape() == true) {
        opalCategory = opal.getOpalClass(MissingCategory.class);
      }
      CategoryClass opalOnyxVariable = manager.create(qname, CategoryClass.class);
      opalOnyxVariable.getRdfsSubClassOf().add(opalCategory);
      opalOnyxVariable.setCode(onyxCategory.getAlternateName());

      QName parentQName = qnameStrategy.getQName(onyxVariable.getParent());
      org.openrdf.concepts.rdfs.Class parentVariable = manager.find(org.openrdf.concepts.rdfs.Class.class, parentQName);

      for(Object c : parentVariable.getRdfsSubClassOf()) {
        if(c instanceof Restriction) {
          Restriction r = (Restriction) c;
          if(r.getOwlAllValuesFrom() != null) {
            Class allValues = (Class) r.getOwlAllValuesFrom();
            allValues.getOwlUnionOf().add(opalOnyxVariable);
          }
        }
      }

      return opalOnyxVariable;
    }

    public boolean handles(Variable var) {
      return var instanceof Category;
    }
  }

  public class CategoricalHandler implements Handler {
    public DataItemClass handle(Variable onyxVariable) {
      QName qname = qnameStrategy.getQName(onyxVariable);

      Class opalVariable = opal.getOpalClass(CategoricalVariable.class);
      DataItemClass opalOnyxVariable = manager.create(qname, DataItemClass.class);
      opalOnyxVariable.getRdfsSubClassOf().add(opalVariable);
      opalOnyxVariable.setMultiple(onyxVariable.isMultiple());

      Restriction r = manager.create(Restriction.class);
      org.openrdf.concepts.owl.ObjectProperty hasCategory = opal.getOpalProperty(hasCategory.class);
      r.setOwlOnProperty(hasCategory);
      if(onyxVariable.isMultiple()) {
        r.setOwlMinCardinality(BigInteger.ONE);
      } else {
        r.setOwlCardinality(BigInteger.ONE);
      }
      opalOnyxVariable.getRdfsSubClassOf().add(r);

      r = manager.create(Restriction.class);
      Class union = manager.create(Class.class);
      org.openrdf.concepts.rdf.List<? extends Class> l = manager.create(org.openrdf.concepts.rdf.List.class);
      union.setOwlUnionOf(l);
      r.setOwlOnProperty(hasCategory);
      r.setOwlAllValuesFrom(union);
      opalOnyxVariable.getRdfsSubClassOf().add(r);

      return opalOnyxVariable;
    }

    public boolean handles(Variable var) {
      return var.isCategorial();
    }
  }

  public class ContinuousHandler implements Handler {
    public DataItemClass handle(Variable onyxVariable) {
      QName qname = qnameStrategy.getQName(onyxVariable);

      Class opalVariable = opal.getOpalClass(ContinuousVariable.class);

      DataItemClass opalOnyxVariable = manager.create(qname, DataItemClass.class);
      opalOnyxVariable.getRdfsSubClassOf().add(opalVariable);

      Restriction r = manager.create(Restriction.class);
      DatatypeProperty dataValue = opal.getOpalProperty(dataValue.class);
      r.setOwlOnProperty(dataValue);
      r.setOwlCardinality(BigInteger.ONE);
      opalOnyxVariable.getRdfsSubClassOf().add(r);

      return opalOnyxVariable;
    }

    public boolean handles(Variable var) {
      return var.isCategorial() == false && var.getDataType() != null;
    }
  }

  public class DataVariableHandler implements Handler {
    public DataItemClass handle(Variable onyxVariable) {
      QName qname = qnameStrategy.getQName(onyxVariable);
      Class opalVariable = opal.getOpalClass(DataVariable.class);

      DataItemClass opalOnyxVariable = manager.create(qname, DataItemClass.class);
      opalOnyxVariable.getRdfsSubClassOf().add(opalVariable);
      return opalOnyxVariable;
    }

    public boolean handles(Variable var) {
      // Catch all
      return true;
    }
  }

  public class OccurrenceHandler implements Handler {
    public DataItemClass handle(Variable onyxVariable) {
      QName qname = qnameStrategy.getQName(onyxVariable);

      org.openrdf.concepts.owl.Class opalVariable = opal.getOpalClass(OccurrenceItem.class);

      DataItemClass opalOnyxVariable = manager.create(qname, DataItemClass.class);
      opalOnyxVariable.getRdfsSubClassOf().add(opalVariable);

      return opalOnyxVariable;
    }

    public boolean handles(Variable var) {
      return var.isRepeatable();
    }
  }

  private void annotate(DataItemClass opalOnyxVariable, List<Attribute> attributes) {
    for(Attribute attr : attributes) {
      log.debug("{}@{}={}", new Object[] { attr.getKey(), attr.getLocale(), attr.getValue().toString() });
      manager.setLocale(attr.getLocale());
      if(attr.getKey().equals("label")) {
        opalOnyxVariable.setRdfsLabel(attr.getValue().toString());
      } else if(attr.getKey().equals(VariableHelper.CONDITION)) {
        opalOnyxVariable.setCondition(attr.getValue().toString());
      } else if(attr.getKey().equals(VariableHelper.OCCURRENCE)) {
        opalOnyxVariable.setOccurrence(attr.getValue().toString());
      } else if(attr.getKey().equals(VariableHelper.SOURCE)) {
        opalOnyxVariable.setSource(attr.getValue().toString());
      } else if(attr.getKey().equals(VariableHelper.VALIDATION)) {
        opalOnyxVariable.setValidation(attr.getValue().toString());
      }

      manager.setLocale(null);
    }
  }

}
