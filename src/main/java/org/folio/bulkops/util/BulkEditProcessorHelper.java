package org.folio.bulkops.util;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.dto.IdentifierType.ACCESSION_NUMBER;
import static org.folio.bulkops.domain.dto.IdentifierType.BARCODE;
import static org.folio.bulkops.domain.dto.IdentifierType.EXTERNAL_SYSTEM_ID;
import static org.folio.bulkops.domain.dto.IdentifierType.FORMER_IDS;
import static org.folio.bulkops.domain.dto.IdentifierType.HOLDINGS_RECORD_ID;
import static org.folio.bulkops.domain.dto.IdentifierType.HRID;
import static org.folio.bulkops.domain.dto.IdentifierType.ID;
import static org.folio.bulkops.domain.dto.IdentifierType.INSTANCE_HRID;
import static org.folio.bulkops.domain.dto.IdentifierType.ISBN;
import static org.folio.bulkops.domain.dto.IdentifierType.ISSN;
import static org.folio.bulkops.domain.dto.IdentifierType.ITEM_BARCODE;
import static org.folio.bulkops.domain.dto.IdentifierType.USER_NAME;
import static org.folio.bulkops.util.Constants.DATE_TIME_PATTERN;
import static org.folio.bulkops.util.Constants.DATE_WITHOUT_TIME_PATTERN;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.dto.IdentifierType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.Optional;
import java.util.TimeZone;

@UtilityClass
public class BulkEditProcessorHelper {
  private static final String MATCH_PATTERN = "%s=%s";
  private static final String EXACT_MATCH_PATTERN = "%s==%s";
  private static final DateFormat dateFormat;
  private static final DateFormat dateWithoutTimeFormat;
  private static final EnumMap<IdentifierType, String> identifiersMap = new EnumMap<>(IdentifierType.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    dateWithoutTimeFormat = new SimpleDateFormat(DATE_WITHOUT_TIME_PATTERN);
    dateWithoutTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    identifiersMap.put(ID, "id");
    identifiersMap.put(BARCODE, "barcode");
    identifiersMap.put(HRID, "hrid");
    identifiersMap.put(FORMER_IDS, "formerIds");
    identifiersMap.put(INSTANCE_HRID, "instanceHrid");
    identifiersMap.put(ITEM_BARCODE, "itemBarcode");
    identifiersMap.put(ACCESSION_NUMBER, "accessionNumber");
    identifiersMap.put(HOLDINGS_RECORD_ID, "holdingsRecordId");
    identifiersMap.put(USER_NAME, "username");
    identifiersMap.put(EXTERNAL_SYSTEM_ID, "externalSystemId");
    identifiersMap.put(ISSN, "ISSN");
    identifiersMap.put(ISBN, "ISBN");
  }

  public static String dateToString(Date date) {
    return nonNull(date) ? dateFormat.format(date) : EMPTY;
  }

  public static String resolveIdentifier(String identifier) {
    return identifiersMap.get(IdentifierType.fromValue(identifier));
  }

  public static String getMatchPattern(String identifierType) {
    return FORMER_IDS == IdentifierType.fromValue(identifierType) ? MATCH_PATTERN : EXACT_MATCH_PATTERN;
  }

  public static Optional<String> ofEmptyString(String string) {
    return StringUtils.isNotEmpty(string) ? Optional.of(string) : Optional.empty();
  }

  public static String getResponseAsString(Object obj) {
    String response;
    try {
      response = OBJECT_MAPPER.writeValueAsString(obj);
    } catch (Exception e) {
      response = "Cannot be parsed";
    }
    return response;
  }
}
