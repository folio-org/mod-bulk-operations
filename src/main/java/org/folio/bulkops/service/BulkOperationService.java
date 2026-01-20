package org.folio.bulkops.service;

import static com.opencsv.ICSVWriter.DEFAULT_SEPARATOR;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.folio.bulkops.batch.JobCommandHelper.prepareJobParameters;
import static org.folio.bulkops.domain.dto.ApproachType.IN_APP;
import static org.folio.bulkops.domain.dto.ApproachType.MANUAL;
import static org.folio.bulkops.domain.dto.ApproachType.QUERY;
import static org.folio.bulkops.domain.dto.BulkOperationStep.UPLOAD;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE_MARC;
import static org.folio.bulkops.domain.dto.IdentifierType.HRID;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION_IN_PROGRESS;
import static org.folio.bulkops.domain.dto.OperationStatusType.EXECUTING_QUERY;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_RECORDS;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.SAVING_RECORDS_LOCALLY;
import static org.folio.bulkops.util.Constants.BULK_EDIT_IDENTIFIERS;
import static org.folio.bulkops.util.Constants.CHANGED_CSV_PATH_TEMPLATE;
import static org.folio.bulkops.util.Constants.ERROR_COMMITTING_FILE_NAME_PREFIX;
import static org.folio.bulkops.util.Constants.ERROR_MATCHING_FILE_NAME_PREFIX;
import static org.folio.bulkops.util.Constants.FILE_UPLOAD_ERROR;
import static org.folio.bulkops.util.Constants.HYPHEN;
import static org.folio.bulkops.util.Constants.MARC;
import static org.folio.bulkops.util.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.bulkops.util.ErrorCode.ERROR_MESSAGE_PATTERN;
import static org.folio.bulkops.util.ErrorCode.ERROR_NOT_CONFIRM_CHANGES_S3_ISSUE;
import static org.folio.bulkops.util.ErrorCode.ERROR_UPLOAD_IDENTIFIERS_S3_ISSUE;
import static org.folio.bulkops.util.Utils.resolveEntityClass;
import static org.folio.bulkops.util.Utils.resolveExtendedEntityClass;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;
import static org.folio.spring.utils.FolioExecutionContextUtils.prepareContextForTenant;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.batch.ExportJobManagerSync;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.bean.StatusType;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationStart;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.DataImportJobExecution;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.dto.QueryRequest;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationDataProcessing;
import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.BadRequestException;
import org.folio.bulkops.exception.IllegalOperationStateException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.OptimisticLockingException;
import org.folio.bulkops.exception.ServerErrorException;
import org.folio.bulkops.exception.WritePermissionDoesNotExist;
import org.folio.bulkops.processor.UpdatedEntityHolder;
import org.folio.bulkops.processor.folio.DataProcessorFactory;
import org.folio.bulkops.processor.marc.MarcInstanceDataProcessor;
import org.folio.bulkops.repository.BulkOperationDataProcessingRepository;
import org.folio.bulkops.repository.BulkOperationExecutionRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.BulkOperationsEntityCsvWriter;
import org.folio.bulkops.util.CsvHelper;
import org.folio.bulkops.util.IdentifiersResolver;
import org.folio.bulkops.util.MarcCsvHelper;
import org.folio.bulkops.util.Utils;
import org.folio.s3.exception.S3ClientException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.marc4j.MarcStreamReader;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Log4j2
@RequiredArgsConstructor
public class BulkOperationService {
  public static final String FILE_UPLOADING_FAILED = "File uploading failed";
  public static final String STEP_IS_NOT_APPLICABLE_FOR_BULK_OPERATION_STATUS =
      "Step %s is not applicable for bulk operation status %s";
  public static final String ERROR_STARTING_BULK_OPERATION = "Error starting Bulk Operation";
  public static final String MSG_BULK_EDIT_SUPPORTED_FOR_MARC_ONLY =
      "Instance with source %s is not supported by MARC records bulk edit "
          + "and cannot be updated.";
  public static final String MSG_CONFIRM_FAILED = "Confirm failed";

  private final BulkOperationRepository bulkOperationRepository;
  private final RuleService ruleService;
  private final BulkOperationDataProcessingRepository dataProcessingRepository;
  private final BulkOperationExecutionRepository executionRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final ObjectMapper objectMapper;
  private final DataProcessorFactory dataProcessorFactory;
  private final ErrorService errorService;
  private final LogFilesService logFilesService;
  private final RecordUpdateService recordUpdateService;
  private final EntityTypeService entityTypeService;
  private final MarcInstanceDataProcessor marcInstanceDataProcessor;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final ConsortiaService consortiaService;
  private final UserClient userClient;
  private final MarcUpdateService marcUpdateService;
  private final MetadataProviderService metadataProviderService;
  private final SrsService srsService;
  private final MarcCsvHelper marcCsvHelper;
  private final BulkOperationServiceHelper bulkOperationServiceHelper;
  private final QueryService queryService;
  private final ExportJobManagerSync exportJobManagerSync;
  private final List<Job> jobs;
  private static final int OPERATION_UPDATING_STEP = 100;
  private static final String MODIFIED_JSON_PATH_TEMPLATE = "%s/json/%s-Modified-%s.json";
  private static final String PREVIEW_CSV_PATH_TEMPLATE = "%s/%s-Updates-Preview-CSV-%s.csv";
  private static final String PREVIEW_JSON_PATH_TEMPLATE =
      "%s/json/%s-Updates-Preview-JSON-%s.json";
  private static final String PREVIEW_MARC_PATH_TEMPLATE = "%s/%s-Updates-Preview-MARC-%s.mrc";
  private static final String PREVIEW_MARC_CSV_PATH_TEMPLATE =
      "%s/%s-Updates-Preview-MARC-CSV-%s.csv";
  private static final String CHANGED_JSON_PATH_TEMPLATE = "%s/json/%s-Changed-Records-%s.json";
  private static final String CHANGED_JSON_PREVIEW_PATH_TEMPLATE =
      "%s/json/%s-Changed-Records-Preview-%s.json";

