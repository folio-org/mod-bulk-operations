package org.folio.bulkops.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
import org.folio.bulkops.domain.converter.CustomMappingStrategy;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationStart;
import org.folio.bulkops.domain.dto.BulkOperationStep;
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
import org.folio.bulkops.processor.UpdatedEntityHolder;
import org.folio.bulkops.repository.BulkOperationDataProcessingRepository;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationExecutionRepository;
import org.folio.bulkops.repository.BulkOperationProcessingContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.opencsv.ICSVWriter.DEFAULT_SEPARATOR;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.domain.dto.ApproachType.IN_APP;
import static org.folio.bulkops.domain.dto.ApproachType.MANUAL;
import static org.folio.bulkops.domain.dto.ApproachType.QUERY;
import static org.folio.bulkops.domain.dto.BulkOperationStep.UPLOAD;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_RECORDS;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;
import static org.folio.bulkops.util.Utils.resolveEntityClass;

@Service
@Log4j2
@RequiredArgsConstructor
public class BulkOperationService {
  public static final String FILE_UPLOADING_FAILED_REASON = "File uploading failed, reason: %s";
  public static final String STEP_S_IS_NOT_APPLICABLE_FOR_BULK_OPERATION_STATUS = "Step %s is not applicable for bulk operation status %s";
  public static final String ERROR_STARTING_BULK_OPERATION = "Error starting Bulk Operation: ";
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

  private static final int OPERATION_UPDATING_STEP = 100;

  private final ExecutorService executor = Executors.newCachedThreadPool();

  public BulkOperation uploadCsvFile(EntityType entityType, IdentifierType identifierType, boolean manual, UUID operationId, UUID xOkapiUserId, MultipartFile multipartFile) {

    String errorMessage = null;
    BulkOperation operation;

    if (manual) {
      if (operationId == null) {
        throw new NotFoundException("File uploading failed, reason: query parameter operationId is required for csv approach");
      } else {

        operation = bulkOperationRepository.findById(operationId)
          .orElseThrow(() -> new NotFoundException("Bulk operation was not found by id=" + operationId));

        try {
          var linkToThePreviewFile = remoteFileSystemClient.put(multipartFile.getInputStream(), operation.getId() + "/" + multipartFile.getOriginalFilename());
          operation.setLinkToModifiedRecordsCsvFile(linkToThePreviewFile);

          var numOfLines = remoteFileSystemClient.getNumOfLines(linkToThePreviewFile) - 1;
          operation.setTotalNumOfRecords(numOfLines);
          operation.setProcessedNumOfRecords(numOfLines);
          operation.setMatchedNumOfRecords(numOfLines);

        } catch (Exception e) {
          log.error(ERROR_STARTING_BULK_OPERATION + e.getCause());
          errorMessage = String.format(FILE_UPLOADING_FAILED_REASON, e.getMessage());
        }
      }
      operation.setApproach(MANUAL);
    } else {
      operation = bulkOperationRepository.save(BulkOperation.builder()
        .id(UUID.randomUUID())
        .entityType(entityType)
        .identifierType(identifierType)
        .status(NEW)
        .startTime(LocalDateTime.now())
        .build());
      try {
        var linkToTriggeringFile = remoteFileSystemClient.put(multipartFile.getInputStream(), operation.getId() + "/" + multipartFile.getOriginalFilename());
        operation.setLinkToTriggeringCsvFile(linkToTriggeringFile);
      } catch (Exception e) {
        log.error(ERROR_STARTING_BULK_OPERATION + e);
        errorMessage = String.format(FILE_UPLOADING_FAILED_REASON, e.getMessage());
      }
    }

    if (nonNull(errorMessage)) {
      log.error(errorMessage);
      operation.setStatus(FAILED);
      operation.setErrorMessage(errorMessage);
      operation.setEndTime(LocalDateTime.now());
    }

    operation.setUserId(xOkapiUserId);

    return bulkOperationRepository.save(operation);
  }

