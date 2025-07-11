package org.folio.bulkops.controller;

import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE_MARC;
import static org.folio.bulkops.domain.dto.EntityType.ITEM;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.domain.dto.FileContentType.COMMITTED_RECORDS_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.COMMITTED_RECORDS_MARC_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.COMMITTING_CHANGES_ERROR_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.MATCHED_RECORDS_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.PROPOSED_CHANGES_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.PROPOSED_CHANGES_MARC_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.RECORD_MATCHING_ERROR_FILE;
import static org.folio.bulkops.domain.dto.FileContentType.TRIGGERING_FILE;
import static org.folio.bulkops.domain.dto.IdentifierType.BARCODE;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationType.UPDATE;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
import org.folio.bulkops.domain.dto.ProfileDto;
import org.folio.bulkops.domain.dto.ProfileRequest;
import org.folio.bulkops.exception.ProfileLockedException;
import org.folio.bulkops.processor.note.HoldingsNotesProcessor;
import org.folio.bulkops.processor.note.ItemNoteProcessor;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.BulkOperationService;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ListUsersService;
import org.folio.bulkops.service.LogFilesService;
import org.folio.bulkops.service.RuleService;
import org.folio.bulkops.service.ProfileService;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.domain.dto.ProfilesDto;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.json.JSONObject;
import org.folio.bulkops.domain.dto.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class BulkOperationControllerTest extends BaseTest {

  @MockitoBean
  private BulkOperationService bulkOperationService;
  @MockitoBean
  private ProfileService profileService;
  @MockitoBean
  private RemoteFileSystemClient remoteFileSystemClient;
  @MockitoBean
  private RuleService ruleService;
  @MockitoBean
  private ItemNoteProcessor itemNoteProcessor;
  @MockitoBean
  private HoldingsNotesProcessor holdingsNotesProcessor;
  @MockitoBean
  private ListUsersService listUsersService;
  @MockitoBean
  private LogFilesService logFilesService;
  @MockitoBean
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
    when(ruleService.saveRules(eq(bulkOperation), any(BulkOperationRuleCollection.class)))
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

      if (PROPOSED_CHANGES_MARC_FILE.equals(type) && INSTANCE_MARC.equals(entityType)) {
        verify(remoteFileSystemClient).get("G");
      } else if (COMMITTED_RECORDS_MARC_FILE.equals(type) && INSTANCE_MARC.equals(entityType)) {
        verify(remoteFileSystemClient).get("J");
      }
    }
  }

  @ParameterizedTest
  @MethodSource("fileContentTypeToEntityTypeCollection")
  void shouldAddUtf8BomToDownloadedCSV(FileContentType fileContentType, org.folio.bulkops.domain.dto.EntityType entityType) throws Exception {
    var content = "content";
    var csvfileName = "csvFileName.csv";
    var mrcfileName = "mrcFileName.mrc";
    var operationId = UUID.randomUUID();
    byte[] utf8bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

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
      .linkToCommittedRecordsMarcFile(mrcfileName)
      .entityType(entityType)
      .build());
    when(itemNoteProcessor.processCsvContent(any(), any())).thenReturn(content.getBytes());
    when(holdingsNotesProcessor.processCsvContent(any(), any())).thenReturn(content.getBytes());

    var result = mockMvc.perform(get(format("/bulk-operations/%s/download?fileContentType=%s", operationId, fileContentType))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk()).andReturn();
    var actual = result.getResponse().getContentAsByteArray();

    if (Set.of(PROPOSED_CHANGES_MARC_FILE, COMMITTED_RECORDS_MARC_FILE).contains(fileContentType)
      && entityType == INSTANCE_MARC) {
      assertNotEquals(content.getBytes().length + utf8bom.length, actual.length);
    } else {
      assertEquals(content.getBytes().length + utf8bom.length, actual.length);
      var actualUtf8bom = Arrays.copyOfRange(actual, 0, utf8bom.length);
      assertArrayEquals(utf8bom, actualUtf8bom);
    }
  }

  @ParameterizedTest
  @MethodSource("fileContentTypeToEntityTypeCollection")
  void shouldNotAddUtf8BomToDownloadedCSVIfAlreadyPresent(FileContentType fileContentType, org.folio.bulkops.domain.dto.EntityType entityType) throws Exception {
    var content = "content";
    var csvfileName = "csvFileName.csv";
    var mrcfileName = "mrcFileName.mrc";
    var operationId = UUID.randomUUID();
    byte[] utf8bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    when(consortiaService.isTenantCentral(any())).thenReturn(false);
    var stream = new SequenceInputStream(new ByteArrayInputStream(utf8bom), new ByteArrayInputStream(content.getBytes()));
    when(remoteFileSystemClient.get(any(String.class))).thenReturn(stream);
    when(bulkOperationService.getOperationById(any(UUID.class))).thenReturn(BulkOperation.builder()
      .id(UUID.randomUUID())
      .linkToTriggeringCsvFile(csvfileName)
      .linkToMatchedRecordsCsvFile(csvfileName)
      .linkToMatchedRecordsErrorsCsvFile(csvfileName)
      .linkToModifiedRecordsCsvFile(csvfileName)
      .linkToCommittedRecordsCsvFile(csvfileName)
      .linkToCommittedRecordsErrorsCsvFile(csvfileName)
      .linkToModifiedRecordsMarcFile(mrcfileName)
      .linkToCommittedRecordsMarcFile(mrcfileName)
      .entityType(entityType)
      .build());
    when(itemNoteProcessor.processCsvContent(any(), any())).thenReturn(content.getBytes());
    when(holdingsNotesProcessor.processCsvContent(any(), any())).thenReturn(content.getBytes());

    var result = mockMvc.perform(get(format("/bulk-operations/%s/download?fileContentType=%s", operationId, fileContentType))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk()).andReturn();
    var actual = result.getResponse().getContentAsByteArray();

    assertEquals(content.getBytes().length + utf8bom.length, actual.length);
    var actualUtf8bom = Arrays.copyOfRange(actual, 0, utf8bom.length);
    assertArrayEquals(utf8bom, actualUtf8bom);
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

    verify(ruleService).saveMarcRules(eq(bulkOperation), any(BulkOperationMarcRuleCollection.class));
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
      Arguments.of(PROPOSED_CHANGES_MARC_FILE, INSTANCE_MARC),
      Arguments.of(COMMITTED_RECORDS_FILE, ITEM),
      Arguments.of(COMMITTED_RECORDS_FILE, HOLDINGS_RECORD),
      Arguments.of(COMMITTED_RECORDS_FILE, USER),
      Arguments.of(COMMITTED_RECORDS_FILE, INSTANCE),
      Arguments.of(COMMITTED_RECORDS_MARC_FILE, INSTANCE_MARC),
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
      .linkToCommittedRecordsMarcFile("J")
      .linkToCommittedRecordsErrorsCsvFile("K")
      .entityType(entityType)
      .build());

    when(itemNoteTypeClient.getNoteTypes(any(Integer.class))).thenReturn(NoteTypeCollection.builder()
      .itemNoteTypes(List.of(NoteType.builder().name("name_1").build()))
      .build());

    when(holdingsNoteTypeClient.getNoteTypes(any(Integer.class))).thenReturn(HoldingsNoteTypeCollection.builder()
      .holdingsNoteTypes(List.of(HoldingsNoteType.builder().name("name_1").build()))
      .build());
  }

  @Test
  void shouldReturnProfileSummaries() throws Exception {
    ProfileDto profile1 = buildProfileSummaryDto(UUID.randomUUID(), "Test Profile 1", "Sample description 1",  false);
    ProfileDto profile2 = buildProfileSummaryDto(UUID.randomUUID(), "Test Profile 2", "Sample description 2", false);

    ProfilesDto summaryDto = new ProfilesDto();
    summaryDto.setTotalRecords(2L);
    summaryDto.setContent(List.of(profile1, profile2));

    String query = null;
    Integer offset = null;
    Integer limit = null;

    when(profileService.getProfiles(query, offset, limit)).thenReturn(summaryDto);

    var requestBuilder = get("/bulk-operations/profiles")
      .headers(defaultHeaders())
      .contentType(APPLICATION_JSON);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords", is(2)))
      .andExpect(jsonPath("$.content[0].name", is(profile1.getName())))
      .andExpect(jsonPath("$.content[0].description", is(profile1.getDescription())))
      .andExpect(jsonPath("$.content[0].locked", is(profile1.getLocked())))
      .andExpect(jsonPath("$.content[1].name", is(profile2.getName())))
      .andExpect(jsonPath("$.content[1].description", is(profile2.getDescription())))
      .andExpect(jsonPath("$.content[1].locked", is(profile2.getLocked())));
  }


  @Test
  void shouldCreateProfile() throws Exception {
    UUID profileId = UUID.randomUUID();

    ProfileRequest request = new ProfileRequest();
    request.setName("test");
    request.setLocked(false);
    request.setEntityType(USER);

    ProfileDto createdProfile = buildProfileDto(profileId, "Updated Name", "Updated description", USER, false);

    when(profileService.createProfile(any(ProfileRequest.class)))
      .thenReturn(createdProfile);

    mockMvc.perform(post("/bulk-operations/profiles")
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id", is(createdProfile.getId().toString())))
      .andExpect(jsonPath("$.name", is(createdProfile.getName())));
  }

  @Test
  void shouldReturnProfilesWithQueryAndPagination() throws Exception {
    ProfileDto profile = buildProfileSummaryDto(UUID.randomUUID(), "Filtered Profile", "Desc", false);

    ProfilesDto summaryDto = new ProfilesDto();
    summaryDto.setTotalRecords(1L);
    summaryDto.setContent(List.of(profile));

    when(profileService.getProfiles("name==\"Filtered Profile\"", 5, 10)).thenReturn(summaryDto);

    mockMvc.perform(get("/bulk-operations/profiles")
        .param("query", "name==\"Filtered Profile\"")
        .param("offset", "5")
        .param("limit", "10")
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords", is(1)))
      .andExpect(jsonPath("$.content[0].name", is(profile.getName())));
  }

  @Test
  void shouldReturnEmptyProfileList() throws Exception {
    ProfilesDto emptyDto = new ProfilesDto();
    emptyDto.setTotalRecords(0L);
    emptyDto.setContent(List.of());

    when(profileService.getProfiles(any(), any(), any())).thenReturn(emptyDto);

    mockMvc.perform(get("/bulk-operations/profiles")
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords", is(0)))
      .andExpect(jsonPath("$.content", hasSize(0)));
  }


  @Test
  void shouldDeleteProfile() throws Exception {
    var id = UUID.randomUUID();
    doNothing().when(profileService).deleteById(id);

    mockMvc.perform(delete("/bulk-operations/profiles/{id}", id)
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnNotFoundWhenDeletingNonExistentProfile() throws Exception {
    UUID id = UUID.randomUUID();
    doThrow(new NotFoundException("Profile not found with ID: " + id))
      .when(profileService).deleteById(id);

    mockMvc.perform(delete("/bulk-operations/profiles/{id}", id)
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  @Test
  void shouldUpdateProfile() throws Exception {
    UUID profileId = UUID.randomUUID();
    ProfileRequest updateRequest = buildProfileRequest("Updated Name", "Updated description", USER, false);
    ProfileDto updatedProfile = buildProfileDto(profileId, "Updated Name", "Updated description", USER, false);

    when(profileService.updateProfile(eq(profileId), any(ProfileRequest.class)))
      .thenReturn(updatedProfile);

    mockMvc.perform(put("/bulk-operations/profiles/{id}", profileId)
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id", is(updatedProfile.getId().toString())))
      .andExpect(jsonPath("$.name", is(updatedProfile.getName())))
      .andExpect(jsonPath("$.description", is(updatedProfile.getDescription())))
      .andExpect(jsonPath("$.entityType", is(updatedProfile.getEntityType().toString())))
      .andExpect(jsonPath("$.locked", is(updatedProfile.getLocked())));
  }

  @Test
  void shouldReturnNotFoundWhenUpdatingNonExistentProfile() throws Exception {
    UUID profileId = UUID.randomUUID();

    ProfileRequest updateRequest = buildProfileRequest("Updated Name", "Updated description", USER, false);

    when(profileService.updateProfile(eq(profileId), any(ProfileRequest.class)))
      .thenThrow(new NotFoundException("Profile not found with ID: " + profileId));

    mockMvc.perform(put("/bulk-operations/profiles/{id}", profileId)
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
      .andExpect(status().isNotFound());
  }

  @Test
  void deleteLockedProfile_shouldReturnBadRequest() throws Exception {
    UUID profileId = UUID.randomUUID();
    doThrow(new ProfileLockedException("Cannot delete a locked profile " + profileId))
      .when(profileService).deleteById(profileId);

    mockMvc.perform(delete("/bulk-operations/profiles/{id}", profileId)
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isForbidden());
}

  @Test
  void updateLockedProfile_shouldReturnBadRequest() throws Exception {
    UUID profileId = UUID.randomUUID();
    ProfileRequest updateRequest = buildProfileRequest(
      "Updated Name", "Updated description", USER, true
    );

    doThrow(new ProfileLockedException("Cannot update a locked profile"))
      .when(profileService).updateProfile(eq(profileId), any(ProfileRequest.class));

    mockMvc.perform(put("/bulk-operations/profiles/{id}", profileId)
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
      .andExpect(status().isForbidden());
  }

  private ProfileRequest buildProfileRequest(String name, String description, org.folio.bulkops.domain.dto.EntityType entityType, boolean locked) {
    ProfileRequest request = new ProfileRequest();
    request.setName(name);
    request.setDescription(description);
    request.setEntityType(entityType);
    request.setLocked(locked);
    return request;
  }

  private ProfileDto buildProfileDto(UUID id, String name, String description, EntityType entityType, boolean locked) {
    ProfileDto dto = new ProfileDto();
    dto.setId(id);
    dto.setName(name);
    dto.setDescription(description);
    dto.setEntityType(entityType);
    dto.setLocked(locked);
    return dto;
  }

  private ProfileDto buildProfileSummaryDto(UUID id, String name, String description, boolean locked) {
    ProfileDto dto = new ProfileDto();

    dto.setId(id);
    dto.setName(name);
    dto.setDescription(description);
    dto.setLocked(locked);

    return dto;
  }
}