  public static final String TMP_MATCHED_JSON_PATH_TEMPLATE = "%s/json/tmp-matched.json";

  private final ExecutorService executor = Executors.newCachedThreadPool();

  @Value("${application.fqm-query-approach}")
  private boolean fqmQueryApproach;

  public BulkOperation uploadCsvFile(
      EntityType entityType,
      IdentifierType identifierType,
      boolean manual,
      UUID operationId,
      UUID xokapiUserId,
      MultipartFile multipartFile) {
    BulkOperation operation;

    if (manual && operationId == null) {
      throw new NotFoundException(
          "File uploading failed, reason: query parameter operationId is "
              + "required for csv approach");
    } else if (manual) {
      operation =
          bulkOperationRepository
              .findById(operationId)
              .orElseThrow(
                  () -> new NotFoundException("Bulk operation was not found by id=" + operationId));
      operation.setApproach(MANUAL);
    } else {
      operation =
          bulkOperationRepository.save(
              BulkOperation.builder()
                  .id(UUID.randomUUID())
                  .entityType(entityType)
                  .identifierType(identifierType)
                  .status(NEW)
                  .approach(IN_APP)
                  .startTime(LocalDateTime.now())
                  .build());
      if (multipartFile.isEmpty()) {
        handleException(operation, FILE_UPLOAD_ERROR.formatted("file is empty"));
      }
    }

    try {
      if (manual) {
        var linkToThePreviewFile =
            remoteFileSystemClient.put(
                multipartFile.getInputStream(),
                String.format(
                    PREVIEW_CSV_PATH_TEMPLATE,
                    operation.getId(),
                    LocalDate.now(),
                    FilenameUtils.getBaseName(operation.getLinkToTriggeringCsvFile())));
        operation.setLinkToModifiedRecordsCsvFile(linkToThePreviewFile);
        var numOfLines = remoteFileSystemClient.getNumOfLines(linkToThePreviewFile) - 1;
        if (operation.getTotalNumOfRecords() == 0) {
          operation.setTotalNumOfRecords(numOfLines);
        }
        operation.setProcessedNumOfRecords(numOfLines);
        operation.setMatchedNumOfRecords(numOfLines);
        operation.setApproach(MANUAL);
      } else {
        var bomInputStream =
            BOMInputStream.builder().setInputStream(multipartFile.getInputStream()).get();
        var linkToTriggeringFile =
            remoteFileSystemClient.put(
                bomInputStream, operation.getId() + "/" + multipartFile.getOriginalFilename());
        operation.setLinkToTriggeringCsvFile(linkToTriggeringFile);
      }
    } catch (S3ClientException e) {
      handleException(operation, ERROR_UPLOAD_IDENTIFIERS_S3_ISSUE, e);
    } catch (Exception e) {
      handleException(operation, FILE_UPLOADING_FAILED, e);
    }

    operation.setUserId(xokapiUserId);
    return bulkOperationRepository.save(operation);
  }

  public BulkOperation triggerByQuery(UUID userId, QueryRequest queryRequest) {
    var operation = saveQueryBulkOperation(userId, queryRequest);
    if (fqmQueryApproach) {
      log.info("Starting QUERY, FQM is used for data retrieving: true");
      return queryService.retrieveRecordsQueryFlowAsync(operation);
    } else {
      log.info("Starting QUERY, FQM is used for data retrieving: false");
      queryService.saveIdentifiers(operation);
      return startBulkOperation(operation.getId(), userId, new BulkOperationStart().step(UPLOAD));
    }
  }

  private BulkOperation saveQueryBulkOperation(UUID userId, QueryRequest queryRequest) {
    return bulkOperationRepository.save(
        BulkOperation.builder()
            .id(UUID.randomUUID())
            .entityType(
                entityTypeService.getBulkOpsEntityTypeByFqmEntityTypeId(
                    queryRequest.getEntityTypeId()))
            .approach(QUERY)
            .identifierType(IdentifierType.ID)
            .status(EXECUTING_QUERY)
            .startTime(LocalDateTime.now())
            .userId(userId)
            .fqlQuery(queryRequest.getFqlQuery())
            .fqlQueryId(queryRequest.getQueryId())
            .userFriendlyQuery(queryRequest.getUserFriendlyQuery())
            .entityTypeId(queryRequest.getEntityTypeId())
            .build());
  }