  public void confirm(BulkOperation operation)  {

    operation.setProcessedNumOfRecords(0);
    var operationId = operation.getId();

    var clazz = resolveEntityClass(operation.getEntityType());
    var ruleCollection = ruleService.getRules(operationId);

    var dataProcessing = dataProcessingRepository.save(BulkOperationDataProcessing.builder()
      .bulkOperationId(operation.getId())
      .status(StatusType.ACTIVE)
      .startTime(LocalDateTime.now())
      .totalNumOfRecords(operation.getTotalNumOfRecords())
      .processedNumOfRecords(0)
      .build());

    var modifiedJsonFileName = operationId + "/json/modified-" + FilenameUtils.getName(operation.getLinkToMatchedRecordsJsonFile());
    var modifiedCsvFileName = operationId + "/modified-" + FilenameUtils.getName(operation.getLinkToMatchedRecordsCsvFile());

    try (var readerForMatchedJsonFile = remoteFileSystemClient.get(operation.getLinkToMatchedRecordsJsonFile());
         var writerForModifiedCsvFile = remoteFileSystemClient.writer(modifiedCsvFileName);
         var writerForModifiedJsonFile = remoteFileSystemClient.writer(modifiedJsonFileName)) {

      var strategy = new CustomMappingStrategy<BulkOperationsEntity>();

      strategy.setType(clazz);

      StatefulBeanToCsv<BulkOperationsEntity> sbc = new StatefulBeanToCsvBuilder<BulkOperationsEntity>(writerForModifiedCsvFile)
        .withSeparator(DEFAULT_SEPARATOR)
        .withApplyQuotesToAll(false)
        .withMappingStrategy(strategy)
        .build();

      var parser = new JsonFactory().createParser(readerForMatchedJsonFile);
      var iterator = objectMapper.readValues(parser, clazz);

      boolean isChangesPresented = false;
      var committedNumOfErrors = 0;
      var processedNumOfRecords = 0;

      if(iterator.hasNext()) {
        operation.setLinkToModifiedRecordsCsvFile(modifiedCsvFileName);
      }

      while (iterator.hasNext()) {
        var original = iterator.next();
        var modified = processUpdate(original, operation, ruleCollection, clazz);

        if (Objects.nonNull(modified)) {
          sbc.write(modified.getEntity());
          var modifiedRecord = objectMapper.writeValueAsString(modified.getEntity()) + LF;

          if (modified.isChanged()) {
            if (!isChangesPresented) {
              isChangesPresented = true;
            }
            writerForModifiedJsonFile.write(modifiedRecord);
          } else {
            committedNumOfErrors++;
          }
        }

        operation.setCommittedNumOfErrors(committedNumOfErrors);

        dataProcessingRepository.save(dataProcessing
          .withProcessedNumOfRecords(dataProcessing.getProcessedNumOfRecords() + 1)
          .withStatus(iterator.hasNext() ? StatusType.ACTIVE : StatusType.COMPLETED)
          .withEndTime(iterator.hasNext() ? null : LocalDateTime.now()));

        processedNumOfRecords++;
        if (processedNumOfRecords - operation.getProcessedNumOfRecords() > OPERATION_UPDATING_STEP) {
          operation.setProcessedNumOfRecords(processedNumOfRecords);
          bulkOperationRepository.save(operation);
        }
      }

      if (isChangesPresented) {
        operation.setLinkToModifiedRecordsJsonFile(modifiedJsonFileName);
      }

      operation.setApproach(IN_APP);
      operation.setStatus(OperationStatusType.REVIEW_CHANGES);
      operation.setProcessedNumOfRecords(processedNumOfRecords);
      bulkOperationRepository.save(operation);

    } catch (Exception e) {
      log.error(e);
      dataProcessingRepository.save(dataProcessing
        .withStatus(StatusType.FAILED)
        .withEndTime(LocalDateTime.now()));
      operation.setStatus(OperationStatusType.FAILED);
      operation.setEndTime(LocalDateTime.now());
      operation.setErrorMessage("Confirm changes operation failed, reason: " + e.getMessage());
      bulkOperationRepository.save(operation);
    }
  }

  private UpdatedEntityHolder<? extends BulkOperationsEntity> processUpdate(BulkOperationsEntity original, BulkOperation operation, BulkOperationRuleCollection rules, Class<? extends BulkOperationsEntity> entityClass) {
    var processor = dataProcessorFactory.getProcessorFromFactory(entityClass);
    var processingContent = BulkOperationProcessingContent.builder()
      .bulkOperationId(operation.getId())
      .build();
    UpdatedEntityHolder<BulkOperationsEntity> modified = null;
    try {
      processingContent.setIdentifier(original.getIdentifier(operation.getIdentifierType()));
      modified = processor.process(original.getIdentifier(operation.getIdentifierType()), original, rules);
      processingContent.setState(StateType.PROCESSED);
    } catch (Exception e) {
      processingContent = processingContent
        .withState(StateType.FAILED)
        .withErrorMessage("Failed to modify entity, reason:" + e.getMessage());
    }
    processingContentRepository.save(processingContent);
    return modified;
  }

