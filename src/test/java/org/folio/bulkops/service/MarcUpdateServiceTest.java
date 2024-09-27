package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.dto.IdentifierType.HRID;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.service.MarcUpdateService.CHANGED_MARC_PATH_TEMPLATE;
import static org.folio.bulkops.service.MarcUpdateService.MSG_BULK_EDIT_SUPPORTED_FOR_MARC_ONLY;
import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.StatusType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.folio.bulkops.processor.MarcInstanceUpdateProcessor;
import org.folio.bulkops.repository.BulkOperationExecutionRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.marc4j.marc.Record;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.UUID;

class MarcUpdateServiceTest extends BaseTest {
  @MockBean
  private BulkOperationExecutionRepository executionRepository;
  @MockBean
  private RemoteFileSystemClient remoteFileSystemClient;
  @MockBean
  private MarcInstanceUpdateProcessor updateProcessor;
  @MockBean
  private ErrorService errorService;
  @MockBean
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
    assertThat(bulkOperationArgumentCaptor.getValue().getLinkToCommittedRecordsMarcFile()).isEqualTo(pathToCommittedMarcFile);

    verify(executionRepository, times(2)).save(executionArgumentCaptor.capture());
    assertThat(executionArgumentCaptor.getAllValues().get(1).getStatus()).isEqualTo(StatusType.COMPLETED);

    verify(mockMarcWriter).writeRecord(recordArgumentCaptor.capture());
    assertThat(recordArgumentCaptor.getValue().getControlNumberField().getData()).isEqualTo("hrid1");

    verify(errorService).saveError(eq(bulkOperation.getId()), identifierArgumentCaptor.capture(), errorMessageArgumentCaptor.capture());
    assertThat(identifierArgumentCaptor.getValue()).isEqualTo("hrid2");
    assertThat(errorMessageArgumentCaptor.getValue()).isEqualTo(MSG_NO_CHANGE_REQUIRED);
  }

  @Test
  @SneakyThrows
  void shouldFailOperationInCaseOfException() {
    var bulkOperation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .linkToTriggeringCsvFile("triggering.csv")
      .linkToMatchedRecordsMarcFile("matched.mrc")
      .linkToModifiedRecordsMarcFile("modified.mrc")
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

    verify(bulkOperationRepository).save(bulkOperationArgumentCaptor.capture());
    assertThat(bulkOperationArgumentCaptor.getValue().getStatus()).isEqualTo(FAILED);
  }

  @ParameterizedTest
  @EnumSource(value = IdentifierType.class, names = {"ID", "HRID"}, mode = EnumSource.Mode.INCLUDE)
  @SneakyThrows
  void shouldSaveErrorsForFolioInstances(IdentifierType identifierType) {
    var bulkOperation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .linkToMatchedRecordsJsonFile("matched.mrc")
      .identifierType(identifierType)
      .build();
    when(remoteFileSystemClient.get("matched.mrc"))
      .thenReturn(new FileInputStream("src/test/resources/files/extended_instances.json"));

    marcUpdateService.saveErrorsForFolioInstances(bulkOperation);

    verify(errorService).saveError(any(UUID.class), identifierArgumentCaptor.capture(),
      errorMessageArgumentCaptor.capture());
    assertThat(errorMessageArgumentCaptor.getValue()).isEqualTo(MSG_BULK_EDIT_SUPPORTED_FOR_MARC_ONLY);
    assertThat(identifierArgumentCaptor.getValue()).isEqualTo(HRID.equals(identifierType) ? "hrid2" : "139ed15a-edba-4cde-8f92-810a4cec7770");
  }
}
