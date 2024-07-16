package org.folio.bulkops.domain.bean;

public enum PreferredEmailCommunication {

  SUPPORT("Support"), PROGRAMS("Programs"), SERVICES("Services");

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
