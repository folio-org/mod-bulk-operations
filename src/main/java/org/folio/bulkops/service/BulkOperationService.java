package org.folio.bulkops.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_RECORDS;

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
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.bean.StatusType;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.bulkops.domain.bean.ExportType;
import org.folio.bulkops.domain.bean.IdentifierType;
import org.folio.bulkops.domain.bean.Job;
import org.folio.bulkops.domain.bean.JobStatus;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationDataProcessing;
import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.processor.DataProcessorFactory;
import org.folio.bulkops.processor.UpdateProcessorFactory;
import org.folio.bulkops.repository.BulkOperationDataProcessingRepository;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationExecutionRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
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

  public BulkOperation uploadIdentifiers(EntityType entityType, IdentifierType identifierType, MultipartFile multipartFile) {
    var bulkOperation = bulkOperationRepository.save(BulkOperation.builder()
        .entityType(entityType)
        .identifierType(identifierType)
        .status(NEW)
        .startTime(LocalDateTime.now())
      .build());

    String errorMessage = null;
    try {
      var job = dataExportSpringClient.upsertJob(Job.builder()
        .type(ExportType.BULK_EDIT_IDENTIFIERS)
        .entityType(entityType)
        .identifierType(identifierType).build());
      bulkOperation.setDataExportJobId(job.getId());
      if (JobStatus.SCHEDULED.equals(job.getStatus())) {
        bulkEditClient.uploadFile(job.getId(), multipartFile);
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

  public void confirmChanges(UUID bulkOperationId) throws BulkOperationException {
    var bulkOperation = getBulkOperationById(bulkOperationId);

    if (StringUtils.isEmpty(bulkOperation.getLinkToOriginFile())) {
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
      var processor = dataProcessorFactory.getProcessorFromFactory(entityClass);
      var modifiedFileName = bulkOperation.getId() + "/modified-" + FilenameUtils.getName(bulkOperation.getLinkToOriginFile());
      while (scanner.hasNext()) {
        var entity = objectMapper.readValue(scanner.nextLine(), entityClass);
        var updatedEntity = processor.process(entity.getIdentifier(bulkOperation.getIdentifierType()), entity, ruleCollection);
        remoteFileSystemClient.append(new ByteArrayInputStream(objectMapper.writeValueAsString(updatedEntity).concat(scanner.hasNext() ? JSON_STRINGS_DELIMITER : EMPTY).getBytes()), modifiedFileName);
        dataProcessingRepository.save(dataProcessing
          .withProcessedNumOfRecords(dataProcessing.getProcessedNumOfRecords() + 1)
          .withStatus(scanner.hasNext() ? StatusType.ACTIVE : StatusType.COMPLETED)
          .withEndTime(scanner.hasNext() ? null : LocalDateTime.now()));
      }
      bulkOperationRepository.save(bulkOperation
        .withLinkToModifiedFile(modifiedFileName)
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

  public void commitChanges(UUID bulkOperationId) throws BulkOperationException {
    var bulkOperation = getBulkOperationById(bulkOperationId);

    if (StringUtils.isEmpty(bulkOperation.getLinkToOriginFile())) {
      throw new BulkOperationException("Missing link to origin file");
    } else if (StringUtils.isEmpty(bulkOperation.getLinkToModifiedFile())) {
      throw new BulkOperationException("Missing link to modified file");
    }

    var entityClass = resolveEntityClass(bulkOperation.getEntityType());

    performCommitChanges(bulkOperation, entityClass);
  }

  @Async
  public void performCommitChanges(BulkOperation bulkOperation, Class<? extends BulkOperationsEntity> entityClass) {
    bulkOperationRepository.save(bulkOperation.withStatus(OperationStatusType.APPLY_CHANGES));
    var execution = executionRepository.save(BulkOperationExecution.builder()
      .bulkOperationId(bulkOperation.getId())
      .startTime(LocalDateTime.now())
      .processedRecords(0)
      .status(StatusType.ACTIVE)
      .build());
    try (var origin = new Scanner(remoteFileSystemClient.get(bulkOperation.getLinkToOriginFile()));
      var modified = new Scanner(remoteFileSystemClient.get(bulkOperation.getLinkToModifiedFile()))) {
      var resultFileName = bulkOperation.getId() + "/result-" + FilenameUtils.getName(bulkOperation.getLinkToOriginFile());
      while (origin.hasNext() && modified.hasNext()) {
        var resultString = updateEntityIfNeeded(origin.nextLine(), modified.nextLine(), bulkOperation, entityClass);
        remoteFileSystemClient.append(new ByteArrayInputStream(resultString.concat(origin.hasNext() ? JSON_STRINGS_DELIMITER : EMPTY).getBytes()), resultFileName);
        execution = execution
          .withStatus(origin.hasNext() ? StatusType.ACTIVE : StatusType.COMPLETED)
          .withProcessedRecords(execution.getProcessedRecords() + 1)
          .withEndTime(origin.hasNext() ? null : LocalDateTime.now());
      }
      bulkOperation = bulkOperation
        .withStatus(OperationStatusType.COMPLETED)
        .withEndTime(LocalDateTime.now())
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
    bulkOperationRepository.save(bulkOperation);
  }

  private String updateEntityIfNeeded(String originalString, String modifiedString, BulkOperation bulkOperation, Class<? extends BulkOperationsEntity> entityClass) {
    if (!originalString.equals(modifiedString)) {
      var updater = updateProcessorFactory.getProcessorFromFactory(entityClass);
      var executionContent = BulkOperationExecutionContent.builder()
        .bulkOperationId(bulkOperation.getId())
        .build();
      try {
        var entity = objectMapper.readValue(originalString, entityClass);
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

  public UnifiedTable getPreview(UUID bulkOperationId, int limit) throws BulkOperationException {
    var bulkOperation = getBulkOperationById(bulkOperationId);
    var entityClass = resolveEntityClass(bulkOperation.getEntityType());
    switch (bulkOperation.getStatus()) {
    case DATA_MODIFICATION:
      return buildPreview(bulkOperation.getLinkToOriginFile(), entityClass, limit);
    case REVIEW_CHANGES:
      return buildPreview(bulkOperation.getLinkToModifiedFile(), entityClass, limit);
    case COMPLETED:
      return buildPreview(bulkOperation.getLinkToResultFile(), entityClass, limit);
    default:
      throw new BulkOperationException("Preview is not available");
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

  public BulkOperation getBulkOperationById(UUID bulkOperationId) throws BulkOperationException {
    return bulkOperationRepository.findById(bulkOperationId)
      .orElseThrow(() -> new BulkOperationException("BulkOperation was not found by id=" + bulkOperationId));
  }
}
