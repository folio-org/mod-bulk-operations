package org.folio.bulkops.controller;

import static org.folio.bulkops.domain.dto.EntityType.INSTANCE;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE_MARC;
import static org.folio.bulkops.domain.dto.FileContentType.COMMITTED_RECORDS_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.COMMITTING_CHANGES_ERROR_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.MATCHED_RECORDS_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.PROPOSED_CHANGES_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.RECORD_MATCHING_ERROR_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.TRIGGERING_FILE;
import static org.folio.bulkops.util.Constants.CSV_EXTENSION;
import static org.folio.bulkops.util.Constants.NON_PRINTING_DELIMITER;
import static org.folio.bulkops.util.Constants.SPLIT_NOTE_ENTITIES;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.codehaus.plexus.util.FileUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.dto.BulkOperationCollection;
import org.folio.bulkops.domain.dto.BulkOperationDto;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationStart;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.Errors;
import org.folio.bulkops.domain.dto.FileContentType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.QueryRequest;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.bulkops.domain.dto.Users;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.mapper.BulkOperationMapper;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.processor.note.NoteProcessorFactory;
import org.folio.bulkops.rest.resource.BulkOperationsApi;
import org.folio.bulkops.service.BulkOperationService;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.ListUsersService;
import org.folio.bulkops.service.LogFilesService;
import org.folio.bulkops.service.PreviewService;
import org.folio.bulkops.service.RuleService;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Log4j2
public class BulkOperationController implements BulkOperationsApi {
  private final BulkOperationService bulkOperationService;
  private final PreviewService previewService;
  private final BulkOperationMapper bulkOperationMapper;
  private final JpaCqlRepository<BulkOperation, UUID> bulkOperationCqlRepository;
  private final ErrorService errorService;
  private final RuleService ruleService;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final LogFilesService logFilesService;
  private final ListUsersService listUsersService;
  private final NoteProcessorFactory noteProcessorFactory;
  private final BulkOperationRepository bulkOperationRepository;

