package org.folio.bulkops.service;

import static java.util.Objects.nonNull;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_RECORDS;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.BulkEditClient;
import org.folio.bulkops.client.DataExportSpringClient;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.bean.ExportType;
import org.folio.bulkops.domain.bean.IdentifierType;
import org.folio.bulkops.domain.bean.Job;
import org.folio.bulkops.domain.bean.JobStatus;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@Log4j2
@RequiredArgsConstructor
public class BulkOperationService {
  private final BulkOperationRepository bulkOperationRepository;
  private final DataExportSpringClient dataExportSpringClient;
  private final BulkEditClient bulkEditClient;

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
}
