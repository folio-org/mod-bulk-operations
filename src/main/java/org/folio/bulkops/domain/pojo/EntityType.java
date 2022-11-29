package org.folio.bulkops.domain.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EntityType {

  USER("USER"),

  ITEM("ITEM"),

  HOLDING("HOLDING");

  private String value;

  EntityType(String value) {
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
  public static EntityType fromValue(String value) {
    for (EntityType entityType : EntityType.values()) {
      if (entityType.value.equals(value)) {
        return entityType;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