  @Override
  public ResponseEntity<BulkOperationCollection> getBulkOperationCollection(String query, Integer offset, Integer limit) {
    var page = bulkOperationCqlRepository.findByCql(query, OffsetRequest.of(Objects.isNull(offset) ? 0 : offset, Objects.isNull(limit) ? Integer.MAX_VALUE : limit));
    return new ResponseEntity<>(new BulkOperationCollection().bulkOperations(bulkOperationMapper.mapToDtoList(page.toList())).totalRecords((int) page.getTotalElements()), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Errors> getErrorsPreviewByOperationId(UUID operationId, Integer limit) {
    return new ResponseEntity<>(errorService.getErrorsPreviewByBulkOperationId(operationId, limit), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<UnifiedTable> getPreviewByOperationId(UUID operationId, BulkOperationStep step, Integer limit, Integer offset) {
    var bulkOperation = bulkOperationService.getBulkOperationOrThrow(operationId);
    return new ResponseEntity<>(previewService.getPreview(bulkOperation, step, Objects.isNull(offset) ? 0 : offset, limit), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<BulkOperationRuleCollection> postContentUpdates(UUID operationId, BulkOperationRuleCollection bulkOperationRuleCollection) {
    var operation = bulkOperationService.getBulkOperationOrThrow(operationId);
    var rules = ruleService.saveRules(bulkOperationRuleCollection);

    if (INSTANCE_MARC.equals(operation.getEntityType())) {
      operation.setEntityType(INSTANCE);
      bulkOperationRepository.save(operation);
    }

    bulkOperationService.clearOperationProcessing(operation);

    return ResponseEntity.ok(rules);
  }

  @Override
  public ResponseEntity<BulkOperationMarcRuleCollection> postMarcContentUpdates(UUID operationId, BulkOperationMarcRuleCollection bulkOperationMarcRuleCollection) {
    var operation = bulkOperationService.getBulkOperationOrThrow(operationId);
    operation.setEntityType(INSTANCE_MARC);
    bulkOperationRepository.save(operation);
    var rules = ruleService.saveMarcRules(bulkOperationMarcRuleCollection);

    bulkOperationService.clearOperationProcessing(operation);

    return ResponseEntity.ok(rules);
  }

  @Override
  public ResponseEntity<BulkOperationDto> startBulkOperation(UUID operationId, BulkOperationStart bulkOperationStart, UUID xOkapiUserId) {
      return new ResponseEntity<>(bulkOperationMapper.mapToDto(bulkOperationService.startBulkOperation(operationId, xOkapiUserId, bulkOperationStart)), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<BulkOperationDto> uploadCsvFile(EntityType entityType, IdentifierType identifierType, Boolean manual, UUID operationId, UUID xOkapiUserId, MultipartFile file) {
    return new ResponseEntity<>(bulkOperationMapper.mapToDto(bulkOperationService.uploadCsvFile(entityType, identifierType, manual, operationId, xOkapiUserId, file)), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<BulkOperationDto> getBulkOperationById(UUID operationId) {
    return new ResponseEntity<>(bulkOperationMapper.mapToDto(bulkOperationService.getOperationById(operationId)), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Resource> downloadFileByOperationId(
    UUID operationId, FileContentType fileContentType) {
    var bulkOperation = bulkOperationService.getOperationById(operationId);

    String path;

    if (fileContentType == TRIGGERING_FILE) {
      path = bulkOperation.getLinkToTriggeringCsvFile();
    } else if (fileContentType == MATCHED_RECORDS_FILE) {
      path = bulkOperation.getLinkToMatchedRecordsCsvFile();
    } else if (fileContentType == RECORD_MATCHING_ERROR_FILE) {
      path = bulkOperation.getLinkToMatchedRecordsErrorsCsvFile();
    } else if (fileContentType == PROPOSED_CHANGES_FILE) {
      path = INSTANCE_MARC.equals(bulkOperation.getEntityType()) ?
        bulkOperation.getLinkToModifiedRecordsMarcFile() :
        bulkOperation.getLinkToModifiedRecordsCsvFile();
    } else if (fileContentType == COMMITTED_RECORDS_FILE) {
      path = INSTANCE_MARC.equals(bulkOperation.getEntityType()) ?
        bulkOperation.getLinkToCommittedRecordsMarcFile() :
        bulkOperation.getLinkToCommittedRecordsCsvFile();
    } else if (fileContentType == COMMITTING_CHANGES_ERROR_FILE) {
      path = bulkOperation.getLinkToCommittedRecordsErrorsCsvFile();
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    if (Objects.isNull(path)) {
      return ResponseEntity.ok().build();
    } else {
      try (var is = remoteFileSystemClient.get(path)) {
        var content = CSV_EXTENSION.equalsIgnoreCase(FilenameUtils.getExtension(path)) ?
          ArrayUtils.removeAllOccurrences(is.readAllBytes(), (byte) NON_PRINTING_DELIMITER) :
          is.readAllBytes();
        var entityType = bulkOperation.getEntityType().getValue();
        if (isDownloadPreview(fileContentType) && SPLIT_NOTE_ENTITIES.contains(entityType)) {
          content = noteProcessorFactory.getNoteProcessor(entityType).processNotes(content, bulkOperation);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(content.length);
        var decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        headers.setContentDispositionFormData(FileUtils.filename(decodedPath), FileUtils.filename(decodedPath));
        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(content));
      } catch (IOException e) {
        log.error(e);
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
  }

  private boolean isDownloadPreview(FileContentType fileContentType) {
    return Set.of(MATCHED_RECORDS_FILE, PROPOSED_CHANGES_FILE, COMMITTED_RECORDS_FILE).contains(fileContentType);
  }

  @Override
  public ResponseEntity<Void> cleanUpLogFiles() {
    log.info("Cleaning up log files...");
    logFilesService.clearLogFiles();
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Users> getListUsers(String query, Integer offset, Integer limit) {
    return new ResponseEntity<>(listUsersService.getListUsers(query, offset, limit), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> deleteFileByNameAndOperationId(UUID operationId, String fileName) {
    log.info("Deleting file {} for bulk operation id={}", fileName, operationId);
    logFilesService.deleteFileByOperationIdAndName(operationId, fileName);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> cancelOperationById(UUID operationId) {
    log.info("Cancelling bulk operation id={}", operationId);
    bulkOperationService.cancelOperationById(operationId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<List<String>> getListUsedTenants(UUID operationId) {
    var bulkOperation = bulkOperationService.getOperationById(operationId);
    return new ResponseEntity<>(bulkOperation.getUsedTenants(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<BulkOperationDto> triggerBulkEditByQuery(UUID xOkapiUserId, QueryRequest queryRequest) {
    return new ResponseEntity<>(bulkOperationMapper.mapToDto(bulkOperationService.triggerByQuery(xOkapiUserId, queryRequest)), HttpStatus.OK);
  }
}
