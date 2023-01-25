package org.folio.bulkops.service;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.LF;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.folio.bulkops.client.BulkEditClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.dto.Error;
import org.folio.bulkops.domain.dto.Errors;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.domain.entity.BulkOperationProcessingContent;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationProcessingContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.util.ObjectUtils;

@Service
@RequiredArgsConstructor
public class ErrorService {
  private static final String POSTFIX_ERROR_MESSAGE_NON_NULL = " AND errorMessage<>null";
  private final BulkOperationRepository operationRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final BulkOperationExecutionContentRepository executionContentRepository;
  private final JpaCqlRepository<BulkOperationExecutionContent, UUID> executionContentCqlRepository;
  private final BulkOperationProcessingContentRepository processingContentRepository;
  private final BulkEditClient bulkEditClient;

  public void saveError(UUID bulkOperationId, String identifier, String errorMessage) {
    executionContentRepository.save(BulkOperationExecutionContent.builder()
        .identifier(identifier)
        .bulkOperationId(bulkOperationId)
        .state(StateType.FAILED)
        .errorMessage(errorMessage)
      .build());
  }

  public Errors getErrorsPreviewByBulkOperationId(UUID bulkOperationId, int limit) {
    var bulkOperation = operationRepository.findById(bulkOperationId)
      .orElseThrow(() -> new NotFoundException("BulkOperation was not found by id=" + bulkOperationId));
    switch (bulkOperation.getStatus()) {
    case DATA_MODIFICATION:
      return bulkEditClient.getErrorsPreview(bulkOperation.getDataExportJobId(), limit);
    case REVIEW_CHANGES:
      return getProcessingErrors(bulkOperationId, limit);
    case COMPLETED:
      return getExecutionErrors(bulkOperationId, limit);
    default:
      throw new NotFoundException("Errors preview is not available");
    }
  }

  public String getErrorsCsvByBulkOperationId(UUID bulkOperationId) {
    return getErrorsPreviewByBulkOperationId(bulkOperationId, Integer.MAX_VALUE).getErrors().stream()
      .map(error -> String.join(",", ObjectUtils.isEmpty(error.getParameters()) ? EMPTY : error.getParameters().get(0).getValue(), error.getMessage()))
      .collect(Collectors.joining("\n"));
  }

  private Errors getProcessingErrors(UUID bulkOperationId, int limit) {
    var errorPage = processingContentRepository.findByBulkOperationIdAndErrorMessageIsNotNull(bulkOperationId, OffsetRequest.of(0, limit));
    var errors = errorPage.toList().stream()
      .map(this::processingContentToError)
      .collect(Collectors.toList());
    return new Errors()
      .errors(errors)
      .totalRecords((int) errorPage.getTotalElements());
  }

  private Error processingContentToError(BulkOperationProcessingContent content) {
    return new Error()
      .message(content.getErrorMessage())
      .parameters(Collections.singletonList(new Parameter()
        .key("IDENTIFIER")
        .value(content.getIdentifier())));
  }

  private Errors getExecutionErrors(UUID bulkOperationId, int limit) {
    var errorPage = executionContentRepository.findByBulkOperationIdAndErrorMessageIsNotNull(bulkOperationId, OffsetRequest.of(0, limit));
    var errors = errorPage.toList().stream()
      .map(this::executionContentToError)
      .collect(Collectors.toList());
    return new Errors()
      .errors(errors)
      .totalRecords((int) errorPage.getTotalElements());
  }

  private Error executionContentToError(BulkOperationExecutionContent content) {
    return new Error()
      .message(content.getErrorMessage())
      .parameters(Collections.singletonList(new Parameter()
        .key("IDENTIFIER")
        .value(content.getIdentifier())));
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
        .map(BulkOperation::getLinkToMatchingRecordsFile)
        .map(FilenameUtils::getName)
        .map(fileName -> "-Errors-" + fileName)
        .orElse("-Errors.csv");
      return remoteFileSystemClient.put(new ByteArrayInputStream(errorsString.getBytes()), bulkOperationId + "/" + errorsFileName);
    }
    return EMPTY;
  }
}
