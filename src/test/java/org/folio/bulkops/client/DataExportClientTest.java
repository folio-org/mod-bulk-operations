package org.folio.bulkops.client;

import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.pojo.Job;
import org.folio.bulkops.domain.pojo.JobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;

import static java.util.Objects.isNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExportClientTest {

  @Mock
  private DataExportClient dataExportClient;

  private static final String HOLDINGS_HRIDS = "src/test/resources/dataExportClient/holdings_hrids.csv";
  private static final String ITEMS_BARCODES = "src/test/resources/dataExportClient/items_barcodes.csv";
  private static final String USERS_UUIDS = "src/test/resources/dataExportClient/users_uuids.csv";

  @SneakyThrows
  @Test
  void shouldUploadFileWithHoldingsHRIDs() {
    when(dataExportClient.uploadFile(any(UUID.class), argThat(file -> file.getName().equals("holdings_hrids"))))
      .thenReturn("3");
    var actualNumOfLines = dataExportClient.uploadFile(UUID.randomUUID(), readFile(HOLDINGS_HRIDS));
    var expectedNumOfLines = "3";
    assertEquals(expectedNumOfLines, actualNumOfLines);
  }

  @SneakyThrows
  @Test
  void shouldUploadFileWithItemsBarcodes() {
    when(dataExportClient.uploadFile(any(UUID.class), argThat(file -> file.getName().equals("items_barcodes"))))
      .thenReturn("7");
    var actualNumOfLines = dataExportClient.uploadFile(UUID.randomUUID(), readFile(ITEMS_BARCODES));
    var expectedNumOfLines = "7";
    assertEquals(expectedNumOfLines, actualNumOfLines);
  }

  @SneakyThrows
  @Test
  void shouldUploadFileWithUsersUUIDs() {
    when(dataExportClient.uploadFile(any(UUID.class), argThat(file -> file.getName().equals("users_uuids"))))
      .thenReturn("5");
    var actualNumOfLines = dataExportClient.uploadFile(UUID.randomUUID(), readFile(USERS_UUIDS));
    var expectedNumOfLines = "5";
    assertEquals(expectedNumOfLines, actualNumOfLines);
  }

  @Test
  void shouldStartJob() {
    final Job newJob = new Job();
    when(dataExportClient.getJob(any(UUID.class))).then(e -> newJob);
    when(dataExportClient.startJob(any(UUID.class)))
      .then(e -> {
        var argumentCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(dataExportClient).startJob(argumentCaptor.capture());
        var job = dataExportClient.getJob(argumentCaptor.getValue());
        job.setStartTime(new Date());
        job.setStatus(JobStatus.SCHEDULED);
        return StringUtils.EMPTY;
      });
    when(dataExportClient.upsertJob(any(Job.class))).then(e -> {
      if (isNull(newJob.getId())) {
        newJob.setId(UUID.randomUUID());
      }
      var argumentCaptor = ArgumentCaptor.forClass(Job.class);
      verify(dataExportClient).upsertJob(argumentCaptor.capture());
      var capturedJob = argumentCaptor.getValue();
      newJob.setStatus(capturedJob.getStatus());
      newJob.setStartTime(capturedJob.getStartTime());
      return capturedJob;
    });
    var upsertedJob = dataExportClient.upsertJob(newJob);
    dataExportClient.startJob(upsertedJob.getId());
    var actualStatus = dataExportClient.getJob(upsertedJob.getId()).getStatus();
    var expectedStatus = JobStatus.SCHEDULED;
    assertEquals(expectedStatus, actualStatus);
    assertNotNull(newJob.getStartTime());
  }

  private MultipartFile readFile(String fileName) throws IOException {
    byte [] content = Files.readAllBytes(Paths.get(fileName));
    return new MockMultipartFile(FilenameUtils.getBaseName(fileName), content);
  }
}
