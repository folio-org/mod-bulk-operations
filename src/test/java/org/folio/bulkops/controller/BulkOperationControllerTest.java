package org.folio.bulkops.controller;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.domain.dto.BulkOperationStart;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRuleDetailsRepository;
import org.folio.bulkops.repository.BulkOperationRuleRepository;
import org.folio.bulkops.service.BulkOperationService;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.RuleService;
import org.folio.spring.cql.JpaCqlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static org.folio.bulkops.domain.dto.ApproachType.IN_APP;
import static org.folio.bulkops.domain.dto.ApproachType.QUERY;
import static org.folio.bulkops.domain.dto.BulkOperationStep.UPLOAD;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;
import static org.testcontainers.shaded.org.hamcrest.Matchers.equalTo;
import static org.testcontainers.shaded.org.hamcrest.Matchers.nullValue;

class BulkOperationControllerTest extends BaseTest {
  public static final String TRIGGERING_FILE_NAME = "barcodes.csv";
  public static final String MODIFIED_FILE_NAME = "modified-barcodes.csv";
  @Autowired
  private BulkOperationService bulkOperationService;
  @Autowired
  private ErrorService errorService;
  @Autowired
  private RuleService ruleService;
  @Autowired
  private BulkOperationRuleRepository ruleRepository;
  @Autowired
  private BulkOperationRuleDetailsRepository ruleDetailsRepository;
  @Autowired
  private RemoteFileSystemClient client;
  @Autowired
  private JpaCqlRepository<BulkOperation, UUID> bulkOperationCqlRepository;

  @Test
  @SneakyThrows
  void shouldNotStartBulkOperationWithWrongState() {
    var operationId = UUID.randomUUID();

    var result = OBJECT_MAPPER.readValue(mockMvc.perform(post(format("/bulk-operations/%s/start", operationId))
        .content(OBJECT_MAPPER.writeValueAsString(new BulkOperationStart().approach(QUERY).step(UPLOAD).query("barcode=*")))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString(), BulkOperation.class);

    assertThat(operationId, equalTo(result.getId()));
  }

  @SneakyThrows
  @ParameterizedTest
  @EnumSource(value = EntityType.class)
  void shouldUploadIdentifiers(EntityType entity) {
    var identifiers = "123\n456\n789";
    var file = new MockMultipartFile("file", TRIGGERING_FILE_NAME, MediaType.TEXT_PLAIN_VALUE, identifiers.getBytes(Charset.defaultCharset()));

    var initial = OBJECT_MAPPER.readValue(mockMvc.perform(multipart(format("/bulk-operations/upload?entityType=%s&identifierType=BARCODE", entity.getValue()))
        .file(file)
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString(), BulkOperation.class);

    assertThat(initial.getLinkToTriggeringCsvFile(), equalTo(initial.getId() + "/" + TRIGGERING_FILE_NAME));
    assertThat(initial.getStatus(), equalTo(NEW));

    // Get bulk operation by id
    var retrieved = OBJECT_MAPPER.readValue(mockMvc.perform(get(format("/bulk-operations/%s", initial.getId()))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString(), BulkOperation.class);

    assertThat(initial, equalTo(retrieved));
    assertThat(retrieved.getStatus(), equalTo(NEW));
  }

  @SneakyThrows
  @Test
  void shouldUploadFileForManualApproach() {
    var identifiers = "123\n456\n789";
    var file = new MockMultipartFile("file", TRIGGERING_FILE_NAME, MediaType.TEXT_PLAIN_VALUE, identifiers.getBytes(Charset.defaultCharset()));

    var initial = OBJECT_MAPPER.readValue(mockMvc.perform(multipart("/bulk-operations/upload?entityType=USER&identifierType=BARCODE")
        .file(file)
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString(), BulkOperation.class);

    assertThat(initial.getLinkToTriggeringCsvFile(), equalTo(initial.getId() + "/" + TRIGGERING_FILE_NAME));
    assertThat(initial.getLinkToModifiedRecordsCsvFile(), nullValue());
    assertThat(initial.getStatus(), equalTo(NEW));

    var content = "123,456,789";
    var matched = new MockMultipartFile("file", MODIFIED_FILE_NAME, MediaType.TEXT_PLAIN_VALUE, content.getBytes(Charset.defaultCharset()));

    var manual = OBJECT_MAPPER.readValue(mockMvc.perform(multipart(format("/bulk-operations/upload?entityType=USER&identifierType=BARCODE&manual=true&operationId=%s", initial.getId()))
        .file(matched)
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString(), BulkOperation.class);

    assertThat(manual.getLinkToModifiedRecordsCsvFile(), equalTo(initial.getId() + "/" + MODIFIED_FILE_NAME));
    assertThat(manual.getStatus(), equalTo(NEW));

  }

  @SneakyThrows
  @ParameterizedTest
  @EnumSource(value = BulkOperationStep.class)
  void shouldNotStartBulkOperationIfOperationWasNotFound(BulkOperationStep step) {
    var operationId = UUID.randomUUID();

    mockMvc.perform(post(format("/bulk-operations/%s/start", operationId))
        .content(OBJECT_MAPPER.writeValueAsString(new BulkOperationStart().approach(IN_APP).step(step)))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound());

    mockMvc.perform(post(format("/bulk-operations/%s/start", operationId))
        .content(OBJECT_MAPPER.writeValueAsString(new BulkOperationStart().approach(ApproachType.MANUAL).step(step)))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound());

    if (UPLOAD != step) {
      mockMvc.perform(post(format("/bulk-operations/%s/start", operationId))
          .content(OBJECT_MAPPER.writeValueAsString(new BulkOperationStart().approach(ApproachType.QUERY).step(step)))
          .headers(defaultHeaders())
          .contentType(APPLICATION_JSON))
        .andExpect(status().isNotFound());
    }
  }

  @Test
  @SneakyThrows
  void shouldPostContentUpdates() {
    var operationId = UUID.randomUUID();

      bulkOperationCqlRepository.save(BulkOperation.builder()
        .id(operationId)
        .build());

    var contentUpdates = new BulkOperationRuleCollection()
      .bulkOperationRules(List.of(new BulkOperationRule()
        .id(UUID.randomUUID())
        .bulkOperationId(operationId)
        .ruleDetails(new BulkOperationRuleRuleDetails()
          .option(UpdateOptionType.PERMANENT_LOCATION)
          .actions(List.of(new Action()
            .type(UpdateActionType.REPLACE_WITH)
            .updated("location"))))))
      .totalRecords(1);

    mockMvc.perform(post(format("/bulk-operations/%s/content-update", operationId))
        .content(OBJECT_MAPPER.writeValueAsString(contentUpdates))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk());
  }

}
