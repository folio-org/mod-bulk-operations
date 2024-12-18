package org.folio.bulkops.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.folio.bulkops.BaseTest;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

class DataImportJobCompletionReceiverServiceTest extends BaseTest {

  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  private DataImportJobCompletionReceiverService receiverService;

  @MockBean
  private BulkOperationService bulkOperationService;

  @SpyBean
  private FolioExecutionContext folioExecutionContext;

  @Autowired
  private FolioModuleMetadata folioModuleMetadata;

  @Test
  @SneakyThrows
  void testReceiveJobExecutionUpdate() {
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getFolioModuleMetadata()).thenReturn(folioModuleMetadata);

    var topic = "folio.Default.diku.DI_JOB_COMPLETED";
    var msg = Files.readString(Path.of("src/test/resources/files/kafka/topic_payload.json"));

    kafkaTemplate.send(new ProducerRecord<>(topic, msg));

    var dataImportJobProfileIdCaptor = ArgumentCaptor.forClass(UUID.class);
    Awaitility.await().untilAsserted(() -> verify(bulkOperationService, times(1))
      .processDataImportResult(dataImportJobProfileIdCaptor.capture()));

  }
}
