package org.folio.bulkops.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.configs.kafka.KafkaService;
import org.folio.bulkops.domain.bean.Job;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class DataExportJobUpdateReceiverService {

  private final FolioModuleMetadata folioModuleMetadata;
  private final DataExportJobUpdateService dataExportJobUpdateService;

  @KafkaListener(
    id = KafkaService.EVENT_LISTENER_ID,
    containerFactory = "kafkaListenerContainerFactory",
    topicPattern = "${application.kafka.topic-pattern}",
    groupId = "${application.kafka.group-id}")
  public void receiveJobExecutionUpdate(@Payload Job jobExecutionUpdate, @Headers Map<String, Object> messageHeaders) {
    var defaultFolioExecutionContext = DefaultFolioExecutionContext.fromMessageHeaders(folioModuleMetadata, messageHeaders);

    try (var context = new FolioExecutionContextSetter(defaultFolioExecutionContext)) {
      dataExportJobUpdateService.handleReceivedJobExecutionUpdate(jobExecutionUpdate);
    }
  }
}
