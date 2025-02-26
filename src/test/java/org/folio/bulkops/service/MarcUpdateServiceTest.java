package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.service.MarcUpdateService.CHANGED_MARC_PATH_TEMPLATE;
import static org.folio.bulkops.util.Constants.MSG_NO_MARC_CHANGE_REQUIRED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.StatusType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.folio.bulkops.processor.marc.MarcInstanceUpdateProcessor;
import org.folio.bulkops.repository.BulkOperationExecutionRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.junit.jupiter.api.Test;
import org.marc4j.marc.Record;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.UUID;

class MarcUpdateServiceTest extends BaseTest {
   @MockitoBean
  private BulkOperationExecutionRepository executionRepository;
   @MockitoBean
  private RemoteFileSystemClient remoteFileSystemClient;
   @MockitoBean
  private MarcInstanceUpdateProcessor updateProcessor;
   @MockitoBean
  private ErrorService errorService;
   @MockitoBean
  private BulkOperationRepository bulkOperationRepository;

  @Captor
  private ArgumentCaptor<Record> recordArgumentCaptor;
  @Captor
  private ArgumentCaptor<String> identifierArgumentCaptor;
  @Captor
  private ArgumentCaptor<String> errorMessageArgumentCaptor;
  @Captor
  private ArgumentCaptor<BulkOperationExecution> executionArgumentCaptor;
  @Captor
  private ArgumentCaptor<BulkOperation> bulkOperationArgumentCaptor;

  @Autowired
  private MarcUpdateService marcUpdateService;

  @Test
  @SneakyThrows
  void shouldCommitInstanceMarc() {
    var bulkOperation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .linkToTriggeringCsvFile("triggering.csv")
      .linkToMatchedRecordsMarcFile("matched.mrc")
      .linkToModifiedRecordsMarcFile("modified.mrc")
      .totalNumOfRecords(1)
      .processedNumOfRecords(1)
      .build();
    var execution = BulkOperationExecution.builder().bulkOperationId(bulkOperation.getId()).build();
    var pathToCommittedMarcFile = String.format(CHANGED_MARC_PATH_TEMPLATE, bulkOperation.getId(), LocalDate.now(), "triggering");

    var mockMarcWriter = mock(MarcRemoteStorageWriter.class);
    when(executionRepository.save(any(BulkOperationExecution.class)))
      .thenReturn(execution);
    when(remoteFileSystemClient.marcWriter(pathToCommittedMarcFile))
      .thenReturn(mockMarcWriter);
    when(remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsMarcFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/matched.mrc"));
    when(remoteFileSystemClient.get(bulkOperation.getLinkToModifiedRecordsMarcFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/modified.mrc"));

    marcUpdateService.commitForInstanceMarc(bulkOperation);

    verify(updateProcessor).updateMarcRecords(bulkOperationArgumentCaptor.capture());
    assertThat(bulkOperationArgumentCaptor.getValue().getLinkToCommittedRecordsMarcFile()).isNull();

    verify(executionRepository, times(2)).save(executionArgumentCaptor.capture());
    assertThat(executionArgumentCaptor.getAllValues().get(1).getStatus()).isEqualTo(StatusType.COMPLETED);

    verify(mockMarcWriter).writeRecord(recordArgumentCaptor.capture());
    assertThat(recordArgumentCaptor.getValue().getControlNumberField().getData()).isEqualTo("hrid1");

    verify(errorService).saveError(eq(bulkOperation.getId()), identifierArgumentCaptor.capture(), errorMessageArgumentCaptor.capture(), eq(ErrorType.WARNING));
    assertThat(identifierArgumentCaptor.getValue()).isEqualTo("hrid2");
    assertThat(errorMessageArgumentCaptor.getValue()).isEqualTo(MSG_NO_MARC_CHANGE_REQUIRED);
  }

  @Test
  @SneakyThrows
  void shouldCommitInstanceMarcWhenNoChangesRequired() {

    var operation = new BulkOperation();
    operation.setLinkToModifiedRecordsMarcFile("some link");
    operation.setLinkToCommittedRecordsMarcFile("marc link");
    var execution = BulkOperationExecution.builder().bulkOperationId(operation.getId()).build();

    when(remoteFileSystemClient.marcWriter(any()))
      .thenReturn(mock(MarcRemoteStorageWriter.class));
    when(remoteFileSystemClient.get(operation.getLinkToMatchedRecordsMarcFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/matched.mrc"));
    when(remoteFileSystemClient.get(operation.getLinkToModifiedRecordsMarcFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/modified.mrc"));
    when(executionRepository.save(any(BulkOperationExecution.class)))
      .thenReturn(execution);
    doAnswer(invocationOnMock -> {
      operation.setLinkToCommittedRecordsMarcFile(null);
      return null;
    }).when(updateProcessor).updateMarcRecords(operation);

    marcUpdateService.commitForInstanceMarc(operation);

    verify(updateProcessor).updateMarcRecords(bulkOperationArgumentCaptor.capture());
    assertThat(bulkOperationArgumentCaptor.getValue().getLinkToCommittedRecordsMarcFile()).isNull();

    verify(executionRepository, times(2)).save(executionArgumentCaptor.capture());
    assertThat(executionArgumentCaptor.getAllValues().get(1).getStatus()).isEqualTo(StatusType.COMPLETED);
  }

  @Test
  @SneakyThrows
  void shouldFailOperationInCaseOfException() {
    var bulkOperation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .linkToTriggeringCsvFile("triggering.csv")
      .linkToMatchedRecordsMarcFile("matched.mrc")
      .linkToModifiedRecordsMarcFile("modified.mrc")
      .totalNumOfRecords(1)
      .processedNumOfRecords(1)
      .build();
    var execution = BulkOperationExecution.builder().bulkOperationId(bulkOperation.getId()).build();
    var pathToCommittedMarcFile = String.format(CHANGED_MARC_PATH_TEMPLATE, bulkOperation.getId(), LocalDate.now(), "triggering");

    var mockMarcWriter = mock(MarcRemoteStorageWriter.class);
    doThrow(new IllegalArgumentException())
      .when(mockMarcWriter).writeRecord(any(Record.class));
    when(executionRepository.save(any(BulkOperationExecution.class)))
      .thenReturn(execution);
    when(remoteFileSystemClient.marcWriter(pathToCommittedMarcFile))
      .thenReturn(mockMarcWriter);
    when(remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsMarcFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/matched.mrc"));
    when(remoteFileSystemClient.get(bulkOperation.getLinkToModifiedRecordsMarcFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/modified.mrc"));

    marcUpdateService.commitForInstanceMarc(bulkOperation);

    verify(executionRepository, times(2)).save(executionArgumentCaptor.capture());
    assertThat(executionArgumentCaptor.getAllValues().get(1).getStatus()).isEqualTo(StatusType.FAILED);

    verify(bulkOperationRepository, times(2)).save(bulkOperationArgumentCaptor.capture());
    assertThat(bulkOperationArgumentCaptor.getValue().getStatus()).isEqualTo(FAILED);
  }
}
