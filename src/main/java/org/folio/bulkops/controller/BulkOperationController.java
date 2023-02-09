package org.folio.bulkops.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.codehaus.plexus.util.FileUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.dto.BulkOperationCollection;
import org.folio.bulkops.domain.dto.BulkOperationDto;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationStart;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.Errors;
import org.folio.bulkops.domain.dto.FileContentType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.mapper.BulkOperationMapper;
import org.folio.bulkops.rest.resource.BulkOperationsApi;
import org.folio.bulkops.service.BulkOperationService;
import org.folio.bulkops.service.ErrorService;
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
import java.util.Objects;
import java.util.UUID;

import static org.folio.bulkops.domain.dto.FileContentType.COMMITTED_RECORDS_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.COMMITTING_CHANGES_ERROR_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.MATCHED_RECORDS_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.PROPOSED_CHANGES_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.RECORD_MATCHING_ERROR_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.TRIGGERING_FILE;

@RestController
@RequiredArgsConstructor
@Log4j2
public class BulkOperationController implements BulkOperationsApi {
  private final BulkOperationService bulkOperationService;
  private final BulkOperationMapper bulkOperationMapper;
  private final JpaCqlRepository<BulkOperation, UUID> bulkOperationCqlRepository;
  private final ErrorService errorService;
  private final RuleService ruleService;
  private final RemoteFileSystemClient remoteFileSystemClient;

  @Override
  public ResponseEntity<BulkOperationCollection> getBulkOperationCollection(String query, Integer offset, Integer limit) {
    var page = bulkOperationCqlRepository.findByCQL(query, OffsetRequest.of(Objects.isNull(offset) ? 0 : offset, Objects.isNull(limit) ? Integer.MAX_VALUE : limit));
    return new ResponseEntity<>(new BulkOperationCollection().bulkOperations(bulkOperationMapper.mapToDtoList(page.toList())).totalRecords((int) page.getTotalElements()), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Errors> getErrorsPreviewByOperationId(UUID operationId, Integer limit) {
    return new ResponseEntity<>(errorService.getErrorsPreviewByBulkOperationId(operationId, limit), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<UnifiedTable> getPreviewByOperationId(UUID operationId, BulkOperationStep step, Integer limit) {
    var bulkOperation = bulkOperationService.getBulkOperationOrThrow(operationId);
    return new ResponseEntity<>(bulkOperationService.getPreview(bulkOperation, step, limit), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<BulkOperationRuleCollection> postContentUpdates(UUID operationId, BulkOperationRuleCollection bulkOperationRuleCollection) {
    bulkOperationService.getBulkOperationOrThrow(operationId);
    var rules = ruleService.saveRules(bulkOperationRuleCollection);
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
      path = bulkOperation.getLinkToModifiedRecordsCsvFile();
    } else if (fileContentType == COMMITTED_RECORDS_FILE) {
      path = bulkOperation.getLinkToCommittedRecordsCsvFile();
    } else if (fileContentType == COMMITTING_CHANGES_ERROR_FILE) {
      path = bulkOperation.getLinkToCommittedRecordsErrorsCsvFile();
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    if (Objects.isNull(path)) {
      return ResponseEntity.ok().build();
    } else {
      try (var is = remoteFileSystemClient.get(path)) {
        var content = is.readAllBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(content.length);
        headers.setContentDispositionFormData(FileUtils.filename(path), FileUtils.filename(path));
        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(content));
      } catch (IOException e) {
        log.error(e);
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
  }
}
