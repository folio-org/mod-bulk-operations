package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IdentifierType {

  ID("ID"),

  BARCODE("BARCODE"),

  HRID("HRID"),

  FORMER_IDS("FORMER_IDS"),

  ACCESSION_NUMBER("ACCESSION_NUMBER"),

  HOLDINGS_RECORD_ID("HOLDINGS_RECORD_ID"),

  USER_NAME("USER_NAME"),

  EXTERNAL_SYSTEM_ID("EXTERNAL_SYSTEM_ID"),

  INSTANCE_HRID("INSTANCE_HRID"),

  ITEM_BARCODE("ITEM_BARCODE");

  private String value;

  IdentifierType(String value) {
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
  public static IdentifierType fromValue(String value) {
    for (IdentifierType b : IdentifierType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

