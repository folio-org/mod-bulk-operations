package org.folio.bulkops.domain.converter;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
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

public class CustomFieldsConverter extends BaseConverter<Map<String, Object>> {

  // Workaround for manual approach - extremely unlikely line as temporary delimiter
  // of custom field values for internal processing
  public static final String TEMPORARY_DELIMITER = "±#&";

  @Override
  public Map<String, Object> convertToObject(String value) {
    return Arrays.stream(value.split(ITEM_DELIMITER_PATTERN))
        .map(this::restoreCustomFieldFromString)
        .filter(pair -> isNotEmpty(pair.getKey()))
        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
  }

  @Override
  public String convertToString(Map<String, Object> object) {
    return convertCustomFieldsToString(object);
  }

  private Pair<String, Object> restoreCustomFieldFromString(String s) {
    var pair = stringToPair(s.replace(";", TEMPORARY_DELIMITER));
    var name = pair.getKey();
    var value = pair.getValue();
    var customField = UserReferenceHelper.service().getCustomFieldByName(name);
    if (ObjectUtils.isNotEmpty(customField) && ObjectUtils.isNotEmpty(customField.getType())) {
      return switch (customField.getType()) {
        case DATE_PICKER -> Pair.of(customField.getRefId(), value);
        case SINGLE_CHECKBOX -> Pair.of(customField.getRefId(), Boolean.parseBoolean(value));
        case TEXTBOX_LONG, TEXTBOX_SHORT ->
            Pair.of(customField.getRefId(), value.replace(LINE_BREAK_REPLACEMENT, LINE_BREAK));
        case SINGLE_SELECT_DROPDOWN, RADIO_BUTTON ->
            Pair.of(customField.getRefId(), restoreValueId(customField, value));
        case MULTI_SELECT_DROPDOWN ->
            Pair.of(customField.getRefId(), restoreValueIds(customField, value));
      };
    }
    return Pair.of(EMPTY, new Object());
  }

  private Pair<String, String> stringToPair(String value) {
    var tokens = value.split(KEY_VALUE_DELIMITER, -1);
    if (tokens.length == 2) {
      return Pair.of(
          SpecialCharacterEscaper.restore(tokens[0]), SpecialCharacterEscaper.restore(tokens[1]));
    } else {
      var msg = "Invalid key/value pair for custom field: " + value;
      throw new EntityFormatException(msg);
    }
  }

  private List<String> restoreValueIds(CustomField customField, String values) {
    return isEmpty(values)
        ? Collections.emptyList()
        : Arrays.stream(values.split(TEMPORARY_DELIMITER))
            .map(token -> restoreValueId(customField, token))
            .toList();
  }

  private String restoreValueId(CustomField customField, String value) {
    var optionalValue =
        customField.getSelectField().getOptions().getValues().stream()
            .filter(selectFieldOption -> Objects.equals(value, selectFieldOption.getValue()))
            .findFirst();
    if (optionalValue.isPresent()) {
      return optionalValue.get().getId();
    } else {
      var msg = "Invalid custom field value: " + value;
      throw new EntityFormatException(msg);
    }
  }

  private String convertCustomFieldsToString(Map<String, Object> map) {
    return map.entrySet().stream()
        .map(this::customFieldToString)
        .filter(StringUtils::isNotEmpty)
        .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String customFieldToString(Map.Entry<String, Object> entry) {
    var customField = UserReferenceHelper.service().getCustomFieldByRefId(entry.getKey());
    return switch (customField.getType()) {
      case DATE_PICKER, TEXTBOX_LONG, TEXTBOX_SHORT, SINGLE_CHECKBOX ->
          customField.getName()
              + KEY_VALUE_DELIMITER
              + (isNull(entry.getValue()) ? EMPTY : entry.getValue().toString());
      case SINGLE_SELECT_DROPDOWN, RADIO_BUTTON ->
          customField.getName()
              + KEY_VALUE_DELIMITER
              + (isNull(entry.getValue())
                  ? EMPTY
                  : extractValueById(customField, entry.getValue().toString()));
      case MULTI_SELECT_DROPDOWN ->
          customField.getName()
              + KEY_VALUE_DELIMITER
              + (entry.getValue() instanceof ArrayList<?> values
                  ? values.stream()
                      .filter(Objects::nonNull)
                      .map(v -> extractValueById(customField, v.toString()))
                      .filter(ObjectUtils::isNotEmpty)
                      .collect(Collectors.joining(ARRAY_DELIMITER))
                  : EMPTY);
    };
  }

  private String extractValueById(CustomField customField, String id) {
    var optionalValue =
        customField.getSelectField().getOptions().getValues().stream()
            .filter(selectFieldOption -> Objects.equals(id, selectFieldOption.getId()))
            .findFirst();
    return optionalValue.isPresent() ? optionalValue.get().getValue() : EMPTY;
  }
}
