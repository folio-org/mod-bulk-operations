package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEWED_NO_MARC_RECORDS;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;
import static org.folio.bulkops.util.Constants.DATA_IMPORT_ERROR_DISCARDED;
import static org.folio.bulkops.util.Constants.MSG_NO_ADMINISTRATIVE_CHANGE_REQUIRED;
import static org.folio.bulkops.util.Constants.MSG_NO_MARC_CHANGE_REQUIRED;
import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.BulkEditClient;
import org.folio.bulkops.client.MetadataProviderClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.JobLogEntry;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.dto.Error;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.Errors;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.DataImportException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.Constants;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import lombok.RequiredArgsConstructor;

@Service
@Log4j2
@RequiredArgsConstructor
public class ErrorService {
  public static final String IDENTIFIER = "IDENTIFIER";
  public static final String LINK = "LINK";
  private final BulkOperationRepository operationRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final BulkOperationExecutionContentRepository executionContentRepository;
  private final BulkEditClient bulkEditClient;
  private final MetadataProviderClient metadataProviderClient;

  public void saveError(UUID bulkOperationId, String identifier,  String errorMessage, String uiErrorMessage, String link, ErrorType errorType) {
    if (Set.of(MSG_NO_CHANGE_REQUIRED, MSG_NO_ADMINISTRATIVE_CHANGE_REQUIRED, MSG_NO_MARC_CHANGE_REQUIRED).contains(errorMessage)
      && executionContentRepository.findFirstByBulkOperationIdAndIdentifier(bulkOperationId, identifier).isPresent()) {
      return;
    }
    executionContentRepository.save(BulkOperationExecutionContent.builder()
      .identifier(identifier)
      .bulkOperationId(bulkOperationId)
      .state(StateType.FAILED)
      .errorMessage(errorMessage)
      .uiErrorMessage(uiErrorMessage)
      .errorType(errorType)
      .linkToFailedEntity(link)
      .build());
  }

  public void saveError(BulkOperationExecutionContent bulkOperationExecutionContent) {
    executionContentRepository.save(bulkOperationExecutionContent);
  }

  public void saveError(UUID bulkOperationId, String identifier,  String errorMessage, ErrorType errorType) {
    saveError(bulkOperationId, identifier, errorMessage, null, null, errorType);
  }

  @Transactional
  public void deleteErrorsByBulkOperationId(UUID bulkOperationId) {
    executionContentRepository.deleteByBulkOperationId(bulkOperationId);
    log.info("Errors deleted for bulk operation {}", bulkOperationId);
  }

  public Errors getErrorsPreviewByBulkOperationId(UUID bulkOperationId, int limit, int offset, ErrorType errorType) {
    var bulkOperation = operationRepository.findById(bulkOperationId)
      .orElseThrow(() -> new NotFoundException("BulkOperation was not found by id=" + bulkOperationId));
    if (Set.of(DATA_MODIFICATION, REVIEW_CHANGES, REVIEWED_NO_MARC_RECORDS).contains(bulkOperation.getStatus()) || COMPLETED_WITH_ERRORS == bulkOperation.getStatus() && noCommittedErrors(bulkOperation) && noCommittedWarnings(bulkOperation)) {
      var errors = bulkEditClient.getErrorsPreview(bulkOperation.getDataExportJobId(), limit);
      return new Errors().errors(errors.getErrors().stream()
          .map(this::prepareInternalErrorRepresentation)
          .toList())
        .totalRecords(errors.getTotalRecords());
    } else if (COMPLETED == bulkOperation.getStatus() || COMPLETED_WITH_ERRORS == bulkOperation.getStatus()) {
      return getExecutionErrors(bulkOperationId, limit, offset, errorType);
    } else {
      throw new NotFoundException("Errors preview is not available");
    }
  }

