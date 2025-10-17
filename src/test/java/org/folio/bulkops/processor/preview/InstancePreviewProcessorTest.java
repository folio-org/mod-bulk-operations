package org.folio.bulkops.processor.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.dto.Row;
import org.junit.jupiter.api.Test;

class InstancePreviewProcessorTest {
  @Test
  void getProcessedTypeShouldReturnInstanceClass() {
    InstancePreviewProcessor processor = new InstancePreviewProcessor();
    assertEquals(Instance.class, processor.getProcessedType());
  }

  @Test
  void transformToRowShouldReturnRow() {
    InstancePreviewProcessor processor = new InstancePreviewProcessor();
    Instance entity = new Instance();
    Row row = processor.transformToRow(entity);
    assertNotNull(row);
  }
}
