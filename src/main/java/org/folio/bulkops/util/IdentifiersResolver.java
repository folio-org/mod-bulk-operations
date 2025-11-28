package org.folio.bulkops.util;

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

import java.util.EnumMap;
import lombok.experimental.UtilityClass;
import org.folio.bulkops.domain.dto.IdentifierType;

@UtilityClass
public class IdentifiersResolver {
  private static final EnumMap<IdentifierType, String> identifiersMap =
      new EnumMap<>(IdentifierType.class);

  static {
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

  public static String resolve(IdentifierType identifierType) {
    return identifiersMap.get(identifierType);
  }
}