  public void saveErrorsFromDataImport(UUID bulkOperationId, UUID dataImportJobId) {
    log.info("Saving errors from DataImport, bulkOperationId = {}, dataImportJobId = {}", bulkOperationId, dataImportJobId);
    var bulkOperation = operationRepository.findById(bulkOperationId)
      .orElseThrow(() -> new NotFoundException("BulkOperation was not found by id=" + bulkOperationId));
    var identifierType = bulkOperation.getIdentifierType();
    try {
      var jobLogEntries = metadataProviderClient.getJobLogEntries(dataImportJobId.toString(), Integer.MAX_VALUE)
        .getEntries().stream()
        .filter(entry -> nonNull(entry.getError()))
        .toList();
      jobLogEntries.forEach(errorEntry -> {
        List<String> identifierList = null;
        var relatedInstanceInfo = errorEntry.getRelatedInstanceInfo();
        if (identifierType == IdentifierType.ID) {
          identifierList = relatedInstanceInfo.getIdList();
        } else if (identifierType == IdentifierType.INSTANCE_HRID) {
          identifierList = relatedInstanceInfo.getHridList();
        }
        var identifier = CollectionUtils.isEmpty(identifierList) ? null : identifierList.get(0);
        if (errorEntry.getSourceRecordActionStatus() == JobLogEntry.ActionStatus.DISCARDED && errorEntry.getError().isEmpty()) {
          errorEntry.setError(DATA_IMPORT_ERROR_DISCARDED);
        }
        if (!errorEntry.getError().isEmpty()) {
          saveError(bulkOperationId, identifier, errorEntry.getError(), ErrorType.ERROR);
        }
      });
    } catch (Exception e) {
      log.error("Problem with retrieving logs from MetadataProvider", e);
      throw new DataImportException(e);
    }
  }

  private boolean noCommittedErrors(BulkOperation bulkOperation) {
    return isNull(bulkOperation.getCommittedNumOfErrors()) || bulkOperation.getCommittedNumOfErrors() == 0;
  }

  private boolean noCommittedWarnings(BulkOperation bulkOperation) {
    return isNull(bulkOperation.getCommittedNumOfWarnings()) || bulkOperation.getCommittedNumOfWarnings() == 0;
  }

  private Error prepareInternalErrorRepresentation(Error e) {
    var error= e.getMessage().split(Constants.COMMA_DELIMETER);
    return new Error().message(error[1]).parameters(List.of(new Parameter().key(IDENTIFIER).value(error[0])));
  }

  public String getErrorsCsvByBulkOperationId(UUID bulkOperationId, int offset, ErrorType errorType) {
    return getErrorsPreviewByBulkOperationId(bulkOperationId, Integer.MAX_VALUE, offset, errorType).getErrors().stream()
      .map(error -> String.join(Constants.COMMA_DELIMETER, ObjectUtils.isEmpty(error.getParameters()) ? EMPTY : error.getParameters().get(0).getValue(), error.getMessage()))
      .collect(Collectors.joining(Constants.NEW_LINE_SEPARATOR));
  }

  private Errors getExecutionErrors(UUID bulkOperationId, int limit, int offset, ErrorType errorType) {
    Page<BulkOperationExecutionContent> errorPage;
    if (isNull(errorType)) {
      errorPage = executionContentRepository.findByBulkOperationIdAndErrorMessageIsNotNullOrderByErrorType(bulkOperationId, OffsetRequest.of(offset, limit));
    } else {
      errorPage = executionContentRepository.findByBulkOperationIdAndErrorMessageIsNotNullAndErrorTypeIsOrderByErrorType(bulkOperationId, OffsetRequest.of(offset, limit), errorType);
    }
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
      .parameters(parameters)
      .type(content.getErrorType());
  }

  public String uploadErrorsToStorage(UUID bulkOperationId) {
    var contents = executionContentRepository.findByBulkOperationIdAndErrorMessageIsNotNullOrderByErrorType(bulkOperationId, OffsetRequest.of(0, Integer.MAX_VALUE));
    if (!contents.isEmpty()) {
      var errorsString = contents.stream()
        .map(content -> String.join(Constants.COMMA_DELIMETER, content.getIdentifier(), content.getErrorMessage(), content.getErrorType().getValue()))
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

  public int getCommittedNumOfErrors(UUID bulkOperationId) {
    return executionContentRepository.countAllByBulkOperationIdAndErrorMessageIsNotNullAndErrorTypeIs(bulkOperationId, ErrorType.ERROR);
  }

  public int getCommittedNumOfWarnings(UUID bulkOperationId) {
    return executionContentRepository.countAllByBulkOperationIdAndErrorMessageIsNotNullAndErrorTypeIs(bulkOperationId, ErrorType.WARNING);
  }

}