  public void confirm(BulkOperationDataProcessing dataProcessing) {
    var operationId = dataProcessing.getBulkOperationId();
    var operation = getBulkOperationOrThrow(operationId);

    var ruleCollection = ruleService.getRules(dataProcessing.getBulkOperationId());
    var clazz = resolveEntityClass(operation.getEntityType());
    var extendedClazz = resolveExtendedEntityClass(operation.getEntityType());

    var triggeringFileName = FilenameUtils.getBaseName(operation.getLinkToTriggeringCsvFile());
    var modifiedJsonFileName =
        String.format(
            MODIFIED_JSON_PATH_TEMPLATE, operationId, LocalDate.now(), triggeringFileName);
    var modifiedJsonPreviewFileName =
        String.format(PREVIEW_JSON_PATH_TEMPLATE, operationId, LocalDate.now(), triggeringFileName);
    var modifiedPreviewCsvFileName =
        String.format(PREVIEW_CSV_PATH_TEMPLATE, operationId, LocalDate.now(), triggeringFileName);

    try (var readerForMatchedJsonFile =
            remoteFileSystemClient.get(operation.getLinkToMatchedRecordsJsonFile());
        var writerForModifiedPreviewCsvFile =
            remoteFileSystemClient.writer(modifiedPreviewCsvFileName);
        var writerForModifiedJsonFile = remoteFileSystemClient.writer(modifiedJsonFileName);
        var writerForModifiedJsonPreviewFile =
            remoteFileSystemClient.writer(modifiedJsonPreviewFileName)) {

      var csvWriter = new BulkOperationsEntityCsvWriter(writerForModifiedPreviewCsvFile, clazz);

      var iterator =
          objectMapper.readValues(
              new JsonFactory().createParser(readerForMatchedJsonFile), extendedClazz);

      var processedNumOfRecords = 0;

      while (iterator.hasNext()) {
        var original = iterator.next();
        if (INSTANCE_MARC.equals(operation.getEntityType())
            && original instanceof ExtendedInstance extendedInstance
            && !MARC.equals(extendedInstance.getEntity().getSource())) {
          var instance = extendedInstance.getEntity();
          var identifier =
              HRID.equals(operation.getIdentifierType()) ? instance.getHrid() : instance.getId();
          errorService.saveError(
              operation.getId(),
              identifier,
              MSG_BULK_EDIT_SUPPORTED_FOR_MARC_ONLY.formatted(instance.getSource()),
              ErrorType.ERROR);
          continue;
        }
        var modified = processUpdate(original, operation, ruleCollection, extendedClazz);
        List<BulkOperationExecutionContent> bulkOperationExecutionContents = new ArrayList<>();
        if (Objects.nonNull(modified)) {
          // Prepare CSV for download and JSON for preview
          if (isMemberTenant(folioExecutionContext.getTenantId()) || clazz == User.class) {
            CsvHelper.writeBeanToCsv(
                operation,
                csvWriter,
                modified.getPreview().getRecordBulkOperationEntity(),
                bulkOperationExecutionContents);
            writerForModifiedJsonPreviewFile.write(
                objectMapper.writeValueAsString(modified.getPreview()) + LF);
          } else {
            var tenantIdOfEntity = modified.getPreview().getTenant();
            try (var ignored =
                new FolioExecutionContextSetter(
                    prepareContextForTenant(
                        tenantIdOfEntity, folioModuleMetadata, folioExecutionContext))) {
              modified.getPreview().setTenantToNotes(operation.getTenantNotePairs());
              CsvHelper.writeBeanToCsv(
                  operation,
                  csvWriter,
                  modified.getPreview().getRecordBulkOperationEntity(),
                  bulkOperationExecutionContents);
              writerForModifiedJsonPreviewFile.write(
                  objectMapper.writeValueAsString(modified.getPreview()) + LF);
            }
          }
          var modifiedRecord = objectMapper.writeValueAsString(modified.getUpdated()) + LF;
          bulkOperationExecutionContents.forEach(errorService::saveError);
          writerForModifiedJsonFile.write(modifiedRecord);
        }

        processedNumOfRecords++;
      }

      if (processedNumOfRecords > 0) {
        operation.setLinkToModifiedRecordsCsvFile(modifiedPreviewCsvFileName);
        operation.setLinkToModifiedRecordsJsonFile(modifiedJsonFileName);
        operation.setLinkToModifiedRecordsJsonPreviewFile(modifiedJsonPreviewFileName);
        saveLinks(operation);
      }

      dataProcessing.setStatus(StatusType.COMPLETED);
      dataProcessing.setProcessedNumOfRecords(processedNumOfRecords);
      dataProcessing.setEndTime(LocalDateTime.now());
    } catch (S3ClientException e) {
      handleException(operation.getId(), dataProcessing, ERROR_NOT_CONFIRM_CHANGES_S3_ISSUE, e);
    } catch (Exception e) {
      handleException(operation.getId(), dataProcessing, MSG_CONFIRM_FAILED, e);
    } finally {
      dataProcessingRepository.save(dataProcessing);
      handleProcessingCompletion(operationId);
    }
  }

  public void confirmForInstanceMarc(BulkOperationDataProcessing dataProcessing) {
    var operationId = dataProcessing.getBulkOperationId();
    var operation = getBulkOperationOrThrow(operationId);

    var ruleCollection = ruleService.getMarcRules(dataProcessing.getBulkOperationId());
    var processedNumOfRecords = 0;
    if (nonNull(operation.getLinkToMatchedRecordsMarcFile())) {
      var triggeringFileName = FilenameUtils.getBaseName(operation.getLinkToTriggeringCsvFile());
      var modifiedMarcFileName =
          String.format(
              PREVIEW_MARC_PATH_TEMPLATE, operationId, LocalDate.now(), triggeringFileName);
      var previewMarcCsvFileName =
          String.format(
              PREVIEW_MARC_CSV_PATH_TEMPLATE, operationId, LocalDate.now(), triggeringFileName);
      try (var writerForModifiedPreviewMarcFile =
              remoteFileSystemClient.marcWriter(modifiedMarcFileName);
          var csvWriter =
              new CSVWriterBuilder(remoteFileSystemClient.writer(previewMarcCsvFileName))
                  .withSeparator(DEFAULT_SEPARATOR)
                  .build();
          var linkToMatchedRecordsMarcFileStream =
              remoteFileSystemClient.get(operation.getLinkToMatchedRecordsMarcFile())) {
        var matchedRecordsReader = new MarcStreamReader(linkToMatchedRecordsMarcFileStream);
        var currentDate = new Date();
        while (matchedRecordsReader.hasNext()) {
          var marcRecord = matchedRecordsReader.next();
          if (!ruleCollection.getBulkOperationMarcRules().isEmpty()) {
            marcInstanceDataProcessor.update(operation, marcRecord, ruleCollection, currentDate);
          }
          writerForModifiedPreviewMarcFile.writeRecord(marcRecord);
          var data = marcCsvHelper.getModifiedDataForCsv(marcRecord);
          csvWriter.writeNext(data);

          processedNumOfRecords++;
        }

        if (processedNumOfRecords > 0) {
          operation.setLinkToModifiedRecordsMarcFile(modifiedMarcFileName);
          operation.setLinkToModifiedRecordsMarcCsvFile(previewMarcCsvFileName);
          saveLinks(operation);
        }

        dataProcessing.setStatus(StatusType.COMPLETED);
        dataProcessing.setEndTime(LocalDateTime.now());
        dataProcessing.setProcessedNumOfRecords(processedNumOfRecords);
      } catch (S3ClientException e) {
        handleException(operation.getId(), dataProcessing, ERROR_NOT_CONFIRM_CHANGES_S3_ISSUE, e);
      } catch (Exception e) {
        handleException(operation.getId(), dataProcessing, MSG_CONFIRM_FAILED, e);
      } finally {
        dataProcessingRepository.save(dataProcessing);
        handleProcessingCompletion(operationId);
      }
    } else {
      log.error("No link to MARC file, failing operation");
      dataProcessingRepository.save(
          dataProcessing.withStatus(StatusType.FAILED).withEndTime(LocalDateTime.now()));
      operation = getBulkOperationOrThrow(operationId);
      operation.setStatus(OperationStatusType.REVIEWED_NO_MARC_RECORDS);
      operation.setProcessedNumOfRecords(processedNumOfRecords);
      operation.setEndTime(LocalDateTime.now());
      bulkOperationRepository.save(operation);
    }
  }

