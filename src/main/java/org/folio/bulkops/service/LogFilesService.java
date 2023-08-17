package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Service
@RequiredArgsConstructor
@Log4j2
public class LogFilesService {

  private static final int MIN_DAYS_TO_CLEAR_LOG_FILES = 30;

  private final RemoteFileSystemClient remoteFileSystemClient;
  private final BulkOperationRepository bulkOperationRepository;

  public void clearLogFiles() {
    final var back30days = LocalDateTime.now(ZoneId.of("UTC")).minusDays(MIN_DAYS_TO_CLEAR_LOG_FILES);
    var bulkOperations = bulkOperationRepository.findAll();
    var oldOperations = bulkOperations.stream().filter(bulkOperation -> !bulkOperation.isExpired()
      && nonNull(bulkOperation.getEndTime()) && bulkOperation.getEndTime().isBefore(back30days)).toList();
    log.info("Found {} old bulk operations to clear files.", oldOperations.size());
    oldOperations.forEach(bulkOperation -> {
      bulkOperation.setExpired(true);
      removeFiles(bulkOperation);
      bulkOperationRepository.save(bulkOperation);
      log.info("Bulk operation with id {} is older than {} days. All files were removed.",
        bulkOperation.getId(), MIN_DAYS_TO_CLEAR_LOG_FILES);
    });
  }

  private void removeFiles(BulkOperation bulkOperation) {
    if (isNotEmpty(bulkOperation.getLinkToModifiedRecordsJsonFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToModifiedRecordsJsonFile());
      bulkOperation.setLinkToModifiedRecordsJsonFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToTriggeringCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToTriggeringCsvFile());
      bulkOperation.setLinkToTriggeringCsvFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToMatchedRecordsErrorsCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToMatchedRecordsErrorsCsvFile());
      bulkOperation.setLinkToMatchedRecordsErrorsCsvFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToCommittedRecordsJsonFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToCommittedRecordsJsonFile());
      bulkOperation.setLinkToCommittedRecordsJsonFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToMatchedRecordsCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToMatchedRecordsCsvFile());
      bulkOperation.setLinkToMatchedRecordsCsvFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToCommittedRecordsErrorsCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToCommittedRecordsErrorsCsvFile());
      bulkOperation.setLinkToCommittedRecordsErrorsCsvFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToCommittedRecordsCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToCommittedRecordsCsvFile());
      bulkOperation.setLinkToCommittedRecordsCsvFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToMatchedRecordsJsonFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToMatchedRecordsJsonFile());
      bulkOperation.setLinkToMatchedRecordsJsonFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToModifiedRecordsCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToModifiedRecordsCsvFile());
      bulkOperation.setLinkToModifiedRecordsCsvFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToPreviewRecordsJsonFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToPreviewRecordsJsonFile());
      bulkOperation.setLinkToPreviewRecordsJsonFile(null);
    }
  }
}