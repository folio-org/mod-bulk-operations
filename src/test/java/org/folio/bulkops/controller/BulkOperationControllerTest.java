package org.folio.bulkops.controller;

import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE_MARC;
import static org.folio.bulkops.domain.dto.EntityType.ITEM;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.domain.dto.FileContentType.COMMITTED_RECORDS_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.COMMITTING_CHANGES_ERROR_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.MATCHED_RECORDS_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.PROPOSED_CHANGES_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.RECORD_MATCHING_ERROR_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.TRIGGERING_FILE;
import static org.folio.bulkops.domain.dto.IdentifierType.BARCODE;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationType.UPDATE;
import static org.folio.bulkops.util.Constants.UTF8_BOM;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.HoldingsNoteTypeCollection;
import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.bean.NoteTypeCollection;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.FileContentType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.processor.note.HoldingsNotesProcessor;
import org.folio.bulkops.processor.note.ItemNoteProcessor;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.BulkOperationService;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ListUsersService;
import org.folio.bulkops.service.LogFilesService;
import org.folio.bulkops.service.RuleService;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class BulkOperationControllerTest extends BaseTest {

  @MockBean
  private BulkOperationService bulkOperationService;
  @MockBean
  private RemoteFileSystemClient remoteFileSystemClient;
  @MockBean
  private RuleService ruleService;
  @MockBean
  private ItemNoteProcessor itemNoteProcessor;
  @MockBean
  private HoldingsNotesProcessor holdingsNotesProcessor;

  @MockBean
  private ListUsersService listUsersService;

  @MockBean
  private LogFilesService logFilesService;

  @MockBean
  private ConsortiaService consortiaService;

  @Autowired
  private BulkOperationRepository bulkOperationRepository;

  @Test
  @SneakyThrows
  void shouldChangeEntityTypeAndClearProcessingOnPostContentUpdates() {
    var bulkOperation = BulkOperation.builder()
      .id(UUID.fromString("1910fae2-08c7-46e8-a73b-fc35d2639734"))
      .entityType(INSTANCE_MARC)
      .build();
    var content = """
      {
         "bulkOperationRules" : [ {
           "id" : "27749ff8-bffd-4344-853c-bc765c504631",
           "bulkOperationId": "1910fae2-08c7-46e8-a73b-fc35d2639734",
           "rule_details": {
             "option": "ITEM_NOTE",
             "actions": [ {
               "type": "ADD_TO_EXISTING"
             } ]
           }
         } ],
         "totalRecords" : 1
       }
      """;
    when(bulkOperationService.getBulkOperationOrThrow(bulkOperation.getId())).thenReturn(bulkOperation);
    when(ruleService.saveRules(any(BulkOperationRuleCollection.class)))
      .thenReturn(new BulkOperationRuleCollection().bulkOperationRules(Collections.emptyList()).totalRecords(1));

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      bulkOperationRepository.save(bulkOperation);

      mockMvc.perform(post(format("/bulk-operations/%s/content-update", bulkOperation.getId()))
          .headers(defaultHeaders())
          .contentType(APPLICATION_JSON)
          .content(content))
        .andExpect(status().isOk());

      var updatedBulkOperation = bulkOperationRepository.findById(bulkOperation.getId());
      assertThat(updatedBulkOperation).isPresent();
      assertThat(updatedBulkOperation.get().getEntityType()).isEqualTo(INSTANCE);
    }

    verify(bulkOperationService).clearOperationProcessing(any(BulkOperation.class));
  }

  @ParameterizedTest
  @MethodSource("fileContentTypeToEntityTypeCollection")
  void shouldDownloadFileWithPreview(FileContentType type, org.folio.bulkops.domain.dto.EntityType entityType) throws Exception {
    var content = "content";
    when(consortiaService.isTenantCentral(any())).thenReturn(false);
    when(remoteFileSystemClient.get(any(String.class))).thenReturn(new ByteArrayInputStream(content.getBytes()));
    when(itemNoteProcessor.processCsvContent(any(), any())).thenReturn(content.getBytes());
    when(holdingsNotesProcessor.processCsvContent(any(), any())).thenReturn(content.getBytes());

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = UUID.randomUUID();

      mockData(entityType);

      mockMvc.perform(get(format("/bulk-operations/%s/download?fileContentType=%s", operationId, type))
          .headers(defaultHeaders())
          .contentType(APPLICATION_JSON))
        .andExpect(status().isOk());

      if (PROPOSED_CHANGES_FILE.equals(type) && INSTANCE_MARC.equals(entityType)) {
        verify(remoteFileSystemClient).get("G");
      }
    }
  }

  @ParameterizedTest
  @MethodSource("fileContentTypeToEntityTypeCollection")
  void shouldAddUtf8BomToDownloadedCSV(FileContentType fileContentType, org.folio.bulkops.domain.dto.EntityType entityType) throws Exception {var content = "content";
    var csvfileName = "csvFileName.csv";
    var mrcfileName = "mrcFileName.mrc";
    var operationId = UUID.randomUUID();

    when(consortiaService.isTenantCentral(any())).thenReturn(false);
    when(remoteFileSystemClient.get(any(String.class))).thenReturn(new ByteArrayInputStream(content.getBytes()));
    when(bulkOperationService.getOperationById(any(UUID.class))).thenReturn(BulkOperation.builder()
      .id(UUID.randomUUID())
      .linkToTriggeringCsvFile(csvfileName)
      .linkToMatchedRecordsCsvFile(csvfileName)
      .linkToMatchedRecordsErrorsCsvFile(csvfileName)
      .linkToModifiedRecordsCsvFile(csvfileName)
      .linkToCommittedRecordsCsvFile(csvfileName)
      .linkToCommittedRecordsErrorsCsvFile(csvfileName)
      .linkToModifiedRecordsMarcFile(mrcfileName)
      .entityType(entityType)
      .build());
    when(itemNoteProcessor.processCsvContent(any(), any())).thenReturn(content.getBytes());
    when(holdingsNotesProcessor.processCsvContent(any(), any())).thenReturn(content.getBytes());

    var result = mockMvc.perform(get(format("/bulk-operations/%s/download?fileContentType=%s", operationId, fileContentType))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk()).andReturn();
    var actual = result.getResponse().getContentAsByteArray();

    if (fileContentType == PROPOSED_CHANGES_FILE && entityType == INSTANCE_MARC) {
      assertNotEquals(content.getBytes().length + UTF8_BOM.length, actual.length);
    } else {
      assertEquals(content.getBytes().length + UTF8_BOM.length, actual.length);
      var utf8bom = Arrays.copyOfRange(actual, 0, UTF8_BOM.length);
      assertArrayEquals(UTF8_BOM, utf8bom);
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

  @Test
  @SneakyThrows
  void shouldReturnNoContentOnFileDeletionByOperationIdAndFileName() {
    doNothing().when(logFilesService).deleteFileByOperationIdAndName(any(UUID.class), anyString());

    mockMvc.perform(delete(String.format("/bulk-operations/%s/files/file.csv", UUID.randomUUID()))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());
  }

  @Test
  @SneakyThrows
  void shouldPostBulkOperationMarcRules() {
    var bulkoperationId = UUID.fromString("1910fae2-08c7-46e8-a73b-fc35d2639734");
    var content = """
      {
         "bulkOperationMarcRules" : [ {
           "bulkOperationId" : "1910fae2-08c7-46e8-a73b-fc35d2639734",
           "tag" : "500",
           "ind1" : "#",
           "ind2" : "#",
           "subfield" : "h",
           "actions" : [ {
             "name" : "FIND",
             "data" : [ {
               "key" : "VALUE",
               "value" : "text"
             } ]
           } ],
           "parameters" : [ {
             "key" : "OVERRIDE_PROTECTED",
             "value" : "false"
           } ],
           "subfields" : [ {
             "subfield" : "",
             "actions" : [ {
               "name" : "ADD_TO_EXISTING",
               "data" : [ {
                 "key" : "VALUE",
                 "value" : "text"
               } ]
             } ]
           } ]
         } ],
         "totalRecords" : 1
       }
      """;

    var bulkOperation = BulkOperation.builder()
        .id(bulkoperationId).build();

    when(bulkOperationService.getBulkOperationOrThrow(bulkoperationId))
      .thenReturn(bulkOperation);

    mockMvc.perform(post(String.format("/bulk-operations/%s/marc-content-update", bulkoperationId))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)
        .content(content))
      .andExpect(status().isOk());

    verify(ruleService).saveMarcRules(any(BulkOperationMarcRuleCollection.class));
    verify(bulkOperationService).clearOperationProcessing(bulkOperation);
  }

  @Test
  @SneakyThrows
  void shouldGetUsedTenants() {
    var bulkoperationId = UUID.fromString("1910fae2-08c7-46e8-a73b-fc35d2639734");

    var bulkOperation = BulkOperation.builder()
      .id(bulkoperationId).usedTenants(List.of("member1", "member2")).build();

    when(bulkOperationService.getOperationById(bulkoperationId))
      .thenReturn(bulkOperation);

    var res = mockMvc.perform(get(String.format("/bulk-operations/used-tenants/%s", bulkoperationId))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();

    assertEquals("[\"member1\",\"member2\"]", res.getResponse().getContentAsString());
  }

  private static Stream<Arguments> fileContentTypeToEntityTypeCollection() {
    return Stream.of(
      Arguments.of(TRIGGERING_FILE, ITEM),
      Arguments.of(TRIGGERING_FILE, HOLDINGS_RECORD),
      Arguments.of(TRIGGERING_FILE, USER),
      Arguments.of(TRIGGERING_FILE, INSTANCE),
      Arguments.of(MATCHED_RECORDS_FILE, ITEM),
      Arguments.of(MATCHED_RECORDS_FILE, HOLDINGS_RECORD),
      Arguments.of(MATCHED_RECORDS_FILE, USER),
      Arguments.of(MATCHED_RECORDS_FILE, INSTANCE),
      Arguments.of(RECORD_MATCHING_ERROR_FILE, ITEM),
      Arguments.of(RECORD_MATCHING_ERROR_FILE, HOLDINGS_RECORD),
      Arguments.of(RECORD_MATCHING_ERROR_FILE, USER),
      Arguments.of(RECORD_MATCHING_ERROR_FILE, INSTANCE),
      Arguments.of(PROPOSED_CHANGES_FILE, ITEM),
      Arguments.of(PROPOSED_CHANGES_FILE, HOLDINGS_RECORD),
      Arguments.of(PROPOSED_CHANGES_FILE, USER),
      Arguments.of(PROPOSED_CHANGES_FILE, INSTANCE),
      Arguments.of(PROPOSED_CHANGES_FILE, INSTANCE_MARC),
      Arguments.of(COMMITTED_RECORDS_FILE, ITEM),
      Arguments.of(COMMITTED_RECORDS_FILE, HOLDINGS_RECORD),
      Arguments.of(COMMITTED_RECORDS_FILE, USER),
      Arguments.of(COMMITTED_RECORDS_FILE, INSTANCE),
      Arguments.of(COMMITTING_CHANGES_ERROR_FILE, ITEM),
      Arguments.of(COMMITTING_CHANGES_ERROR_FILE, HOLDINGS_RECORD),
      Arguments.of(COMMITTING_CHANGES_ERROR_FILE, USER),
      Arguments.of(COMMITTING_CHANGES_ERROR_FILE, INSTANCE)
    );
  }

  private void mockData(org.folio.bulkops.domain.dto.EntityType entityType) {
    when(remoteFileSystemClient.get(any(String.class))).thenReturn(InputStream.nullInputStream());

    when(bulkOperationService.getOperationById(any(UUID.class))).thenReturn(BulkOperation.builder()
      .id(UUID.randomUUID())
      .linkToTriggeringCsvFile("A")
      .linkToMatchedRecordsJsonFile("B")
      .linkToMatchedRecordsCsvFile("C")
      .linkToMatchedRecordsErrorsCsvFile("D")
      .linkToModifiedRecordsJsonFile("E")
      .linkToModifiedRecordsCsvFile("F")
      .linkToModifiedRecordsMarcFile("G")
      .linkToCommittedRecordsJsonFile("H")
      .linkToCommittedRecordsCsvFile("I")
      .linkToCommittedRecordsErrorsCsvFile("J")
      .entityType(entityType)
      .build());

    when(itemNoteTypeClient.getNoteTypes(any(Integer.class))).thenReturn(NoteTypeCollection.builder()
      .itemNoteTypes(List.of(NoteType.builder().name("name_1").build()))
      .build());

    when(holdingsNoteTypeClient.getNoteTypes(any(Integer.class))).thenReturn(HoldingsNoteTypeCollection.builder()
      .holdingsNoteTypes(List.of(HoldingsNoteType.builder().name("name_1").build()))
      .build());
  }
}
