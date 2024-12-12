package org.folio.bulkops.util;

import static org.folio.bulkops.domain.dto.EntityType.INSTANCE;
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
import static org.folio.bulkops.domain.dto.UpdateOptionType.STATISTICAL_CODE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STATUS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOAN_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOCATION;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.UpdateOptionType;

public class UpdateOptionTypeToFieldResolver {

  private UpdateOptionTypeToFieldResolver() {
  }

  public static Set<String> getFieldsByUpdateOptionTypes(List<UpdateOptionType> options, EntityType entity) {
    return options.stream().map(option -> UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(option, entity)).collect(Collectors.toSet());
  }

  public static String getFieldByUpdateOptionType(UpdateOptionType type, EntityType entity) {
    if (PATRON_GROUP == type) {
      return "Patron group";
    } else if (EXPIRATION_DATE == type) {
      return "Expiration date";
    } else if (EMAIL_ADDRESS == type) {
      return "Email";
    } else if (PERMANENT_LOCATION == type) {
      return buildPermanentLocationFieldByEntityType(entity);
    } else if (TEMPORARY_LOCATION == type) {
      return buildTemporaryLocationFieldByEntityType(entity);
    } else if (PERMANENT_LOAN_TYPE == type) {
      return "Permanent loan type";
    } else if (TEMPORARY_LOAN_TYPE == type) {
      return "Temporary loan type";
    } else if (STATUS == type) {
      return "Status";
    } else if (SUPPRESS_FROM_DISCOVERY == type && entity != USER) {
      return "Suppress from discovery";
    } else if (STAFF_SUPPRESS == type && INSTANCE == entity) {
      return "Staff suppress";
    } else if (ITEM_NOTE == type) {
      return "Notes";
    } else if (ADMINISTRATIVE_NOTE == type) {
      return "Administrative note";
    } else if (CHECK_IN_NOTE == type) {
      return "Check in note";
    } else if (CHECK_OUT_NOTE == type) {
      return "Check out note";
    } else if (HOLDINGS_NOTE == type) {
      return "Notes";
    } else if (ELECTRONIC_ACCESS_URL_RELATIONSHIP == type) {
      return "Electronic access";
    } else if (ELECTRONIC_ACCESS_URI == type) {
      return "Electronic access";
    } else if (ELECTRONIC_ACCESS_LINK_TEXT == type) {
      return "Electronic access";
    } else if (ELECTRONIC_ACCESS_MATERIALS_SPECIFIED == type) {
      return "Electronic access";
    } else if (ELECTRONIC_ACCESS_URL_PUBLIC_NOTE == type) {
      return "Electronic access";
    } else if (INSTANCE_NOTE == type) {
      return "Instance note";
    } else if (STATISTICAL_CODE == type) {
      return "Statistical code";
    } else {
      throw new UnsupportedOperationException("There is no matching for Operation Type: " + type);
    }
  }

  private static String buildPermanentLocationFieldByEntityType(EntityType entityType) {
    return switch (entityType) {
      case ITEM -> "Item permanent location";
      case HOLDINGS_RECORD -> "Holdings permanent location";
      default -> "Permanent location";
    };
  }

  private static String buildTemporaryLocationFieldByEntityType(EntityType entityType) {
    return switch (entityType) {
      case ITEM -> "Item temporary location";
      case HOLDINGS_RECORD -> "Holdings temporary location";
      default -> "Temporary location";
    };
  }
}
