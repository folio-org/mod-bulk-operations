package org.folio.bulkops.service;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class LogFilesService {
  public static final String CSV_PATH_TEMPLATE = "%s/%s.csv";
  public static final String JSON_PATH_TEMPLATE = "%s/json/%s.json";
  private static final int MIN_DAYS_TO_CLEAR_LOG_FILES = 30;

  private final RemoteFileSystemClient remoteFileSystemClient;
  private final BulkOperationRepository bulkOperationRepository;

  public void clearLogFiles() {
    final var back30days = LocalDateTime.now(ZoneId.of("UTC")).minusDays(MIN_DAYS_TO_CLEAR_LOG_FILES);
    var bulkOperations = bulkOperationRepository.findAll();
    var oldOperations = bulkOperations.stream().filter(bulkOperation -> {
      var time = nonNull(bulkOperation.getEndTime()) ? bulkOperation.getEndTime() : bulkOperation.getStartTime();
      return !bulkOperation.isExpired() && nonNull(time) && time.isBefore(back30days);
    }).toList();
    log.info("Found {} old bulk operations to clear files.", oldOperations.size());
    oldOperations.forEach(bulkOperation -> {
      bulkOperation.setExpired(true);
      removeCommittedFiles(bulkOperation);
      bulkOperationRepository.save(bulkOperation);
      log.info("Bulk operation with id {} is older than {} days. All files were removed.",
        bulkOperation.getId(), MIN_DAYS_TO_CLEAR_LOG_FILES);
    });
  }

  public void removeCommittedFiles(BulkOperation bulkOperation) {
    removeTriggeringAndMatchedRecordsFiles(bulkOperation);
    removeModifiedFiles(bulkOperation);
    if (isNotEmpty(bulkOperation.getLinkToCommittedRecordsJsonFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToCommittedRecordsJsonFile());
      bulkOperation.setLinkToCommittedRecordsJsonFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToCommittedRecordsErrorsCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToCommittedRecordsErrorsCsvFile());
      bulkOperation.setLinkToCommittedRecordsErrorsCsvFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToCommittedRecordsCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToCommittedRecordsCsvFile());
      bulkOperation.setLinkToCommittedRecordsCsvFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToPreviewRecordsJsonFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToPreviewRecordsJsonFile());
      bulkOperation.setLinkToPreviewRecordsJsonFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToCommittedRecordsMarcFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToCommittedRecordsMarcFile());
      bulkOperation.setLinkToCommittedRecordsMarcFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToCommittedRecordsMarcCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToCommittedRecordsMarcCsvFile());
      bulkOperation.setLinkToCommittedRecordsMarcCsvFile(null);
    }
  }

  public void deleteFileByOperationIdAndName(UUID operationId, String fileName) {
    var baseName = FilenameUtils.getBaseName(fileName);
    var csvPath = format(CSV_PATH_TEMPLATE, operationId, baseName);
    var jsonPath = format(JSON_PATH_TEMPLATE, operationId, baseName);
    remoteFileSystemClient.remove(csvPath, jsonPath);
    log.info("Deleted: {}, {}", csvPath, jsonPath);
  }

  public void removeTriggeringAndMatchedRecordsFiles(BulkOperation bulkOperation) {
    log.info("Attempting to delete triggering and matched records files...");
    if (isNotEmpty(bulkOperation.getLinkToTriggeringCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToTriggeringCsvFile());
      bulkOperation.setLinkToTriggeringCsvFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToMatchedRecordsCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToMatchedRecordsCsvFile());
      bulkOperation.setLinkToMatchedRecordsCsvFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToMatchedRecordsMarcFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToMatchedRecordsMarcFile());
      bulkOperation.setLinkToMatchedRecordsMarcFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToMatchedRecordsJsonFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToMatchedRecordsJsonFile());
      bulkOperation.setLinkToMatchedRecordsJsonFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToMatchedRecordsErrorsCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToMatchedRecordsErrorsCsvFile());
      bulkOperation.setLinkToMatchedRecordsErrorsCsvFile(null);
    }
  }

  public void removeModifiedFiles(BulkOperation bulkOperation) {
    log.info("Attempting to delete modified records files...");
    if (isNotEmpty(bulkOperation.getLinkToModifiedRecordsJsonFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToModifiedRecordsJsonFile());
      bulkOperation.setLinkToModifiedRecordsJsonFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToModifiedRecordsCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToModifiedRecordsCsvFile());
      bulkOperation.setLinkToModifiedRecordsCsvFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToModifiedRecordsMarcFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToModifiedRecordsMarcFile());
      bulkOperation.setLinkToModifiedRecordsMarcFile(null);
    }
    if (isNotEmpty(bulkOperation.getLinkToModifiedRecordsMarcCsvFile())) {
      remoteFileSystemClient.remove(bulkOperation.getLinkToModifiedRecordsMarcCsvFile());
      bulkOperation.setLinkToModifiedRecordsMarcCsvFile(null);
    }
    bulkOperationRepository.save(bulkOperation);
  }
}
