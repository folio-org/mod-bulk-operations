package org.folio.bulkops.processor;

import org.folio.bulkops.domain.entity.BulkOperation;

import java.io.IOException;

public interface MarcUpdateProcessor {
  void updateMarcRecords(BulkOperation bulkOperation) throws IOException;
}
