package org.folio.bulkops.adapters.impl.users;

import static org.folio.bulkops.domain.dto.DataType.STRING;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.DataType;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum UserHeaderBuilder {
  ID("User id", STRING, false),
  NAME("User name", STRING, false),
  EXTERNAL_SYSTEM_ID("External system id", STRING, false),
  BARCODE("Barcode", STRING, false),
  ACTIVE("Active", STRING, false),
  TYPE("Type", STRING, false),
  PATRON_GROUP("Patron group", STRING, false),
  DEPARTMENTS("Departments", STRING, false),
  PROXY_FOR("Proxy for", STRING, false),
  LAST_NAME("Last name", STRING, false),
  FIRST_NAME("First name", STRING, false),
  MIDDLE_NAME("Middle name", STRING, false),
  PREFERRED_FIRST_NAME("Preferred first name", STRING, false),
  EMAIL("Email", STRING, false),
  PHONE("Phone", STRING, false),
  MOBILE_PHONE("Mobile phone", STRING, false),
  DATE_OF_BIRTH("Date Of Birth", STRING, false),
  ADDRESSES("Addresses", STRING, false),
  PREFERRED_CONTACT_TYPE_ID("Preferred contact type id", STRING, false),
  ENROLLMENT_DATE("Enrollment date", STRING, false),
  EXPIRATION_DATE("Expiration date", STRING, false),
  CREATED_DATE("Created cate", STRING, false),
  UPDATED_DATE("Updated date", STRING, false),
  TAGS("Tags", STRING, false),
  CUSTOM_FIELDS("Custom fields", STRING, false);

  private String value;
  private DataType dataType;
  private boolean visible;

  public static List<Cell> getHeaders() {
    return Arrays.stream(values())
      .map(v -> new Cell().value(v.value).dataType(v.dataType).visible(v.visible))
      .collect(Collectors.toList());
  }
}
