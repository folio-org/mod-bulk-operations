package org.folio.bulkops.processor;

import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.marc4j.marc.Record;

import java.util.Date;

public interface MarcDataProcessor {
  void update(BulkOperation operation, Record marcRecord, BulkOperationMarcRuleCollection bulkOperationMarcRuleCollection, Date currentDate);
}
