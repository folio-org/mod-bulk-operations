package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Format {
  TEXT("TEXT"),

  EMAIL("EMAIL"),

  URL("URL"),

  NUMBER("NUMBER");

  private final String value;

  Format(String value) {
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
  public static Format fromValue(String value) {
    for (Format b : Format.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
