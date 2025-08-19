package org.folio.bulkops.batch;

import static java.util.Optional.ofNullable;
import static org.folio.bulkops.domain.bean.JobParameterNames.BULK_OPERATION_ID;
import static org.folio.bulkops.util.Constants.NUMBER_OF_PROCESSED_IDENTIFIERS;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.ErrorService;
import org.springframework.batch.core.annotation.OnSkipInProcess;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@RequiredArgsConstructor
@StepScope
@Component
public class BulkEditSkipListener {
  private final ErrorService errorService;
  private final BulkOperationRepository bulkOperationRepository;

  @Value("#{stepExecution.jobExecution}")
  private JobExecution jobExecution;

  @OnSkipInProcess
  public void onSkipInProcess(ItemIdentifier itemIdentifier, BulkEditException exception) {
    ofNullable(jobExecution.getJobParameters().getString(BULK_OPERATION_ID))
      .map(UUID::fromString)
      .map(bulkOperationRepository::findById)
      .flatMap(opt -> opt)
      .ifPresent(bulkOperation ->
        errorService.saveError(bulkOperation.getId(), itemIdentifier.getItemId(), exception.getMessage(), exception.getErrorType()));
    var context = jobExecution.getExecutionContext();
    int processed = 1;
    if (context.containsKey(NUMBER_OF_PROCESSED_IDENTIFIERS)) {
      processed += context.getInt(NUMBER_OF_PROCESSED_IDENTIFIERS);
    }
    context.putInt(NUMBER_OF_PROCESSED_IDENTIFIERS, processed);
  }
}
