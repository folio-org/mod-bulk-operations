package org.folio.bulkops.controller;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.dto.FileContentType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.BulkOperationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.util.UUID;

import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BulkOperationControllerTest extends BaseTest {

  @MockBean
  private BulkOperationService bulkOperationService;
  @MockBean
  private RemoteFileSystemClient remoteFileSystemClient;

  @ParameterizedTest
  @EnumSource(value = FileContentType.class)
  void shouldDownloadFileWithPreview(FileContentType type) throws Exception {
    var operationId = UUID.randomUUID();

    when(remoteFileSystemClient.get(any(String.class))).thenReturn(InputStream.nullInputStream());

    when(bulkOperationService.getOperationById(any(UUID.class))).thenReturn(BulkOperation.builder()
      .id(UUID.randomUUID())
      .linkToTriggeringCsvFile("A")
      .linkToMatchedRecordsJsonFile("B")
      .linkToMatchedRecordsCsvFile("C")
      .linkToMatchedRecordsErrorsCsvFile("D")
      .linkToModifiedRecordsJsonFile("E")
      .linkToModifiedRecordsCsvFile("F")
      .linkToCommittedRecordsJsonFile("G")
      .linkToCommittedRecordsCsvFile("H")
      .linkToCommittedRecordsErrorsCsvFile("I")
      .build());

    mockMvc.perform(get(format("/bulk-operations/%s/download?fileContentType=%s", operationId, type))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk());
  }
}
