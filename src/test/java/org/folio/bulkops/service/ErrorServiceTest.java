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
import static org.folio.bulkops.util.Constants.MSG_NO_ADMINISTRATIVE_CHANGE_REQUIRED;
import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;
import static org.folio.bulkops.util.Constants.MSG_NO_MARC_CHANGE_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static wiremock.org.hamcrest.MatcherAssert.assertThat;
import static wiremock.org.hamcrest.Matchers.anyOf;
import static wiremock.org.hamcrest.Matchers.contains;
import static wiremock.org.hamcrest.Matchers.equalTo;
import static wiremock.org.hamcrest.Matchers.hasSize;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.JobLogEntry;
import org.folio.bulkops.domain.bean.RelatedInstanceInfo;
import org.folio.bulkops.domain.dto.Error;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.domain.entity.BulkOperationProcessingContent;
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

  private UUID bulkOperationId;

  @BeforeEach()
  void saveBulkOperation() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {

      bulkOperationId = bulkOperationRepository.save(BulkOperation.builder()
        .id(UUID.randomUUID())
        .identifierType(IdentifierType.HRID)
        .linkToTriggeringCsvFile("some/path/records.csv")
        .build()).getId();
    }
  }

  @AfterEach
  void clearTestData() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      bulkOperationRepository.deleteById(bulkOperationId);
      executionContentRepository.deleteByBulkOperationId(bulkOperationId);
    }
  }

  @Test
  void shouldSaveError() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorService.saveError(bulkOperationId, "123", "Error message", ErrorType.ERROR);

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
      errorService.saveError(bulkOperationId, "123", "Error message 123", ErrorType.ERROR);
      errorService.saveError(bulkOperationId, "456", "Error message 456", ErrorType.WARNING);

      var expectedFileName = bulkOperationId + "/" + LocalDate.now() + "-Committing-changes-Errors-records.csv";
      when(remoteFileSystemClient.put(any(), eq(expectedFileName))).thenReturn(expectedFileName);

      var result = errorService.uploadErrorsToStorage(bulkOperationId);
      assertThat(result, equalTo(expectedFileName));

      var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
      verify(remoteFileSystemClient).put(streamCaptor.capture(), eq(expectedFileName));
      var actual = new String(streamCaptor.getValue().readAllBytes());
      var actualArr = actual.split("\n");
      Arrays.sort(actualArr);
      var expectedArr = new String[] {"ERROR,123,Error message 123", "WARNING,456,Error message 456"};
      assertArrayEquals(expectedArr, actualArr);
    }
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "COMPLETED" }, mode = EnumSource.Mode.INCLUDE)
  void shouldGetErrorsPreviewByBulkOperationId(OperationStatusType statusType) throws IOException {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = bulkOperationRepository.save(BulkOperation.builder()
        .id(UUID.randomUUID())
        .dataExportJobId(UUID.randomUUID())
          .linkToMatchedRecordsErrorsCsvFile("link-to-errors-csv")
        .status(statusType).build()).getId();

      mockErrorsData(statusType, operationId);

      var actualWithOffset = errorService.getErrorsPreviewByBulkOperationId(operationId, 2, 1, null);
      assertThat(actualWithOffset.getErrors(), hasSize(1));
      assertThat(actualWithOffset.getTotalRecords(), equalTo(2));

      bulkOperationRepository.deleteById(operationId);
    }
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED_WITH_ERRORS", "COMPLETED", "REVIEWED_NO_MARC_RECORDS" }, mode = EnumSource.Mode.EXCLUDE)
  void shouldRejectErrorsPreviewOnWrongOperationStatus(OperationStatusType statusType) {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = bulkOperationRepository.save(BulkOperation.builder().id(UUID.randomUUID()).status(statusType).build()).getId();

      assertThrows(NotFoundException.class, () -> errorService.getErrorsPreviewByBulkOperationId(operationId, 10, 0, ErrorType.ERROR));

      bulkOperationRepository.deleteById(operationId);
    }
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "COMPLETED" }, mode = EnumSource.Mode.INCLUDE)
  void shouldGetErrorsCsvByBulkOperationId(OperationStatusType statusType) throws IOException {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = bulkOperationRepository.save(BulkOperation.builder().id(UUID.randomUUID())
        .linkToMatchedRecordsErrorsCsvFile("link-to-errors-csv")
        .dataExportJobId(UUID.randomUUID())
        .status(statusType).build()).getId();

      var expected = "ERROR,123,No match found\nERROR,456,Invalid format".split(LF);

      mockErrorsData(statusType, operationId);

      var actual = errorService.getErrorsCsvByBulkOperationId(operationId, 0, null).split(LF);
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

      assertThrows(NotFoundException.class, () -> errorService.getErrorsCsvByBulkOperationId(operationId, 0, ErrorType.ERROR));

      bulkOperationRepository.deleteById(operationId);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void shouldReturnErrorsPreviewOnCompletedWithErrors(int committedErrors) throws IOException {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var jobId = UUID.randomUUID();

      var operationId = bulkOperationRepository.save(BulkOperation.builder()
          .id(UUID.randomUUID())
          .status(COMPLETED_WITH_ERRORS)
          .committedNumOfErrors(committedErrors)
          .linkToMatchedRecordsErrorsCsvFile("link-to-errors-csv")
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

      var errors = errorService.getErrorsPreviewByBulkOperationId(operationId, 10, 0, null);

      assertThat(errors.getErrors(), hasSize(2));
      assertThat(errors.getTotalRecords(), equalTo(2));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void shouldReturnOnlyErrorsTotalNumberPreviewOnCompletedWithErrors(int committedErrors) throws IOException {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var jobId = UUID.randomUUID();

      var operationId = bulkOperationRepository.save(BulkOperation.builder()
          .id(UUID.randomUUID())
          .status(COMPLETED_WITH_ERRORS)
          .committedNumOfErrors(committedErrors)
          .linkToMatchedRecordsErrorsCsvFile("link-to-errors-csv")
          .dataExportJobId(jobId)
          .build())
        .getId();

      mockErrorsData(COMPLETED_WITH_ERRORS, operationId);

      if (committedErrors == 1) {
        executionContentRepository.save(BulkOperationExecutionContent.builder()
          .bulkOperationId(operationId)
          .identifier("123")
            .errorType(ErrorType.ERROR)
          .errorMessage("No match found")
          .build());
        executionContentRepository.save(BulkOperationExecutionContent.builder()
          .bulkOperationId(operationId)
          .identifier("456")
            .errorType(ErrorType.ERROR)
          .errorMessage("Invalid format")
          .build());
      }

      var errors = errorService.getErrorsPreviewByBulkOperationId(operationId, 0, 0, null);

      assertThat(errors.getErrors(), hasSize(0));
      assertThat(errors.getTotalRecords(), equalTo(2));
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
        .errorType(ErrorType.ERROR)
        .build());

      var errors = errorService.getErrorsPreviewByBulkOperationId(operationId, 1, 0, ErrorType.ERROR);

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
    final var sourceRecordId = UUID.randomUUID().toString();
    final var dataExportJobId = UUID.randomUUID();
    final var instanceId = UUID.randomUUID().toString();
    final var instanceInfo = new RelatedInstanceInfo()
      .withIdList(List.of(instanceId))
      .withHridList(List.of("instance HRID"));
    var logEntries = List.of(new JobLogEntry()
      .withError("some MARC error #1").withSourceRecordId(sourceRecordId).withRelatedInstanceInfo(
        relatedInstanceInfo ? instanceInfo : new RelatedInstanceInfo().withIdList(List.of()).withHridList(List.of())
      ), new JobLogEntry()
      .withError("some MARC error #2").withSourceRecordId(sourceRecordId).withRelatedInstanceInfo(
        relatedInstanceInfo ? instanceInfo : new RelatedInstanceInfo().withIdList(List.of()).withHridList(List.of())
      ));

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operation = bulkOperationRepository.save(BulkOperation.builder()
        .id(UUID.randomUUID())
        .status(COMPLETED_WITH_ERRORS)
        .identifierType(identifierType)
        .committedNumOfErrors(2)
        .dataExportJobId(dataExportJobId)
        .build());

      errorService.saveErrorsFromDataImport(logEntries, operation);

      var errors = errorService.getErrorsPreviewByBulkOperationId(operation.getId(), 2, 1, ErrorType.ERROR);
      assertThat(errors.getErrors(), hasSize(1));
      assertThat(errors.getTotalRecords(), equalTo(2));
      assertThat(errors.getErrors().stream().map(Error::getMessage).toList(), anyOf(contains("some MARC error #1"), contains("some MARC error #2")));
    }
  }

  @Test
  void shouldNotSaveError_IfErrorFromDataImportIsEmpty() {
    final var dataExportJobId = UUID.randomUUID();
    var logEntries = List.of(new JobLogEntry()
      .withError("").withRelatedInstanceInfo(
        new RelatedInstanceInfo().withIdList(List.of()).withHridList(List.of())
      ));

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operation = bulkOperationRepository.save(BulkOperation.builder()
        .id(UUID.randomUUID())
        .status(COMPLETED)
        .identifierType(IdentifierType.ID)
        .dataExportJobId(dataExportJobId)
        .build());

      errorService.saveErrorsFromDataImport(logEntries, operation);

      var errors = errorService.getErrorsPreviewByBulkOperationId(operation.getId(), 1, 1, ErrorType.ERROR);
      assertThat(errors.getErrors(), hasSize(0));
    }
  }

  @Test
  void testSaveErrorsFromDataImport_whenDiscardedAndNoMessage() {
    final var dataExportJobId = UUID.randomUUID();
    var logEntries = Collections.singletonList(new JobLogEntry()
      .withSourceRecordActionStatus(JobLogEntry.ActionStatus.DISCARDED).withError("")
      .withRelatedInstanceInfo(new RelatedInstanceInfo().withIdList(List.of()).withHridList(List.of())));

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operation = bulkOperationRepository.save(BulkOperation.builder()
        .id(UUID.randomUUID())
        .status(COMPLETED_WITH_ERRORS)
        .identifierType(IdentifierType.ID)
        .committedNumOfErrors(1)
        .dataExportJobId(dataExportJobId)
        .build());

      errorService.saveErrorsFromDataImport(logEntries, operation);

      var errors = errorService.getErrorsPreviewByBulkOperationId(operation.getId(), 1, 0, ErrorType.ERROR);
      assertThat(errors.getErrors(), hasSize(1));
      assertEquals(DATA_IMPORT_ERROR_DISCARDED, errors.getErrors().get(0).getMessage());
    }
  }

  @Test
  void getCommittedNumOfErrorsAndWarningsTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      bulkOperationRepository.save(BulkOperation.builder()
        .id(bulkOperationId)
        .status(COMPLETED_WITH_ERRORS)
        .committedNumOfErrors(2)
        .committedNumOfWarnings(1)
        .linkToCommittedRecordsErrorsCsvFile("link")
        .dataExportJobId(UUID.randomUUID())
        .build());
      executionContentRepository.save(BulkOperationExecutionContent.builder()
        .bulkOperationId(bulkOperationId)
        .identifier("123")
        .errorMessage(format("%s %s", "message", "link"))
        .errorType(ErrorType.ERROR)
        .build());
      executionContentRepository.save(BulkOperationExecutionContent.builder()
        .bulkOperationId(bulkOperationId)
        .identifier("456")
        .errorMessage(format("%s %s", "message", "link"))
        .errorType(ErrorType.ERROR)
        .build());
      executionContentRepository.save(BulkOperationExecutionContent.builder()
        .bulkOperationId(bulkOperationId)
        .identifier("789")
        .errorMessage(format("%s %s", "message", "link"))
        .errorType(ErrorType.WARNING)
        .build());

      assertEquals(2, errorService.getCommittedNumOfErrors(bulkOperationId));
      assertEquals(1, errorService.getCommittedNumOfWarnings(bulkOperationId));
    }
  }

  @Test
  void uploadErrorsToStorageShouldReturnNullWhenContentsIsEmpty() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      assertNull(errorService.uploadErrorsToStorage(bulkOperationId));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {MSG_NO_CHANGE_REQUIRED, MSG_NO_ADMINISTRATIVE_CHANGE_REQUIRED, MSG_NO_MARC_CHANGE_REQUIRED})
  void saveErrorShouldSaveNothingWhenSpecificErrorMsg(String errorMsg) {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      bulkOperationRepository.save(BulkOperation.builder()
        .id(bulkOperationId)
        .status(COMPLETED_WITH_ERRORS)
        .linkToCommittedRecordsErrorsCsvFile("link")
        .dataExportJobId(UUID.randomUUID())
        .build());
      executionContentRepository.save(BulkOperationExecutionContent.builder()
        .bulkOperationId(bulkOperationId)
        .identifier("123")
        .errorMessage(format("%s %s", "message", "link"))
        .errorType(ErrorType.ERROR)
        .build());
      errorService.saveError(bulkOperationId, "123", errorMsg, "ui msg", "link", ErrorType.ERROR);
      assertEquals(1, executionContentRepository.countByBulkOperationIdAndErrorType(bulkOperationId, ErrorType.ERROR));
    }
  }

  @Test
  void saveErrorShouldSaveBulkOperationExecutionContent() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var bulkOperationExecContent = BulkOperationExecutionContent.builder()
        .bulkOperationId(bulkOperationId)
        .identifier("123")
        .errorMessage(format("%s %s", "message", "link"))
        .errorType(ErrorType.ERROR)
        .build();
      bulkOperationRepository.save(BulkOperation.builder()
        .id(bulkOperationId)
        .status(COMPLETED_WITH_ERRORS)
        .linkToCommittedRecordsErrorsCsvFile("link")
        .dataExportJobId(UUID.randomUUID())
        .build());
      errorService.saveError(bulkOperationExecContent);
      assertEquals(1, executionContentRepository.countByBulkOperationIdAndErrorType(bulkOperationId, ErrorType.ERROR));
    }
  }

  @Test
  void shouldDeleteErrorByOperationId() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var bulkOperationExecContent = BulkOperationExecutionContent.builder()
        .bulkOperationId(bulkOperationId)
        .identifier("123")
        .errorMessage(format("%s %s", "message", "link"))
        .errorType(ErrorType.ERROR)
        .build();
      bulkOperationRepository.save(BulkOperation.builder()
        .id(bulkOperationId)
        .status(COMPLETED_WITH_ERRORS)
        .linkToCommittedRecordsErrorsCsvFile("link")
        .dataExportJobId(UUID.randomUUID())
        .build());
      executionContentRepository.save(bulkOperationExecContent);
      assertEquals(1, executionContentRepository.countByBulkOperationIdAndErrorType(bulkOperationId, ErrorType.ERROR));
      errorService.deleteErrorsByBulkOperationId(bulkOperationId);
      assertEquals(0, executionContentRepository.countByBulkOperationIdAndErrorType(bulkOperationId, ErrorType.ERROR));
    }
  }

  @Test
  void shouldReturnNothingForErrorsPreviewIfLinkToMatchedErrorsCsvIsNull() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      bulkOperationRepository.save(BulkOperation.builder()
        .id(bulkOperationId)
        .status(COMPLETED_WITH_ERRORS)
        .linkToCommittedRecordsErrorsCsvFile("link")
        .dataExportJobId(UUID.randomUUID())
        .build());

      var actualNumErrors = errorService.getErrorsPreviewByBulkOperationId(bulkOperationId, 10, 0, ErrorType.ERROR).getTotalRecords();
      assertEquals(0, actualNumErrors);
    }
  }

  @Test
  void shouldReturnNothingForErrorsPreviewForTypeErrorIfOnlyWarningsArePresent() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var bulkOperationExecContent = BulkOperationExecutionContent.builder()
        .bulkOperationId(bulkOperationId)
        .identifier("123")
        .errorMessage(format("%s %s", "message", "link"))
        .errorType(ErrorType.WARNING)
        .build();
      bulkOperationRepository.save(BulkOperation.builder()
        .id(bulkOperationId)
        .status(COMPLETED_WITH_ERRORS)
        .linkToCommittedRecordsErrorsCsvFile("link")
        .committedNumOfWarnings(1)
        .dataExportJobId(UUID.randomUUID())
        .build());
      executionContentRepository.save(bulkOperationExecContent);

      var actualNumErrors = errorService.getErrorsPreviewByBulkOperationId(bulkOperationId, 10, 0, ErrorType.ERROR).getTotalRecords();
      assertEquals(0, actualNumErrors);
    }
  }

  private void mockErrorsData(OperationStatusType statusType, UUID operationId) throws IOException {
    if (DATA_MODIFICATION == statusType || COMPLETED_WITH_ERRORS == statusType) {
      when(remoteFileSystemClient.get(any()))
        .thenReturn(Files.newInputStream(Paths.get("src/test/resources/files/errors.csv")));
      when(remoteFileSystemClient.getNumOfLines(any()))
        .thenReturn(2);
    } else {
      executionContentRepository.save(BulkOperationExecutionContent.builder()
        .bulkOperationId(operationId)
        .identifier("123")
        .errorMessage("No match found")
        .errorType(ErrorType.ERROR)
        .build());
      executionContentRepository.save(BulkOperationExecutionContent.builder()
        .bulkOperationId(operationId)
        .identifier("456")
        .errorMessage("Invalid format")
        .errorType(ErrorType.ERROR)
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
