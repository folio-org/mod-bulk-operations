package org.folio.bulkops.batch;

import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ExportJobManagerSync extends ExportJobManager {
  public ExportJobManagerSync(@Qualifier("asyncJobLauncher") JobOperator jobLauncher) {
    super(jobLauncher);
  }
}