  protected UpdatedEntityHolder<BulkOperationsEntity> processUpdate(
      BulkOperationsEntity original,
      BulkOperation operation,
      BulkOperationRuleCollection rules,
      Class<? extends BulkOperationsEntity> entityClass) {
    var processor = dataProcessorFactory.getProcessorFromFactory(entityClass);
    UpdatedEntityHolder<BulkOperationsEntity> modified = null;
    try {
      modified =
          processor.process(
              original.getRecordBulkOperationEntity().getIdentifier(operation.getIdentifierType()),
              original,
              rules);
    } catch (Exception e) {
      log.error("Failed to modify entity", e);
    }
    return modified;
  }

  public void commit(BulkOperation operation) {

    operation.setCommittedNumOfRecords(0);
    operation.setStatus(APPLY_CHANGES);

    var totalNumOfRecords = operation.getMatchedNumOfRecords();

    boolean hasAdministrativeRules = false;
    boolean hasMarcRules = false;

    if (INSTANCE_MARC.equals(operation.getEntityType())) {
      marcUpdateService.prepareProgress(operation);
      hasAdministrativeRules = ruleService.hasAdministrativeUpdates(operation);
      hasMarcRules = ruleService.hasMarcUpdates(operation);
      var multiplier = getProgressMultiplier(operation, hasAdministrativeRules, hasMarcRules);
      operation.setCommittedNumOfErrors(operation.getCommittedNumOfErrors() * multiplier);
      totalNumOfRecords *= multiplier;
    }

    operation.setTotalNumOfRecords(totalNumOfRecords);
    operation = bulkOperationRepository.save(operation);

    var triggeringFileName = FilenameUtils.getBaseName(operation.getLinkToTriggeringCsvFile());
    var resultCsvFileName =
        String.format(
            CHANGED_CSV_PATH_TEMPLATE, operation.getId(), LocalDate.now(), triggeringFileName);

    var failedInstanceHrids = new HashSet<String>();

    if (INSTANCE_MARC.equals(operation.getEntityType()) && !hasAdministrativeRules) {
      log.info("No administrative data updates, skipping commit");
    } else if (StringUtils.isNotEmpty(operation.getLinkToModifiedRecordsJsonFile())) {
      var entityClass = resolveEntityClass(operation.getEntityType());
      var extendedClass = resolveExtendedEntityClass(operation.getEntityType());

      var operationId = operation.getId();

      var execution =
          executionRepository.save(
              BulkOperationExecution.builder()
                  .bulkOperationId(operationId)
                  .startTime(LocalDateTime.now())
                  .processedRecords(0)
                  .status(StatusType.ACTIVE)
                  .build());

      var resultJsonFileName =
          String.format(
              CHANGED_JSON_PATH_TEMPLATE, operation.getId(), LocalDate.now(), triggeringFileName);
      var resultJsonPreviewFileName =
          String.format(
              CHANGED_JSON_PREVIEW_PATH_TEMPLATE,
              operation.getId(),
              LocalDate.now(),
              triggeringFileName);

      try (var originalFileReader =
              new InputStreamReader(
                  new BufferedInputStream(
                      remoteFileSystemClient.get(operation.getLinkToMatchedRecordsJsonFile())));
          var modifiedFileReader =
              new InputStreamReader(
                  new BufferedInputStream(
                      remoteFileSystemClient.get(operation.getLinkToModifiedRecordsJsonFile())));
          var writerForResultCsvFile = remoteFileSystemClient.writer(resultCsvFileName);
          var writerForResultJsonFile = remoteFileSystemClient.writer(resultJsonFileName);
          var writerForJsonPreviewFile = remoteFileSystemClient.writer(resultJsonPreviewFileName)) {

        var originalFileParser = new JsonFactory().createParser(originalFileReader);
        var originalFileIterator = objectMapper.readValues(originalFileParser, extendedClass);

        var modifiedFileParser = new JsonFactory().createParser(modifiedFileReader);
        var modifiedFileIterator = objectMapper.readValues(modifiedFileParser, extendedClass);

        var csvWriter = new BulkOperationsEntityCsvWriter(writerForResultCsvFile, entityClass);

        int processedNumOfRecords = 0;

        while (hasNextRecord(originalFileIterator, modifiedFileIterator)) {
          var original = originalFileIterator.next();
          if (INSTANCE_MARC.equals(operation.getEntityType())
              && original instanceof ExtendedInstance extendedInstance
              && !MARC.equals(extendedInstance.getEntity().getSource())) {
            continue;
          }
          var modified = modifiedFileIterator.next();
          if (operation.getApproach() == MANUAL && original instanceof User userOriginal) {
            ((User) modified).getPersonal().setPronouns(userOriginal.getPersonal().getPronouns());
          }
          List<BulkOperationExecutionContent> bulkOperationExecutionContents = new ArrayList<>();

          processedNumOfRecords++;

          try {
            var result = recordUpdateService.updateEntity(original, modified, operation);
            var hasNextRecord = hasNextRecord(originalFileIterator, modifiedFileIterator);
            var useCurrentContext =
                isMemberTenant(folioExecutionContext.getTenantId()) || entityClass == User.class;
            var tenantIdOfEntity = result.getTenant();
            if (!useCurrentContext) {
              result.getRecordBulkOperationEntity().setTenant(tenantIdOfEntity);
              result
                  .getRecordBulkOperationEntity()
                  .setTenantToNotes(operation.getTenantNotePairs());
            }
            if (result != original) {
              try (var ignored =
                  useCurrentContext
                      ? null
                      : new FolioExecutionContextSetter(
                          prepareContextForTenant(
                              tenantIdOfEntity, folioModuleMetadata, folioExecutionContext))) {
                writerForResultJsonFile.write(
                    objectMapper.writeValueAsString(result) + getEndOfLineSymbol(hasNextRecord));
                writerForJsonPreviewFile.write(
                    objectMapper.writeValueAsString(result) + getEndOfLineSymbol(hasNextRecord));
                CsvHelper.writeBeanToCsv(
                    operation,
                    csvWriter,
                    result.getRecordBulkOperationEntity(),
                    bulkOperationExecutionContents);
              }
              bulkOperationExecutionContents.forEach(errorService::saveError);
            } else if (original instanceof ExtendedInstance extendedInstance
                && MARC.equals(extendedInstance.getEntity().getSource())) {
              writerForJsonPreviewFile.write(
                  objectMapper.writeValueAsString(result) + getEndOfLineSymbol(hasNextRecord));
            }
          } catch (OptimisticLockingException e) {
            saveFailedInstanceHrid(failedInstanceHrids, original);
            errorService.saveError(
                operationId,
                original.getIdentifier(operation.getIdentifierType()),
                e.getCsvErrorMessage(),
                e.getUiErrorMessage(),
                e.getLinkToFailedEntity(),
                ErrorType.ERROR);
          } catch (WritePermissionDoesNotExist e) {
            saveFailedInstanceHrid(failedInstanceHrids, original);
            var userName =
                userClient.getUserById(folioExecutionContext.getUserId().toString()).getUsername();
            var errorMessage =
                String.format(
                    e.getMessage(),
                    userName,
                    IdentifiersResolver.resolve(operation.getIdentifierType()),
                    original.getIdentifier(operation.getIdentifierType()));
            errorService.saveError(
                operationId,
                original.getIdentifier(operation.getIdentifierType()),
                errorMessage,
                ErrorType.ERROR);
          } catch (Exception e) {
            saveFailedInstanceHrid(failedInstanceHrids, original);
            errorService.saveError(
                operationId,
                original.getIdentifier(operation.getIdentifierType()),
                e.getMessage(),
                ErrorType.ERROR);
          }
          execution =
              execution
                  .withStatus(
                      originalFileIterator.hasNext() ? StatusType.ACTIVE : StatusType.COMPLETED)
                  .withEndTime(originalFileIterator.hasNext() ? null : LocalDateTime.now());
          if (processedNumOfRecords - execution.getProcessedRecords() > OPERATION_UPDATING_STEP) {
            execution.setProcessedRecords(processedNumOfRecords);
            execution = executionRepository.save(execution);
          }
        }

        execution.setProcessedRecords(processedNumOfRecords);
        if (operation.getCommittedNumOfRecords() > 0) {
          operation.setLinkToCommittedRecordsCsvFile(resultCsvFileName);
          operation.setLinkToCommittedRecordsJsonFile(resultJsonFileName);
          operation.setLinkToCommittedRecordsJsonPreviewFile(resultJsonPreviewFileName);
        }
      } catch (Exception e) {
        log.error("Error committing changes", e);
        execution = execution.withStatus(StatusType.FAILED).withEndTime(LocalDateTime.now());
        bulkOperationServiceHelper.failCommit(operation, e);
      }
      executionRepository.save(execution);
    }

    if (!INSTANCE_MARC.equals(operation.getEntityType()) || !hasMarcRules) {
      if (!FAILED.equals(operation.getStatus())) {
        bulkOperationServiceHelper.completeBulkOperation(operation);
      }
    } else {
      marcUpdateService.commitForInstanceMarc(operation, failedInstanceHrids);
    }
  }

