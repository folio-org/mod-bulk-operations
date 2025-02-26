package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.util.Constants.ERROR_COMMITTING_FILE_NAME_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.domain.bean.GetParsedRecordsBatchRequestBody;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationExecutionRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class SrsServiceTest extends BaseTest {
   @MockitoBean
  private SrsClient srsClient;
   @MockitoBean
  private BulkOperationRepository bulkOperationRepository;
   @MockitoBean
  private RemoteFileSystemClient remoteFileSystemClient;
   @MockitoBean
  private ErrorService errorService;
   @MockitoBean
  private BulkOperationExecutionRepository executionRepository;
   @MockitoBean
  private InstanceReferenceService instanceReferenceService;
   @MockitoBean
  private MarcToUnifiedTableRowMapperHelper marcToUnifiedTableRowMapperHelper;
  @Captor
  private ArgumentCaptor<BulkOperation> bulkOperationCaptor;

  @Autowired
  private SrsService srsService;

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  @SneakyThrows
  void shouldRetrieveRecordsFromSrsAndCompleteBulkOperation(int numOfErrors) {
    var operationId = UUID.randomUUID();
    var linkToCommittedRecordsMarcFile = "marcFile.mrc";
    var operation = BulkOperation.builder()
      .id(operationId)
      .linkToCommittedRecordsMarcFile(linkToCommittedRecordsMarcFile)
      .build();

    var marcWriter = Mockito.mock(MarcRemoteStorageWriter.class);
    when(remoteFileSystemClient.marcWriter(anyString()))
      .thenReturn(marcWriter);
    when(srsClient.getParsedRecordsInBatch(any(GetParsedRecordsBatchRequestBody.class)))
      .thenReturn(objectMapper.readTree(Files.readString(Path.of("src/test/resources/files/srs_batch_response.json"))));
    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(operation));
    when(errorService.uploadErrorsToStorage(operationId, ERROR_COMMITTING_FILE_NAME_PREFIX, null))
      .thenReturn("errors.csv");
    when(errorService.getCommittedNumOfErrors(operationId))
      .thenReturn(numOfErrors);
    when(executionRepository.findByBulkOperationId(any(UUID.class))).thenReturn(Optional.empty());
    when(instanceReferenceService.getAllInstanceNoteTypes()).thenReturn(Collections.emptyList());
    when(marcToUnifiedTableRowMapperHelper.fetchContributorType(any(DataField.class)))
      .thenReturn(EMPTY);
    when(remoteFileSystemClient.writer(anyString()))
      .thenReturn(new StringWriter());

    srsService.retrieveMarcInstancesFromSrs(List.of(UUID.randomUUID().toString()), operation);

    verify(marcWriter).writeRecord(any(Record.class));
    verify(bulkOperationRepository, times(2))
      .save(bulkOperationCaptor.capture());
    var expectedStatus = numOfErrors == 0 ? COMPLETED : COMPLETED_WITH_ERRORS;
    assertThat(bulkOperationCaptor.getAllValues().get(1).getStatus())
      .isEqualTo(expectedStatus);
  }

  @SneakyThrows
  @Test
  void shouldRemoveLinkToCommittedIfNoIds() {
    var operationId = UUID.randomUUID();
    var linkToCommittedRecordsMarcFile = "marcFile.mrc";
    var operation = BulkOperation.builder()
      .id(operationId)
      .linkToCommittedRecordsMarcFile(linkToCommittedRecordsMarcFile)
      .build();
    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(operation));
    when(executionRepository.findByBulkOperationId(any(UUID.class))).thenReturn(Optional.empty());

    srsService.retrieveMarcInstancesFromSrs(List.of(), operation);

    assertThat(operation.getLinkToCommittedRecordsMarcFile()).isNull();
  }
}