  public void commit(BulkOperation operation) {

//    if (isEmpty(operation.getLinkToMatchedRecordsJsonFile())) {
//      throw new BulkOperationException("Missing link to origin file");
//    }

    var operationId = operation.getId();
    operation.setCommittedNumOfRecords(0);
    operation.setStatus(OperationStatusType.APPLY_CHANGES);
    operation = bulkOperationRepository.save(operation);

    if (StringUtils.isNotEmpty(operation.getLinkToModifiedRecordsJsonFile())) {
      var entityClass = resolveEntityClass(operation.getEntityType());

      var execution = executionRepository.save(BulkOperationExecution.builder()
        .bulkOperationId(operationId)
        .startTime(LocalDateTime.now())
        .processedRecords(0)
        .status(StatusType.ACTIVE)
        .build());

      var resultJsonFileName = operation.getId() + "/json/result-" + FilenameUtils.getName(operation.getLinkToMatchedRecordsJsonFile());
      var resultCsvFileName = operation.getId() + "/result-" + FilenameUtils.getName(operation.getLinkToMatchedRecordsCsvFile());

      try (var originalFileReader = new InputStreamReader(remoteFileSystemClient.get(operation.getLinkToMatchedRecordsJsonFile()));
           var modifiedFileReader = new InputStreamReader(remoteFileSystemClient.get(operation.getLinkToModifiedRecordsJsonFile()));
           var writerForCsvFile = remoteFileSystemClient.writer(resultCsvFileName);
           var writerForJsonFile = remoteFileSystemClient.writer(resultJsonFileName)) {

        var originalFileParser = new JsonFactory().createParser(originalFileReader);
        var originalFileIterator = objectMapper.readValues(originalFileParser, entityClass);

        var modifiedFileParser = new JsonFactory().createParser(modifiedFileReader);
        var modifiedFileIterator = objectMapper.readValues(modifiedFileParser, entityClass);

        var strategy = new CustomMappingStrategy<BulkOperationsEntity>();

        StatefulBeanToCsv<BulkOperationsEntity> sbc = new StatefulBeanToCsvBuilder<BulkOperationsEntity>(writerForCsvFile)
          .withSeparator(DEFAULT_SEPARATOR)
          .withApplyQuotesToAll(false)
          .withMappingStrategy(strategy)
          .build();

        strategy.setType(entityClass);

        int committedNumOfRecords = 0;
        int committedNumOfErrors = 0;
        int processedNumOfRecords = 0;

        while (hasNextRecord(originalFileIterator, modifiedFileIterator)) {
          var original = originalFileIterator.next();
          var modified = modifiedFileIterator.next();

          processedNumOfRecords++;

          try {
            var result = updateEntityIfNeeded(original, modified, operation, entityClass);
            var hasNextRecord = hasNextRecord(originalFileIterator, modifiedFileIterator);
            writerForJsonFile.write(objectMapper.writeValueAsString(result) + (hasNextRecord ? LF : EMPTY));
            sbc.write(result);
            execution = execution
              .withStatus(originalFileIterator.hasNext() ? StatusType.ACTIVE : StatusType.COMPLETED)
              .withEndTime(originalFileIterator.hasNext() ? null : LocalDateTime.now());
            committedNumOfRecords++;
          } catch (Exception e) {
            committedNumOfErrors++;
            errorService.saveError(operationId, original.getIdentifier(operation.getIdentifierType()), e.getMessage());
          }

          if (committedNumOfRecords - operation.getCommittedNumOfRecords() > OPERATION_UPDATING_STEP) {
            operation.setCommittedNumOfRecords(committedNumOfRecords);
            bulkOperationRepository.save(operation);
          }

          if (processedNumOfRecords - execution.getProcessedRecords() > OPERATION_UPDATING_STEP) {
            execution.setProcessedRecords(processedNumOfRecords);
            executionRepository.save(execution);
          }
        }

        execution.setProcessedRecords(processedNumOfRecords);
        operation.setStatus(OperationStatusType.COMPLETED);
        operation.setProcessedNumOfRecords(committedNumOfRecords);
        operation.setEndTime(LocalDateTime.now());
        operation.setLinkToCommittedRecordsCsvFile(resultCsvFileName);
        operation.setLinkToCommittedRecordsJsonFile(resultJsonFileName);
        operation.setCommittedNumOfErrors((operation.getCommittedNumOfErrors() != null ? operation.getCommittedNumOfErrors() : 0) + committedNumOfErrors);
        operation.setCommittedNumOfRecords(committedNumOfRecords);
      } catch (Exception e) {
        execution = execution
          .withStatus(StatusType.FAILED)
          .withEndTime(LocalDateTime.now());
        operation.setStatus(OperationStatusType.FAILED);
        operation.setEndTime(LocalDateTime.now());
        operation.setErrorMessage(e.getMessage());
      }
      executionRepository.save(execution);
    } else {
      operation.setStatus(OperationStatusType.COMPLETED);
    }

    var linkToCommittingErrorsFile = errorService.uploadErrorsToStorage(operationId);
    operation.setLinkToCommittedRecordsErrorsCsvFile(linkToCommittingErrorsFile);

    bulkOperationRepository.save(operation);
  }

