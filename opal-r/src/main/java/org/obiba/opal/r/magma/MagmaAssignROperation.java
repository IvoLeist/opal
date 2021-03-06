/*
 * Copyright (c) 2020 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.r.magma;

import com.google.common.base.Strings;
import org.obiba.magma.ValueTable;
import org.obiba.opal.core.service.DataExportService;
import org.obiba.opal.core.service.IdentifiersTableService;
import org.obiba.opal.spi.r.AbstractROperation;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;

import javax.validation.constraints.NotNull;
import java.io.InputStream;

/**
 * Assign Magma values (from a table or a variable) to a R symbol.
 */
public class MagmaAssignROperation extends AbstractROperation {

  public enum RClass {
    DATA_FRAME, TIBBLE, TIBBLE_WITH_FACTORS, VECTOR
  }

  @NotNull
  private final IdentifiersTableService identifiersTableService;

  @NotNull
  private final DataExportService dataExportService;

  @NotNull
  private final String symbol;

  @NotNull
  private final String path;

  private final ValueTable valueTable;

  private final String variableFilter;

  private final boolean withMissings;

  private final String identifiersMapping;

  private final String idColumnName;

  private final RClass rClass;

  public MagmaAssignROperation(@NotNull String symbol, @NotNull ValueTable valueTable, @NotNull DataExportService dataExportService, String idColumnName) {
    this.symbol = symbol;
    this.path = "";
    this.valueTable = valueTable;
    this.variableFilter = "";
    this.withMissings = true;
    this.identifiersMapping = "";
    this.identifiersTableService = null;
    this.dataExportService = dataExportService;
    this.idColumnName = idColumnName;
    this.rClass = RClass.TIBBLE;
  }

  public MagmaAssignROperation(@NotNull String symbol, @NotNull String path, String variableFilter,
                               boolean withMissings, String idColumnName, String identifiersMapping, RClass rClass,
                               @NotNull IdentifiersTableService identifiersTableService,
                               @NotNull DataExportService dataExportService) {
    this.symbol = symbol;
    this.path = path;
    this.valueTable = null;
    this.variableFilter = variableFilter;
    this.withMissings = withMissings;
    this.identifiersMapping = identifiersMapping;
    this.idColumnName = idColumnName;
    this.identifiersTableService = identifiersTableService;
    this.dataExportService = dataExportService;
    this.rClass = path.contains(":") ? RClass.VECTOR : RClass.VECTOR.equals(rClass) ? RClass.DATA_FRAME : rClass;
  }

  @Override
  public void doWithConnection() {
    MagmaRConverter converter;
    switch (rClass) {
      case VECTOR:
      case DATA_FRAME:
        converter = new ValueTableDataFrameRConverter(this);
        break;
      case TIBBLE_WITH_FACTORS:
        converter = new ValueTableDataFrameRConverter(this, true);
        break;
      default:
        converter = new ValueTableTibbleRConverter(this);
    }

    converter.doAssign(symbol, path);
  }

  RConnection getRConnection() {
    return getConnection();
  }

  REXP doEval(String script) {
    return eval(script, false);
  }

  void doWriteFile(String fileName, InputStream in) {
    writeFile(fileName, in);
  }

  IdentifiersTableService getIdentifiersTableService() {
    return identifiersTableService;
  }

  DataExportService getDataExportService() {
    return this.dataExportService;
  }

  String getSymbol() {
    return symbol;
  }

  boolean hasValueTable() {
    return valueTable != null;
  }

  ValueTable getValueTable() {
    return valueTable;
  }

  String getIdColumnName() {
    // we need one for tibble assignment and will be removed in the case of a data frame
    return withIdColumn() ? idColumnName : ".id";
  }

  boolean withIdColumn() {
    return !Strings.isNullOrEmpty(idColumnName);
  }

  boolean withMissings() {
    return withMissings;
  }

  String getIdentifiersMapping() {
    return identifiersMapping;
  }

  boolean hasVariableFilter() {
    return !Strings.isNullOrEmpty(variableFilter);
  }

  String getVariableFilter() {
    return variableFilter;
  }

  @Override
  public String toString() {
    return symbol + " <- opal[" + path + "]";
  }

}
