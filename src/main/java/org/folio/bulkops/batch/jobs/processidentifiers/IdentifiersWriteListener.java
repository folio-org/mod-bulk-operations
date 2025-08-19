package org.folio.bulkops.batch.jobs.processidentifiers;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.folio.bulkops.domain.bean.JobParameterNames.BULK_OPERATION_ID;
import static org.folio.bulkops.util.Constants.NUMBER_OF_MATCHED_RECORDS;
import static org.folio.bulkops.util.Constants.NUMBER_OF_PROCESSED_IDENTIFIERS;
import static org.folio.bulkops.util.Constants.TOTAL_CSV_LINES;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class IdentifiersWriteListener<T> implements ItemWriteListener<T> {
  private final BulkOperationRepository bulkOperationRepository;

  @Value("#{stepExecution.jobExecution}")
  private JobExecution jobExecution;

  @Override
  public void beforeWrite(Chunk<? extends T> list) {
    // do nothing
  }

  @Override
  public void afterWrite(Chunk<? extends T> list) {
    var totalCsvLines = jobExecution.getJobParameters().getLong(TOTAL_CSV_LINES);
    int processed = list.size();
    int matched = list.size();
    var context = jobExecution.getExecutionContext();
    if (context.containsKey(NUMBER_OF_PROCESSED_IDENTIFIERS)) {
      processed += context.getInt(NUMBER_OF_PROCESSED_IDENTIFIERS);
    }
    if (context.containsKey(NUMBER_OF_MATCHED_RECORDS)) {
      matched += context.getInt(NUMBER_OF_MATCHED_RECORDS);
    }
    if (nonNull(totalCsvLines) && processed > totalCsvLines) {
      processed = totalCsvLines.intValue();
    }
    context.putInt(NUMBER_OF_PROCESSED_IDENTIFIERS, processed);
    context.putInt(NUMBER_OF_MATCHED_RECORDS, matched);

    var bulkOperation = ofNullable(jobExecution.getJobParameters().getString(BULK_OPERATION_ID))
      .map(UUID::fromString)
      .map(bulkOperationRepository::findById)
      .flatMap(opt -> opt)
      .orElseThrow(() -> new IllegalStateException("Bulk operation was not found, aborting batch execution."));

    bulkOperation.setProcessedNumOfRecords(processed);
    bulkOperationRepository.save(bulkOperation);
  }

}