  private String getEndOfLineSymbol(boolean hasNextRecord) {
    return hasNextRecord ? LF : EMPTY;
  }

  private void saveFailedInstanceHrid(
      Set<String> failedInstanceHrids, BulkOperationsEntity bulkOperationsEntity) {
    var entity = bulkOperationsEntity.getRecordBulkOperationEntity();
    if (entity instanceof Instance instance) {
      failedInstanceHrids.add(instance.getHrid());
    }
  }

  private boolean isMemberTenant(String tenantId) {
    return !consortiaService.isTenantCentral(tenantId);
  }

  public BulkOperation startBulkOperation(
      UUID bulkOperationId, UUID xokapiUserId, BulkOperationStart bulkOperationStart) {
    var step = bulkOperationStart.getStep();
    var bulkOperationApproach = bulkOperationStart.getApproach();
    BulkOperation operation =
        bulkOperationRepository
            .findById(bulkOperationId)
            .orElseThrow(
                () ->
                    new NotFoundException("Bulk operation was not found by id=" + bulkOperationId));
    operation.setUserId(xokapiUserId);

    if (UPLOAD == step) {
      var numOfLines = remoteFileSystemClient.getNumOfLines(operation.getLinkToTriggeringCsvFile());
      if (fqmQueryApproach && operation.getIdentifierType() == IdentifierType.ID) {
        log.info("Starting UPLOAD, bulk operation id={}, FQM is used: true", bulkOperationId);
        operation.setTotalNumOfRecords(numOfLines);
        try (InputStream is = remoteFileSystemClient.get(operation.getLinkToTriggeringCsvFile());
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
          List<UUID> ids = new ArrayList<>();
          List<BulkOperationExecutionContent> bulkOperationExecutionContents =
              Collections.synchronizedList(new ArrayList<>());
          reader
              .lines()
              .forEach(
                  id -> {
                    try {
                      id = removeStart(removeEnd(id, "\""), "\"");
                      ids.add(UUID.fromString(id));
                    } catch (Exception e) {
                      // saving invalid UUID identifiers as "No match found"
                      bulkOperationExecutionContents.add(
                          BulkOperationExecutionContent.builder()
                              .identifier(id)
                              .bulkOperationId(operation.getId())
                              .state(StateType.FAILED)
                              .errorType(ErrorType.ERROR)
                              .errorMessage(NO_MATCH_FOUND_MESSAGE)
                              .build());
                    }
                  });

          queryService.retrieveRecordsIdentifiersFlowAsync(
              ids, operation, bulkOperationExecutionContents);

        } catch (Exception e) {
          log.error(ERROR_STARTING_BULK_OPERATION, e);
          operation.setStatus(FAILED);
          operation.setErrorMessage(e.getMessage());
          operation.setEndTime(LocalDateTime.now());
          bulkOperationRepository.save(operation);
        }
        return operation;
      } else {
        log.info("Starting UPLOAD, bulk operation id={}, FQM is used: false", bulkOperationId);
        operation.setTotalNumOfRecords(numOfLines);
        operation.setStatus(RETRIEVING_RECORDS);
        executor.execute(
            getRunnableWithCurrentFolioContext(
                () -> {
                  try {
                    log.info("Launching batch job");
                    var jobLaunchRequest =
                        new JobLaunchRequest(
                            getBatchJob(operation), prepareJobParameters(operation, numOfLines));
                    exportJobManagerSync.launchJob(jobLaunchRequest);
                  } catch (JobExecutionException e) {
                    log.error(ERROR_STARTING_BULK_OPERATION, e);
                    operation.setStatus(FAILED);
                    operation.setErrorMessage(e.getMessage());
                    operation.setEndTime(LocalDateTime.now());
                    bulkOperationRepository.save(operation);
                  }
                }));
        return bulkOperationRepository.save(operation);
      }
    } else if (BulkOperationStep.EDIT == step) {
      errorService.deleteErrorsByBulkOperationId(bulkOperationId);
      operation.setCommittedNumOfErrors(0);
      operation.setCommittedNumOfWarnings(0);
      bulkOperationRepository.save(operation);
      if (DATA_MODIFICATION.equals(operation.getStatus())
          || REVIEW_CHANGES.equals(operation.getStatus())) {
        if (MANUAL == bulkOperationApproach) {
          executor.execute(getRunnableWithCurrentFolioContext(() -> apply(operation)));
        } else {
          logFilesService.removeModifiedFiles(operation);
          launchProcessing(operation);
        }
        return operation;
      } else {
        throw new BadRequestException(
            format(STEP_IS_NOT_APPLICABLE_FOR_BULK_OPERATION_STATUS, step, operation.getStatus()));
      }
    } else if (BulkOperationStep.COMMIT == step) {
      if (REVIEW_CHANGES.equals(operation.getStatus())) {
        executor.execute(getRunnableWithCurrentFolioContext(() -> commit(operation)));
        return operation;
      } else {
        throw new BadRequestException(
            format(STEP_IS_NOT_APPLICABLE_FOR_BULK_OPERATION_STATUS, step, operation.getStatus()));
      }
    } else {
      throw new IllegalOperationStateException(
          "Bulk operation cannot be started, reason: invalid state: " + operation.getStatus());
    }
  }

