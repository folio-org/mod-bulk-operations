package org.folio.bulkops.service;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.service.ErrorService.IDENTIFIER;
import static org.folio.bulkops.service.ErrorService.LINK;
import static org.folio.bulkops.util.Constants.CSV_MSG_ERROR_TEMPLATE_OPTIMISTIC_LOCKING;
import static org.folio.bulkops.util.Constants.DATA_IMPORT_ERROR_DISCARDED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static wiremock.org.hamcrest.MatcherAssert.assertThat;
import static wiremock.org.hamcrest.Matchers.equalTo;
import static wiremock.org.hamcrest.Matchers.hasSize;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import feign.FeignException;
import feign.Request;
import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.BulkEditClient;
import org.folio.bulkops.client.MetadataProviderClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.domain.bean.ExternalIdsHolder;
import org.folio.bulkops.domain.bean.JobLogEntry;
import org.folio.bulkops.domain.bean.JobLogEntryCollection;
import org.folio.bulkops.domain.bean.RelatedInstanceInfo;
import org.folio.bulkops.domain.bean.SrsRecord;
import org.folio.bulkops.domain.dto.Error;
import org.folio.bulkops.domain.dto.Errors;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.domain.entity.BulkOperationProcessingContent;
import org.folio.bulkops.exception.DataImportException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationProcessingContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ErrorServiceTest extends BaseTest {
  @Autowired
  private ErrorService errorService;

  @Autowired
  private BulkOperationRepository bulkOperationRepository;

  @Autowired
  private BulkOperationExecutionContentRepository executionContentRepository;

  @Autowired
  private BulkOperationProcessingContentRepository processingContentRepository;

  @Autowired
  private JpaCqlRepository<BulkOperationExecutionContent, UUID> executionContentCqlRepository;

  @MockBean
  private RemoteFileSystemClient remoteFileSystemClient;

  @MockBean
  private BulkEditClient bulkEditClient;

  @MockBean
  private MetadataProviderClient metadataProviderClient;

  @MockBean
  private SrsClient srsClient;

  private UUID bulkOperationId;

  @BeforeEach()
  void saveBulkOperation() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {

      bulkOperationId = bulkOperationRepository.save(BulkOperation.builder()
        .id(UUID.randomUUID())
        .linkToTriggeringCsvFile("some/path/records.csv")
        .build()).getId();
    }
  }

  @AfterEach
  void clearTestData() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      bulkOperationRepository.deleteById(bulkOperationId);
    }
  }

  @Test
  void shouldSaveError() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorService.saveError(bulkOperationId, "123", "Error message");

      var result = executionContentCqlRepository.findByCql("bulkOperationId==" + bulkOperationId, OffsetRequest.of(0, 10));

      assertThat(result.toList(), hasSize(1));
      var content = result.iterator().next();
      assertThat(content.getIdentifier(), equalTo("123"));
      assertThat(content.getErrorMessage(), equalTo("Error message"));
    }
  }

  @Test
  @SneakyThrows
  void shouldUploadErrorsAndReturnLinkToFile() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorService.saveError(bulkOperationId, "123", "Error message 123");
      errorService.saveError(bulkOperationId, "456", "Error message 456");

      var expectedFileName = bulkOperationId + "/" + LocalDate.now() + "-Committing-changes-Errors-records.csv";
      when(remoteFileSystemClient.put(any(), eq(expectedFileName))).thenReturn(expectedFileName);

      var result = errorService.uploadErrorsToStorage(bulkOperationId);
      assertThat(result, equalTo(expectedFileName));

      var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
      verify(remoteFileSystemClient).put(streamCaptor.capture(), eq(expectedFileName));
      var actual = new String(streamCaptor.getValue().readAllBytes());
      var actualArr = actual.split("\n");
      Arrays.sort(actualArr);
      var expectedArr = new String[] {"123,Error message 123", "456,Error message 456"};
      assertArrayEquals(expectedArr, actualArr);
    }
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "COMPLETED" }, mode = EnumSource.Mode.INCLUDE)
  void shouldGetErrorsPreviewByBulkOperationId(OperationStatusType statusType) {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = bulkOperationRepository.save(BulkOperation.builder().id(UUID.randomUUID()).dataExportJobId(UUID.randomUUID()).status(statusType).build()).getId();

      var expected = List.of(
        new Error().message("No match found").parameters(List.of(new Parameter().key("IDENTIFIER").value("123"))),
        new Error().message("Invalid format").parameters(List.of(new Parameter().key("IDENTIFIER").value("456")))
      );

      mockErrorsData(statusType, operationId);

      var actual = errorService.getErrorsPreviewByBulkOperationId(operationId, 2);
      assertThat(actual.getErrors(), hasSize(2));

      bulkOperationRepository.deleteById(operationId);
    }
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED_WITH_ERRORS", "COMPLETED", "REVIEWED_NO_MARC_RECORDS" }, mode = EnumSource.Mode.EXCLUDE)
  void shouldRejectErrorsPreviewOnWrongOperationStatus(OperationStatusType statusType) {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = bulkOperationRepository.save(BulkOperation.builder().id(UUID.randomUUID()).status(statusType).build()).getId();

      assertThrows(NotFoundException.class, () -> errorService.getErrorsPreviewByBulkOperationId(operationId, 10));

      bulkOperationRepository.deleteById(operationId);
    }
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "COMPLETED" }, mode = EnumSource.Mode.INCLUDE)
  void shouldGetErrorsCsvByBulkOperationId(OperationStatusType statusType) {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = bulkOperationRepository.save(BulkOperation.builder().id(UUID.randomUUID()).dataExportJobId(UUID.randomUUID()).status(statusType).build()).getId();

      var expected = "123,No match found\n456,Invalid format".split(LF);

      mockErrorsData(statusType, operationId);

      var actual = errorService.getErrorsCsvByBulkOperationId(operationId).split(LF);
      Arrays.sort(expected);
      Arrays.sort(actual);

      assertArrayEquals(expected, actual);
      assertThat(actual.length, equalTo(2));

      bulkOperationRepository.deleteById(operationId);
    }
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED", "COMPLETED_WITH_ERRORS", "REVIEWED_NO_MARC_RECORDS" }, mode = EnumSource.Mode.EXCLUDE)
  void shouldRejectErrorsCsvOnWrongOperationStatus(OperationStatusType statusType) {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = bulkOperationRepository.save(BulkOperation.builder().id(UUID.randomUUID()).status(statusType).build()).getId();

      assertThrows(NotFoundException.class, () -> errorService.getErrorsCsvByBulkOperationId(operationId));

      bulkOperationRepository.deleteById(operationId);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void shouldReturnErrorsPreviewOnCompletedWithErrors(int committedErrors) {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var jobId = UUID.randomUUID();

      var operationId = bulkOperationRepository.save(BulkOperation.builder()
          .id(UUID.randomUUID())
          .status(COMPLETED_WITH_ERRORS)
          .committedNumOfErrors(committedErrors)
          .dataExportJobId(jobId)
          .build())
        .getId();

      mockErrorsData(COMPLETED_WITH_ERRORS, operationId);

      if (committedErrors == 1) {
        executionContentRepository.save(BulkOperationExecutionContent.builder()
          .bulkOperationId(operationId)
          .identifier("123")
          .errorMessage("No match found")
          .build());
        executionContentRepository.save(BulkOperationExecutionContent.builder()
          .bulkOperationId(operationId)
          .identifier("456")
          .errorMessage("Invalid format")
          .build());
      }

      var errors = errorService.getErrorsPreviewByBulkOperationId(operationId, 10);

      assertThat(errors.getErrors(), hasSize(2));
    }
  }

  @Test
  void testOptimisticLockErrorProcessing() {

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = bulkOperationRepository.save(BulkOperation.builder()
          .id(UUID.randomUUID())
          .status(COMPLETED_WITH_ERRORS)
          .committedNumOfErrors(1)
          .dataExportJobId(UUID.randomUUID())
          .build())
        .getId();

      var link = "/inventory/view/ff12f28b-1982-4a8d-982c-c797bf92d479/1028b1eb-0b9d-4fe9-a458-2f0a8570cf9c";
      var message = format(CSV_MSG_ERROR_TEMPLATE_OPTIMISTIC_LOCKING, 2, 1);

      executionContentRepository.save(BulkOperationExecutionContent.builder()
        .bulkOperationId(operationId)
        .identifier("789")
        .errorMessage(format("%s %s", message, link))
        .uiErrorMessage(message)
        .linkToFailedEntity(link)
        .build());

      var errors = errorService.getErrorsPreviewByBulkOperationId(operationId, 1);

      assertThat(errors.getErrors(), hasSize(1));
      assertThat(errors.getErrors().get(0).getParameters(), hasSize(2));

      assertThat(errors.getErrors().get(0).getMessage(), equalTo(message));
      assertThat(errors.getErrors().get(0).getParameters().get(0).getKey(), equalTo(IDENTIFIER));
      assertThat(errors.getErrors().get(0).getParameters().get(0).getValue(), equalTo("789"));
      assertThat(errors.getErrors().get(0).getParameters().get(1).getKey(), equalTo(LINK));
      assertThat(errors.getErrors().get(0).getParameters().get(1).getValue(), equalTo(link));
    }
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
      ID            | true
      INSTANCE_HRID | true
      ID            | false
      INSTANCE_HRID | false
    """, delimiter = '|')
  void testSaveErrorsFromDataImport(IdentifierType identifierType, boolean relatedInstanceInfo) {
    final var dataImportJobId = UUID.randomUUID();
    final var sourceRecordId = UUID.randomUUID().toString();
    final var dataExportJobId = UUID.randomUUID();
    final var instanceId = UUID.randomUUID().toString();
    final var instanceInfo = new RelatedInstanceInfo().withIdList(List.of(instanceId)).withHridList(List.of("instance HRID"));
    when(metadataProviderClient.getJobLogEntries(dataImportJobId.toString(), Integer.MAX_VALUE))
      .thenReturn(new JobLogEntryCollection().withEntries(List.of(new JobLogEntry()
        .withError("some MARC error").withSourceRecordId(sourceRecordId).withRelatedInstanceInfo(
          relatedInstanceInfo ? instanceInfo : new RelatedInstanceInfo().withIdList(List.of()).withHridList(List.of())
        ))));
    when(srsClient.getSrsRecordById(sourceRecordId)).thenReturn(new SrsRecord().withExternalIdsHolder(
      new ExternalIdsHolder().withInstanceHrid("instance HRID").withInstanceId(instanceId)));

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = bulkOperationRepository.save(BulkOperation.builder()
          .id(UUID.randomUUID())
          .status(COMPLETED_WITH_ERRORS)
          .identifierType(identifierType)
          .committedNumOfErrors(1)
          .dataExportJobId(dataExportJobId)
          .build())
        .getId();
      errorService.saveErrorsFromDataImport(operationId, dataImportJobId);
      var errors = errorService.getErrorsPreviewByBulkOperationId(operationId, 1);

      assertThat(errors.getErrors(), hasSize(1));
      assertEquals("some MARC error", errors.getErrors().get(0).getMessage());
    }
  }

  @Test
  void shouldNotSaveError_IfErrorFromDataImportIsEmpty() {
    final var dataImportJobId = UUID.randomUUID();
    final var dataExportJobId = UUID.randomUUID();
    when(metadataProviderClient.getJobLogEntries(dataImportJobId.toString(), Integer.MAX_VALUE))
      .thenReturn(new JobLogEntryCollection().withEntries(List.of(new JobLogEntry()
        .withError("").withRelatedInstanceInfo(
          new RelatedInstanceInfo().withIdList(List.of()).withHridList(List.of())
        ))));

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = bulkOperationRepository.save(BulkOperation.builder()
          .id(UUID.randomUUID())
          .status(COMPLETED)
          .identifierType(IdentifierType.ID)
          .dataExportJobId(dataExportJobId)
          .build())
        .getId();
      errorService.saveErrorsFromDataImport(operationId, dataImportJobId);
      var errors = errorService.getErrorsPreviewByBulkOperationId(operationId, 1);

      assertThat(errors.getErrors(), hasSize(0));
    }
  }

  @Test
  void testSaveErrorsFromDataImport_whenDiscardedAndNoMessage() {
    final var dataImportJobId = UUID.randomUUID();
    final var sourceRecordId = UUID.randomUUID().toString();
    final var dataExportJobId = UUID.randomUUID();
    final var instanceId = UUID.randomUUID().toString();
    when(metadataProviderClient.getJobLogEntries(dataImportJobId.toString(), Integer.MAX_VALUE))
      .thenReturn(new JobLogEntryCollection().withEntries(List.of(new JobLogEntry()
        .withSourceRecordActionStatus(JobLogEntry.ActionStatus.DISCARDED).withError("")
        .withRelatedInstanceInfo(new RelatedInstanceInfo().withIdList(List.of()).withHridList(List.of())))));
    when(srsClient.getSrsRecordById(sourceRecordId)).thenReturn(new SrsRecord().withExternalIdsHolder(
      new ExternalIdsHolder().withInstanceHrid("instance HRID").withInstanceId(instanceId)));

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = bulkOperationRepository.save(BulkOperation.builder()
          .id(UUID.randomUUID())
          .status(COMPLETED_WITH_ERRORS)
          .identifierType(IdentifierType.ID)
          .committedNumOfErrors(1)
          .dataExportJobId(dataExportJobId)
          .build())
        .getId();
      errorService.saveErrorsFromDataImport(operationId, dataImportJobId);
      var errors = errorService.getErrorsPreviewByBulkOperationId(operationId, 1);

      assertThat(errors.getErrors(), hasSize(1));
      assertEquals(DATA_IMPORT_ERROR_DISCARDED, errors.getErrors().get(0).getMessage());
    }
  }

  @Test
  void testDataImportException() {
    final var dataImportJobId = UUID.randomUUID();
    final var dataExportJobId = UUID.randomUUID();
    when(metadataProviderClient.getJobLogEntries(dataImportJobId.toString(), Integer.MAX_VALUE))
      .thenThrow(new FeignException.FeignClientException(403, "some error msg",
        Request.create(Request.HttpMethod.GET, "url", Map.of(), null, null, null), null, null));

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = bulkOperationRepository.save(BulkOperation.builder()
          .id(UUID.randomUUID())
          .status(COMPLETED_WITH_ERRORS)
          .identifierType(IdentifierType.ID)
          .committedNumOfErrors(1)
          .dataExportJobId(dataExportJobId)
          .build())
        .getId();

      assertThrows(DataImportException.class, () -> errorService.saveErrorsFromDataImport(operationId, dataImportJobId));
    }
  }

  private void mockErrorsData(OperationStatusType statusType, UUID operationId) {
    if (DATA_MODIFICATION == statusType || COMPLETED_WITH_ERRORS == statusType) {
      when(bulkEditClient.getErrorsPreview(any(UUID.class), anyInt()))
        .thenReturn(new Errors()
          .errors(List.of(new Error().type("BULK_EDIT_ERROR").message("123,No match found"),
            new Error().type("BULK_EDIT_ERROR").message("456,Invalid format"))));
    } else {
      executionContentRepository.save(BulkOperationExecutionContent.builder()
        .bulkOperationId(operationId)
        .identifier("123")
        .errorMessage("No match found")
        .build());
      executionContentRepository.save(BulkOperationExecutionContent.builder()
        .bulkOperationId(operationId)
        .identifier("456")
        .errorMessage("Invalid format")
        .build());
      processingContentRepository.save(BulkOperationProcessingContent.builder()
        .bulkOperationId(operationId)
        .identifier("123")
        .errorMessage("No match found")
        .build());
      processingContentRepository.save(BulkOperationProcessingContent.builder()
        .bulkOperationId(operationId)
        .identifier("456")
        .errorMessage("Invalid format")
        .build());
    }
  }
}
