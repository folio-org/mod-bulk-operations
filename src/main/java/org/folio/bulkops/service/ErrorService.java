package org.folio.bulkops.service;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.LF;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.dto.StateType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ErrorService {
  private static final String POSTFIX_ERROR_MESSAGE_NON_NULL = " AND errorMessage<>null";
  private final BulkOperationRepository operationRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final BulkOperationExecutionContentRepository executionContentRepository;
  private final JpaCqlRepository<BulkOperationExecutionContent, UUID> executionContentCqlRepository;

  public void saveError(UUID bulkOperationId, String identifier, String errorMessage) {
    executionContentRepository.save(BulkOperationExecutionContent.builder()
        .identifier(identifier)
        .bulkOperationId(bulkOperationId)
        .state(StateType.FAILED)
        .errorMessage(errorMessage)
      .build());
  }

  public Page<BulkOperationExecutionContent> getErrorsByCql(String cql, int offset, int limit) {
    return executionContentCqlRepository.findByCQL(cql.contains("errorMessage") ? cql : cql + POSTFIX_ERROR_MESSAGE_NON_NULL, OffsetRequest.of(offset, limit));
  }

  public String uploadErrorsToStorage(UUID bulkOperationId) {
    var contents = executionContentCqlRepository.findByCQL("bulkOperationId==" + bulkOperationId + POSTFIX_ERROR_MESSAGE_NON_NULL, OffsetRequest.of(0, Integer.MAX_VALUE));
    if (!contents.isEmpty()) {
      var errorsString = contents.stream()
        .map(content -> String.join(",", content.getIdentifier(), content.getErrorMessage()))
        .collect(Collectors.joining(LF));
      var errorsFileName = LocalDate.now().format(ISO_LOCAL_DATE) + operationRepository.findById(bulkOperationId)
        .map(BulkOperation::getLinkToOriginFile)
        .map(FilenameUtils::getName)
        .map(fileName -> "-Errors-" + fileName)
        .orElse("-Errors.csv");
      return remoteFileSystemClient.put(new ByteArrayInputStream(errorsString.getBytes()), bulkOperationId + "/" + errorsFileName);
    }
    return EMPTY;
  }
}
