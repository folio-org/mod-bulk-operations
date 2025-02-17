package org.folio.bulkops.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.configs.kafka.dto.Event;
import org.folio.spring.model.SystemUser;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.service.SystemUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

class DataImportJobCompletionReceiverServiceTest extends BaseTest {

  @Autowired
  private DataImportJobCompletionReceiverService receiverService;

  @MockBean
  private BulkOperationService bulkOperationService;

  @MockBean
  private SystemUserService systemUserService;

  @Test
  void testReceiveJobExecutionUpdate() throws IOException {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(systemUserService.getAuthedSystemUser(any())).thenReturn(new SystemUser("mod-bulk-operation", "http://okapi:9130", "diku", null, ""));
      var message = Files.readString(Path.of("src/test/resources/files/kafka/data_import_job_completed_message.json"));
      var event = OBJECT_MAPPER.readValue(message, Event.class);
      var payload = OBJECT_MAPPER.readValue(event.getEventPayload(), org.folio.bulkops.domain.dto.DataImportJobExecution.class);
      receiverService.receiveJobExecutionUpdate(payload, Map.of());
      var dataImportJobProfileIdCaptor = ArgumentCaptor.forClass(UUID.class);
      verify(bulkOperationService, times(1)).processDataImportResult(dataImportJobProfileIdCaptor.capture());
    }
  }

  @Test
  void shouldIgnoreNonBulkOperationJobExecutionUpdate() throws IOException {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(systemUserService.getAuthedSystemUser(any())).thenReturn(new SystemUser("mod-bulk-operation", "http://okapi:9130", "diku", null, ""));
      var message = Files.readString(Path.of("src/test/resources/files/kafka/data_import_job_completed_non_bulk_edit_message.json"));
      var event = OBJECT_MAPPER.readValue(message, Event.class);
      var payload = OBJECT_MAPPER.readValue(event.getEventPayload(), org.folio.bulkops.domain.dto.DataImportJobExecution.class);
      receiverService.receiveJobExecutionUpdate(payload, Map.of());
      var dataImportJobProfileIdCaptor = ArgumentCaptor.forClass(UUID.class);
      verify(bulkOperationService, never()).processDataImportResult(dataImportJobProfileIdCaptor.capture());
    }
  }

  @Test
  @SneakyThrows
  void shouldHandleExceptionDuringMessageProcessing() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(systemUserService.getAuthedSystemUser(any())).thenReturn(new SystemUser("mod-bulk-operation", "http://okapi:9130", "diku", null, ""));
      doThrow(new RuntimeException("Processing exception")).when(bulkOperationService).processDataImportResult(any(UUID.class));
      var message = Files.readString(Path.of("src/test/resources/files/kafka/data_import_job_completed_message.json"));
      var event = OBJECT_MAPPER.readValue(message, Event.class);
      var payload = OBJECT_MAPPER.readValue(event.getEventPayload(), org.folio.bulkops.domain.dto.DataImportJobExecution.class);

      assertDoesNotThrow(() -> receiverService.receiveJobExecutionUpdate(payload, Map.of()));
    }
  }
}
