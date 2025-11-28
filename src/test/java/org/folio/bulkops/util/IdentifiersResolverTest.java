package org.folio.bulkops.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.bulkops.domain.dto.IdentifierType;
import org.junit.jupiter.api.Test;

class IdentifiersResolverTest {

  @Test
  void testResolve() {
    assertEquals("id", IdentifiersResolver.resolve(IdentifierType.ID));
    assertEquals("barcode", IdentifiersResolver.resolve(IdentifierType.BARCODE));
    assertEquals("hrid", IdentifiersResolver.resolve(IdentifierType.HRID));
    assertEquals("formerIds", IdentifiersResolver.resolve(IdentifierType.FORMER_IDS));
    assertEquals("instanceHrid", IdentifiersResolver.resolve(IdentifierType.INSTANCE_HRID));
    assertEquals("itemBarcode", IdentifiersResolver.resolve(IdentifierType.ITEM_BARCODE));
    assertEquals("accessionNumber", IdentifiersResolver.resolve(IdentifierType.ACCESSION_NUMBER));
    assertEquals(
        "holdingsRecordId", IdentifiersResolver.resolve(IdentifierType.HOLDINGS_RECORD_ID));
    assertEquals("username", IdentifiersResolver.resolve(IdentifierType.USER_NAME));
    assertEquals(
        "externalSystemId", IdentifiersResolver.resolve(IdentifierType.EXTERNAL_SYSTEM_ID));
    assertEquals("ISSN", IdentifiersResolver.resolve(IdentifierType.ISSN));
    assertEquals("ISBN", IdentifiersResolver.resolve(IdentifierType.ISBN));
  }
}
