package org.folio.bulkops.adapters;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.DataType;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.folio.bulkops.domain.dto.DataType.DATE_TIME;
import static org.folio.bulkops.domain.dto.DataType.STRING;

@Component
@Log4j2
@RequiredArgsConstructor
public class UserUnifiedTableHeaderBuilder implements UnifiedTableHeaderBuilder<User> {
  @Override
  public UnifiedTable getEmptyTableWithHeaders() {
    return new UnifiedTable().header(UserHeaderBuilder.getHeaders());
  }

  @Override
  public Class<User> getProcessedType() {
    return User.class;
  }

  @AllArgsConstructor
  public enum UserHeaderBuilder {
    NAME("User name", STRING, true),
    ID("User id", STRING, false),
    EXTERNAL_SYSTEM_ID("External system id", STRING, false),
    BARCODE("Barcode", STRING, true),
    ACTIVE("Active", STRING, true),
    TYPE("Type", STRING, false),
    PATRON_GROUP("Patron group", STRING, true),
    DEPARTMENTS("Departments", STRING, false),
    PROXY_FOR("Proxy for", STRING, false),
    LAST_NAME("Last name", STRING, true),
    FIRST_NAME("First name", STRING, true),
    MIDDLE_NAME("Middle name", STRING, false),
    PREFERRED_FIRST_NAME("Preferred first name", STRING, false),
    EMAIL("Email", STRING, false),
    PHONE("Phone", STRING, false),
    MOBILE_PHONE("Mobile phone", STRING, false),
    DATE_OF_BIRTH("Date Of Birth", DATE_TIME, false),
    ADDRESSES("Addresses", STRING, false),
    PREFERRED_CONTACT_TYPE_ID("Preferred contact type id", STRING, false),
    ENROLLMENT_DATE("Enrollment date", DATE_TIME, false),
    EXPIRATION_DATE("Expiration date", DATE_TIME, false),
    CREATED_DATE("Created cate", DATE_TIME, false),
    UPDATED_DATE("Updated date", DATE_TIME, false),
    TAGS("Tags", STRING, false),
    CUSTOM_FIELDS("Custom fields", STRING, false);

    private final String value;
    private final DataType dataType;
    private final boolean visible;

    public static List<Cell> getHeaders() {
      return Arrays.stream(values())
        .map(v -> new Cell().value(v.value).dataType(v.dataType).visible(v.visible))
        .toList();
    }
  }
}
