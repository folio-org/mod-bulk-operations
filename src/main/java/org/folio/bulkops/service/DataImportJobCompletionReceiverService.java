package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.KafkaEventDI;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class DataImportJobCompletionReceiverService {
  private final BulkOperationService bulkOperationService;
  private final FolioModuleMetadata folioModuleMetadata;

  @KafkaListener(
    containerFactory = "kafkaListenerContainerFactoryDI",
    topicPattern = "${application.kafka.topic-pattern-di}",
    groupId = "${application.kafka.group-id}")
  public void receiveJobExecutionUpdate(@Payload KafkaEventDI event, @Headers Map<String, Object> messageHeaders) {
    var defaultFolioExecutionContext = DefaultFolioExecutionContext.fromMessageHeaders(folioModuleMetadata, messageHeaders);
    try (var context = new FolioExecutionContextSetter(defaultFolioExecutionContext)) {
      log.info("Received event from DI: {}.", event);
      var importProfileId = event.getEventPayload().getJobProfileInfo().getId();
      bulkOperationService.processDataImportResult(importProfileId);
    }
  }
}
