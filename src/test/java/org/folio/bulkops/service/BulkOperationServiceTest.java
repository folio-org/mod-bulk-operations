package org.folio.bulkops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.BulkEditClient;
import org.folio.bulkops.client.DataExportSpringClient;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.bean.IdentifierType;
import org.folio.bulkops.domain.bean.Job;
import org.folio.bulkops.domain.bean.JobStatus;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.util.UUID;

class BulkOperationServiceTest extends BaseTest {
  @Autowired
  private BulkOperationService bulkOperationService;

  @MockBean
  private BulkOperationRepository bulkOperationRepository;

  @MockBean
  private DataExportSpringClient dataExportSpringClient;

  @MockBean
  private BulkEditClient bulkEditClient;

  @Test
  @SneakyThrows
  void shouldUploadIdentifiers() {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/barcodes.csv").readAllBytes());

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(UUID.randomUUID()).build());

    var jobId = UUID.randomUUID();
    when(dataExportSpringClient.upsertJob(any(Job.class)))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.SCHEDULED).build());

    when(dataExportSpringClient.getJob(jobId))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.IN_PROGRESS).build());

    when(bulkEditClient.uploadFile(eq(jobId), any(MultipartFile.class)))
      .thenReturn("3");

    bulkOperationService.uploadIdentifiers(EntityType.USER, IdentifierType.BARCODE, file);

    verify(dataExportSpringClient).upsertJob(any(Job.class));
    verify(dataExportSpringClient).getJob(jobId);
    verify(bulkEditClient).uploadFile(jobId, file);
    verify(bulkEditClient, times(0)).startJob(jobId);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.NEW, operationCaptor.getAllValues().get(0).getStatus());
    assertEquals(OperationStatusType.RETRIEVING_RECORDS, operationCaptor.getAllValues().get(1).getStatus());
  }

  @Test
  @SneakyThrows
  void shouldUploadIdentifiersAndStartJobIfJobWasNotStarted() {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/barcodes.csv").readAllBytes());

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(UUID.randomUUID()).build());

    var jobId = UUID.randomUUID();
    when(dataExportSpringClient.upsertJob(any(Job.class)))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.SCHEDULED).build());

    when(dataExportSpringClient.getJob(jobId))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.SCHEDULED).build());

    when(bulkEditClient.uploadFile(eq(jobId), any(MultipartFile.class)))
      .thenReturn("3");

    bulkOperationService.uploadIdentifiers(EntityType.USER, IdentifierType.BARCODE, file);

    verify(dataExportSpringClient).upsertJob(any(Job.class));
    verify(dataExportSpringClient).getJob(jobId);
    verify(bulkEditClient).uploadFile(jobId, file);
    verify(bulkEditClient).startJob(jobId);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.NEW, operationCaptor.getAllValues().get(0).getStatus());
    assertEquals(OperationStatusType.RETRIEVING_RECORDS, operationCaptor.getAllValues().get(1).getStatus());
  }

  @ParameterizedTest
  @EnumSource(value = JobStatus.class, names = { "FAILED", "SCHEDULED" }, mode = EnumSource.Mode.INCLUDE)
  @SneakyThrows
  void shouldFailOperationWhenDataExportJobFails(JobStatus jobStatus) {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/barcodes.csv").readAllBytes());

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(UUID.randomUUID()).build());

    var jobId = UUID.randomUUID();
    when(dataExportSpringClient.upsertJob(any(Job.class)))
      .thenReturn(Job.builder().id(jobId).status(jobStatus).build());

    when(dataExportSpringClient.getJob(jobId))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.FAILED).build());

    bulkOperationService.uploadIdentifiers(EntityType.USER, IdentifierType.BARCODE, file);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.FAILED, operationCaptor.getAllValues().get(1).getStatus());
  }
}
