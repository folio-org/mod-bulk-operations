package org.folio.bulkops.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.domain.dto.ApproachType.IN_APP;
import static org.folio.bulkops.domain.dto.ApproachType.MANUAL;
import static org.folio.bulkops.domain.dto.ApproachType.QUERY;
import static org.folio.bulkops.domain.dto.BulkOperationStep.COMMIT;
import static org.folio.bulkops.domain.dto.BulkOperationStep.EDIT;
import static org.folio.bulkops.domain.dto.BulkOperationStep.UPLOAD;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_RECORDS;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;
import static org.folio.bulkops.util.Utils.hasNextRecord;

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
import org.folio.bulkops.processor.UpdatedEntityHolder;
import org.folio.bulkops.repository.BulkOperationDataProcessingRepository;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationExecutionRepository;
import org.folio.bulkops.repository.BulkOperationProcessingContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonFactory;
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

  public BulkOperation uploadCsvFile(EntityType entityType, IdentifierType identifierType, Boolean manual, UUID operationId, UUID xOkapiUserId, MultipartFile multipartFile) {

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

    bulkOperation.setUserId(xOkapiUserId);

    return bulkOperationRepository.save(bulkOperation);
  }

  public void confirmBulkOperation(UUID bulkOperationId) throws BulkOperationException {
    var bulkOperation = getBulkOperationOrThrow(bulkOperationId);

    var linkToMatchedRecordsJsonFile = bulkOperation.getLinkToMatchedRecordsJsonFile();

    if (isEmpty(linkToMatchedRecordsJsonFile)) {
      throw new BulkOperationException("Missing link to origin file");
    }

    var clazz = resolveEntityClass(bulkOperation.getEntityType());

    var rules = ruleService.getRules(bulkOperationId);

    var dataProcessing = dataProcessingRepository.save(BulkOperationDataProcessing.builder()
      .bulkOperationId(bulkOperation.getId())
      .status(StatusType.ACTIVE)
      .startTime(LocalDateTime.now())
      .totalNumOfRecords(bulkOperation.getTotalNumOfRecords())
      .processedNumOfRecords(0)
      .build());

    var modifiedJsonFileName = bulkOperationId + "/json/modified-" + FilenameUtils.getName(linkToMatchedRecordsJsonFile);
    var modifiedForPreviewJsonFileName = bulkOperationId + "/json/preview-" + FilenameUtils.getName(linkToMatchedRecordsJsonFile);
    var modifiedCsvFileName = bulkOperationId + "/modified-" + FilenameUtils.getName(bulkOperation.getLinkToMatchedRecordsCsvFile());


    try (var matchedJsonFileReader = remoteFileSystemClient.get(linkToMatchedRecordsJsonFile);
         var modifiedCsvFileWriter = remoteFileSystemClient.writer(modifiedCsvFileName)) {

      var strategy = new CustomMappingStrategy<BulkOperationsEntity>();
      strategy.setType(clazz);

      StatefulBeanToCsv<BulkOperationsEntity> sbc = new StatefulBeanToCsvBuilder<BulkOperationsEntity>(modifiedCsvFileWriter)
        .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
        .withApplyQuotesToAll(false)
        .withMappingStrategy(strategy)
        .build();

      var iterator = objectMapper.readValues(new JsonFactory().createParser(matchedJsonFileReader), clazz);

      boolean isChangesPresented = false;
      var committedNumOfErrors = 0;

      while (iterator.hasNext()) {

        var modified = processUpdateEntities(iterator.next(), bulkOperation, rules, clazz);

        // TODO Correct implementation via OpenCSV currently works only for User.class
        if (User.class == clazz) {
          sbc.write(modified.getEntity());
        }

        var result = objectMapper.writeValueAsString(modified.getEntity()) + LF;

        remoteFileSystemClient.append(new ByteArrayInputStream(result.getBytes()), modifiedForPreviewJsonFileName);

        bulkOperation.setLinkToModifiedForPreviewRecordsJsonFile(modifiedForPreviewJsonFileName);
        bulkOperation.setLinkToModifiedRecordsCsvFile(modifiedCsvFileName);

        if (modified.isChanged()) {
          if (!isChangesPresented) {
            isChangesPresented = true;
            bulkOperation.setLinkToModifiedRecordsJsonFile(modifiedJsonFileName);
          }
          remoteFileSystemClient.append(new ByteArrayInputStream(result.getBytes()), modifiedJsonFileName);
        } else {
          committedNumOfErrors++;
        }

        bulkOperation.setCommittedNumOfErrors(committedNumOfErrors);

        dataProcessingRepository.save(dataProcessing
          .withProcessedNumOfRecords(dataProcessing.getProcessedNumOfRecords() + 1)
          .withStatus(iterator.hasNext() ? StatusType.ACTIVE : StatusType.COMPLETED)
          .withEndTime(iterator.hasNext() ? null : LocalDateTime.now()));
      }

      //TODO This is workaround and potential source of OOM - should be refactored via OpenCSV like it is done for User.class
      if (!clazz.equals(User.class)) {
        modifiedCsvFileWriter.write(getCsvPreviewForBulkOperation(bulkOperation, EDIT));
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

  private UpdatedEntityHolder<? extends BulkOperationsEntity> processUpdateEntities(BulkOperationsEntity original, BulkOperation bulkOperation, BulkOperationRuleCollection rules, Class<? extends BulkOperationsEntity> entityClass) {
    var processor = dataProcessorFactory.getProcessorFromFactory(entityClass);
    var processingContent = BulkOperationProcessingContent.builder()
      .bulkOperationId(bulkOperation.getId())
      .build();
    UpdatedEntityHolder<BulkOperationsEntity> updatedEntityHolder = null;
    try {
      processingContent.setIdentifier(original.getIdentifier(bulkOperation.getIdentifierType()));
      updatedEntityHolder = processor.process(original.getIdentifier(bulkOperation.getIdentifierType()), original, rules);
      processingContent.setState(StateType.PROCESSED);
    } catch (Exception e) {
      processingContent = processingContent
        .withState(StateType.FAILED)
        .withErrorMessage("Failed to modify entity, reason:" + e.getMessage());
    }
    processingContentRepository.save(processingContent);
    return updatedEntityHolder;
  }

  public BulkOperation commitBulkOperation(UUID bulkOperationId) throws BulkOperationException {
    var bulkOperation = getBulkOperationOrThrow(bulkOperationId);

    if (isEmpty(bulkOperation.getLinkToMatchedRecordsJsonFile())) {
      throw new BulkOperationException("Missing link to origin file");
    }

    bulkOperation = bulkOperationRepository.save(bulkOperation.withStatus(OperationStatusType.APPLY_CHANGES));

    if (StringUtils.isNotEmpty(bulkOperation.getLinkToModifiedRecordsJsonFile())) {
      var clazz = resolveEntityClass(bulkOperation.getEntityType());
      var operationId = bulkOperation.getId();
      var execution = executionRepository.save(BulkOperationExecution.builder()
        .bulkOperationId(operationId)
        .startTime(LocalDateTime.now())
        .processedRecords(0)
        .status(StatusType.ACTIVE)
        .build());

      var resultJsonFileName = bulkOperation.getId() + "/json/result-" + FilenameUtils.getName(bulkOperation.getLinkToMatchedRecordsJsonFile());
      var resultCsvFileName = bulkOperation.getId() + "/result-" + FilenameUtils.getName(bulkOperation.getLinkToMatchedRecordsCsvFile());

      try (var originalFileReader = new InputStreamReader(remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsJsonFile()));
           var modifiedFileReader = new InputStreamReader(remoteFileSystemClient.get(bulkOperation.getLinkToModifiedRecordsJsonFile()));
           var writerForCsvFile = remoteFileSystemClient.writer(resultCsvFileName)) {

        var originalFileIterator = objectMapper.readValues(new JsonFactory().createParser(originalFileReader), clazz);
        var modifiedFileIterator = objectMapper.readValues(new JsonFactory().createParser(modifiedFileReader), clazz);

        var strategy = new CustomMappingStrategy<BulkOperationsEntity>();
        strategy.setType(clazz);

        StatefulBeanToCsv<BulkOperationsEntity> sbc = new StatefulBeanToCsvBuilder<BulkOperationsEntity>(writerForCsvFile)
          .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
          .withApplyQuotesToAll(false)
          .withMappingStrategy(strategy)
          .build();

        int committedNumOfRecords = 0, committedNumOfErrors = 0;

        while (hasNextRecord(originalFileIterator, modifiedFileIterator)) {
          var original = originalFileIterator.next();
          var modified = modifiedFileIterator.next();
          try {
            var result = updateEntityIfNeeded(original, modified, bulkOperation, clazz);
            var hasNextRecord = hasNextRecord(originalFileIterator, modifiedFileIterator);
            remoteFileSystemClient.append(new ByteArrayInputStream((objectMapper.writeValueAsString(result) + (hasNextRecord ? LF : EMPTY)).getBytes()), resultJsonFileName);

            // TODO Should be refactored to use open csv
            if (User.class == clazz) {
              sbc.write(result);
            }

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
          .withLinkToCommittedRecordsJsonFile(resultJsonFileName)
          .withCommittedNumOfErrors(bulkOperation.getCommittedNumOfErrors() + committedNumOfErrors)
          .withCommittedNumOfRecords(committedNumOfRecords);

        // TODO Should be refactored to use open csv
        if (User.class != clazz) {
          writerForCsvFile.write(getCsvPreviewForBulkOperation(bulkOperation, COMMIT));
        }

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

  public UnifiedTable getPreview(BulkOperation bulkOperation, BulkOperationStep step, int limit) {
    try {
      var entityClass = resolveEntityClass(bulkOperation.getEntityType());
      switch (step) {
        case UPLOAD:
          return buildPreview(bulkOperation.getLinkToMatchedRecordsJsonFile(), entityClass, limit);
        case EDIT:
          return buildPreview(bulkOperation.getLinkToModifiedForPreviewRecordsJsonFile(), entityClass, limit);
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
      log.error(e);
      return adapter.getEmptyTableWithHeaders();
    }
  }



  public String getCsvPreviewForBulkOperation(BulkOperation bulkOperation, BulkOperationStep step) {
    var table = getPreview(bulkOperation, step, Integer.MAX_VALUE);
    return table.getHeader()
      .stream()
      .map(Cell::getValue)
      .collect(Collectors.joining(",")) + LF + table.getRows()
      .stream()
      .map(this::rowToCsvLine)
      .collect(Collectors.joining(LF));
  }

  public BulkOperation startBulkOperation(UUID bulkOperationId, UUID xOkapiUserId, BulkOperationStart bulkOperationStart) {
    var step = bulkOperationStart.getStep();
    var approach = bulkOperationStart.getApproach();
    var bulkOperation = getBulkOperation(bulkOperationId, bulkOperationStart, step);
    bulkOperation.setUserId(xOkapiUserId);
    bulkOperation.setApproach(approach);

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
        if (DATA_MODIFICATION.equals(bulkOperation.getStatus()) || REVIEW_CHANGES.equals(bulkOperation.getStatus())) {
          if (MANUAL == approach) {
            applyChanges(bulkOperation);
          } else {
            deletePreviouslyCreatedArtifacts(bulkOperationId, bulkOperation);
            confirmBulkOperation(bulkOperationId);
          }
          return bulkOperation;
        } else {
          throw new BadRequestException(String.format("Step %s is not applicable for bulk operation status %s", step, bulkOperation.getStatus()));
        }
      } else if (BulkOperationStep.COMMIT == step) {
        if (REVIEW_CHANGES.equals(bulkOperation.getStatus())) {
          return commitBulkOperation(bulkOperationId);
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

  private BulkOperation getBulkOperation(UUID bulkOperationId, BulkOperationStart bulkOperationStart, BulkOperationStep step) {
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
    return bulkOperation;
  }

  private void deletePreviouslyCreatedArtifacts(UUID bulkOperationId, BulkOperation bulkOperation) {
    errorService.deleteErrorsByBulkOperationId(bulkOperationId);
    if (Objects.nonNull(bulkOperation.getLinkToModifiedRecordsJsonFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToModifiedRecordsJsonFile());
    }
    if (Objects.nonNull(bulkOperation.getLinkToModifiedRecordsCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToModifiedRecordsCsvFile());
    }
    if (Objects.nonNull(bulkOperation.getLinkToModifiedForPreviewRecordsJsonFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToModifiedForPreviewRecordsJsonFile());
    }
    bulkOperation.setCommittedNumOfErrors(0);
  }

  private BulkOperation applyChanges(BulkOperation bulkOperation) {
    var bulkOperationId = bulkOperation.getId();
    var linkToMatchedRecordsJsonFile = bulkOperation.getLinkToMatchedRecordsJsonFile();
    var linkToModifiedCsvFile = bulkOperation.getLinkToModifiedRecordsCsvFile();
    var linkToModifiedRecordsJson = bulkOperationId + "/json/modified-" + FilenameUtils.getName(linkToMatchedRecordsJsonFile);
    var linkToModifiedForPreviewRecordsJson = bulkOperationId + "/json/preview-" + FilenameUtils.getName(linkToMatchedRecordsJsonFile);

    try (Reader originalFileReader = new InputStreamReader(remoteFileSystemClient.get(linkToMatchedRecordsJsonFile));
         Reader modifiedFileReader = new InputStreamReader(remoteFileSystemClient.get(linkToModifiedCsvFile));
         Writer modifiedRecordsJsonFileWriter = remoteFileSystemClient.writer(linkToModifiedRecordsJson)) {

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

        remoteFileSystemClient.append(new ByteArrayInputStream((objectMapper.writeValueAsString(modifiedEntity) + LF).getBytes()), linkToModifiedForPreviewRecordsJson);

        if (EqualsBuilder.reflectionEquals(originalEntity, modifiedEntity, true, entityType, "metadata", "createdDate", "updatedDate")) {
          committedNumOfErrors++;
          errorService.saveError(bulkOperationId, originalEntity.getIdentifier(bulkOperation.getIdentifierType()), "No change in value required");
        } else {
          modifiedRecordsJsonFileWriter.write(objectMapper.writeValueAsString(modifiedEntity));
        }
      }

      bulkOperation.setCommittedNumOfErrors(committedNumOfErrors);
      bulkOperation.setStatus(REVIEW_CHANGES);
      bulkOperation.setLinkToModifiedRecordsJsonFile(linkToModifiedRecordsJson);
      bulkOperation.setLinkToModifiedForPreviewRecordsJsonFile(linkToModifiedForPreviewRecordsJson);
      bulkOperationRepository.save(bulkOperation);
      return bulkOperation;

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
      .map(Utils::prepareForCsv)
      .collect(Collectors.joining(","));
  }
}