  private BulkOperationsEntity updateEntityIfNeeded(BulkOperationsEntity original, BulkOperationsEntity modified, BulkOperation operation, Class<? extends BulkOperationsEntity> entityClass) {
    if (!original.equals(modified)) {
      var updater = updateProcessorFactory.getProcessorFromFactory(entityClass);
      var executionContent = BulkOperationExecutionContent.builder()
        .bulkOperationId(operation.getId())
        .build();
        executionContent.setIdentifier(modified.getIdentifier(operation.getIdentifierType()));
        updater.updateRecord(modified);
        executionContentRepository.save(executionContent.withState(StateType.PROCESSED));
        return modified;
    }
    return original;
  }

  public UnifiedTable getPreview(BulkOperation operation, BulkOperationStep step, int offset, int limit) {
      var entityClass = resolveEntityClass(operation.getEntityType());
      return switch (step) {
        case UPLOAD -> buildPreviewFromCsvFile(operation.getLinkToMatchedRecordsCsvFile(), entityClass, offset, limit);
        case EDIT -> buildPreviewFromCsvFile(operation.getLinkToModifiedRecordsCsvFile(), entityClass, offset, limit);
        case COMMIT -> buildPreviewFromCsvFile(operation.getLinkToCommittedRecordsCsvFile(), entityClass, offset, limit);
      };
  }

  private UnifiedTable buildPreviewFromCsvFile(String pathToFile, Class<? extends BulkOperationsEntity> clazz, int offset, int limit) {
    var adapter = modClientAdapterFactory.getModClientAdapter(clazz);
    var table = adapter.getEmptyTableWithHeaders();
    try (Reader reader = new InputStreamReader(remoteFileSystemClient.get(pathToFile))) {
      try (CSVReader csvReader = new CSVReader(reader)) {
        csvReader.skip(offset + 1);
        String[] line;
        while ((line = csvReader.readNext()) != null && csvReader.getLinesRead() < limit + 2) {
          var row = new Row();
          row.setRow(Arrays.stream(line).collect(Collectors.toList()));
          table.addRowsItem(row);
        }
      }
      return table;
    } catch (Exception e) {
      log.error(e);
    }
    return table;
  }

