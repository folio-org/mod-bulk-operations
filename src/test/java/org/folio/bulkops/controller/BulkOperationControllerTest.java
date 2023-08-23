package org.folio.bulkops.controller;

import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.domain.dto.IdentifierType.BARCODE;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationType.UPDATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.FileContentType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.BulkOperationService;
import org.folio.bulkops.service.ListUsersService;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class BulkOperationControllerTest extends BaseTest {

  @MockBean
  private BulkOperationService bulkOperationService;
  @MockBean
  private RemoteFileSystemClient remoteFileSystemClient;

  @MockBean
  private ListUsersService listUsersService;

  @Autowired
  private BulkOperationRepository bulkOperationRepository;

  @ParameterizedTest
  @EnumSource(value = FileContentType.class)
  void shouldDownloadFileWithPreview(FileContentType type) throws Exception {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
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

  @Test
  void shouldHaveHrIdWhenGetBulkOperationCollection() throws Exception {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      IntStream.range(0, 5).forEach(i -> bulkOperationRepository.save(BulkOperation.builder()
        .id(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .operationType(UPDATE)
        .entityType(USER)
        .identifierType(BARCODE)
        .status(NEW)
        .dataExportJobId(UUID.randomUUID())
        .totalNumOfRecords(1)
        .processedNumOfRecords(0)
        .executionChunkSize(5)
        .startTime(LocalDateTime.now())
        .build()));
      mockMvc.perform(get("/bulk-operations?query=(entityType==\"USER\") sortby startTime/sort.descending&limit=50&offset=0")
          .headers(defaultHeaders())
          .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(result -> {
          var bulkOperations = new JSONObject(result.getResponse().getContentAsString()).getJSONArray("bulkOperations");
          for (int i = 0; i < bulkOperations.length(); i++) {
            var bulkOperation = bulkOperations.getJSONObject(i);
            assertThat(bulkOperation.getInt("hrId") > 0);
          }
        });
    }
  }

  @Test
  void shouldReturnListUsers() throws Exception {
    var userIds = new UUID[]{UUID.randomUUID(), UUID.randomUUID()};
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      IntStream.range(0, 2).forEach(i -> bulkOperationRepository.save(BulkOperation.builder()
        .id(UUID.randomUUID())
        .userId(userIds[i])
        .build()));
      when(userClient.getUserById(userIds[0].toString()))
        .thenReturn(new User().withId(userIds[0].toString()).withPersonal(new Personal().withFirstName("Test")
          .withLastName("Test")));
      when(userClient.getUserById(userIds[1].toString()))
        .thenReturn(new User().withId(userIds[0].toString()).withPersonal(new Personal().withFirstName("Test")
          .withLastName("Test")));
      when(listUsersService.getListUsers(anyString(), anyInt(), anyInt()))
        .thenReturn(new org.folio.bulkops.domain.dto.Users().users(List.of(
            new org.folio.bulkops.domain.dto.User().id(UUID.randomUUID()).firstName("fname").lastName("lname"),
            new org.folio.bulkops.domain.dto.User().id(UUID.randomUUID()).firstName("fname 1").lastName("lname 1")))
          .totalRecords(2));
      mockMvc.perform(get("/bulk-operations/list-users?query=(entityType==\"USER\") sortby startTime/sort.descending&limit=50&offset=0")
          .headers(defaultHeaders())
          .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(result -> {
          var listUsers = new JSONObject(result.getResponse().getContentAsString());
          assertThat(listUsers.getInt("total_records") == 2);
          assertThat(listUsers.getJSONArray("users").length() == 2);
        });
    }
  }
}
