package org.folio.bulkops.processor;

import org.folio.bulkops.domain.entity.BulkOperation;

public interface CommitProcessor {
  void processCommit(BulkOperation bulkOperation);
}