  private Job getBatchJob(BulkOperation bulkOperation) {
    var jobName = BULK_EDIT_IDENTIFIERS + HYPHEN + bulkOperation.getEntityType();
    return jobs.stream()
        .filter(job -> job.getName().contains(jobName))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Batch job config was not found, aborting"));
  }

  public void apply(BulkOperation operation) {
    operation.setProcessedNumOfRecords(0);
    var bulkOperationId = operation.getId();
    var linkToModifiedRecordsCsvFile = operation.getLinkToModifiedRecordsCsvFile();
    var linkToModifiedRecordsJsonFile =
        String.format(
            MODIFIED_JSON_PATH_TEMPLATE,
            bulkOperationId,
            LocalDate.now(),
            FilenameUtils.getBaseName(operation.getLinkToTriggeringCsvFile()));
    try (Reader readerForModifiedCsvFile =
            new InputStreamReader(remoteFileSystemClient.get(linkToModifiedRecordsCsvFile));
        Writer writerForModifiedJsonFile =
            remoteFileSystemClient.writer(linkToModifiedRecordsJsonFile)) {

      var clazz = resolveEntityClass(operation.getEntityType());

      CsvToBean<BulkOperationsEntity> csvToBean =
          new CsvToBeanBuilder<BulkOperationsEntity>(readerForModifiedCsvFile)
              .withType(clazz)
              .withSkipLines(1)
              .withThrowExceptions(false)
              .build();

      var modifiedCsvFileIterator = csvToBean.iterator();

      var processedNumOfRecords = 0;

      while (modifiedCsvFileIterator.hasNext()) {
        var modifiedEntity = modifiedCsvFileIterator.next();
        var modifiedEntityString =
            objectMapper.writeValueAsString(modifiedEntity)
                + (modifiedCsvFileIterator.hasNext() ? LF : EMPTY);

        writerForModifiedJsonFile.write(modifiedEntityString);
        processedNumOfRecords++;
        if (processedNumOfRecords - operation.getProcessedNumOfRecords()
            > OPERATION_UPDATING_STEP) {
          operation.setProcessedNumOfRecords(processedNumOfRecords);
          bulkOperationRepository.save(operation);
        }
      }
      csvToBean
          .getCapturedExceptions()
          .forEach(
              e ->
                  errorService.saveError(
                      operation.getId(),
                      Utils.getIdentifierForManualApproach(
                          e.getLine(), operation.getIdentifierType()),
                      e.getMessage(),
                      ErrorType.ERROR));
      csvToBean.getCapturedExceptions().clear();
      operation.setProcessedNumOfRecords(processedNumOfRecords);
      operation.setStatus(REVIEW_CHANGES);
      operation.setLinkToModifiedRecordsJsonFile(linkToModifiedRecordsJsonFile);
      bulkOperationRepository
          .findById(operation.getId())
          .ifPresent(
              op -> {
                operation.setCommittedNumOfErrors(op.getCommittedNumOfErrors());
                operation.setCommittedNumOfWarnings(op.getCommittedNumOfWarnings());
              });
    } catch (Exception e) {
      operation.setErrorMessage("Error applying changes: " + e.getCause());

      throw new ServerErrorException(e.getMessage());
    } finally {
      bulkOperationRepository.save(operation);
    }
  }

