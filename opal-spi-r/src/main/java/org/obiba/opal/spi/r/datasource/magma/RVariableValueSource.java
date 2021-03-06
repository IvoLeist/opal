/*
 * Copyright (c) 2020 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.spi.r.datasource.magma;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.obiba.magma.*;
import org.obiba.magma.type.*;
import org.obiba.opal.spi.r.RUtils;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.RList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The R variable represents the column of a tibble.
 */
class RVariableValueSource extends AbstractVariableValueSource implements VariableValueSource, VectorSource {

  private static final Logger log = LoggerFactory.getLogger(RVariableValueSource.class);

  public static final String EPOCH = "1970-01-01";

  private RValueTable valueTable;

  private final String colName;

  private final List<String> colClasses;

  private final List<String> colTypes;

  private final int position;

  private Variable variable;

  RVariableValueSource(RValueTable valueTable, RList column, int position) throws REXPMismatchException {
    this.valueTable = valueTable;
    this.colName = column.at("name").asString();
    this.colClasses = Lists.newArrayList(column.at("class").asStrings());
    this.colTypes = Lists.newArrayList(column.at("type").asString().split("\\+"));
    REXP colAttr = column.at("attributes");
    this.position = position;
    initialiseVariable(colAttr);
  }

  @Override
  public Variable getVariable() {
    return variable;
  }

  @Override
  public ValueType getValueType() {
    return variable.getValueType();
  }

  @Override
  public Iterable<Value> getValues(List<VariableEntity> entities) {
    return null;
  }

  @Override
  public Value getValue(ValueSet valueSet) {
    Map<Integer, List<Object>> columnValues = ((RValueSet) valueSet).getValuesByPosition();
    if (!columnValues.containsKey(position))
      return variable.isRepeatable() ? getValueType().nullSequence() : getValueType().nullValue();
    return getValue(columnValues.get(position));
  }

  @Override
  public boolean supportVectorSource() {
    return true;
  }

  @Override
  public VectorSource asVectorSource() throws VectorSourceNotSupportedException {
    return this;
  }

  //
  // Private methods
  //

  private void initialiseVariable(REXP attr) {
    String repeatableProp = extractProperty(attr, "opal.repeatable");
    boolean repeatable = Strings.isNullOrEmpty(repeatableProp) ?
        valueTable.isMultilines() : ("1.0".equals(repeatableProp) || "1".equals(repeatableProp));

    int index = position;
    String indexStr = extractProperty(attr, "opal.index");
    if (!Strings.isNullOrEmpty(indexStr)) {
      try {
        index = new Double(indexStr).intValue();
      } catch (NumberFormatException e) {
        // ignore
      }
    }

    this.variable = VariableBean.Builder.newVariable(colName, extractValueType(attr), valueTable.getEntityType())
        .unit(extractProperty(attr, "opal.unit"))
        .referencedEntityType(extractProperty(attr, "opal.referenced_entity_type"))
        .mimeType(extractProperty(attr, "opal.mime_type"))
        .repeatable(repeatable)
        .occurrenceGroup(extractProperty(attr, "opal.occurrence_group"))
        .addAttributes(extractAttributes(attr))
        .addCategories(extractCategories(attr))
        .index(index)
        .build();
  }

  private ValueType extractValueType(REXP attr) {
    ValueType type = TextType.get();
    // column's data type has the priority
    if (isNumeric()) type = isInteger() ? IntegerType.get() : DecimalType.get();
    else if (isInteger()) type = IntegerType.get();
    else if (isDecimal()) type = DecimalType.get();
    else if (isDate()) type = DateType.get();
    else if (isDateTime()) type = DateTimeType.get();
    else if (isBoolean()) type = BooleanType.get();
    else if (isBinary()) type = BinaryType.get();
    else {
      // fallback to opal.value_type attribute, if defined
      String typePropertyStr = extractProperty(attr, "opal.value_type");
      if (!Strings.isNullOrEmpty(typePropertyStr)) {
        try {
          type = ValueType.Factory.forName(typePropertyStr);
        } catch (Exception e) {
          // ignore
        }
      }
    }
    log.debug("Tibble '{}' has column '{}' of class '{}' mapped to {}", valueTable.getSymbol(), colName, Joiner.on(", ").join(colClasses), type.getName());
    return type;
  }

  private boolean isNumeric() {
    return colClasses.contains("numeric");
  }

