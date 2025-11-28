package org.folio.bulkops.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.bean.JobParameterNames.BULK_OPERATION_ID;
import static org.folio.bulkops.util.Constants.NUMBER_OF_PROCESSED_IDENTIFIERS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.Optional;
import java.util.UUID;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.ErrorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.item.ExecutionContext;

class BulkEditSkipListenerTest {

  @Mock private ErrorService errorService;
  @Mock private BulkOperationRepository bulkOperationRepository;
  @InjectMocks private BulkEditSkipListener skipListener;

  private JobExecution jobExecution;
  private UUID bulkOperationId;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    bulkOperationId = UUID.randomUUID();
    var jobParametersBuilder = new JobParametersBuilder();
    jobParametersBuilder.addString(BULK_OPERATION_ID, bulkOperationId.toString());
    jobExecution = new JobExecution(1L, jobParametersBuilder.toJobParameters());
    setField(skipListener, "jobExecution", jobExecution);
  }

  @Test
  void onSkipInProcess_shouldSaveErrorAndIncrementProcessed() {
    ItemIdentifier identifier = new ItemIdentifier().withItemId("item-1");
    BulkEditException exception = new BulkEditException("error", null);
    BulkOperation bulkOperation = BulkOperation.builder().id(bulkOperationId).build();

    when(bulkOperationRepository.findById(bulkOperationId)).thenReturn(Optional.of(bulkOperation));

    skipListener.onSkipInProcess(identifier, exception);

    verify(errorService).saveError(eq(bulkOperationId), eq("item-1"), eq("error"), isNull());
    assertThat(jobExecution.getExecutionContext().getInt(NUMBER_OF_PROCESSED_IDENTIFIERS))
        .isEqualTo(1);
  }

  @Test
  void onSkipInProcess_shouldIncrementProcessedIfAlreadyPresent() {
    BulkOperation bulkOperation = new BulkOperation();
    bulkOperation.setId(bulkOperationId);

    when(bulkOperationRepository.findById(bulkOperationId)).thenReturn(Optional.of(bulkOperation));

    ExecutionContext context = jobExecution.getExecutionContext();
    context.putInt(NUMBER_OF_PROCESSED_IDENTIFIERS, 5);

    ItemIdentifier identifier = new ItemIdentifier().withItemId("item-2");
    BulkEditException exception = new BulkEditException("err", null);

    skipListener.onSkipInProcess(identifier, exception);

    assertThat(context.getInt(NUMBER_OF_PROCESSED_IDENTIFIERS)).isEqualTo(6);
  }

  @Test
  void onSkipInProcess_shouldNotSaveErrorIfBulkOperationNotFound() {
    ItemIdentifier identifier = new ItemIdentifier().withItemId("item-3");
    BulkEditException exception = new BulkEditException("err", null);

    when(bulkOperationRepository.findById(bulkOperationId)).thenReturn(Optional.empty());

    skipListener.onSkipInProcess(identifier, exception);

    verify(errorService, never()).saveError(any(), any(), any(), any());
    assertThat(jobExecution.getExecutionContext().getInt(NUMBER_OF_PROCESSED_IDENTIFIERS))
        .isEqualTo(1);
  }
}