  @Transactional
  public void clearOperationProcessing(BulkOperation operation) {
    dataProcessingRepository.deleteAllByBulkOperationId(operation.getId());
    operation.setStatus(DATA_MODIFICATION);
    bulkOperationRepository.save(operation);
  }

  public BulkOperation getOperationById(UUID bulkOperationId) {
    var operation = getBulkOperationOrThrow(bulkOperationId);
    return switch (operation.getStatus()) {
      case DATA_MODIFICATION -> {
        var processing = dataProcessingRepository.findById(bulkOperationId);
        if (processing.isPresent() && StatusType.ACTIVE.equals(processing.get().getStatus())) {
          operation.setProcessedNumOfRecords(processing.get().getProcessedNumOfRecords());
        }
        yield operation;
      }
      case APPLY_CHANGES -> {
        var execution = executionRepository.findByBulkOperationId(bulkOperationId);
        if (execution.isPresent() && StatusType.ACTIVE.equals(execution.get().getStatus())) {
          var processedNumOfRecords = execution.get().getProcessedRecords();
          if (INSTANCE_MARC.equals(operation.getEntityType())) {
            operation.setProcessedNumOfRecords(
                operation.getCommittedNumOfErrors() + processedNumOfRecords);
          }
          operation.setProcessedNumOfRecords(processedNumOfRecords);
        }
        yield bulkOperationRepository.save(operation);
      }
      case APPLY_MARC_CHANGES -> {
        var executions =
            metadataProviderService.getJobExecutions(operation.getDataImportJobProfileId());
        updateBulkOperationBasedOnDataImportState(executions, operation);
        var updatedOperation = bulkOperationRepository.save(operation);
        // while commit is in progress, no download links should be available
        updatedOperation.setLinkToCommittedRecordsCsvFile(null);
        updatedOperation.setLinkToCommittedRecordsErrorsCsvFile(null);
        updatedOperation.setLinkToCommittedRecordsMarcFile(null);
        yield updatedOperation;
      }
      default -> operation;
    };
  }

  public void processDataImportResult(BulkOperation bulkOperation) {
    var executions =
        metadataProviderService.getJobExecutions(bulkOperation.getDataImportJobProfileId());
    updateBulkOperationBasedOnDataImportState(executions, bulkOperation);
    if (metadataProviderService.isDataImportJobCompleted(executions)) {
      executor.execute(
          getRunnableWithCurrentFolioContext(
              () -> {
                var logEntries =
                    metadataProviderService.getJobLogEntries(bulkOperation, executions);
                errorService.saveErrorsFromDataImport(logEntries, bulkOperation);
                var updatedIds = metadataProviderService.fetchUpdatedInstanceIds(logEntries);
                srsService.retrieveMarcInstancesFromSrs(updatedIds, bulkOperation);
              }));
    }
  }

  private void updateBulkOperationBasedOnDataImportState(
      List<DataImportJobExecution> executions, BulkOperation operation) {
    var numOfCommittedAdministrativeUpdates =
        executionRepository.findAllByBulkOperationId(operation.getId()).stream()
            .filter(execution -> StatusType.COMPLETED.equals(execution.getStatus()))
            .map(BulkOperationExecution::getProcessedRecords)
            .max(Integer::compareTo)
            .orElse(0);
    var processedNumOfRecords = metadataProviderService.calculateProgress(executions).getCurrent();
    operation.setProcessedNumOfRecords(
        operation.getCommittedNumOfErrors()
            + numOfCommittedAdministrativeUpdates
            + processedNumOfRecords);
  }

  public BulkOperation getBulkOperationOrThrow(UUID operationId) {
    return bulkOperationRepository
        .findById(operationId)
        .orElseThrow(
            () -> new NotFoundException("BulkOperation was not found by id=" + operationId));
  }

  private boolean hasNextRecord(
      MappingIterator<? extends BulkOperationsEntity> originalFileIterator,
      MappingIterator<? extends BulkOperationsEntity> modifiedFileIterator) {
    return originalFileIterator.hasNext() && modifiedFileIterator.hasNext();
  }

  public void cancelOperationById(UUID operationId) {
    var operation = getBulkOperationOrThrow(operationId);
    if (Set.of(NEW, RETRIEVING_RECORDS, SAVING_RECORDS_LOCALLY).contains(operation.getStatus())) {
      logFilesService.removeTriggeringAndMatchedRecordsFiles(operation);
    } else if (Set.of(DATA_MODIFICATION, REVIEW_CHANGES).contains(operation.getStatus())
        && MANUAL.equals(operation.getApproach())) {
      logFilesService.removeModifiedFiles(operation);
    } else {
      throw new IllegalOperationStateException(
          String.format("Operation with status %s cannot be cancelled", operation.getStatus()));
    }
    bulkOperationRepository.save(operation);
  }

  private void handleException(BulkOperation operation, String message, Exception exception) {
    log.error(message, exception);
    operation.setErrorMessage(format(ERROR_MESSAGE_PATTERN, message, exception.getMessage()));
    operation.setStatus(FAILED);
    operation.setEndTime(LocalDateTime.now());
    var linkToMatchingErrorsFile =
        errorService.uploadErrorsToStorage(
            operation.getId(), ERROR_MATCHING_FILE_NAME_PREFIX, operation.getErrorMessage());
    operation.setLinkToMatchedRecordsErrorsCsvFile(linkToMatchingErrorsFile);
  }