  public BulkOperation startBulkOperation(UUID bulkOperationId, UUID xOkapiUserId, BulkOperationStart bulkOperationStart) {
    var step = bulkOperationStart.getStep();
    var approach = bulkOperationStart.getApproach();
    BulkOperation operation;
    if (QUERY == bulkOperationStart.getApproach() && UPLOAD == step) {
      operation = BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(bulkOperationStart.getEntityType())
        .identifierType(bulkOperationStart.getEntityCustomIdentifierType())
        .approach(QUERY)
        .status(NEW)
        .startTime(LocalDateTime.now())
        .build();
    } else {
      operation = bulkOperationRepository.findById(bulkOperationId)
        .orElseThrow(() -> new NotFoundException("Bulk operation was not found bu id=" + bulkOperationId));
    }
    operation.setUserId(xOkapiUserId);

    String errorMessage = null;
    if (UPLOAD == step) {
      errorMessage = executeDataExportJob(bulkOperationStart, step, approach, operation, errorMessage);

      if (nonNull(errorMessage)) {
        log.error(errorMessage);
        operation.setStatus(FAILED);
        operation.setErrorMessage(errorMessage);
        operation.setEndTime(LocalDateTime.now());
      }
      bulkOperationRepository.save(operation);
      return operation;
    } else if (BulkOperationStep.EDIT == step) {
      errorService.deleteErrorsByBulkOperationId(bulkOperationId);
      operation.setCommittedNumOfErrors(0);
      if (DATA_MODIFICATION.equals(operation.getStatus()) || REVIEW_CHANGES.equals(operation.getStatus())) {
        if (MANUAL == approach) {
          executor.execute(() -> apply(operation));
        } else {
          clear(operation);
          executor.execute(() -> confirm(operation));
        }
        return operation;
      } else {
        throw new BadRequestException(String.format(STEP_S_IS_NOT_APPLICABLE_FOR_BULK_OPERATION_STATUS, step, operation.getStatus()));
      }
    } else if (BulkOperationStep.COMMIT == step) {
      if (REVIEW_CHANGES.equals(operation.getStatus())) {
        executor.execute(() -> commit(operation));
        return operation;
      } else {
        throw new BadRequestException(String.format(STEP_S_IS_NOT_APPLICABLE_FOR_BULK_OPERATION_STATUS, step, operation.getStatus()));
      }
    } else {
      throw new IllegalOperationStateException("Bulk operation cannot be started, reason: invalid state: " + operation.getStatus());
    }
  }

  private void clear(BulkOperation operation) {
    if (isNotEmpty(operation.getLinkToModifiedRecordsJsonFile())) {
      remoteFileSystemClient.remove(operation.getLinkToModifiedRecordsJsonFile());
      operation.setLinkToModifiedRecordsJsonFile(null);
    }
    if (isNotEmpty(operation.getLinkToModifiedRecordsCsvFile())) {
      remoteFileSystemClient.remove(operation.getLinkToModifiedRecordsCsvFile());
      operation.setLinkToModifiedRecordsCsvFile(null);
    }
  }

  private String executeDataExportJob(BulkOperationStart bulkOperationStart, BulkOperationStep step, ApproachType approach, BulkOperation operation, String errorMessage) {
    try {
      if (NEW.equals(operation.getStatus())) {
        if (MANUAL != approach) {
          var job = dataExportSpringClient.upsertJob(Job.builder()
            .type((QUERY == approach) ?
              ExportType.BULK_EDIT_QUERY :
              ExportType.BULK_EDIT_IDENTIFIERS)
            .entityType(operation.getEntityType())
            .exportTypeSpecificParameters((QUERY == approach) ?
              new ExportTypeSpecificParameters()
            .withQuery(bulkOperationStart.getQuery()) :
              new ExportTypeSpecificParameters())
            .identifierType(operation.getIdentifierType()).build());
          operation.setDataExportJobId(job.getId());
          bulkOperationRepository.save(operation);

          if (JobStatus.SCHEDULED.equals(job.getStatus())) {
            if (QUERY != approach) {
              uploadCsvFile(job.getId(), new FolioMultiPartFile(FilenameUtils.getName(operation.getLinkToTriggeringCsvFile()), "application/json", remoteFileSystemClient.get(operation.getLinkToTriggeringCsvFile())));
              job = dataExportSpringClient.getJob(job.getId());
            }

            if (JobStatus.FAILED.equals(job.getStatus())) {
              errorMessage = "Data export job failed";
            } else {
              if (QUERY != approach && JobStatus.SCHEDULED.equals(job.getStatus())) {
                bulkEditClient.startJob(job.getId());
              }
              operation.setStatus(RETRIEVING_RECORDS);
            }
          } else {
            errorMessage = String.format("File uploading failed - invalid job status: %s (expected: SCHEDULED)", job.getStatus().getValue());
          }
        }
      } else {
        throw new BadRequestException(String.format(STEP_S_IS_NOT_APPLICABLE_FOR_BULK_OPERATION_STATUS, step, operation.getStatus()));
      }
    } catch (Exception e) {
      log.error(ERROR_STARTING_BULK_OPERATION + e.getCause());
      errorMessage = String.format(FILE_UPLOADING_FAILED_REASON, e.getMessage());
    }
    return errorMessage;
  }

