package org.folio.bulkops.processor.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.Row;
import org.junit.jupiter.api.Test;

class AbstractPreviewProcessorTest {
  @Test
  void transformToRowShouldReturnRow() {
    TestPreviewProcessor processor = new TestPreviewProcessor();
    TestEntity entity = new TestEntity();
    Row row = processor.transformToRow(entity);
    assertNotNull(row);
  }

  @Test
  void getProcessedTypeShouldReturnCorrectClass() {
    TestPreviewProcessor processor = new TestPreviewProcessor();
    assertEquals(TestEntity.class, processor.getProcessedType());
  }

  static class TestEntity implements BulkOperationsEntity {
    @Override
    public String getIdentifier(IdentifierType type) {
      return "test-id";
    }

    @Override
    public Integer entityVersion() {
      return 1;
    }
  }

  static class TestPreviewProcessor extends AbstractPreviewProcessor<TestEntity> {
    @Override
    public Class<TestEntity> getProcessedType() {
      return TestEntity.class;
    }
  }
}