  private void handleException(BulkOperation operation, String message) {
    log.error(message);
    operation.setErrorMessage(message);
    operation.setStatus(FAILED);
    operation.setEndTime(LocalDateTime.now());
    var linkToMatchingErrorsFile =
        errorService.uploadErrorsToStorage(
            operation.getId(), ERROR_MATCHING_FILE_NAME_PREFIX, operation.getErrorMessage());
    operation.setLinkToMatchedRecordsErrorsCsvFile(linkToMatchingErrorsFile);
  }

  private void handleException(
      UUID operationId,
      BulkOperationDataProcessing dataProcessing,
      String message,
      Exception exception) {
    var operation = getBulkOperationOrThrow(operationId);
    dataProcessingRepository.save(
        dataProcessing.withStatus(StatusType.FAILED).withEndTime(LocalDateTime.now()));
    log.error(message, exception);
    operation.setErrorMessage(format(ERROR_MESSAGE_PATTERN, message, exception.getMessage()));
    operation.setStatus(OperationStatusType.FAILED);
    operation.setEndTime(LocalDateTime.now());
    var linkToCommittingErrorsFile =
        errorService.uploadErrorsToStorage(
            operation.getId(), ERROR_COMMITTING_FILE_NAME_PREFIX, operation.getErrorMessage());
    operation.setLinkToCommittedRecordsErrorsCsvFile(linkToCommittingErrorsFile);
    bulkOperationRepository.save(operation);
  }

  private void launchProcessing(BulkOperation operation) {
    operation.setStatus(DATA_MODIFICATION_IN_PROGRESS);
    bulkOperationRepository.save(operation);

    var folioProcessing =
        dataProcessingRepository.save(
            BulkOperationDataProcessing.builder()
                .bulkOperationId(operation.getId())
                .status(StatusType.ACTIVE)
                .startTime(LocalDateTime.now())
                .totalNumOfRecords(operation.getTotalNumOfRecords())
                .processedNumOfRecords(0)
                .build());

    if (INSTANCE_MARC.equals(operation.getEntityType())) {
      var marcProcessing =
          dataProcessingRepository.save(
              BulkOperationDataProcessing.builder()
                  .bulkOperationId(operation.getId())
                  .status(StatusType.ACTIVE)
                  .startTime(LocalDateTime.now())
                  .totalNumOfRecords(operation.getTotalNumOfRecords())
                  .processedNumOfRecords(0)
                  .build());
      executor.execute(
          getRunnableWithCurrentFolioContext(() -> confirmForInstanceMarc(marcProcessing)));
    }
    executor.execute(getRunnableWithCurrentFolioContext(() -> confirm(folioProcessing)));
  }

  private synchronized void handleProcessingCompletion(UUID operationId) {
    bulkOperationRepository
        .findById(operationId)
        .ifPresent(
            operation -> {
              if (DATA_MODIFICATION_IN_PROGRESS.equals(operation.getStatus())) {
                var processingList =
                    dataProcessingRepository.findAllByBulkOperationId(operation.getId());
                if (isCompletedSuccessfully(processingList)) {
                  var processedNumOfRecords =
                      processingList.stream()
                          .map(BulkOperationDataProcessing::getProcessedNumOfRecords)
                          .mapToInt(v -> v)
                          .max()
                          .orElseThrow(
                              () ->
                                  new IllegalStateException(
                                      "Failed to get processed num of records"));
                  operation.setApproach(IN_APP);
                  operation.setStatus(OperationStatusType.REVIEW_CHANGES);
                  operation.setProcessedNumOfRecords(processedNumOfRecords);
                  bulkOperationRepository.save(operation);
                  log.info(
                      "Bulk operation id={} confirm processing completed successfully",
                      operation.getId());
                }
              } else if (FAILED.equals(operation.getStatus())) {
                log.info("Bulk operation id={} failed, clearing modified files", operation.getId());
                logFilesService.removeModifiedFiles(operation);
              }
            });
  }

  private synchronized void saveLinks(BulkOperation source) {
    var dest = getBulkOperationOrThrow(source.getId());
    if (nonNull(source.getLinkToModifiedRecordsCsvFile())) {
      dest.setLinkToModifiedRecordsCsvFile(source.getLinkToModifiedRecordsCsvFile());
    }
    if (nonNull(source.getLinkToModifiedRecordsJsonFile())) {
      dest.setLinkToModifiedRecordsJsonFile(source.getLinkToModifiedRecordsJsonFile());
    }
    if (nonNull(source.getLinkToModifiedRecordsJsonPreviewFile())) {
      dest.setLinkToModifiedRecordsJsonPreviewFile(
          source.getLinkToModifiedRecordsJsonPreviewFile());
    }
    if (nonNull(source.getLinkToModifiedRecordsMarcFile())) {
      dest.setLinkToModifiedRecordsMarcFile(source.getLinkToModifiedRecordsMarcFile());
    }
    if (nonNull(source.getLinkToModifiedRecordsMarcCsvFile())) {
      dest.setLinkToModifiedRecordsMarcCsvFile(source.getLinkToModifiedRecordsMarcCsvFile());
    }
    bulkOperationRepository.save(dest);
  }

  private boolean isCompletedSuccessfully(List<BulkOperationDataProcessing> list) {
    return list.stream().allMatch(p -> StatusType.COMPLETED.equals(p.getStatus()));
  }

  private int getProgressMultiplier(
      BulkOperation operation, boolean hasAdministrativeRules, boolean hasMarcRules) {
    if (hasMarcRules && nonNull(operation.getLinkToModifiedRecordsMarcFile())) {
      return (hasAdministrativeRules && nonNull(operation.getLinkToModifiedRecordsJsonFile()))
          ? 3
          : 2;
    }
    return 1;
  }
}
