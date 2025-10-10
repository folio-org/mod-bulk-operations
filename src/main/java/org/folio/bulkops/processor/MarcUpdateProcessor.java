package org.folio.bulkops.processor;

import java.io.IOException;
import org.folio.bulkops.domain.entity.BulkOperation;

public interface MarcUpdateProcessor {
  void updateMarcRecords(BulkOperation bulkOperation) throws IOException;
}
