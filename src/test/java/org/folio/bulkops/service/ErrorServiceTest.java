package org.folio.bulkops.service;

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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.DataExportWorkerClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.dto.Error;
import org.folio.bulkops.domain.dto.Errors;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
  private DataExportWorkerClient workerClient;

  private UUID bulkOperationId;

  @BeforeEach
  void saveBulkOperation() {
    bulkOperationId = bulkOperationRepository.save(BulkOperation.builder()
      .linkToOriginFile("some/path/records.csv")
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
    var operationId = bulkOperationRepository.save(BulkOperation.builder().dataExportJobId(UUID.randomUUID()).status(statusType).build()).getId();

    when(workerClient.getErrorsPreview(any(UUID.class), eq(2)))
      .thenReturn(new Errors()
        .errors(List.of(new Error(), new Error())));

    prepareErrors(operationId);

    var preview = errorService.getErrorsPreviewByBulkOperationId(operationId, 2);
    assertThat(preview.getErrors(), hasSize(2));

    bulkOperationRepository.deleteById(operationId);
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED" }, mode = EnumSource.Mode.EXCLUDE)
  void shouldRejectErrorsPreviewOnWrongOperationStatus(OperationStatusType statusType) {
    var operationId = bulkOperationRepository.save(BulkOperation.builder().status(statusType).build()).getId();

    assertThrows(NotFoundException.class, () -> errorService.getErrorsPreviewByBulkOperationId(operationId, 10));

    bulkOperationRepository.deleteById(operationId);
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED" }, mode = EnumSource.Mode.INCLUDE)
  void shouldGetErrorsCsvByBulkOperationId(OperationStatusType statusType) {
    var operationId = bulkOperationRepository.save(BulkOperation.builder().dataExportJobId(UUID.randomUUID()).status(statusType).build()).getId();

    when(workerClient.getErrorsPreview(any(UUID.class), anyInt()))
      .thenReturn(new Errors()
        .errors(List.of(new Error(), new Error(), new Error())));

    prepareErrors(operationId);

    var csvString = errorService.getErrorsCsvByBulkOperationId(operationId);
    assertThat(new BufferedReader(new StringReader(csvString)).lines().count(), equalTo(3L));

    bulkOperationRepository.deleteById(operationId);
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED" }, mode = EnumSource.Mode.EXCLUDE)
  void shouldRejectErrorsCsvOnWrongOperationStatus(OperationStatusType statusType) {
    var operationId = bulkOperationRepository.save(BulkOperation.builder().status(statusType).build()).getId();

    assertThrows(NotFoundException.class, () -> errorService.getErrorsCsvByBulkOperationId(operationId));

    bulkOperationRepository.deleteById(operationId);
  }

  private void prepareErrors(UUID operationId) {
    executionContentRepository.save(BulkOperationExecutionContent.builder()
      .bulkOperationId(operationId)
      .identifier("1")
      .errorMessage("Error")
      .build());
    executionContentRepository.save(BulkOperationExecutionContent.builder()
      .bulkOperationId(operationId)
      .identifier("2")
      .errorMessage("Error")
      .build());
    executionContentRepository.save(BulkOperationExecutionContent.builder()
      .bulkOperationId(operationId)
      .identifier("3")
      .errorMessage("Error")
      .build());

    processingContentRepository.save(BulkOperationProcessingContent.builder()
      .bulkOperationId(operationId)
      .identifier("1")
      .errorMessage("Error")
      .build());
    processingContentRepository.save(BulkOperationProcessingContent.builder()
      .bulkOperationId(operationId)
      .identifier("2")
      .errorMessage("Error")
      .build());
    processingContentRepository.save(BulkOperationProcessingContent.builder()
      .bulkOperationId(operationId)
      .identifier("3")
      .errorMessage("Error")
      .build());
  }
}
