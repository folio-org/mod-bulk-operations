package org.folio.bulkops.batch;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ExportJobManagerSync extends ExportJobManager {
  public ExportJobManagerSync(@Qualifier("asyncJobLauncher") JobLauncher jobLauncher) {
    super(jobLauncher);
  }
}
