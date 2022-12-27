package org.folio.bulkops.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static wiremock.org.hamcrest.MatcherAssert.assertThat;
import static wiremock.org.hamcrest.Matchers.equalTo;
import static wiremock.org.hamcrest.Matchers.hasSize;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.IntStream;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
  private JpaCqlRepository<BulkOperationExecutionContent, UUID> executionContentCqlRepository;

  @MockBean
  private RemoteFileSystemClient remoteFileSystemClient;

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
}