  private boolean isInteger() {
    return colClasses.contains("integer") || colTypes.contains("int");
  }

  private boolean isDecimal() {
    return colTypes.contains("dbl");
  }

  private boolean isBoolean() {
    return colClasses.contains("logical");
  }

  private boolean isBinary() {
    return colClasses.contains("raw");
  }

  private boolean isDate() {
    return colClasses.contains("Date");
  }

  private boolean isDateTime() {
    return colClasses.contains("POSIXct") || colClasses.contains("POSIXt");
  }

  private String extractProperty(REXP attr, String property) {
    if (attr == null || !attr.isList()) return null;
    try {
      RList rList = attr.asList();
      if (!rList.isNamed()) return null;
      for (String key : rList.keys()) {
        if (property.equals(key)) {
          REXP value = rList.at(key);
          return value.asString();
        }
      }
    } catch (REXPMismatchException e) {
      // ignore
    }
    return null;
  }

  private boolean hasCategories() {
    return colClasses.contains("labelled") || colClasses.contains("haven_labelled")
        || colClasses.contains("labelled_spss") || colClasses.contains("haven_labelled_spss")
        || colClasses.contains("factor");
  }

  private List<Attribute> extractAttributes(REXP attr) {
    List<Attribute> attributes = Lists.newArrayList();
    if (attr == null || !attr.isList()) return attributes;
    try {
      RList rList = attr.asList();
      if (!rList.isNamed()) return attributes;
      for (String key : rList.keys()) {
        if (key.equals("labels")) continue;
        REXP value = rList.at(key);
        String name = key;
        String namespace = null;
        if (key.contains("::")) {
          String[] nsn = key.split("::");
          name = nsn[1];
          namespace = nsn[0];
        }
        if (value.isString() && value.length() > 0) {
          String strValue = value.asStrings()[0];
          if (Strings.isNullOrEmpty(namespace) && ("label".equals(name) || "description".equals(name)))
            attributes.addAll(extractLocalizedAttributes(namespace, name, strValue));
          else
            attributes.add(Attribute.Builder.newAttribute(name).withNamespace(namespace).withValue(strValue).build());
        } else {
          log.debug("Attribute value is a {}", value.getClass().getName());
        }
      }

    } catch (REXPMismatchException e) {
      // ignore
      log.warn("Error while parsing variable attributes: {}", colName, e);
    }
    return attributes;
  }

  private List<Attribute> extractLocalizedAttributes(String namespace, String name, String value) {
    List<Attribute> attributes = Lists.newArrayList();
    String[] strValues = value.split("\\|");
    Pattern pattern = Pattern.compile("^\\(([a-z]{2})\\) (.+)");
    for (String strValue : strValues) {
      Matcher matcher = pattern.matcher(strValue.trim());
      if (matcher.find()) {
        String localeStr = matcher.group(1);
        if (!RUtils.isLocaleValid(localeStr))
          localeStr = valueTable.getDefaultLocale();
        attributes.add(Attribute.Builder.newAttribute(name).withNamespace(namespace)
            .withLocale(localeStr).withValue(matcher.group(2)).build());
      } else if (Strings.isNullOrEmpty(namespace) && ("label".equals(name) || "description".equals(name)))
        attributes.add(Attribute.Builder.newAttribute(name).withLocale(valueTable.getDefaultLocale()).withValue(strValue).build());
      else
        attributes.add(Attribute.Builder.newAttribute(name).withNamespace(namespace).withValue(strValue).build());
    }
    return attributes;
  }

  private List<Category> extractCategories(REXP attr) {
    if (colClasses.contains("labelled") || colClasses.contains("haven_labelled")
        || colClasses.contains("labelled_spss") || colClasses.contains("haven_labelled_spss")) {
      REXP naValues = extractAttribute(attr, "na_values");
      REXP naRange = extractAttribute(attr, "na_range");
      return extractCategoriesFromLabels(extractAttribute(attr, "labels"), naValues, naRange);
    } else if (colClasses.contains("factor"))
      return extractCategoriesFromLevels(extractAttribute(attr, "levels"));
    return Lists.newArrayList();
  }

