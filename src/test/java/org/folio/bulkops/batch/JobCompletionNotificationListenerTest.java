package org.folio.bulkops.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.bulkops.domain.bean.JobParameterNames.BULK_OPERATION_ID;
import static org.folio.bulkops.domain.bean.JobParameterNames.STORAGE_FILE_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.STORAGE_MARC_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_LOCAL_MARC_PATH;
import static org.folio.bulkops.util.Constants.IDENTIFIERS_FILE_NAME;
import static org.folio.bulkops.util.Constants.NUMBER_OF_MATCHED_RECORDS;
import static org.folio.bulkops.util.Constants.NUMBER_OF_PROCESSED_IDENTIFIERS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.ServerErrorException;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.ErrorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectMapper;

class JobCompletionNotificationListenerTest {

  @Mock private BulkOperationRepository bulkOperationRepository;
  @Mock private ErrorService errorService;
  @Mock private ObjectMapper objectMapper;
  @Mock private RemoteFileSystemClient remoteFileSystemClient;
  @InjectMocks private JobCompletionNotificationListener listener;

  private UUID bulkOperationId;
  private BulkOperation bulkOperation;
  private JobExecution jobExecution;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    bulkOperationId = UUID.randomUUID();
    bulkOperation = BulkOperation.builder().id(bulkOperationId).build();
    var jobParameters =
        new JobParametersBuilder()
            .addString(BULK_OPERATION_ID, bulkOperationId.toString())
            .addString(TEMP_LOCAL_FILE_PATH, "/tmp/file")
            .addString(STORAGE_FILE_PATH, "/storage/file")
            .addString(TEMP_LOCAL_MARC_PATH, "/tmp/marc")
            .addString(STORAGE_MARC_PATH, "/storage/marc")
            .addString(IDENTIFIERS_FILE_NAME, "/tmp/identifiers")
            .toJobParameters();
    jobExecution = new JobExecution(1L, new JobInstance(1L, "testJob"), jobParameters);
    when(bulkOperationRepository.findById(bulkOperationId)).thenReturn(Optional.of(bulkOperation));
    when(errorService.uploadErrorsToStorage(any(), any(), any())).thenReturn("errors.csv");
    when(errorService.getCommittedNumOfErrors(any())).thenReturn(2);
    when(errorService.getCommittedNumOfWarnings(any())).thenReturn(1);
  }

  @Test
  void beforeJob_shouldUpdateBulkOperationWithMatchedAndProcessedCounts() {
    jobExecution.getExecutionContext().putInt(NUMBER_OF_PROCESSED_IDENTIFIERS, 5);
    jobExecution.getExecutionContext().putInt(NUMBER_OF_MATCHED_RECORDS, 3);

    listener.beforeJob(jobExecution);

    verify(bulkOperationRepository)
        .save(
            argThat(op -> op.getProcessedNumOfRecords() == 5 && op.getMatchedNumOfRecords() == 3));
  }

  @Test
  void afterJob_shouldSetStatusCompletedWithErrorsOnCompleted() {
    jobExecution.setStatus(BatchStatus.COMPLETED);

    listener.afterJob(jobExecution);

    verify(bulkOperationRepository)
        .save(
            argThat(
                op ->
                    op.getStatus() == OperationStatusType.COMPLETED_WITH_ERRORS
                        && op.getEndTime() != null));
  }

  @Test
  void afterJob_shouldSetStatusFailedOnFailed() {
    jobExecution.setStatus(BatchStatus.FAILED);
    Exception ex = new RuntimeException("fail");
    jobExecution.addFailureException(ex);

    listener.afterJob(jobExecution);

    verify(bulkOperationRepository)
        .save(
            argThat(
                op ->
                    op.getStatus() == OperationStatusType.FAILED
                        && op.getErrorMessage().contains("fail")
                        && op.getEndTime() != null));
  }

  @Test
  void afterJob_shouldSetStatusFailedOnAbandoned() {
    jobExecution.setStatus(BatchStatus.ABANDONED);

    listener.afterJob(jobExecution);

    verify(bulkOperationRepository)
        .save(
            argThat(op -> op.getStatus() == OperationStatusType.FAILED && op.getEndTime() != null));
  }

  @Test
  void afterJob_shouldSetErrorAndWarningCountsAndLinks() {
    jobExecution.setStatus(BatchStatus.COMPLETED);

    listener.afterJob(jobExecution);

    assertThat(bulkOperation.getLinkToMatchedRecordsErrorsCsvFile()).isEqualTo("errors.csv");
    assertThat(bulkOperation.getMatchedNumOfErrors()).isEqualTo(2);
    assertThat(bulkOperation.getMatchedNumOfWarnings()).isEqualTo(1);
  }

  @Test
  void setsUsedTenantsWhenJobCompletesSuccessfully() throws Exception {
    UUID operationId = UUID.randomUUID();
    BulkOperation operation =
        BulkOperation.builder()
            .id(operationId)
            .entityType(org.folio.bulkops.domain.dto.EntityType.ITEM)
            .linkToMatchedRecordsJsonFile("valid-file.json")
            .build();

    when(bulkOperationRepository.findById(operationId)).thenReturn(Optional.of(operation));
    when(remoteFileSystemClient.get("missing-file.json"))
        .thenThrow(new RuntimeException("File not found"));
    when(objectMapper.createParser(any(InputStream.class)))
        .thenReturn(new ObjectMapper().createParser("[]"));
    MappingIterator<BulkOperationsEntity> emptyMappingIterator =
        (MappingIterator<BulkOperationsEntity>) Mockito.mock(MappingIterator.class);
    when(emptyMappingIterator.hasNext()).thenReturn(false);
    when(objectMapper.readValues(any(), any(Class.class))).thenReturn(emptyMappingIterator);

    JobParameters jobParameters =
        new JobParametersBuilder()
            .addString(BULK_OPERATION_ID, operationId.toString())
            .toJobParameters();
    JobExecution job = new JobExecution(1L, new JobInstance(1L, "testJob"), jobParameters);
    job.setStatus(org.springframework.batch.core.BatchStatus.COMPLETED);

    listener.afterJob(job);

    verify(bulkOperationRepository, atLeastOnce()).save(argThat(op -> op.getUsedTenants() != null));
  }

  @Test
  void setsErrorStatusAndMessageWhenJsonFileIsMissing() {
    UUID operationId = UUID.randomUUID();
    BulkOperation operation =
        BulkOperation.builder()
            .id(operationId)
            .entityType(org.folio.bulkops.domain.dto.EntityType.ITEM)
            .linkToMatchedRecordsJsonFile("missing-file.json")
            .build();

    when(bulkOperationRepository.findById(operationId)).thenReturn(Optional.of(operation));
    when(remoteFileSystemClient.get("missing-file.json"))
        .thenThrow(new RuntimeException("File not found"));

    JobParameters jobParameters =
        new JobParametersBuilder()
            .addString(BULK_OPERATION_ID, operationId.toString())
            .toJobParameters();
    JobExecution execution = new JobExecution(2L, new JobInstance(1L, "testJob"), jobParameters);
    execution.setStatus(org.springframework.batch.core.BatchStatus.COMPLETED);

    assertThatThrownBy(() -> listener.afterJob(execution))
        .isInstanceOf(ServerErrorException.class)
        .hasMessageContaining("Error getting tenants list");

    verify(bulkOperationRepository, atLeastOnce())
        .save(
            argThat(
                op ->
                    op.getStatus() == OperationStatusType.FAILED
                        && op.getErrorMessage().contains("Error getting tenants list")));
  }
}
