package org.folio.bulkops.domain.converter;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.bulkops.util.Constants.KEY_VALUE_DELIMITER;
import static org.folio.bulkops.util.Constants.LINE_BREAK;
import static org.folio.bulkops.util.Constants.LINE_BREAK_REPLACEMENT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.bulkops.domain.bean.CustomField;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.exception.EntityFormatException;
import org.folio.bulkops.service.UserReferenceHelper;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class CustomFieldsConverter extends AbstractBeanField<String, Map<String, Object>> {
  @Override
  protected Map<String, Object> convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    try {
     if (isNotEmpty(value)) {
       return Arrays.stream(value.split(ITEM_DELIMITER_PATTERN))
         .map(this::restoreCustomFieldValue)
         .filter(pair -> isNotEmpty(pair.getKey()))
         .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
     }
    } catch (Exception e) {
      throw new CsvConstraintViolationException(String.format("Error while created custom fields: %s", e.getMessage()));
    }
    return Collections.emptyMap();
  }

  @Override
  protected String convertToWrite(Object value) {
    if (ObjectUtils.isNotEmpty(value)) {
      return customFieldsToString((Map<String, Object>) value);
    }
    return EMPTY;
  }

  private Pair<String, Object> restoreCustomFieldValue(String s) {
    var valuePair = stringToPair(s);
    var fieldName = valuePair.getKey();
    var fieldValue = valuePair.getValue();
    var customField = UserReferenceHelper.service().getCustomFieldByName(fieldName);
    if (ObjectUtils.isNotEmpty(customField) && ObjectUtils.isNotEmpty(customField.getType())) {
      return switch (customField.getType()) {
        case SINGLE_CHECKBOX -> Pair.of(customField.getRefId(), Boolean.parseBoolean(fieldValue));
        case TEXTBOX_LONG, TEXTBOX_SHORT ->
          Pair.of(customField.getRefId(), fieldValue.replace(LINE_BREAK_REPLACEMENT, LINE_BREAK));
        case SINGLE_SELECT_DROPDOWN, RADIO_BUTTON ->
          Pair.of(customField.getRefId(), restoreValueId(customField, fieldValue));
        case MULTI_SELECT_DROPDOWN -> Pair.of(customField.getRefId(), restoreValueIds(customField, fieldValue));
      };
    }
    return Pair.of(EMPTY, new Object());

  }

  private Pair<String, String> stringToPair(String value) {
    var tokens = value.split(KEY_VALUE_DELIMITER, -1);
    if (tokens.length == 2) {
      return Pair.of(SpecialCharacterEscaper.restore(tokens[0]), SpecialCharacterEscaper.restore(tokens[1]));
    } else {
      var msg = "Invalid key/value pair: " + value;
      throw new EntityFormatException(msg);
    }
  }

  private List<String> restoreValueIds(CustomField customField, String values) {
    return isEmpty(values) ?
      Collections.emptyList() :
      Arrays.stream(values.split(ARRAY_DELIMITER))
        .map(token -> restoreValueId(customField, token))
        .toList();
  }

  private String restoreValueId(CustomField customField, String value) {
    var optionalValue = customField.getSelectField().getOptions().getValues().stream()
      .filter(selectFieldOption -> Objects.equals(value, selectFieldOption.getValue()))
      .findFirst();
    if (optionalValue.isPresent()) {
      return optionalValue.get().getId();
    } else {
      var msg = "Invalid custom field value: " + value;
      throw new EntityFormatException(msg);
    }
  }

  private String customFieldsToString(Map<String, Object> map) {
    return map.entrySet().stream()
      .map(this::customFieldToString)
      .filter(StringUtils::isNotEmpty)
      .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String customFieldToString(Map.Entry<String, Object> entry) {
    var customField = UserReferenceHelper.service().getCustomFieldByRefId(entry.getKey());
    return switch (customField.getType()) {
      case TEXTBOX_LONG, TEXTBOX_SHORT, SINGLE_CHECKBOX ->
        escape(customField.getName()) + KEY_VALUE_DELIMITER + (isNull(entry.getValue()) ? EMPTY : escape(entry.getValue().toString()));
      case SINGLE_SELECT_DROPDOWN, RADIO_BUTTON ->
        escape(customField.getName()) + KEY_VALUE_DELIMITER + (isNull(entry.getValue()) ? EMPTY : escape(extractValueById(customField, entry.getValue().toString())));
      case MULTI_SELECT_DROPDOWN ->
        escape(customField.getName()) + KEY_VALUE_DELIMITER +
          (entry.getValue() instanceof ArrayList<?> values ?
            values.stream()
              .filter(Objects::nonNull)
              .map(v -> escape(extractValueById(customField, v.toString())))
              .filter(ObjectUtils::isNotEmpty)
              .collect(Collectors.joining(ARRAY_DELIMITER)) :
            EMPTY);
    };
  }

  private String extractValueById(CustomField customField, String id) {
    var optionalValue = customField.getSelectField().getOptions().getValues().stream()
      .filter(selectFieldOption -> Objects.equals(id, selectFieldOption.getId()))
      .findFirst();
    return optionalValue.isPresent() ? optionalValue.get().getValue() : EMPTY;
  }
}
