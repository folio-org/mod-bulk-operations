package org.folio.bulkops.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.BulkEditClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.dto.Error;
import org.folio.bulkops.domain.dto.Errors;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.domain.entity.BulkOperationProcessingContent;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationProcessingContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import lombok.SneakyThrows;

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

  private UUID bulkOperationId;

  @BeforeEach
  void saveBulkOperation() {
    bulkOperationId = bulkOperationRepository.save(BulkOperation.builder()
      .id(UUID.randomUUID())
      .linkToMatchedRecordsCsvFile("some/path/records.csv")
      .build()).getId();
  }

  @AfterEach
  void clearTestData() {
    bulkOperationRepository.deleteById(bulkOperationId);
  }

  @Test
  void shouldSaveError() {
    errorService.saveError(bulkOperationId, "123", "Error message");

    var result = executionContentCqlRepository.findByCQL("bulkOperationId==" + bulkOperationId, OffsetRequest.of(0, 10));

    assertThat(result.toList(), hasSize(1));
    var content = result.iterator().next();
    assertThat(content.getIdentifier(), equalTo("123"));
    assertThat(content.getErrorMessage(), equalTo("Error message"));
  }

  @Test
  void shouldGetErrorsByCqlQuery() {
    IntStream.range(0, 10).forEach(i -> errorService.saveError(bulkOperationId, "123", i % 2 == 0 ? null : "Error message"));

    var result = errorService.getErrorsByCql("bulkOperationId==" + bulkOperationId, 0, 3);
    assertThat(result.getTotalElements(), equalTo(5L));
    assertThat(result.getSize(), equalTo(3));
    assertTrue(result.get().allMatch(content -> "Error message".equals(content.getErrorMessage())));
  }

  @Test
  @SneakyThrows
  void shouldUploadErrorsAndReturnLinkToFile() {
    errorService.saveError(bulkOperationId, "123", "Error message 123");
    errorService.saveError(bulkOperationId, "456", "Error message 456");

    var expectedFileName = bulkOperationId + "/" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "-Errors-records.csv";
    when(remoteFileSystemClient.put(any(), eq(expectedFileName))).thenReturn(expectedFileName);

    var result = errorService.uploadErrorsToStorage(bulkOperationId);
    assertThat(result, equalTo(expectedFileName));

    var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(remoteFileSystemClient).put(streamCaptor.capture(), eq(expectedFileName));
    assertThat("123,Error message 123\n456,Error message 456", equalTo(new String(streamCaptor.getValue().readAllBytes())));
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED" }, mode = EnumSource.Mode.INCLUDE)
  void shouldGetErrorsPreviewByBulkOperationId(OperationStatusType statusType) {
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

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED_WITH_ERRORS", "COMPLETED" }, mode = EnumSource.Mode.EXCLUDE)
  void shouldRejectErrorsPreviewOnWrongOperationStatus(OperationStatusType statusType) {
    var operationId = bulkOperationRepository.save(BulkOperation.builder().id(UUID.randomUUID()).status(statusType).build()).getId();

    assertThrows(NotFoundException.class, () -> errorService.getErrorsPreviewByBulkOperationId(operationId, 10));

    bulkOperationRepository.deleteById(operationId);
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED" }, mode = EnumSource.Mode.INCLUDE)
  void shouldGetErrorsCsvByBulkOperationId(OperationStatusType statusType) {
    var operationId = bulkOperationRepository.save(BulkOperation.builder().id(UUID.randomUUID()).dataExportJobId(UUID.randomUUID()).status(statusType).build()).getId();

    var expected = "123,No match found\n456,Invalid format".split(LF);

    mockErrorsData(statusType, operationId);

    var actual = errorService.getErrorsCsvByBulkOperationId(operationId).split(LF);
    Arrays.sort(expected); Arrays.sort(actual);

    assertArrayEquals(expected, actual);
    assertThat(actual.length, equalTo(2));

    bulkOperationRepository.deleteById(operationId);
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED", "COMPLETED_WITH_ERRORS" }, mode = EnumSource.Mode.EXCLUDE)
  void shouldRejectErrorsCsvOnWrongOperationStatus(OperationStatusType statusType) {
    var operationId = bulkOperationRepository.save(BulkOperation.builder().id(UUID.randomUUID()).status(statusType).build()).getId();

    assertThrows(NotFoundException.class, () -> errorService.getErrorsCsvByBulkOperationId(operationId));

    bulkOperationRepository.deleteById(operationId);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(ints = {0, 1})
  void shouldReturnErrorsPreviewOnCompletedWithErrors(Integer committedErrors) {
    var jobId = UUID.randomUUID();

    var operationId = bulkOperationRepository.save(BulkOperation.builder()
        .id(UUID.randomUUID())
        .status(COMPLETED_WITH_ERRORS)
        .committedNumOfErrors(committedErrors)
        .dataExportJobId(jobId)
        .build())
      .getId();

    mockErrorsData(COMPLETED_WITH_ERRORS, operationId);

    if (nonNull(committedErrors) && committedErrors == 1) {
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
