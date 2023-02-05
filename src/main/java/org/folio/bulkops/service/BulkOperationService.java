package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.domain.dto.ApproachType.IN_APP;
import static org.folio.bulkops.domain.dto.ApproachType.MANUAL;
import static org.folio.bulkops.domain.dto.ApproachType.QUERY;
import static org.folio.bulkops.domain.dto.BulkOperationStep.EDIT;
import static org.folio.bulkops.domain.dto.BulkOperationStep.UPLOAD;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_RECORDS;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingIterator;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.folio.bulkops.adapters.ModClientAdapterFactory;
import org.folio.bulkops.client.BulkEditClient;
import org.folio.bulkops.client.DataExportSpringClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.ExportType;
import org.folio.bulkops.domain.bean.ExportTypeSpecificParameters;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.Job;
import org.folio.bulkops.domain.bean.JobStatus;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.bean.StatusType;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationStart;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationDataProcessing;
import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.domain.entity.BulkOperationProcessingContent;
import org.folio.bulkops.exception.BadRequestException;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.IllegalOperationStateException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.ServerErrorException;
import org.folio.bulkops.processor.DataProcessorFactory;
import org.folio.bulkops.processor.UpdateProcessorFactory;
import org.folio.bulkops.repository.BulkOperationDataProcessingRepository;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationExecutionRepository;
import org.folio.bulkops.repository.BulkOperationProcessingContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class BulkOperationService {
  public static final String PREVIEW_IS_NOT_AVAILABLE = "Preview is not available";
  @Value("${application.file-uploading.max-retry-count}")
  private int maxRetryCount;

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

  public BulkOperation uploadCsvFile(EntityType entityType, IdentifierType identifierType, Boolean manual, UUID operationId, MultipartFile multipartFile) {

    String errorMessage = null;
    BulkOperation bulkOperation;

    if (manual) {
      if (operationId == null) {
        throw new NotFoundException("File uploading failed, reason: query parameter operationId is required for csv approach");
      } else {

        bulkOperation = bulkOperationRepository.findById(operationId)
          .orElseThrow(() -> new NotFoundException("Bulk operation was not found by id=" + operationId));

        try {
          var linkToThePreviewFile = remoteFileSystemClient.put(multipartFile.getInputStream(), bulkOperation.getId() + "/" + multipartFile.getOriginalFilename());
          bulkOperation.setLinkToModifiedRecordsCsvFile(linkToThePreviewFile);

          var value = (int) new BufferedReader(new InputStreamReader(remoteFileSystemClient.get(linkToThePreviewFile))).lines().count() - 1;
          bulkOperation.setTotalNumOfRecords(value);
          bulkOperation.setProcessedNumOfRecords(value);
          bulkOperation.setMatchedNumOfRecords(value);

        } catch (Exception e) {
          log.error("Error starting Bulk Operation: " + e.getCause());
          errorMessage = String.format("File uploading failed, reason: %s", e.getMessage());
        }
      }
      bulkOperation.setApproach(MANUAL);
    } else {
      bulkOperation = bulkOperationRepository.save(BulkOperation.builder()
        .id(UUID.randomUUID())
        .entityType(entityType)
        .identifierType(identifierType)
        .status(NEW)
        .startTime(LocalDateTime.now())
        .build());
      try {
        var linkToTriggeringFile = remoteFileSystemClient.put(multipartFile.getInputStream(), bulkOperation.getId() + "/" + multipartFile.getOriginalFilename());
        bulkOperation.setLinkToTriggeringCsvFile(linkToTriggeringFile);
      } catch (Exception e) {
        log.error("Error starting Bulk Operation: " + e);
        errorMessage = String.format("File uploading failed, reason: %s", e.getMessage());
      }
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

    if (isEmpty(bulkOperation.getLinkToMatchedRecordsJsonFile())) {
      throw new BulkOperationException("Missing link to origin file");
    }

    var entityClass = resolveEntityClass(bulkOperation.getEntityType());
    applyRules(bulkOperation, ruleService.getRules(bulkOperationId), entityClass);
  }

  public void applyRules(BulkOperation bulkOperation, BulkOperationRuleCollection ruleCollection, Class<? extends BulkOperationsEntity> entityClass) {

    var bulkOperationId = bulkOperation.getId();

    var dataProcessing = dataProcessingRepository.save(BulkOperationDataProcessing.builder()
      .bulkOperationId(bulkOperation.getId())
      .status(StatusType.ACTIVE)
      .startTime(LocalDateTime.now())
      .totalNumOfRecords(bulkOperation.getTotalNumOfRecords())
      .processedNumOfRecords(0)
      .build());

    var modifiedJsonFileName = bulkOperationId + "/json/modified-" + FilenameUtils.getName(bulkOperation.getLinkToMatchedRecordsJsonFile());
    var modifiedCsvFileName = bulkOperationId + "/modified-" + FilenameUtils.getName(bulkOperation.getLinkToMatchedRecordsCsvFile());

    try (var reader = remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsJsonFile());
         var writerForCsvFile = remoteFileSystemClient.writer(modifiedCsvFileName)) {

      var strategy = new ColumnPositionMappingStrategy<BulkOperationsEntity>();

      StatefulBeanToCsv<BulkOperationsEntity> sbc = new StatefulBeanToCsvBuilder<BulkOperationsEntity>(writerForCsvFile)
        .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
        .withApplyQuotesToAll(false)
        .withMappingStrategy(strategy)
        .build();

      strategy.setType(entityClass);

      var parser = new JsonFactory().createParser(reader);
      var iterator = objectMapper.readValues(parser, entityClass);

      boolean isChangesPresented = false;
      var committedNumOfErrors = 0;

      while (iterator.hasNext()) {
        var original = iterator.next();
        var modified = processUpdate(original, bulkOperation, ruleCollection, entityClass);

        if (Objects.nonNull(modified)) {
          if (!isChangesPresented) {
            isChangesPresented = true;
          }

          // TODO Correct implementation via OpenCSV currently works only for User.class
          if (entityClass.equals(User.class)) {
            sbc.write(modified);
          }
          remoteFileSystemClient.append(new ByteArrayInputStream((objectMapper.writeValueAsString(modified) + LF).getBytes()), modifiedJsonFileName);
        } else {
          committedNumOfErrors++;
        }

        bulkOperation.setCommittedNumOfErrors(committedNumOfErrors);

        dataProcessingRepository.save(dataProcessing
          .withProcessedNumOfRecords(dataProcessing.getProcessedNumOfRecords() + 1)
          .withStatus(iterator.hasNext() ? StatusType.ACTIVE : StatusType.COMPLETED)
          .withEndTime(iterator.hasNext() ? null : LocalDateTime.now()));
      }

      if (isChangesPresented) {
        bulkOperation.setLinkToModifiedRecordsJsonFile(modifiedJsonFileName);
        bulkOperation.setLinkToModifiedRecordsCsvFile(modifiedCsvFileName);
      }

      //TODO This is workaround and potential source of OOM - should be refactored via OpenCSV like it is done for User.class
      if (!entityClass.equals(User.class)) {
        writerForCsvFile.write(getCsvPreviewByBulkOperationId(bulkOperationId, EDIT));
      }

      bulkOperationRepository.save(bulkOperation
        .withApproach(IN_APP)
        .withStatus(OperationStatusType.REVIEW_CHANGES));

    } catch (Exception e) {
      log.error(e);
      dataProcessingRepository.save(dataProcessing
        .withStatus(StatusType.FAILED)
        .withEndTime(LocalDateTime.now()));
      bulkOperationRepository.save(bulkOperation
        .withStatus(OperationStatusType.FAILED)
        .withEndTime(LocalDateTime.now())
        .withErrorMessage("Confirm changes operation failed, reason: " + e.getMessage()));
    }
  }

  private BulkOperationsEntity processUpdate(BulkOperationsEntity original, BulkOperation bulkOperation, BulkOperationRuleCollection rules, Class<? extends BulkOperationsEntity> entityClass) {
    var processor = dataProcessorFactory.getProcessorFromFactory(entityClass);
    var processingContent = BulkOperationProcessingContent.builder()
      .bulkOperationId(bulkOperation.getId())
      .build();
    BulkOperationsEntity modified = null;
    try {
      processingContent.setIdentifier(original.getIdentifier(bulkOperation.getIdentifierType()));
      modified = processor.process(original.getIdentifier(bulkOperation.getIdentifierType()), original, rules);
      processingContent.setState(StateType.PROCESSED);
    } catch (Exception e) {
      processingContent = processingContent
        .withState(StateType.FAILED)
        .withErrorMessage("Failed to modify entity, reason:" + e.getMessage());
    }
    processingContentRepository.save(processingContent);
    return modified;
  }

  public BulkOperation commit(UUID bulkOperationId) throws BulkOperationException {
    var bulkOperation = getBulkOperationOrThrow(bulkOperationId);

    if (isEmpty(bulkOperation.getLinkToMatchedRecordsJsonFile())) {
      throw new BulkOperationException("Missing link to origin file");
    }

    bulkOperation = bulkOperationRepository.save(bulkOperation.withStatus(OperationStatusType.APPLY_CHANGES));

    if (StringUtils.isNotEmpty(bulkOperation.getLinkToModifiedRecordsJsonFile())) {
      var entityClass = resolveEntityClass(bulkOperation.getEntityType());
      var operationId = bulkOperation.getId();
      var execution = executionRepository.save(BulkOperationExecution.builder()
        .bulkOperationId(operationId)
        .startTime(LocalDateTime.now())
        .processedRecords(0)
        .status(StatusType.ACTIVE)
        .build());

      var resultFileName = bulkOperation.getId() + "/json/result-" + FilenameUtils.getName(bulkOperation.getLinkToMatchedRecordsJsonFile());
      var resultCsvFileName = bulkOperation.getId() + "/result-" + FilenameUtils.getName(bulkOperation.getLinkToMatchedRecordsCsvFile());

      try (var originalFileReader = new InputStreamReader(remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsJsonFile()));
           var modifiedFileReader = new InputStreamReader(remoteFileSystemClient.get(bulkOperation.getLinkToModifiedRecordsJsonFile()));
           var writerForCsvFile = remoteFileSystemClient.writer(resultCsvFileName)) {

        var originalFileParser = new JsonFactory().createParser(originalFileReader);
        var originalFileIterator = objectMapper.readValues(originalFileParser, entityClass);

        var modifiedFileParser = new JsonFactory().createParser(modifiedFileReader);
        var modifiedFileIterator = objectMapper.readValues(modifiedFileParser, entityClass);

        var strategy = new ColumnPositionMappingStrategy<BulkOperationsEntity>();

        StatefulBeanToCsv<BulkOperationsEntity> sbc = new StatefulBeanToCsvBuilder<BulkOperationsEntity>(writerForCsvFile)
          .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
          .withApplyQuotesToAll(false)
          .withMappingStrategy(strategy)
          .build();

        strategy.setType(entityClass);

        int committedNumOfRecords = 0;
        int committedNumOfErrors = 0;
        while (hasNextRecord(originalFileIterator, modifiedFileIterator)) {
          var original = originalFileIterator.next();
          var modified = modifiedFileIterator.next();
          try {
            var result = updateEntityIfNeeded(original, modified, bulkOperation, entityClass);
            var hasNextRecord = hasNextRecord(originalFileIterator, modifiedFileIterator);
            remoteFileSystemClient.append(new ByteArrayInputStream((objectMapper.writeValueAsString(result) + (hasNextRecord ? LF : EMPTY)).getBytes()), resultFileName);
            sbc.write(result);
            execution = execution.withStatus(originalFileIterator.hasNext() ? StatusType.ACTIVE : StatusType.COMPLETED)
              .withProcessedRecords(execution.getProcessedRecords() + 1)
              .withEndTime(originalFileIterator.hasNext() ? null : LocalDateTime.now());
            committedNumOfRecords++;
          } catch (Exception e) {
            committedNumOfErrors++;
            errorService.saveError(bulkOperationId, original.getIdentifier(bulkOperation.getIdentifierType()), e.getMessage());
          }
        }

        bulkOperation = bulkOperation.withStatus(OperationStatusType.COMPLETED)
          .withEndTime(LocalDateTime.now())
          .withLinkToCommittedRecordsCsvFile(resultCsvFileName)
          .withLinkToCommittedRecordsJsonFile(resultFileName)
          .withCommittedNumOfErrors(bulkOperation.getCommittedNumOfErrors() + committedNumOfErrors)
          .withCommittedNumOfRecords(committedNumOfRecords);

        executionRepository.save(execution);

      } catch (Exception e) {
        execution = execution.withStatus(StatusType.FAILED)
          .withEndTime(LocalDateTime.now());
        bulkOperation = bulkOperation.withStatus(OperationStatusType.FAILED)
          .withEndTime(LocalDateTime.now())
          .withErrorMessage(e.getMessage());
      }
      executionRepository.save(execution);
    } else {
      bulkOperation.setStatus(OperationStatusType.COMPLETED);
    }

    var linkToCommittingErrorsFile = errorService.uploadErrorsToStorage(bulkOperationId);
    bulkOperation.setLinkToCommittedRecordsErrorsCsvFile(linkToCommittingErrorsFile);

    return bulkOperationRepository.save(bulkOperation);
  }

  private BulkOperationsEntity updateEntityIfNeeded(BulkOperationsEntity original, BulkOperationsEntity modified, BulkOperation bulkOperation, Class<? extends BulkOperationsEntity> entityClass) {
    if (!original.equals(modified)) {
      var updater = updateProcessorFactory.getProcessorFromFactory(entityClass);
      var executionContent = BulkOperationExecutionContent.builder()
        .bulkOperationId(bulkOperation.getId())
        .build();
      try {
        executionContent.setIdentifier(modified.getIdentifier(bulkOperation.getIdentifierType()));
        updater.updateRecord(modified);
        executionContentRepository.save(executionContent.withState(StateType.PROCESSED));
        return modified;
      } catch (Exception e) {
        log.error("Error updating entity: " + e.getCause());
        executionContentRepository.save(executionContent
          .withState(StateType.FAILED)
          .withErrorMessage("Failed to update entity, reason:" + e.getMessage()));
      }
    }
    return original;
  }

  public UnifiedTable getPreview(UUID bulkOperationId, BulkOperationStep step, int limit) {
    try {
      var bulkOperation = getBulkOperationOrThrow(bulkOperationId);
      var entityClass = resolveEntityClass(bulkOperation.getEntityType());
      switch (step) {
        case UPLOAD:
          return buildPreview(bulkOperation.getLinkToMatchedRecordsJsonFile(), entityClass, limit);
        case EDIT:
          return buildPreview(bulkOperation.getLinkToModifiedRecordsJsonFile(), entityClass, limit);
        case COMMIT:
          return buildPreview(bulkOperation.getLinkToCommittedRecordsJsonFile(), entityClass, limit);
        default:
          throw new BulkOperationException(PREVIEW_IS_NOT_AVAILABLE);
      }
    } catch (BulkOperationException e) {
      log.error(e.getCause());
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
    } catch (Exception e) {
      return adapter.getEmptyTableWithHeaders();
    }
  }



  public String getCsvPreviewByBulkOperationId(UUID bulkOperationId, BulkOperationStep step) {
    var table = getPreview(bulkOperationId, step, Integer.MAX_VALUE);
    return table.getHeader()
      .stream()
      .map(Cell::getValue)
      .collect(Collectors.joining(",")) + LF + table.getRows()
      .stream()
      .map(this::rowToCsvLine)
      .collect(Collectors.joining(LF));
  }

  public BulkOperation startBulkOperation(UUID bulkOperationId, BulkOperationStart bulkOperationStart) {
    var step = bulkOperationStart.getStep();
    var approach = bulkOperationStart.getApproach();
    BulkOperation bulkOperation;
    if (QUERY == bulkOperationStart.getApproach() && UPLOAD == step) {
      bulkOperation = BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(bulkOperationStart.getEntityType())
        .identifierType(bulkOperationStart.getEntityCustomIdentifierType())
        .approach(QUERY)
        .status(NEW)
        .startTime(LocalDateTime.now())
        .build();
    } else {
      bulkOperation = bulkOperationRepository.findById(bulkOperationId)
        .orElseThrow(() -> new NotFoundException("Bulk operation was not found bu id=" + bulkOperationId));
    }

    String errorMessage = null;
    try {
      if (UPLOAD == step) {
        try {
          if (NEW.equals(bulkOperation.getStatus())) {
            if (MANUAL != approach) {
              var job = dataExportSpringClient.upsertJob(Job.builder()
                .type((QUERY == approach) ?
                  ExportType.BULK_EDIT_QUERY :
                  ExportType.BULK_EDIT_IDENTIFIERS)
                .entityType(bulkOperation.getEntityType())
                .exportTypeSpecificParameters((QUERY == approach) ?
                  new ExportTypeSpecificParameters()
                .withQuery(bulkOperationStart.getQuery()) :
                  new ExportTypeSpecificParameters())
                .identifierType(bulkOperation.getIdentifierType()).build());
              bulkOperation.setDataExportJobId(job.getId());
              bulkOperationRepository.save(bulkOperation);

              if (JobStatus.SCHEDULED.equals(job.getStatus())) {
                if (QUERY != approach) {
                  uploadCsvFile(job.getId(), new FolioMultiPartFile(FilenameUtils.getName(bulkOperation.getLinkToTriggeringCsvFile()), "application/json", remoteFileSystemClient.get(bulkOperation.getLinkToTriggeringCsvFile())));
                  job = dataExportSpringClient.getJob(job.getId());
                }

                if (JobStatus.FAILED.equals(job.getStatus())) {
                  errorMessage = "Data export job failed";
                } else {
                  if (QUERY != approach && JobStatus.SCHEDULED.equals(job.getStatus())) {
                    bulkEditClient.startJob(job.getId());
                  }
                  bulkOperation.setStatus(RETRIEVING_RECORDS);
                }
              } else {
                errorMessage = String.format("File uploading failed - invalid job status: %s (expected: SCHEDULED)", job.getStatus().getValue());
              }
            }
          } else {
            throw new BadRequestException(String.format("Step %s is not applicable for bulk operation status %s", step, bulkOperation.getStatus()));
          }
        } catch (Exception e) {
          log.error("Error starting Bulk Operation: " + e.getCause());
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
      } else if (BulkOperationStep.EDIT == step) {
        errorService.deleteErrorsByBulkOperationId(bulkOperationId);
        bulkOperation.setCommittedNumOfErrors(0);
        if (bulkOperation.getLinkToModifiedRecordsJsonFile() != null) {
          remoteFileSystemClient.remove(bulkOperation.getLinkToModifiedRecordsJsonFile());
          remoteFileSystemClient.remove(bulkOperation.getLinkToModifiedRecordsCsvFile());
        }
        if (DATA_MODIFICATION.equals(bulkOperation.getStatus()) || REVIEW_CHANGES.equals(bulkOperation.getStatus())) {
          if (MANUAL == approach) {
            apply(bulkOperation);
          } else {
            confirm(bulkOperationId);
          }
          return bulkOperation;
        } else {
          throw new BadRequestException(String.format("Step %s is not applicable for bulk operation status %s", step, bulkOperation.getStatus()));
        }
      } else if (BulkOperationStep.COMMIT == step) {
        if (REVIEW_CHANGES.equals(bulkOperation.getStatus())) {
          return commit(bulkOperationId);
        } else {
          throw new BadRequestException(String.format("Step %s is not applicable for bulk operation status %s", step, bulkOperation.getStatus()));
        }
      } else if (COMPLETED.equals(bulkOperation.getStatus())) {
        return bulkOperation;
      } else {
        throw new IllegalOperationStateException("Bulk operation cannot be started, reason: invalid state: " + bulkOperation.getStatus());
      }
    } catch (BulkOperationException e) {
      log.error(e.getCause());
      throw new IllegalOperationStateException("Bulk operation cannot be started, reason: " + e.getMessage());
    }
  }

  private void apply(BulkOperation bulkOperation) {
    var bulkOperationId = bulkOperation.getId();
    var linkToOriginFile = bulkOperation.getLinkToMatchedRecordsJsonFile();
    var linkToModifiedCsvFile = bulkOperation.getLinkToModifiedRecordsCsvFile();
    var linkToModifiedFile = bulkOperationId + "/json/modified-" + FilenameUtils.getName(linkToOriginFile);

    try (Reader originalFileReader = new InputStreamReader(remoteFileSystemClient.get(linkToOriginFile));
         Reader modifiedFileReader = new InputStreamReader(remoteFileSystemClient.get(linkToModifiedCsvFile));
         Writer writer = remoteFileSystemClient.writer(linkToModifiedFile)) {

      CsvToBean<User> csvToBean = new CsvToBeanBuilder<User>(modifiedFileReader)
        .withType(User.class)
        .withSkipLines(1)
        .build();

      var modifiedCsvFileIterator = csvToBean.iterator();

      var parser = new JsonFactory().createParser(originalFileReader);
      var entityType = resolveEntityClass(bulkOperation.getEntityType());

      var originalJsonFileIterator = objectMapper.readValues(parser, entityType);
      var committedNumOfErrors = 0;

      while (originalJsonFileIterator.hasNext() && modifiedCsvFileIterator.hasNext()) {
        var originalEntity = originalJsonFileIterator.next();
        var modifiedEntity = modifiedCsvFileIterator.next();

        if (EqualsBuilder.reflectionEquals(originalEntity, modifiedEntity, true, entityType, "metadata", "createdDate", "updatedDate")) {
          committedNumOfErrors++;
          errorService.saveError(bulkOperationId, originalEntity.getIdentifier(bulkOperation.getIdentifierType()), "No change in value required");
        } else {
          writer.write(objectMapper.writeValueAsString(modifiedEntity));
        }
      }

      bulkOperation.setCommittedNumOfErrors(committedNumOfErrors);
      bulkOperation.setStatus(REVIEW_CHANGES);
      bulkOperation.setLinkToModifiedRecordsJsonFile(linkToModifiedFile);
      bulkOperationRepository.save(bulkOperation);

    } catch (Exception e) {
      log.error("Error applying changes: " + e.getCause());
      throw new ServerErrorException(e.getMessage());
    }
  }

  private String uploadCsvFile(UUID dataExportJobId, MultipartFile file) throws BulkOperationException {
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
      log.error("Failed to convert string to POJO: " + e.getCause());
      return null;
    }
  }

  private Class<? extends BulkOperationsEntity> resolveEntityClass(EntityType entityType) throws BulkOperationException {
    switch (entityType) {
    case USER:
      return User.class;
    case ITEM:
      return Item.class;
      case HOLDINGS_RECORD:
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

  private boolean hasNextRecord(MappingIterator<? extends BulkOperationsEntity> originalFileIterator, MappingIterator<? extends BulkOperationsEntity> modifiedFileIterator) {
    return originalFileIterator.hasNext() && modifiedFileIterator.hasNext();
  }
}
