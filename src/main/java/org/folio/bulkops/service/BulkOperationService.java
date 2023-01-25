package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_RECORDS;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.adapters.ModClientAdapterFactory;
import org.folio.bulkops.client.BulkEditClient;
import org.folio.bulkops.client.DataExportSpringClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.ExportTypeSpecificParameters;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.bean.StatusType;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.bulkops.domain.bean.ExportType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.bean.Job;
import org.folio.bulkops.domain.bean.JobStatus;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationDataProcessing;
import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.domain.entity.BulkOperationProcessingContent;

import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.IllegalOperationStateException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.processor.DataProcessorFactory;
import org.folio.bulkops.processor.UpdateProcessorFactory;
import org.folio.bulkops.repository.BulkOperationDataProcessingRepository;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationExecutionRepository;
import org.folio.bulkops.repository.BulkOperationProcessingContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class BulkOperationService {
  public static final String PREVIEW_IS_NOT_AVAILABLE = "Preview is not available";
  @Value("${application.file-uploading.max-retry-count}")
  private int maxRetryCount;

  private static final String JSON_STRINGS_DELIMITER = ",\n";
  private final BulkOperationRepository bulkOperationRepository;
  private final DataExportSpringClient dataExportSpringClient;
  private final BulkEditClient bulkEditClient;
  private final RuleService ruleService;
  private final BulkOperationDataProcessingRepository dataProcessingRepository;
  private final BulkOperationExecutionRepository executionRepository;
  private final BulkOperationExecutionContentRepository executionContentRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final ObjectMapper objectMapper;
  private final DataProcessorFactory dataProcessorFactory;
  private final UpdateProcessorFactory updateProcessorFactory;
  private final ModClientAdapterFactory modClientAdapterFactory;
  private final BulkOperationProcessingContentRepository processingContentRepository;

  private final ErrorService errorService;

  public BulkOperation uploadIdentifiers(EntityType entityType, IdentifierType identifierType, MultipartFile multipartFile) {
    var bulkOperation = bulkOperationRepository.save(BulkOperation.builder()
        .entityType(entityType)
        .identifierType(identifierType)
        .status(NEW)
        .startTime(LocalDateTime.now())
      .build());

    String errorMessage = null;

    try {
      var linkToTriggeringFile = remoteFileSystemClient.put(multipartFile.getInputStream(), bulkOperation.getId() + "/" + multipartFile.getOriginalFilename());
      bulkOperation.setLinkToTriggeringFile(linkToTriggeringFile);
    } catch (Exception e) {
      errorMessage = String.format("File uploading failed, reason: %s", e.getMessage());
    }

    if (nonNull(errorMessage)) {
      log.error(errorMessage);
      bulkOperation = bulkOperation
        .withStatus(FAILED)
        .withErrorMessage(errorMessage)
        .withEndTime(LocalDateTime.now());
    }

    return bulkOperationRepository.save(bulkOperation);
  }

  public void confirm(UUID bulkOperationId) throws BulkOperationException {
    var bulkOperation = getBulkOperationOrThrow(bulkOperationId);

    if (isEmpty(bulkOperation.getLinkToOriginFile())) {
      throw new BulkOperationException("Missing link to origin file");
    }

    var entityClass = resolveEntityClass(bulkOperation.getEntityType());

    applyRules(bulkOperation, ruleService.getRules(bulkOperationId), entityClass);
  }

  @Async
  public void applyRules(BulkOperation bulkOperation, BulkOperationRuleCollection ruleCollection, Class<? extends BulkOperationsEntity> entityClass) {
    var dataProcessing = dataProcessingRepository.save(BulkOperationDataProcessing.builder()
      .bulkOperationId(bulkOperation.getId())
      .status(StatusType.ACTIVE)
      .startTime(LocalDateTime.now())
      .totalNumOfRecords(bulkOperation.getTotalNumOfRecords())
      .processedNumOfRecords(0)
      .build());
    try (var scanner = new Scanner(remoteFileSystemClient.get(bulkOperation.getLinkToOriginFile()))) {
      var modifiedFileName = bulkOperation.getId() + "/json/modified-" + FilenameUtils.getName(bulkOperation.getLinkToOriginFile());
      var modifiedCsvFileName = bulkOperation.getId() + "/modified-" + FilenameUtils.getName(bulkOperation.getLinkToOriginFile());
      boolean isModifiedFileEmpty = true;
      while (scanner.hasNext()) {
        var modifiedString = processUpdate(scanner.nextLine(), bulkOperation, ruleCollection, entityClass);

        if (StringUtils.isNotEmpty(modifiedString)) {
          isModifiedFileEmpty = false;
          remoteFileSystemClient.append(new ByteArrayInputStream(modifiedString.concat(scanner.hasNext() ? JSON_STRINGS_DELIMITER : EMPTY).getBytes()), modifiedFileName);
          bulkOperation.setLinkToModifiedFile(modifiedFileName);
        }

        dataProcessingRepository.save(dataProcessing
          .withProcessedNumOfRecords(dataProcessing.getProcessedNumOfRecords() + 1)
          .withStatus(scanner.hasNext() ? StatusType.ACTIVE : StatusType.COMPLETED)
          .withEndTime(scanner.hasNext() ? null : LocalDateTime.now()));
      }

      if (!isModifiedFileEmpty) {
        try (var is = new ByteArrayInputStream(getCsvPreviewFromUnifiedTable(buildPreview(modifiedFileName, entityClass, Integer.MAX_VALUE)).getBytes())) {
          remoteFileSystemClient.put(is, modifiedCsvFileName);
        }
        bulkOperation.setLinkToThePreviewFile(modifiedCsvFileName);
      }

      bulkOperationRepository.save(bulkOperation
        .withStatus(OperationStatusType.REVIEW_CHANGES));
    } catch (Exception e) {
      dataProcessingRepository.save(dataProcessing
        .withStatus(StatusType.FAILED)
        .withEndTime(LocalDateTime.now()));
      bulkOperationRepository.save(bulkOperation
        .withStatus(OperationStatusType.FAILED)
        .withEndTime(LocalDateTime.now())
        .withErrorMessage("Confirm changes operation failed, reason: " + e.getMessage()));
    }
  }

  private String processUpdate(String entityString, BulkOperation bulkOperation, BulkOperationRuleCollection rules, Class<? extends BulkOperationsEntity> entityClass) {
    var processor = dataProcessorFactory.getProcessorFromFactory(entityClass);
    var processingContent = BulkOperationProcessingContent.builder()
      .bulkOperationId(bulkOperation.getId())
      .build();
    try {
      var entity = objectMapper.readValue(entityString, entityClass);
      processingContent.setIdentifier(entity.getIdentifier(bulkOperation.getIdentifierType()));
      entity = processor.process(entity.getIdentifier(bulkOperation.getIdentifierType()), entity, rules);
      if (Objects.nonNull(entity)) {
        entityString = objectMapper.writeValueAsString(entity);
        processingContent.setState(StateType.PROCESSED);
      } else {
        return EMPTY;
      }
    } catch (Exception e) {
      processingContent = processingContent
        .withState(StateType.FAILED)
        .withErrorMessage("Failed to modify entity, reason:" + e.getMessage());
    }
    processingContentRepository.save(processingContent);
    return entityString;
  }

  public BulkOperation commit(UUID bulkOperationId) throws BulkOperationException {
    var bulkOperation = getBulkOperationOrThrow(bulkOperationId);

    if (isEmpty(bulkOperation.getLinkToOriginFile())) {
      throw new BulkOperationException("Missing link to origin file");
    }

    bulkOperation = bulkOperationRepository.save(bulkOperation.withStatus(OperationStatusType.APPLY_CHANGES));

    if (StringUtils.isNotEmpty(bulkOperation.getLinkToModifiedFile())) {
      var entityClass = resolveEntityClass(bulkOperation.getEntityType());
      var operationId = bulkOperation.getId();
      var execution = executionRepository.save(BulkOperationExecution.builder()
        .bulkOperationId(operationId)
        .startTime(LocalDateTime.now())
        .processedRecords(0)
        .status(StatusType.ACTIVE)
        .build());
      try (var origin = new Scanner(remoteFileSystemClient.get(bulkOperation.getLinkToOriginFile()));
           var modified = new Scanner(remoteFileSystemClient.get(bulkOperation.getLinkToModifiedFile()))) {
        var resultFileName = bulkOperation.getId() + "/json/result-" + FilenameUtils.getName(bulkOperation.getLinkToOriginFile());
        var resultCsvFileName = bulkOperation.getId() + "/result-" + FilenameUtils.getName(bulkOperation.getLinkToMatchingRecordsFile());
        while (origin.hasNext() && modified.hasNext()) {
          var resultString = updateEntityIfNeeded(origin.nextLine(), modified.nextLine(), bulkOperation, entityClass);
          remoteFileSystemClient.append(new ByteArrayInputStream(resultString.concat(origin.hasNext() ? JSON_STRINGS_DELIMITER : EMPTY).getBytes()), resultFileName);
          execution = execution
            .withStatus(origin.hasNext() ? StatusType.ACTIVE : StatusType.COMPLETED)
            .withProcessedRecords(execution.getProcessedRecords() + 1)
            .withEndTime(origin.hasNext() ? null : LocalDateTime.now());
        }

        try (var is = new ByteArrayInputStream(getCsvPreviewFromUnifiedTable(buildPreview(resultFileName, entityClass, Integer.MAX_VALUE)).getBytes())) {
          remoteFileSystemClient.put(is, resultCsvFileName);
        }

        bulkOperation = bulkOperation
          .withStatus(OperationStatusType.COMPLETED)
          .withEndTime(LocalDateTime.now())
          .withLinkToUpdatedRecordsFile(resultCsvFileName)
          .withLinkToResultFile(resultFileName);
      } catch (Exception e) {
        execution = execution
          .withStatus(StatusType.FAILED)
          .withEndTime(LocalDateTime.now());
        bulkOperation = bulkOperation
          .withStatus(OperationStatusType.FAILED)
          .withEndTime(LocalDateTime.now())
          .withErrorMessage(e.getMessage());
      }
      executionRepository.save(execution);
    } else {
      bulkOperation.setStatus(OperationStatusType.COMPLETED);
    }

    var linkToCommittingErrorsFile = errorService.uploadErrorsToStorage(bulkOperationId);
    bulkOperation.setLinkToCommittingErrorsFile(linkToCommittingErrorsFile);

    return bulkOperationRepository.save(bulkOperation);
  }


  private String updateEntityIfNeeded(String originalString, String modifiedString, BulkOperation bulkOperation, Class<? extends BulkOperationsEntity> entityClass) {
    if (!originalString.equals(modifiedString)) {
      var updater = updateProcessorFactory.getProcessorFromFactory(entityClass);
      var executionContent = BulkOperationExecutionContent.builder()
        .bulkOperationId(bulkOperation.getId())
        .build();
      try {
        var entity = objectMapper.readValue(modifiedString, entityClass);
        executionContent.setIdentifier(entity.getIdentifier(bulkOperation.getIdentifierType()));
        updater.updateRecord(entity);
        executionContentRepository.save(executionContent.withState(StateType.PROCESSED));
        return modifiedString;
      } catch (Exception e) {
        executionContentRepository.save(executionContent
          .withState(StateType.FAILED)
          .withErrorMessage("Failed to update entity, reason:" + e.getMessage()));
      }
    }
    return originalString;
  }

  public UnifiedTable getPreview(UUID bulkOperationId, int limit) {
    try {
      var bulkOperation = getBulkOperationOrThrow(bulkOperationId);
      var entityClass = resolveEntityClass(bulkOperation.getEntityType());
      switch (bulkOperation.getStatus()) {
      case DATA_MODIFICATION:
        if (isEmpty(bulkOperation.getLinkToOriginFile())) {
          throw new BulkOperationException(PREVIEW_IS_NOT_AVAILABLE);
        }
        return buildPreview(bulkOperation.getLinkToOriginFile(), entityClass, limit);
      case REVIEW_CHANGES:
        if (isEmpty(bulkOperation.getLinkToModifiedFile())) {
          throw new BulkOperationException(PREVIEW_IS_NOT_AVAILABLE);
        }
        return buildPreview(bulkOperation.getLinkToModifiedFile(), entityClass, limit);
      case COMPLETED:
        if (isEmpty(bulkOperation.getLinkToResultFile())) {
          throw new BulkOperationException(PREVIEW_IS_NOT_AVAILABLE);
        }
        return buildPreview(bulkOperation.getLinkToResultFile(), entityClass, limit);
      default:
        throw new BulkOperationException(PREVIEW_IS_NOT_AVAILABLE);
      }
    } catch (BulkOperationException e) {
      log.error(e.getMessage());
      throw new NotFoundException(e.getMessage());
    }
  }

  private UnifiedTable buildPreview(String pathToFile, Class<? extends BulkOperationsEntity> entityClass, int limit) throws BulkOperationException {
    var adapter = modClientAdapterFactory.getModClientAdapter(entityClass);
    try (var reader = new BufferedReader(new InputStreamReader(remoteFileSystemClient.get(pathToFile)))) {
      return adapter.getEmptyTableWithHeaders().rows(reader.lines()
        .limit(limit)
        .map(s -> stringToPojoOrNull(s, entityClass))
        .filter(Objects::nonNull)
        .map(adapter::convertEntityToUnifiedTableRow)
        .collect(Collectors.toList()));
    } catch (IOException e) {
      var msg = "Build preview failed, reason: " + e.getMessage();
      log.error(msg);
      throw new BulkOperationException(msg);
    }
  }

  public String getCsvPreviewFromUnifiedTable(UnifiedTable table) {
    return table.getHeader()
      .stream()
      .map(Cell::getValue)
      .collect(Collectors.joining(",")) + LF + table.getRows()
      .stream()
      .map(this::rowToCsvLine)
      .collect(Collectors.joining(LF));
  }

  public String getCsvPreviewByBulkOperationId(UUID bulkOperationId) {
    var table = getPreview(bulkOperationId, Integer.MAX_VALUE);
    return getCsvPreviewFromUnifiedTable(table);
  }

  public BulkOperation startBulkOperation(UUID bulkOperationId, ApproachType approachType) {
    var bulkOperation = bulkOperationRepository.findById(bulkOperationId)
      .orElseThrow(() -> new NotFoundException("Bulk operation was not found bu id=" + bulkOperationId));
    String errorMessage = null;
    try {
      if (NEW.equals(bulkOperation.getStatus())) {
        try {
          if (ApproachType.IN_APP == approachType) {
            var job = dataExportSpringClient.upsertJob(Job.builder()
              .type(ExportType.BULK_EDIT_IDENTIFIERS)
              .entityType(bulkOperation.getEntityType())
              .exportTypeSpecificParameters(new ExportTypeSpecificParameters())
              .identifierType(bulkOperation.getIdentifierType()).build());
            bulkOperation.setDataExportJobId(job.getId());
            bulkOperationRepository.save(bulkOperation);

            if (JobStatus.SCHEDULED.equals(job.getStatus())) {
              uploadIdentifiers(job.getId(), new FolioMultiPartFile(FilenameUtils.getName(bulkOperation.getLinkToTriggeringFile()), "application/json", remoteFileSystemClient.get(bulkOperation.getLinkToTriggeringFile())));
              job = dataExportSpringClient.getJob(job.getId());
              if (JobStatus.FAILED.equals(job.getStatus())) {
                errorMessage = "Data export job failed";
              } else {
                if (JobStatus.SCHEDULED.equals(job.getStatus())) {
                  bulkEditClient.startJob(job.getId());
                }
                bulkOperation.setStatus(RETRIEVING_RECORDS);
              }
            } else {
              errorMessage = String.format("File uploading failed - invalid job status: %s (expected: SCHEDULED)", job.getStatus().getValue());
            }
          } else {
            // MANUAL APPROACH
          }

        } catch (Exception e) {
          errorMessage = String.format("File uploading failed, reason: %s", e.getMessage());
        }

        if (nonNull(errorMessage)) {
          log.error(errorMessage);
          bulkOperation = bulkOperation
            .withStatus(FAILED)
            .withErrorMessage(errorMessage)
            .withEndTime(LocalDateTime.now());
        }
        bulkOperationRepository.save(bulkOperation);
        return bulkOperation;
      } else if (DATA_MODIFICATION.equals(bulkOperation.getStatus())) {
        confirm(bulkOperationId);
        return bulkOperation;
      } else if (REVIEW_CHANGES.equals(bulkOperation.getStatus())) {
        return commit(bulkOperationId);
      } else {
        throw new IllegalOperationStateException("Bulk operation cannot be started, reason: invalid state: " + bulkOperation.getStatus());
      }
    } catch (BulkOperationException e) {
      throw new IllegalOperationStateException("Bulk operation cannot be started, reason: " + e.getMessage());
    }
  }

  private String uploadIdentifiers(UUID dataExportJobId, MultipartFile file) throws BulkOperationException {
    var retryCount = 0;
    while (true) {
      try {
        return bulkEditClient.uploadFile(dataExportJobId, file);
      } catch (NotFoundException e) {
        if (++retryCount == maxRetryCount) {
          throw new BulkOperationException("Failed to upload file with identifiers: data export job was not found");
        }
      }
    }
  }

  public BulkOperation getOperationById(UUID bulkOperationId) {
    var operation = getBulkOperationOrThrow(bulkOperationId);
    if (DATA_MODIFICATION.equals(operation.getStatus())) {
      var processing = dataProcessingRepository.findByBulkOperationId(bulkOperationId);
      if (processing.isPresent() && StatusType.ACTIVE.equals(processing.get().getStatus())) {
        return operation.withProcessedNumOfRecords(processing.get().getProcessedNumOfRecords());
      }
    } else if (APPLY_CHANGES.equals(operation.getStatus())) {
      var execution = executionRepository.findByBulkOperationId(bulkOperationId);
      if (execution.isPresent() && StatusType.ACTIVE.equals(execution.get().getStatus())) {
        return operation.withProcessedNumOfRecords(execution.get().getProcessedRecords());
      }
    }
    return operation;
  }

  private <T> T stringToPojoOrNull(String string, Class<T> clazz) {
    try {
      return objectMapper.readValue(string, clazz);
    } catch (IOException e) {
      log.error("Failed to convert string to POJO: {}", e.getMessage());
      return null;
    }
  }

  private Class<? extends BulkOperationsEntity> resolveEntityClass(EntityType entityType) throws BulkOperationException {
    switch (entityType) {
    case USER:
      return User.class;
    case ITEM:
      return Item.class;
    case HOLDING:
      return HoldingsRecord.class;
    default:
      throw new BulkOperationException("Unsupported entity type: " + entityType);
    }
  }

  public BulkOperation getBulkOperationOrThrow(UUID bulkOperationId) {
    return bulkOperationRepository.findById(bulkOperationId)
      .orElseThrow(() -> new NotFoundException("BulkOperation was not found by id=" + bulkOperationId));
  }

  private String rowToCsvLine(Row row) {
    return row.getRow().stream()
      .map(this::prepareForCsv)
      .collect(Collectors.joining(","));
  }

  private String prepareForCsv(String s) {
    if (isNull(s)) {
      return "";
    }

    if (s.contains("\"")) {
      s = s.replace("\"", "\"\"");
    }

    if (s.contains(LF)) {
      s = s.replace(LF, "\\n");
    }

    if (s.contains(",")) {
      s = "\"" + s + "\"";
    }

    return s;
  }
}
