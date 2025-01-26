package org.folio.bulkops.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.awaitility.Awaitility;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.configs.kafka.dto.Event;
import org.folio.spring.model.SystemUser;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.service.SystemUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

class DataImportJobCompletionReceiverServiceTest extends BaseTest {

  @Autowired
  private KafkaTemplate<String, Event> kafkaTemplate;

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
      var topic = "folio.Default.diku.DI_JOB_COMPLETED";
      var event = Files.readString(Path.of("src/test/resources/files/kafka/data_import_job_completed_message.json"));
      kafkaTemplate.send(topic, OBJECT_MAPPER.readValue(event, Event.class));
      var dataImportJobProfileIdCaptor = ArgumentCaptor.forClass(UUID.class);
      Awaitility.await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> verify(bulkOperationService, times(1))
        .processDataImportResult(dataImportJobProfileIdCaptor.capture()));
    }
  }
}