  public void apply(BulkOperation operation) {
    operation.setProcessedNumOfRecords(0);
    var bulkOperationId = operation.getId();
    var linkToMatchedRecordsJsonFile = operation.getLinkToMatchedRecordsJsonFile();
    var linkToModifiedRecordsCsvFile = operation.getLinkToModifiedRecordsCsvFile();
    var linkToModifiedRecordsJsonFile = bulkOperationId + "/json/modified-" + FilenameUtils.getName(linkToMatchedRecordsJsonFile);
    try (Reader readerForMatchedJsonFile = new InputStreamReader(remoteFileSystemClient.get(linkToMatchedRecordsJsonFile));
         Reader readerForModifiedCsvFile = new InputStreamReader(remoteFileSystemClient.get(linkToModifiedRecordsCsvFile));
         Writer writerForModifiedJsonFile = remoteFileSystemClient.writer(linkToModifiedRecordsJsonFile)) {

      var clazz = resolveEntityClass(operation.getEntityType());

      CsvToBean<BulkOperationsEntity> csvToBean = new CsvToBeanBuilder<BulkOperationsEntity>(readerForModifiedCsvFile)
        .withType(clazz)
        .withSkipLines(1)
        .build();

      var modifiedCsvFileIterator = csvToBean.iterator();

      var parser = new JsonFactory().createParser(readerForMatchedJsonFile);
      var entityType = resolveEntityClass(operation.getEntityType());

      var originalJsonFileIterator = objectMapper.readValues(parser, entityType);
      var committedNumOfErrors = 0;
      var processedNumOfRecords = 0;

      while (originalJsonFileIterator.hasNext() && modifiedCsvFileIterator.hasNext()) {
        var originalEntity = originalJsonFileIterator.next();
        var modifiedEntity = modifiedCsvFileIterator.next();

        var modifiedEntityString = objectMapper.writeValueAsString(modifiedEntity) + (originalJsonFileIterator.hasNext() && modifiedCsvFileIterator.hasNext() ? LF : EMPTY);

        if (EqualsBuilder.reflectionEquals(originalEntity, modifiedEntity, true, entityType, "metadata", "createdDate", "updatedDate")) {
          committedNumOfErrors++;
          errorService.saveError(bulkOperationId, originalEntity.getIdentifier(operation.getIdentifierType()), "No change in value required");
        } else {
          writerForModifiedJsonFile.write(modifiedEntityString);
        }
        processedNumOfRecords++;
        if (processedNumOfRecords - operation.getProcessedNumOfRecords() > OPERATION_UPDATING_STEP) {
          operation.setProcessedNumOfRecords(processedNumOfRecords);
          bulkOperationRepository.save(operation);
        }
      }
      operation.setCommittedNumOfErrors(committedNumOfErrors);
      operation.setProcessedNumOfRecords(processedNumOfRecords);
      operation.setStatus(REVIEW_CHANGES);
      operation.setLinkToModifiedRecordsJsonFile(linkToModifiedRecordsJsonFile);
      bulkOperationRepository.save(operation);

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
        operation.setProcessedNumOfRecords(processing.get().getProcessedNumOfRecords());
        return operation;
      }
    } else if (APPLY_CHANGES.equals(operation.getStatus())) {
      var execution = executionRepository.findByBulkOperationId(bulkOperationId);
      if (execution.isPresent() && StatusType.ACTIVE.equals(execution.get().getStatus())) {
        operation.setProcessedNumOfRecords(execution.get().getProcessedRecords());
        return operation;
      }
    }
    return operation;
  }

  public BulkOperation getBulkOperationOrThrow(UUID operationId) {
    return bulkOperationRepository.findById(operationId)
      .orElseThrow(() -> new NotFoundException("BulkOperation was not found by id=" + operationId));
  }

  private boolean hasNextRecord(MappingIterator<? extends BulkOperationsEntity> originalFileIterator, MappingIterator<? extends BulkOperationsEntity> modifiedFileIterator) {
    return originalFileIterator.hasNext() && modifiedFileIterator.hasNext();
  }
}
