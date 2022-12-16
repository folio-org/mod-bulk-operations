package org.folio.bulkops.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static wiremock.org.hamcrest.MatcherAssert.assertThat;
import static wiremock.org.hamcrest.Matchers.containsInAnyOrder;
import static wiremock.org.hamcrest.Matchers.equalTo;
import static wiremock.org.hamcrest.Matchers.hasSize;
import static org.folio.bulkops.domain.dto.ErrorParameterName.IDENTIFIER;

import lombok.SneakyThrows;
import org.folio.bulkops.domain.dto.Error;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationError;
import org.folio.bulkops.repository.BulkOperationErrorRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.repository.RemoteFileSystemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class ErrorServiceTest extends BaseTest {
  @Autowired
  private BulkOperationErrorRepository bulkOperationErrorRepository;

  @Autowired
  private ErrorService errorService;

  @Autowired
  private BulkOperationRepository bulkOperationRepository;

  @MockBean
  private RemoteFileSystemRepository remoteFileSystemRepository;

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
    var error = new Error()
      .message("Error message")
      .parameters(Collections.singletonList(new Parameter()
        .key(IDENTIFIER.getValue())
        .value("123456789")));

    errorService.saveError(bulkOperationId, error);

    var result = bulkOperationErrorRepository.findAllByBulkOperationId(bulkOperationId, PageRequest.of(0, Integer.MAX_VALUE)).toList();

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getError(), equalTo(error));
  }

  @Test
  void shouldGetErrorsByBulkOperationIdWithLimit() {
    IntStream.range(0, 10).forEach(this::saveError);

    var result = errorService.getErrors(bulkOperationId, 5);

    assertThat(result, hasSize(5));

    var identifiers = result.stream()
        .map(Error::getParameters)
        .flatMap(List::stream)
        .map(Parameter::getValue)
        .map(Integer::parseInt)
        .collect(Collectors.toList());

    assertThat(identifiers, containsInAnyOrder(0, 1, 2, 3, 4));
  }

  @Test
  @SneakyThrows
  void shouldUploadErrorsAndReturnLinkToFile() {
    bulkOperationErrorRepository.save(BulkOperationError.builder()
      .bulkOperationId(bulkOperationId)
      .error(new Error()
        .message("Error message 123")
        .parameters(Collections.singletonList(new Parameter()
          .key(IDENTIFIER.getValue())
          .value("123"))))
      .build());
    bulkOperationErrorRepository.save(BulkOperationError.builder()
      .bulkOperationId(bulkOperationId)
      .error(new Error()
        .message("Error message 456")
        .parameters(Collections.singletonList(new Parameter()
          .key(IDENTIFIER.getValue())
          .value("456"))))
      .build());

    var expectedFileName = bulkOperationId + "/" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "-Errors-records.csv";
    when(remoteFileSystemRepository.put(any(), eq(expectedFileName))).thenReturn(expectedFileName);

    var result = errorService.uploadErrorsToStorage(bulkOperationId);
    assertThat(result, equalTo(expectedFileName));

    var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(remoteFileSystemRepository).put(streamCaptor.capture(), eq(expectedFileName));
    assertThat("123,Error message 123\n456,Error message 456", equalTo(new String(streamCaptor.getValue().readAllBytes())));
  }

  private void saveError(int barcode) {
    bulkOperationErrorRepository.save(BulkOperationError.builder()
        .bulkOperationId(bulkOperationId)
        .error(new Error()
          .message("Error message")
          .parameters(Collections.singletonList(new Parameter()
            .key(IDENTIFIER.getValue())
            .value(Integer.toString(barcode)))))
      .build());
  }
}
