package org.folio.bulkops.controller;

import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;
import static org.testcontainers.shaded.org.hamcrest.Matchers.equalTo;
import static org.testcontainers.shaded.org.hamcrest.Matchers.hasSize;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.BulkOperationCollection;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.domain.dto.BulkOperationStart;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.DataType;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.Error;
import org.folio.bulkops.domain.dto.Errors;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.IllegalOperationStateException;
import org.folio.bulkops.service.BulkOperationService;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.RuleService;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

class BulkOperationControllerTest extends BaseTest {
  @MockBean
  private BulkOperationService bulkOperationService;
  @MockBean
  private ErrorService errorService;
  @MockBean
  private RuleService ruleService;
  @Autowired
  private JpaCqlRepository<BulkOperation, UUID> bulkOperationCqlRepository;

  @Test
  @SneakyThrows
  void shouldDownloadErrorsByBulkOperationId() {
    var operationId = UUID.randomUUID();
    var csvString = "1,Error\n2,Error";
    when(errorService.getErrorsCsvByBulkOperationId(operationId))
      .thenReturn(csvString);

    var response = mockMvc.perform(get(String.format("/bulk-operations/%s/errors/download", operationId))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();

    assertThat(response.getResponse().getContentAsString(), equalTo(csvString));
  }

//  @Test
//  @SneakyThrows
//  void shouldDownloadPreviewByBulkOperationId() {
//    var operationId = UUID.randomUUID();
//    var csvString = "1,Val1\n2,Val2";
//    when(bulkOperationService.getCsvPreviewByBulkOperationId(operationId))
//      .thenReturn(csvString);
//
//    var response = mockMvc.perform(get(String.format("/bulk-operations/%s/preview/download", operationId))
//        .headers(defaultHeaders())
//        .contentType(APPLICATION_JSON))
//      .andExpect(status().isOk())
//      .andReturn();
//
//    assertThat(response.getResponse().getContentAsString(), equalTo(csvString));
//  }

  @ParameterizedTest
  @CsvSource(value = { ",", ",10", "0,10", "10,5" }, delimiter = ',')
  @SneakyThrows
  void shouldGetBulkOperationsByQuery(Integer offset, Integer limit) {

    IntStream.range(0, 20).forEach(i -> bulkOperationCqlRepository.save(BulkOperation.builder()
      .status(DATA_MODIFICATION)
      .build()));
    bulkOperationCqlRepository.save(BulkOperation.builder()
      .status(OperationStatusType.REVIEW_CHANGES)
      .build());
    bulkOperationCqlRepository.save(BulkOperation.builder()
      .status(OperationStatusType.APPLY_CHANGES)
      .build());


    var query = "/bulk-operations?query=status==\"DATA_MODIFICATION\""
      .concat(Objects.isNull(offset) ? EMPTY : "&offset=" + offset)
      .concat(Objects.isNull(limit) ? EMPTY : "&limit=" + limit);
    var response = mockMvc.perform(get(query)
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();

    var bulkOperations = OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), BulkOperationCollection.class);

    assertThat(bulkOperations.getBulkOperations(), hasSize(Objects.isNull(limit) ? 20 : limit));
    assertTrue(bulkOperations.getBulkOperations().stream()
      .allMatch(bulkOperationDto -> DATA_MODIFICATION.equals(bulkOperationDto.getStatus())));
  }

  @Test
  @SneakyThrows
  void shouldGetErrorsPreviewByBulkOperationId() {
    var operationId = UUID.randomUUID();
    when(errorService.getErrorsPreviewByBulkOperationId(operationId, 2))
      .thenReturn(new Errors().errors(List.of(new Error(), new Error())));

    var response = mockMvc.perform(get(String.format("/bulk-operations/%s/errors?limit=2", operationId))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();

    var errors = OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), Errors.class);

