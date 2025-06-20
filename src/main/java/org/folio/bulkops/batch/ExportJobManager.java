package org.folio.bulkops.batch;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.batch.integration.launch.JobLaunchingMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class ExportJobManager {

  private final JobLaunchingMessageHandler jobLaunchingMessageHandler;

  @Autowired
  public ExportJobManager(@Qualifier("asyncJobLauncher") JobLauncher jobLauncher) {
    jobLaunchingMessageHandler = new JobLaunchingMessageHandler(jobLauncher);
  }

  public JobExecution launchJob(JobLaunchRequest jobLaunchRequest) throws JobExecutionException {
    log.info("Launching {}.", jobLaunchRequest);
    return jobLaunchingMessageHandler.launch(jobLaunchRequest);
  }
}
