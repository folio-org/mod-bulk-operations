package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.bulkops.domain.bean.CustomField;
import org.folio.bulkops.domain.bean.CustomFieldTypes;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.UserFormatException;
import org.folio.bulkops.service.UserReferenceService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.adapters.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.adapters.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.adapters.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.bulkops.adapters.Constants.KEY_VALUE_DELIMITER;
import static org.folio.bulkops.adapters.Constants.LINE_BREAK;
import static org.folio.bulkops.adapters.Constants.LINE_BREAK_REPLACEMENT;

public class CustomFieldsConverter extends AbstractBeanField<String, Map<String, Object>> {
  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    if (isNotEmpty(value)) {
      return Arrays.stream(value.split(ITEM_DELIMITER_PATTERN))
        .map(this::restoreCustomFieldValue)
        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }
    return null;
  }

  @Override
  protected String convertToWrite(Object value) {
    var map = (Map<String, Object>) value;
    return nonNull(map) ? customFieldsToString(map) : EMPTY;
  }

  private Pair<String, Object> restoreCustomFieldValue(String s) {
    var valuePair = stringToPair(s);
    var fieldName = valuePair.getKey();
    var fieldValue = valuePair.getValue();
    var customField = UserReferenceService.service().getCustomFieldByName(fieldName);
    switch (customField.getType()) {
      case SINGLE_CHECKBOX:
        return Pair.of(customField.getRefId(), Boolean.parseBoolean(fieldValue));
      case TEXTBOX_LONG:
      case TEXTBOX_SHORT:
        return Pair.of(customField.getRefId(), fieldValue.replace(LINE_BREAK_REPLACEMENT, LINE_BREAK));
      case SINGLE_SELECT_DROPDOWN:
      case RADIO_BUTTON:
        return Pair.of(customField.getRefId(), restoreValueId(customField, fieldValue));
      case MULTI_SELECT_DROPDOWN:
        return Pair.of(customField.getRefId(), restoreValueIds(customField, fieldValue));
      default:
        throw new UserFormatException("Invalid custom field: " + s);
    }
  }

  private Pair<String, String> stringToPair(String value) {
    var tokens = value.split(KEY_VALUE_DELIMITER, -1);
    if (tokens.length == 2) {
      return Pair.of(SpecialCharacterEscaper.restore(tokens[0]), SpecialCharacterEscaper.restore(tokens[1]));
    } else {
      var msg = "Invalid key/value pair: " + value;
      throw new UserFormatException(msg);
    }
  }

  private List<String> restoreValueIds(CustomField customField, String values) {
    return isEmpty(values) ?
      Collections.emptyList() :
      Arrays.stream(values.split(ARRAY_DELIMITER))
        .map(token -> restoreValueId(customField, token))
        .collect(Collectors.toList());
  }

  private String restoreValueId(CustomField customField, String value) {
    var optionalValue = customField.getSelectField().getOptions().getValues().stream()
      .filter(selectFieldOption -> Objects.equals(value, selectFieldOption.getValue()))
      .findFirst();
    if (optionalValue.isPresent()) {
      return optionalValue.get().getId();
    } else {
      var msg = "Invalid custom field value: " + value;
      throw new UserFormatException(msg);
    }
  }

  private String customFieldsToString(Map<String, Object> map) {
    return map.entrySet().stream()
      .map(this::customFieldToString)
      .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String customFieldToString(Map.Entry<String, Object> entry) {
    var customField = new CustomField().withType(CustomFieldTypes.TEXTBOX_LONG).withName(entry.getKey()).withRefId(entry.getValue().toString()); // UserReferenceService.service().getCustomFieldByRefId(entry.getKey());
    switch (customField.getType()) {
      case TEXTBOX_LONG:
      case TEXTBOX_SHORT:
      case SINGLE_CHECKBOX:
        if (entry.getValue() instanceof String) {
          return SpecialCharacterEscaper.escape(customField.getName()) + KEY_VALUE_DELIMITER + SpecialCharacterEscaper.escape((String) entry.getValue());
        } else {
          return SpecialCharacterEscaper.escape(customField.getName()) + KEY_VALUE_DELIMITER + entry.getValue();
        }
      case SINGLE_SELECT_DROPDOWN:
      case RADIO_BUTTON:
        return SpecialCharacterEscaper.escape(customField.getName()) + KEY_VALUE_DELIMITER + SpecialCharacterEscaper.escape(extractValueById(customField, entry.getValue().toString()));
      case MULTI_SELECT_DROPDOWN:
        var values = (ArrayList) entry.getValue();
        return SpecialCharacterEscaper.escape(customField.getName()) + KEY_VALUE_DELIMITER + values.stream()
          .map(v -> SpecialCharacterEscaper.escape(extractValueById(customField, v.toString())))
          .collect(Collectors.joining(ARRAY_DELIMITER));
      default:
        throw new NotFoundException("Invalid custom field: " + entry);
    }
  }

  private String extractValueById(CustomField customField, String id) {
    var optionalValue = customField.getSelectField().getOptions().getValues().stream()
      .filter(selectFieldOption -> Objects.equals(id, selectFieldOption.getId()))
      .findFirst();
    return optionalValue.isPresent() ? optionalValue.get().getValue() : EMPTY;
  }
}