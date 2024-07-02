package org.folio.bulkops.processor;

;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.marc4j.marc.Record;
import org.springframework.stereotype.Component;

@Component
public class MarcInstanceDataProcessor {

  public void update(Record marcRecord, BulkOperationMarcRuleCollection bulkOperationMarcRuleCollection) {
    var fieldsByTag = marcRecord.getVariableFields("20");
  }
}
