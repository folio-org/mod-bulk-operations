package org.folio.bulkops.util;

import org.folio.bulkops.domain.dto.EntityType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE;
import static org.folio.bulkops.domain.dto.EntityType.ITEM;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.CHECK_IN_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.CHECK_OUT_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ELECTRONIC_ACCESS_LINK_TEXT;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ELECTRONIC_ACCESS_MATERIALS_SPECIFIED;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ELECTRONIC_ACCESS_URI;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ELECTRONIC_ACCESS_URL_PUBLIC_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ELECTRONIC_ACCESS_URL_RELATIONSHIP;
import static org.folio.bulkops.domain.dto.UpdateOptionType.EMAIL_ADDRESS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.EXPIRATION_DATE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.HOLDINGS_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.INSTANCE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ITEM_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PATRON_GROUP;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOAN_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOCATION;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STAFF_SUPPRESS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STATUS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOAN_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOCATION;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateOptionTypeToFieldResolverTest {


  @ParameterizedTest()
  @MethodSource("fieldToOptionToEntity")
  void getFieldByUpdateOptionTypeTest(String expected, org.folio.bulkops.domain.dto.UpdateOptionType type, EntityType entityType) {
    assertEquals(expected, UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(type, entityType));
  }

  private static Stream<Arguments> fieldToOptionToEntity() {
    return Stream.of(
      Arguments.of("Patron group", PATRON_GROUP, ITEM),
      Arguments.of("Expiration date", EXPIRATION_DATE, ITEM),
      Arguments.of("Email", EMAIL_ADDRESS, ITEM),
      Arguments.of("Item permanent location", PERMANENT_LOCATION, ITEM),
      Arguments.of("Holdings permanent location", PERMANENT_LOCATION, HOLDINGS_RECORD),
      Arguments.of("Permanent location", PERMANENT_LOCATION, USER),
      Arguments.of("Item temporary location", TEMPORARY_LOCATION, ITEM),
      Arguments.of("Holdings temporary location", TEMPORARY_LOCATION, HOLDINGS_RECORD),
      Arguments.of("Temporary location", TEMPORARY_LOCATION, USER),
      Arguments.of("Permanent loan type", PERMANENT_LOAN_TYPE, ITEM),
      Arguments.of("Temporary loan type", TEMPORARY_LOAN_TYPE, ITEM),
      Arguments.of("Status", STATUS, ITEM),
      Arguments.of("Suppress from discovery", SUPPRESS_FROM_DISCOVERY, ITEM),
      Arguments.of("Staff suppress", STAFF_SUPPRESS, INSTANCE),
      Arguments.of("Notes", ITEM_NOTE, INSTANCE),
      Arguments.of("Administrative note", ADMINISTRATIVE_NOTE, INSTANCE),
      Arguments.of("Check In Notes", CHECK_IN_NOTE, INSTANCE),
      Arguments.of("Check Out Notes", CHECK_OUT_NOTE, INSTANCE),
      Arguments.of("Notes", HOLDINGS_NOTE, INSTANCE),
      Arguments.of("Electronic access", ELECTRONIC_ACCESS_URL_RELATIONSHIP, INSTANCE),
      Arguments.of("Electronic access", ELECTRONIC_ACCESS_URI, INSTANCE),
      Arguments.of("Electronic access", ELECTRONIC_ACCESS_LINK_TEXT, INSTANCE),
      Arguments.of("Electronic access", ELECTRONIC_ACCESS_MATERIALS_SPECIFIED, INSTANCE),
      Arguments.of("Electronic access", ELECTRONIC_ACCESS_URL_PUBLIC_NOTE, INSTANCE),
      Arguments.of("Instance note", INSTANCE_NOTE, INSTANCE)
      );
  }

}
