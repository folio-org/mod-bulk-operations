package org.folio.bulkops.domain.bean;

public enum PreferredEmailCommunications {

  SUPPORT("Support"), PROGRAMS("Programs"), SERVICES("Services");

  private String value;

  PreferredEmailCommunications(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static PreferredEmailCommunications fromValue(String value) {
    for (PreferredEmailCommunications communication : PreferredEmailCommunications.values()) {
      if (communication.getValue().equalsIgnoreCase(value)) {
        return communication;
      }
    }
    throw new IllegalArgumentException("Unrecognized enum value: " + value);
  }
}
