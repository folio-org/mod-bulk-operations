package org.folio.bulkops.domain.bean;

import java.util.Arrays;
import java.util.Optional;

public enum PreferredContactType {
  EMAIL("002", "Email"),
  MAIL("001", "Mail (Primary Address)"),
  TEXT_MESSAGE("003", "Text Message");

  private String id;

  private String name;

  PreferredContactType(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public static Optional<PreferredContactType> getById(String id) {
    return Arrays.stream(values()).filter(type -> type.id.equals(id)).findFirst();
  }

  public String getId() {
    return id;
  }
}
