package org.folio.bulkops.processor.marc;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.DataImportClient;
import org.folio.bulkops.client.DataImportProfilesClient;
import org.folio.bulkops.client.DataImportRestS3UploadClient;
import org.folio.bulkops.client.DataImportUploadClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.ActionProfile;
import org.folio.bulkops.domain.bean.ActionProfilePost;
import org.folio.bulkops.domain.bean.AssembleStorageFileRequestBody;
import org.folio.bulkops.domain.bean.FileDefinition;
import org.folio.bulkops.domain.bean.JobProfile;
import org.folio.bulkops.domain.bean.JobProfilePost;
import org.folio.bulkops.domain.bean.MappingProfile;
import org.folio.bulkops.domain.bean.MappingProfilePost;
import org.folio.bulkops.domain.bean.MatchProfile;
import org.folio.bulkops.domain.bean.MatchProfilePost;
import org.folio.bulkops.domain.bean.SplitStatus;
import org.folio.bulkops.domain.bean.UploadFileDefinition;
import org.folio.bulkops.domain.bean.UploadUrlResponse;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.processor.marc.MarcInstanceUpdateProcessor;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.ErrorService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.UUID;

class MarcInstanceUpdateProcessorTest extends BaseTest {
  @MockBean
  private DataImportClient dataImportClient;
  @MockBean
  private DataImportUploadClient dataImportUploadClient;
  @MockBean
  private DataImportProfilesClient dataImportProfilesClient;
  @MockBean
  private RemoteFileSystemClient remoteFileSystemClient;
  @MockBean
  private BulkOperationRepository bulkOperationRepository;
  @MockBean
  private DataImportRestS3UploadClient dataImportRestS3UploadClient;
  @MockBean
  private ErrorService errorService;

  @Captor
  private ArgumentCaptor<BulkOperation> bulkOperationCaptor;

  @Autowired
  private MarcInstanceUpdateProcessor marcInstanceUpdateProcessor;

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @SneakyThrows
  void shouldUpdateMarcInstances(boolean isSplitStatusEnabled) {
    var bulkOperation = BulkOperation.builder().linkToCommittedRecordsMarcFile("committed.mrc").build();

    when(remoteFileSystemClient.get(bulkOperation.getLinkToCommittedRecordsMarcFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/matched.mrc"));
    when(dataImportProfilesClient.createMatchProfile(any(MatchProfilePost.class)))
      .thenReturn(MatchProfile.builder().id(UUID.randomUUID().toString()).build());
    when(dataImportProfilesClient.createMappingProfile(any(MappingProfilePost.class)))
      .thenReturn(MappingProfile.builder().id(UUID.randomUUID().toString()).build());
    when(dataImportProfilesClient.createActionProfile(any(ActionProfilePost.class)))
      .thenReturn(ActionProfile.builder().id(UUID.randomUUID().toString()).build());
    var jobProfileId = UUID.randomUUID();
    when(dataImportProfilesClient.createJobProfile(any(JobProfilePost.class)))
      .thenReturn(JobProfile.builder().id(jobProfileId.toString()).build());
    when(dataImportClient.uploadFileDefinitions(any(UploadFileDefinition.class)))
      .thenReturn(UploadFileDefinition.builder()
        .id(UUID.randomUUID().toString())
        .fileDefinitions(Collections.singletonList(FileDefinition.builder()
          .id(UUID.randomUUID().toString())
          .name("name")
          .build()))
        .build());
    when(dataImportClient.getSplitStatus())
      .thenReturn(SplitStatus.builder().splitStatus(isSplitStatusEnabled).build());
    when(dataImportClient.getUploadUrl(anyString()))
      .thenReturn(UploadUrlResponse.builder().url("url").build());
    var headers = new HttpHeaders();
    headers.add(HttpHeaders.ETAG, "etag");
    when(dataImportRestS3UploadClient.uploadFile(anyString(), any(byte[].class)))
      .thenReturn(new ResponseEntity<>(headers, HttpStatus.OK));
    when(dataImportClient.getUploadDefinitionById(anyString()))
      .thenReturn(UploadFileDefinition.builder()
        .id(UUID.randomUUID().toString())
        .fileDefinitions(Collections.singletonList(FileDefinition.builder().id(UUID.randomUUID().toString()).build()))
        .build());
    when(dataImportUploadClient.uploadFileDefinitionsFiles(anyString(), anyString(), any(byte[].class)))
      .thenReturn(UploadFileDefinition.builder()
        .id(UUID.randomUUID().toString())
        .fileDefinitions(Collections.singletonList(FileDefinition.builder()
          .id(UUID.randomUUID().toString())
          .name("name")
          .build()))
        .build());

    marcInstanceUpdateProcessor.updateMarcRecords(bulkOperation);

    verify(dataImportProfilesClient).createMatchProfile(any(MatchProfilePost.class));
    verify(dataImportProfilesClient, times(2)).createMappingProfile(any(MappingProfilePost.class));
    verify(dataImportProfilesClient, times(2)).createActionProfile(any(ActionProfilePost.class));
    verify(dataImportProfilesClient).createJobProfile(any(JobProfilePost.class));

    if (isSplitStatusEnabled) {
      verify(dataImportClient).getUploadUrl(anyString());
      verify(dataImportRestS3UploadClient).uploadFile(anyString(), any(byte[].class));
      verify(dataImportClient).assembleStorageFile(anyString(), anyString(), any(AssembleStorageFileRequestBody.class));
    } else {
      verify(dataImportUploadClient).uploadFileDefinitionsFiles(anyString(), anyString(), any(byte[].class));
    }

    verify(bulkOperationRepository).save(bulkOperationCaptor.capture());
    assertThat(bulkOperationCaptor.getValue().getDataImportJobProfileId()).isEqualTo(jobProfileId);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  @SneakyThrows
  void shouldNotUploadEmptyFile(int numOfErrors) {
    var operationId = UUID.randomUUID();
    var bulkOperation = BulkOperation.builder()
      .id(operationId)
      .linkToCommittedRecordsMarcFile("committed.mrc").build();

    when(remoteFileSystemClient.get(bulkOperation.getLinkToCommittedRecordsMarcFile()))
      .thenReturn(new ByteArrayInputStream(EMPTY.getBytes()));
    when(errorService.getCommittedNumOfErrors(operationId)).thenReturn(numOfErrors);

    marcInstanceUpdateProcessor.updateMarcRecords(bulkOperation);

    verify(errorService).uploadErrorsToStorage(operationId);

    verify(bulkOperationRepository).save(bulkOperationCaptor.capture());
    assertThat(bulkOperationCaptor.getValue().getLinkToCommittedRecordsMarcFile()).isNull();

    var expectedStatus = numOfErrors == 0 ? COMPLETED : COMPLETED_WITH_ERRORS;
    assertThat(bulkOperationCaptor.getValue().getStatus()).isEqualTo(expectedStatus);
  }
}
