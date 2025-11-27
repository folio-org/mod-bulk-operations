package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CustomFieldTypes {
  DATE_PICKER("DATE_PICKER"),
  RADIO_BUTTON("RADIO_BUTTON"),
  SINGLE_CHECKBOX("SINGLE_CHECKBOX"),
  SINGLE_SELECT_DROPDOWN("SINGLE_SELECT_DROPDOWN"),
  MULTI_SELECT_DROPDOWN("MULTI_SELECT_DROPDOWN"),
  TEXTBOX_SHORT("TEXTBOX_SHORT"),
  TEXTBOX_LONG("TEXTBOX_LONG");

  private final String value;

  CustomFieldTypes(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static CustomFieldTypes fromValue(String value) {
    for (CustomFieldTypes b : CustomFieldTypes.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
