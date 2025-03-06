package org.folio.bulkops.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.configs.kafka.dto.Event;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.spring.model.SystemUser;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.service.SystemUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class DataImportJobCompletionReceiverServiceTest extends BaseTest {

  @Autowired
  private DataImportJobCompletionReceiverService receiverService;

   @MockitoBean
  private BulkOperationService bulkOperationService;

   @MockitoBean
  private SystemUserService systemUserService;

   @MockitoBean
  private BulkOperationRepository bulkOperationRepository;

  @Test
  void testReceiveJobExecutionUpdate() throws IOException {
    var jobProfileId = UUID.fromString("e34d7b92-9b83-11eb-a8b3-0242ac130003");
    var bulkOperation = BulkOperation.builder().id(UUID.randomUUID()).build();
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(bulkOperationRepository.findByDataImportJobProfileId(jobProfileId))
        .thenReturn(Optional.of(bulkOperation));
      when(systemUserService.getAuthedSystemUser(any())).thenReturn(new SystemUser("mod-bulk-operation", "http://okapi:9130", "diku", null, ""));
      var message = Files.readString(Path.of("src/test/resources/files/kafka/data_import_job_completed_message.json"));
      var event = OBJECT_MAPPER.readValue(message, Event.class);
      var payload = OBJECT_MAPPER.readValue(event.getEventPayload(), org.folio.bulkops.domain.dto.DataImportJobExecution.class);
      receiverService.receiveJobExecutionUpdate(payload, Map.of());
      verify(bulkOperationService).processDataImportResult(bulkOperation);
    }
  }

  @Test
  void shouldIgnoreNonBulkOperationJobExecutionUpdate() throws IOException {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(bulkOperationRepository.findByDataImportJobProfileId(any(UUID.class))).thenReturn(Optional.empty());
      when(systemUserService.getAuthedSystemUser(any())).thenReturn(new SystemUser("mod-bulk-operation", "http://okapi:9130", "diku", null, ""));
      var message = Files.readString(Path.of("src/test/resources/files/kafka/data_import_job_completed_non_bulk_edit_message.json"));
      var event = OBJECT_MAPPER.readValue(message, Event.class);
      var payload = OBJECT_MAPPER.readValue(event.getEventPayload(), org.folio.bulkops.domain.dto.DataImportJobExecution.class);

      receiverService.receiveJobExecutionUpdate(payload, Map.of());

      verify(bulkOperationService, never()).processDataImportResult(any(BulkOperation.class));
    }
  }

  @Test
  @SneakyThrows
  void shouldHandleExceptionDuringMessageProcessing() {
    var jobProfileId = UUID.fromString("e34d7b92-9b83-11eb-a8b3-0242ac130003");
    var bulkOperation = BulkOperation.builder().id(UUID.randomUUID()).build();
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(bulkOperationRepository.findByDataImportJobProfileId(jobProfileId))
        .thenReturn(Optional.of(bulkOperation));
      when(systemUserService.getAuthedSystemUser(any())).thenReturn(new SystemUser("mod-bulk-operation", "http://okapi:9130", "diku", null, ""));
      doThrow(new RuntimeException("Processing exception")).when(bulkOperationService).processDataImportResult(bulkOperation);
      var message = Files.readString(Path.of("src/test/resources/files/kafka/data_import_job_completed_message.json"));
      var event = OBJECT_MAPPER.readValue(message, Event.class);
      var payload = OBJECT_MAPPER.readValue(event.getEventPayload(), org.folio.bulkops.domain.dto.DataImportJobExecution.class);

      assertDoesNotThrow(() -> receiverService.receiveJobExecutionUpdate(payload, Map.of()));
    }
  }
}
