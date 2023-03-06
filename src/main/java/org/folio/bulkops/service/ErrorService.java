package org.folio.bulkops.service;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationProcessingContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.Constants;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ErrorService {
  private static final String POSTFIX_ERROR_MESSAGE_NON_NULL = " AND errorMessage<>null";
  public static final String IDENTIFIER = "IDENTIFIER";
  private final BulkOperationRepository operationRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final BulkOperationExecutionContentRepository executionContentRepository;
  private final JpaCqlRepository<BulkOperationExecutionContent, UUID> executionContentCqlRepository;
  private final BulkOperationProcessingContentRepository processingContentRepository;
  private final BulkEditClient bulkEditClient;

  public void saveError(UUID bulkOperationId, String identifier,  String errorMessage) {
    executionContentRepository.save(BulkOperationExecutionContent.builder()
        .identifier(identifier)
        .bulkOperationId(bulkOperationId)
        .state(StateType.FAILED)
        .errorMessage(errorMessage)
      .build());
  }

  @Transactional
  public void deleteErrorsByBulkOperationId(UUID bulkOperationId) {
    executionContentRepository.deleteByBulkOperationId(bulkOperationId);
  }

  public Errors getErrorsPreviewByBulkOperationId(UUID bulkOperationId, int limit) {
    var bulkOperation = operationRepository.findById(bulkOperationId)
      .orElseThrow(() -> new NotFoundException("BulkOperation was not found by id=" + bulkOperationId));
    if (DATA_MODIFICATION == bulkOperation.getStatus() || COMPLETED_WITH_ERRORS == bulkOperation.getStatus() && noCommittedErrors(bulkOperation)) {
      var errors = bulkEditClient.getErrorsPreview(bulkOperation.getDataExportJobId(), limit);
      return new Errors().errors(errors.getErrors().stream()
          .map(this::prepareInternalErrorRepresentation)
          .toList())
        .totalRecords(errors.getTotalRecords());
    } else if (REVIEW_CHANGES == bulkOperation.getStatus() || COMPLETED == bulkOperation.getStatus() || COMPLETED_WITH_ERRORS == bulkOperation.getStatus()) {
      return getExecutionErrors(bulkOperationId, limit);
    } else {
      throw new NotFoundException("Errors preview is not available");
    }
  }

  private boolean noCommittedErrors(BulkOperation bulkOperation) {
    return Objects.isNull(bulkOperation.getCommittedNumOfErrors()) || bulkOperation.getCommittedNumOfErrors() == 0;
  }

  private Error prepareInternalErrorRepresentation(Error e) {
    var error= e.getMessage().split(Constants.COMMA_DELIMETER);
    return new Error().message(error[1]).parameters(List.of(new Parameter().key(IDENTIFIER).value(error[0])));
  }

  public String getErrorsCsvByBulkOperationId(UUID bulkOperationId) {
    return getErrorsPreviewByBulkOperationId(bulkOperationId, Integer.MAX_VALUE).getErrors().stream()
      .map(error -> String.join(Constants.COMMA_DELIMETER, ObjectUtils.isEmpty(error.getParameters()) ? EMPTY : error.getParameters().get(0).getValue(), error.getMessage()))
      .collect(Collectors.joining(Constants.NEW_LINE_SEPARATOR));
  }

  private Errors getExecutionErrors(UUID bulkOperationId, int limit) {
    var errorPage = executionContentRepository.findByBulkOperationIdAndErrorMessageIsNotNull(bulkOperationId, OffsetRequest.of(0, limit));
    var errors = errorPage.toList().stream()
      .map(this::executionContentToError)
      .toList();
    return new Errors()
      .errors(errors)
      .totalRecords((int) errorPage.getTotalElements());
  }

  private Error executionContentToError(BulkOperationExecutionContent content) {
    return new Error()
      .message(content.getErrorMessage())
      .parameters(Collections.singletonList(new Parameter()
        .key(IDENTIFIER)
        .value(content.getIdentifier())));
  }

  public Page<BulkOperationExecutionContent> getErrorsByCql(String cql, int offset, int limit) {
    return executionContentCqlRepository.findByCQL(cql.contains("errorMessage") ? cql : cql + POSTFIX_ERROR_MESSAGE_NON_NULL, OffsetRequest.of(offset, limit));
  }

  public String uploadErrorsToStorage(UUID bulkOperationId) {
    var contents = executionContentCqlRepository.findByCQL("bulkOperationId==" + bulkOperationId + POSTFIX_ERROR_MESSAGE_NON_NULL, OffsetRequest.of(0, Integer.MAX_VALUE));
    if (!contents.isEmpty()) {
      var errorsString = contents.stream()
        .map(content -> String.join(Constants.COMMA_DELIMETER, content.getIdentifier(), content.getErrorMessage()))
        .collect(Collectors.joining(LF));
      var errorsFileName = LocalDate.now().format(ISO_LOCAL_DATE) + operationRepository.findById(bulkOperationId)
        .map(BulkOperation::getLinkToMatchedRecordsCsvFile)
        .map(FilenameUtils::getName)
        .map(fileName -> "-Errors-" + fileName)
        .orElse("-Errors.csv");
      return remoteFileSystemClient.put(new ByteArrayInputStream(errorsString.getBytes()), bulkOperationId + "/" + errorsFileName);
    }
    return EMPTY;
  }
}