  /**
   * Extract categories and flag missings appropriatly.
   *
   * @param labels        Category values
   * @param missings      Discrete missings
   * @param missingsRange Range of missings
   * @return
   */
  private List<Category> extractCategoriesFromLabels(REXP labels, REXP missings, REXP missingsRange) {
    List<Category> categories = Lists.newArrayList();
    if (labels == null) return categories;
    try {
      String[] catLabels = null;
      if (labels.hasAttribute("names")) {
        catLabels = labels.getAttribute("names").asStrings();
      }
      List<String> missingNames = Lists.newArrayList();
      if (missings != null) {
        for (String name : missings.asStrings()) {
          missingNames.add(normalizeCategoryName(name));
        }
      }

      int i = 0;
      for (String name : labels.asStrings()) {
        if ("NaN".equals(name) && (labels.isNumeric() || labels.isInteger())) {
          // skip
          i++;
        } else {
          String catName = normalizeCategoryName(name);
          Category.Builder builder = Category.Builder.newCategory(catName);
          if (catLabels != null) {
            extractLocalizedAttributes(null, "label", catLabels[i]).forEach(builder::addAttribute);
          }
          builder.missing(missingNames.contains(catName)
              || (labels.isInteger() && integerRangeContains(missingsRange, labels.asIntegers()[i]))
              || (labels.isNumeric() && doubleRangeContains(missingsRange, labels.asDoubles()[i])));
          categories.add(builder.build());
          i++;
        }
      }
    } catch (REXPMismatchException e) {
      // ignore
      log.warn("Error while parsing variable categories: {}", colName, e);
    }
    return categories;
  }

  private boolean integerRangeContains(REXP range, int value) throws REXPMismatchException {
    if (range == null) return false;
    int min = range.asIntegers()[0];
    int max = range.asIntegers()[1];
    return value >= min && value <= max;
  }

  private boolean doubleRangeContains(REXP range, double value) throws REXPMismatchException {
    if (range == null) return false;
    double min = range.asDoubles()[0];
    double max = range.asDoubles()[1];
    return value >= min && value <= max;
  }

  private String normalizeCategoryName(String name) {
    return (isNumeric() || isInteger() || isDecimal()) && name.endsWith(".0") ? name.substring(0, name.length() - 2) : name;
  }

  private List<Category> extractCategoriesFromLevels(REXP levels) {
    log.warn("Extracting '{}' categories factor levels not implemented yet", colName);
    return Lists.newArrayList();
  }

  private REXP extractAttribute(REXP attr, String attrName) {
    if (attr == null || !attr.isList()) return null;
    try {
      RList rList = attr.asList();
      if (!rList.isNamed() || !rList.containsKey(attrName)) return null;
      return rList.at(attrName);
    } catch (REXPMismatchException e) {
      return null;
    }
  }

  private Value getValue(List<Object> strValues) {
    if (strValues == null || strValues.size() == 0)
      return variable.isRepeatable() ? getValueType().nullSequence() : getValueType().nullValue();
    return variable.isRepeatable() ?
        getValueType().sequenceOf(Lists.newArrayList(strValues).stream().map(this::getSingleValue).collect(Collectors.toList())) :
        getSingleValue(strValues.get(0));
  }

  private Value getSingleValue(Object objValue) {
    if (isDate())
      return getDateFromEpoch(objValue);
    else if (isDateTime())
      return getDateTimeFromEpoch(objValue);
    else if (isNumeric())
      return getNumeric(objValue);
    return getValueType().valueOf(objValue);
  }

  private Value getNumeric(Object objValue) {
    if (objValue == null ||
        (objValue instanceof Double && ((Double) objValue).isNaN())) return getValueType().nullValue();
    return getValueType().valueOf(objValue);
  }

  /**
   * R dates are serialized as a number of days since epoch (1970-01-01).
   *
   * @param objValue
   * @return
   */
  private Value getDateFromEpoch(Object objValue) {
    if (objValue == null || "NaN".equals(objValue)) return getValueType().nullValue();
    try {
      Double dbl = (Double) objValue;
      if (dbl.isNaN()) return getValueType().nullValue();
      Date value = new Date(dbl.longValue() * 24 * 3600 * 1000);
      return getValueType().valueOf(value);
    } catch (Exception e) {
      return getValueType().nullValue();
    }
  }

  private Value getDateTimeFromEpoch(Object objValue) {
    if (objValue == null || "NaN".equals(objValue)) return getValueType().nullValue();
    try {
      Double dbl = (Double) objValue;
      if (dbl.isNaN()) return getValueType().nullValue();
      Date value = new Date(dbl.longValue() * 1000);
      return getValueType().valueOf(value);
    } catch (Exception e) {
      return getValueType().nullValue();
    }
  }
}
