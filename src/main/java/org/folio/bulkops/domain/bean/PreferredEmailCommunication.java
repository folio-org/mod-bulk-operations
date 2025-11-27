package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PreferredEmailCommunication {
  @JsonProperty("Support")
  SUPPORT("Support"),
  @JsonProperty("Programs")
  PROGRAMS("Programs"),
  @JsonProperty("Services")
  SERVICES("Services");

  private String value;

  PreferredEmailCommunication(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static PreferredEmailCommunication fromValue(String value) {
    for (PreferredEmailCommunication communication : PreferredEmailCommunication.values()) {
      if (communication.getValue().equalsIgnoreCase(value)) {
        return communication;
      }
    }
    throw new IllegalArgumentException("Unrecognized enum value: " + value);
  }
}
