package org.folio.bulkops.processor.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.dto.Row;
import org.junit.jupiter.api.Test;

class HoldingsPreviewProcessorTest {
  @Test
  void getProcessedTypeShouldReturnHoldingsClass() {
    HoldingsPreviewProcessor processor = new HoldingsPreviewProcessor();
    assertEquals(HoldingsRecord.class, processor.getProcessedType());
  }

  @Test
  void transformToRowShouldReturnRow() {
    HoldingsPreviewProcessor processor = new HoldingsPreviewProcessor();
    HoldingsRecord entity = new HoldingsRecord();
    Row row = processor.transformToRow(entity);
    assertNotNull(row);
  }
}
