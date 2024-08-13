package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;
import static org.folio.bulkops.util.Constants.COMMA_DELIMETER;
import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.Constants;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import lombok.RequiredArgsConstructor;

@Service
@Log4j2
@RequiredArgsConstructor
public class ErrorService {
  private static final String POSTFIX_ERROR_MESSAGE_NON_NULL = " AND errorMessage<null";
  public static final String IDENTIFIER = "IDENTIFIER";
  public static final String LINK = "LINK";
  private final BulkOperationRepository operationRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final BulkOperationExecutionContentRepository executionContentRepository;
  private final BulkEditClient bulkEditClient;

  public void saveError(UUID bulkOperationId, String identifier,  String errorMessage, String uiErrorMessage, String link) {
    executionContentRepository.save(BulkOperationExecutionContent.builder()
      .identifier(identifier)
      .bulkOperationId(bulkOperationId)
      .state(StateType.FAILED)
      .errorMessage(errorMessage)
      .uiErrorMessage(uiErrorMessage)
      .linkToFailedEntity(link)
      .build());
  }

  public void saveError(UUID bulkOperationId, String identifier,  String errorMessage) {
    saveError(bulkOperationId, identifier, errorMessage, null, null);
  }

  @Transactional
  public void deleteErrorsByBulkOperationId(UUID bulkOperationId) {
    executionContentRepository.deleteByBulkOperationId(bulkOperationId);
    log.info("Errors deleted for bulk operation {}", bulkOperationId);
  }

  public Errors getErrorsPreviewByBulkOperationId(UUID bulkOperationId, int limit) {
    var bulkOperation = operationRepository.findById(bulkOperationId)
      .orElseThrow(() -> new NotFoundException("BulkOperation was not found by id=" + bulkOperationId));
    if (Set.of(DATA_MODIFICATION, REVIEW_CHANGES).contains(bulkOperation.getStatus()) || COMPLETED_WITH_ERRORS == bulkOperation.getStatus() && noCommittedErrors(bulkOperation)) {
      var errors = bulkEditClient.getErrorsPreview(bulkOperation.getDataExportJobId(), limit);
      return new Errors().errors(errors.getErrors().stream()
          .map(this::prepareInternalErrorRepresentation)
          .toList())
        .totalRecords(errors.getTotalRecords());
    } else if (COMPLETED == bulkOperation.getStatus() || COMPLETED_WITH_ERRORS == bulkOperation.getStatus()) {
      return getExecutionErrors(bulkOperationId, limit);
    } else {
      throw new NotFoundException("Errors preview is not available");
    }
  }

  private boolean noCommittedErrors(BulkOperation bulkOperation) {
    return isNull(bulkOperation.getCommittedNumOfErrors()) || bulkOperation.getCommittedNumOfErrors() == 0;
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
      .map(this::executionContentToFolioError)
      .toList();
    return new Errors()
      .errors(errors)
      .totalRecords((int) errorPage.getTotalElements());
  }

  /**
   * Convert BulkOperationExecutionContent to Error for preview on UI
   * @param content {@link BulkOperationExecutionContent}
   * @return {@link Error} with message equals to specific UI representation (uiErrorMessage) if it is not blank,
   * otherwise - the same message as it is presented in csv file with errors (csvErrorMessage)
   */
  private Error executionContentToFolioError(BulkOperationExecutionContent content) {

    List<Parameter> parameters = new ArrayList<>();
    parameters.add(new Parameter().key(IDENTIFIER).value(content.getIdentifier()));
    if (StringUtils.isNotBlank(content.getLinkToFailedEntity())) {
      parameters.add(new Parameter().key(LINK).value(content.getLinkToFailedEntity()));
    }

    return new Error()
      .message(StringUtils.isNotBlank(content.getUiErrorMessage()) ? content.getUiErrorMessage() : content.getErrorMessage())
      .parameters(parameters);
  }

  public String uploadErrorsToStorage(BulkOperation bulkOperation) {
    var bulkOperationId = bulkOperation.getId();
    var contents = executionContentRepository.findByBulkOperationIdAndErrorMessageIsNotNull(bulkOperationId, OffsetRequest.of(0, Integer.MAX_VALUE));
    if (!contents.isEmpty()) {
      bulkOperation.setCommittedNumOfErrors((int) contents.getTotalElements());
      var errorsString = contents.stream()
        .collect(groupingBy(BulkOperationExecutionContent::getIdentifier,
          LinkedHashMap::new,
          mapping(BulkOperationExecutionContent::getErrorMessage, toCollection(LinkedHashSet::new))))
        .entrySet().stream()
        .map(es -> errorsToString(es.getKey(), es.getValue()))
        .collect(Collectors.joining(LF));
      var errorsFileName = LocalDate.now() + operationRepository.findById(bulkOperationId)
        .map(BulkOperation::getLinkToTriggeringCsvFile)
        .map(FilenameUtils::getName)
        .map(fileName -> "-Committing-changes-Errors-" + fileName)
        .orElse("-Errors.csv");
      return remoteFileSystemClient.put(new ByteArrayInputStream(errorsString.getBytes()), bulkOperationId + "/" + errorsFileName);
    }
    return null;
  }

  private String errorsToString(String identifier, Set<String> errorMessages) {
    return removeRedundantErrors(errorMessages).stream()
      .map(msg -> String.join(COMMA_DELIMETER, identifier, msg))
      .collect(Collectors.joining(LF));
  }

  private Set<String> removeRedundantErrors(Set<String> errors) {
    if (errors.contains(MSG_NO_CHANGE_REQUIRED) && errors.size() > 1) {
      errors.remove(MSG_NO_CHANGE_REQUIRED);
    }
    return errors;
  }
}
