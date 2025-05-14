package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEWED_NO_MARC_RECORDS;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;
import static org.folio.bulkops.util.Constants.COMMA_DELIMETER;
import static org.folio.bulkops.util.Constants.DATA_IMPORT_ERROR_DISCARDED;
import static org.folio.bulkops.util.Constants.ERROR_FILE_NAME_ENDING;
import static org.folio.bulkops.util.Constants.ERROR_MATCHING_FILE_NAME_PREFIX;
import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.MetadataProviderClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.JobLogEntry;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.dto.Error;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.Errors;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
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

  private static final int IDX_ERROR_IDENTIFIER = 1;
  private static final int IDX_ERROR_MSG = 2;
  private static final int IDX_ERROR_TYPE = 0;
  private final BulkOperationRepository operationRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final BulkOperationExecutionContentRepository executionContentRepository;
  private final MetadataProviderClient metadataProviderClient;

  public void saveError(UUID bulkOperationId, String identifier,  String errorMessage, String uiErrorMessage, String link, ErrorType errorType) {
    if (MSG_NO_CHANGE_REQUIRED.equals(errorMessage)
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
      var pathToMatchedRecordsErrorsCsvFile = bulkOperation.getLinkToMatchedRecordsErrorsCsvFile();
      if (StringUtils.isEmpty(pathToMatchedRecordsErrorsCsvFile)) {
        return new Errors()
          .errors(List.of())
          .totalRecords(0);
      }
      ArrayList<Error> errors = new ArrayList<>();
      AtomicInteger counter = new AtomicInteger();
      try (var reader = new BufferedReader(new InputStreamReader(remoteFileSystemClient.get(pathToMatchedRecordsErrorsCsvFile)))) {
        reader.lines().forEach(line -> {
          var message = line.split(Constants.COMMA_DELIMETER, 3);
          if (isNull(errorType) || Objects.equals(errorType.getValue(), message[IDX_ERROR_TYPE])) {
            counter.incrementAndGet();
            if (offset < counter.get() && errors.size() < limit) {
              errors.add(new Error().message(message[IDX_ERROR_MSG])
                .parameters(List.of(new Parameter().key(IDENTIFIER).value(message[IDX_ERROR_IDENTIFIER])))
                .type(ErrorType.fromValue(message[IDX_ERROR_TYPE])));
            }
          }
        });
      } catch (IOException e) {
        log.error("Error reading errors from file", e);
        throw new NotFoundException("Cannot process matching errors");
      }
      return new Errors().errors(errors).totalRecords(counter.get());
    } else if (COMPLETED == bulkOperation.getStatus() || COMPLETED_WITH_ERRORS == bulkOperation.getStatus()) {
      return getExecutionErrors(bulkOperationId, limit, offset, errorType);
    } else {
      throw new NotFoundException("Errors preview is not available");
    }
  }

  public void saveErrorsFromDataImport(List<JobLogEntry> logEntries, BulkOperation bulkOperation) {
    log.info("Saving errors from DataImport, total entries = {}", logEntries.size());
    logEntries.stream()
      .filter(entry -> nonNull(entry.getError()))
      .forEach(entry -> processDataImportErrorLogEntry(entry, bulkOperation));
  }

  private void processDataImportErrorLogEntry(JobLogEntry errorEntry, BulkOperation bulkOperation) {
    List<String> identifierList = null;
    var relatedInstanceInfo = errorEntry.getRelatedInstanceInfo();
    if (IdentifierType.ID.equals(bulkOperation.getIdentifierType())) {
      identifierList = relatedInstanceInfo.getIdList();
    } else if (IdentifierType.INSTANCE_HRID.equals(bulkOperation.getIdentifierType())) {
      identifierList = relatedInstanceInfo.getHridList();
    }
    var identifier = CollectionUtils.isEmpty(identifierList) ? null : identifierList.get(0);
    if (errorEntry.getSourceRecordActionStatus() == JobLogEntry.ActionStatus.DISCARDED && errorEntry.getError().isEmpty()) {
      errorEntry.setError(DATA_IMPORT_ERROR_DISCARDED);
    }
    if (!errorEntry.getError().isEmpty()) {
      saveError(bulkOperation.getId(), identifier, errorEntry.getError(), ErrorType.ERROR);
    }
  }

  private boolean noCommittedErrors(BulkOperation bulkOperation) {
    return bulkOperation.getCommittedNumOfErrors() == 0;
  }

  private boolean noCommittedWarnings(BulkOperation bulkOperation) {
    return bulkOperation.getCommittedNumOfWarnings() == 0;
  }

  public String getErrorsCsvByBulkOperationId(UUID bulkOperationId, int offset, ErrorType errorType) {
    return getErrorsPreviewByBulkOperationId(bulkOperationId, Integer.MAX_VALUE, offset, errorType).getErrors().stream()
      .map(error -> String.join(Constants.COMMA_DELIMETER, ObjectUtils.isEmpty(error.getParameters()) ? EMPTY : error.getType().getValue(), error.getParameters().get(0).getValue(), error.getMessage()))
      .collect(Collectors.joining(Constants.NEW_LINE_SEPARATOR));
  }

  private Errors getExecutionErrors(UUID bulkOperationId, int limit, int offset, ErrorType errorType) {
    int totalRecords = ofNullable(errorType)
      .map(errType -> (int) executionContentRepository.countByBulkOperationIdAndErrorType(bulkOperationId, errType))
      .orElseGet(() -> (int) executionContentRepository.countAllByBulkOperationIdAndErrorMessageIsNotNull(bulkOperationId));
    if (limit == 0) {
      return new Errors()
        .errors(List.of())
        .totalRecords(totalRecords);
    }
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
      .totalRecords(totalRecords);
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

  public String uploadErrorsToStorage(UUID bulkOperationId, String fileNamePrefix, String errorString) {
    String errors;
    if (errorString != null) {
      errors = errorString;
    } else {
      var contents = executionContentRepository.findByBulkOperationIdAndErrorMessageIsNotNullOrderByErrorType(
        bulkOperationId, OffsetRequest.of(0, Integer.MAX_VALUE));
      if (contents.isEmpty()) {
        return null;
      }
      errors = contents.stream()
        .map(content -> String.join(Constants.COMMA_DELIMETER,
          content.getErrorType().getValue(),
          content.getIdentifier(),
          content.getErrorMessage()))
        .collect(Collectors.joining(LF));
    }
    var errorsFileName = LocalDate.now() + operationRepository.findById(bulkOperationId)
      .map(BulkOperation::getLinkToTriggeringCsvFile)
      .map(FilenameUtils::getName)
      .map(fileName -> fileNamePrefix + fileName)
      .orElse(ERROR_FILE_NAME_ENDING);
    return remoteFileSystemClient.put(new ByteArrayInputStream(errors.getBytes()), bulkOperationId + "/" + errorsFileName);
  }

  public void saveErrorsAfterQuery(List<BulkOperationExecutionContent> bulkOperationExecutionContents, BulkOperation operation) {
    StringBuilder sb = new StringBuilder();
    bulkOperationExecutionContents.forEach(exCont -> {
      if (exCont.getErrorType() == ErrorType.ERROR) {
        operation.setMatchedNumOfErrors(operation.getMatchedNumOfErrors() + 1);
      }
      if (exCont.getErrorType() == ErrorType.WARNING) {
        operation.setMatchedNumOfWarnings(operation.getMatchedNumOfWarnings() + 1);
      }
      var errorLine = "%s%s%s%s%s%s".formatted(exCont.getErrorType(), COMMA_DELIMETER,
        StringUtils.strip(exCont.getIdentifier(), "\""), COMMA_DELIMETER, exCont.getErrorMessage(), System.lineSeparator());
      sb.append(errorLine);
    });
    if (!sb.isEmpty()) {
      var linkToMatchingErrorsFile = uploadErrorsToStorage(operation.getId(), ERROR_MATCHING_FILE_NAME_PREFIX, sb.toString());
      operation.setLinkToMatchedRecordsErrorsCsvFile(linkToMatchingErrorsFile);
    }
  }

  public int getCommittedNumOfErrors(UUID bulkOperationId) {
    return executionContentRepository.countAllByBulkOperationIdAndErrorMessageIsNotNullAndErrorTypeIs(bulkOperationId, ErrorType.ERROR);
  }

  public int getCommittedNumOfWarnings(UUID bulkOperationId) {
    return executionContentRepository.countAllByBulkOperationIdAndErrorMessageIsNotNullAndErrorTypeIs(bulkOperationId, ErrorType.WARNING);
  }

}
