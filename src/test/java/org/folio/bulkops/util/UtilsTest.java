package org.folio.bulkops.util;

import org.folio.bulkops.domain.dto.IdentifierType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTest {

  @Test
  void getIdentifierForManualApproach() {
    String[] line = {"username","id","external-system-id","barcode","","","","","","","","","","","","","","","","","",""};

    var barcode = Utils.getIdentifierForManualApproach(line, IdentifierType.BARCODE);
    assertEquals("barcode", barcode);

    var externalSystemId = Utils.getIdentifierForManualApproach(line, IdentifierType.EXTERNAL_SYSTEM_ID);
    assertEquals("external-system-id", externalSystemId);

    var userName = Utils.getIdentifierForManualApproach(line, IdentifierType.USER_NAME);
    assertEquals("username", userName);

    var id = Utils.getIdentifierForManualApproach(line, IdentifierType.ID);
    assertEquals("id", id);
  }
}