    assertThat(errors.getErrors(), hasSize(2));
  }

  @Test
  @SneakyThrows
  void shouldGetPreviewByBulkOperationId() {
    var operationId = UUID.randomUUID();

    var cells = List.of(
      new Cell().value("Holdings record id").dataType(DataType.STRING).visible(true),
      new Cell().value("Hrid").dataType(DataType.STRING).visible(true));

    var rows = List.of(new Row().addRowItem(UUID.randomUUID().toString()).addRowItem("Hrid1"),
      new Row().addRowItem(UUID.randomUUID().toString()).addRowItem("Hrid2"));

    when(bulkOperationService.getPreview(operationId, BulkOperationStep.UPLOAD, 2))
      .thenReturn(new UnifiedTable()
        .header(cells)
        .rows(rows));

    var response = mockMvc.perform(get(String.format("/bulk-operations/%s/preview?limit=2", operationId))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();

    var table = OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UnifiedTable.class);

    assertThat(table.getHeader(), hasSize(2));
    assertThat(table.getHeader(), equalTo(cells));
    assertThat(table.getRows(), hasSize(2));
    assertThat(table.getRows(), equalTo(rows));
  }

  @Test
  @SneakyThrows
  void shouldPostContentUpdates() {
    var operationId = UUID.randomUUID();

    var contentUpdates = new BulkOperationRuleCollection()
      .bulkOperationRules(List.of(new BulkOperationRule()
        .bulkOperationId(operationId)
        .ruleDetails(new BulkOperationRuleRuleDetails()
          .option(UpdateOptionType.PERMANENT_LOCATION)
          .actions(List.of(new Action()
            .type(UpdateActionType.REPLACE_WITH)
            .updated("location"))))))
      .totalRecords(1);

    when(bulkOperationService.getBulkOperationOrThrow(operationId))
      .thenReturn(BulkOperation.builder().build());

    mockMvc.perform(post(String.format("/bulk-operations/%s/content-update", operationId))
        .content(OBJECT_MAPPER.writeValueAsString(contentUpdates))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk());

    verify(ruleService).saveRules(contentUpdates);
  }

  @Test
  @SneakyThrows
  void shouldStartBulkOperationById() {
    var operationId = UUID.randomUUID();

    when(bulkOperationService.startBulkOperation(operationId, any(UUID.class), new BulkOperationStart().approach(ApproachType.IN_APP).step(BulkOperationStep.UPLOAD)))
      .thenReturn(BulkOperation.builder().id(operationId).build());

    var response = mockMvc.perform(post(String.format("/bulk-operations/%s/start?approachType=IN_APP", operationId))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();

    var operation = OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), BulkOperation.class);
    assertThat(operation.getId(), equalTo(operationId));
  }

  @Test
  @SneakyThrows
  void shouldNotStartBulkOperationWithWrongState() {
    var operationId = UUID.randomUUID();

    when(bulkOperationService.startBulkOperation(operationId, any(UUID.class), new BulkOperationStart().approach(ApproachType.IN_APP).step(BulkOperationStep.UPLOAD)))
      .thenThrow(new IllegalOperationStateException("Bulk operation cannot be started"));

    mockMvc.perform(post(String.format("/bulk-operations/%s/start", operationId))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  @SneakyThrows
  void shouldNotStartBulkOperationIfOperationWasNotFound() {
    var operationId = UUID.randomUUID();

    when(bulkOperationService.startBulkOperation(operationId, any(UUID.class), new BulkOperationStart().approach(ApproachType.IN_APP).step(BulkOperationStep.UPLOAD)))
      .thenThrow(new NotFoundException("Bulk operation was not found"));

    mockMvc.perform(post(String.format("/bulk-operations/%s/start?approachType=IN_APP", operationId))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  void shouldUploadIdentifiers() {
    var operationId = UUID.randomUUID();
    var identifiers = "123\n456\n789";
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, identifiers.getBytes());

    when(bulkOperationService.uploadCsvFile(eq(EntityType.USER), eq(IdentifierType.BARCODE), anyBoolean(), any(), any(), any(MultipartFile.class)))
      .thenReturn(BulkOperation.builder().id(operationId).build());

    var result = mockMvc.perform(multipart("/bulk-operations/upload?entityType=USER&identifierType=BARCODE")
        .file(file)
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andReturn();

    var operation = OBJECT_MAPPER.readValue(result.getResponse().getContentAsString(), BulkOperation.class);
    assertThat(operation.getId(), equalTo(operationId));

    var fileCaptor = ArgumentCaptor.forClass(MultipartFile.class);
    verify(bulkOperationService).uploadCsvFile(eq(EntityType.USER), eq(IdentifierType.BARCODE), anyBoolean(), any(), any(), fileCaptor.capture());
    assertThat(new String(fileCaptor.getValue().getInputStream().readAllBytes()), equalTo(identifiers));
  }

  @Test
  @SneakyThrows
  void shouldGetBulkOperationById() {
    var operationId = UUID.randomUUID();

    when(bulkOperationService.getOperationById(operationId))
      .thenReturn(BulkOperation.builder().id(operationId).build());

    var response = mockMvc.perform(get(String.format("/bulk-operations/%s", operationId))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();

    var operation = OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), BulkOperation.class);
    assertThat(operation.getId(), equalTo(operationId));
  }
}
