package org.folio.bulkops.client;

import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.folio.bulkops.domain.dto.Job;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

  // TODO //NOSONAR
  @Test
  void shouldDeleteFile() {
    when(dataExportClient.deleteFile(any(UUID.class)))
      .thenReturn(null);
    assertNull(dataExportClient.deleteFile(UUID.randomUUID()));
  }

  private MultipartFile readFile(String fileName) throws IOException {
    byte [] content = Files.readAllBytes(Paths.get(fileName));
    return new MockMultipartFile(FilenameUtils.getBaseName(fileName), content);
  }
}
