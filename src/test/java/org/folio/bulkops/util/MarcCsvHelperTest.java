package org.folio.bulkops.util;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.MappingRulesClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.InstanceNoteType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.InstanceReferenceService;
import org.folio.bulkops.service.RuleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.marc4j.marc.impl.DataFieldImpl;
import org.marc4j.marc.impl.LeaderImpl;
import org.marc4j.marc.impl.RecordImpl;
import org.marc4j.marc.impl.SubfieldImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

class MarcCsvHelperTest extends BaseTest {
  @Autowired
  private MarcCsvHelper marcCsvHelper;
  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private InstanceReferenceService instanceReferenceService;
  @MockitoBean
  private MappingRulesClient mappingRulesClient;
  @MockitoBean
  private RemoteFileSystemClient remoteFileSystemClient;
  @MockitoBean
  private RuleService ruleService;

  @Test
  void shouldGetModifiedDataForCsv() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("00714cam a2200205 a 4500"));
    var field = new DataFieldImpl("245", ' ', ' ');
    var subfield = new SubfieldImpl('a', "Sample title");
    field.addSubfield(subfield);
    marcRecord.addVariableField(field);
    field = new DataFieldImpl("500", ' ', ' ');
    subfield = new SubfieldImpl('a', "General note");
    field.addSubfield(subfield);
    marcRecord.addVariableField(field);

    when(instanceReferenceService.getAllInstanceNoteTypes())
      .thenReturn(singletonList(new InstanceNoteType().name("General note")));

    var res = marcCsvHelper.getModifiedDataForCsv(marcRecord);

    assertThat(res[12]).isEqualTo("Sample title");
    assertThat(res[25]).isEqualTo("General note;General note;false");
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = {"REVIEW_CHANGES", "COMPLETED", "COMPLETED_WITH_ERRORS", "APPLY_CHANGES"}, mode = EnumSource.Mode.INCLUDE)
  @SneakyThrows
  void shouldEnrichCsvWithMarcChanges(OperationStatusType statusType) {
    var fileName = "file.csv";
    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .status(statusType)
      .entityType(EntityType.INSTANCE_MARC)
      .linkToModifiedRecordsMarcCsvFile(fileName)
      .linkToCommittedRecordsMarcCsvFile(fileName)
      .build();

    var content = new FileInputStream("src/test/resources/files/instance_with_special_characters.csv").readAllBytes();

    when(mappingRulesClient.getMarcBibMappingRules())
      .thenReturn(Files.readString(Path.of("src/test/resources/files/mappingRulesResponse.json")));
    when(remoteFileSystemClient.get(fileName))
      .thenReturn(new FileInputStream("src/test/resources/files/sample_marc_csv.csv"));
    when(ruleService.getMarcRules(operationId))
      .thenReturn(new BulkOperationMarcRuleCollection()
        .bulkOperationMarcRules(singletonList(new BulkOperationMarcRule().tag("500"))));

    var res = marcCsvHelper.enrichCsvWithMarcChanges(content, operation);

    var expectedInstanceNotes = APPLY_CHANGES.equals(statusType) ?
      "General note;General note text;false" :
      "General note;Changed general note;false";

    assertThat(new String(res)).contains(expectedInstanceNotes);

    if (!APPLY_CHANGES.equals(statusType)) {
      assertThat(new String(res)).contains("\"Sample\n note\"");
      assertThat(new String(res)).contains("Index \"\"title");
      assertThat(new String(res)).contains("\"Sample, contributor\"");
    }
  }

  @Test
  @SneakyThrows
  void shouldNotEnrichCsvInCaseOfException() {
    var fileName = "file.csv";
    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .status(REVIEW_CHANGES)
      .entityType(EntityType.INSTANCE_MARC)
      .linkToModifiedRecordsMarcCsvFile(fileName)
      .linkToCommittedRecordsMarcCsvFile(fileName)
      .build();

    var content = new FileInputStream("src/test/resources/files/instance.csv").readAllBytes();

    when(mappingRulesClient.getMarcBibMappingRules())
      .thenReturn(Files.readString(Path.of("src/test/resources/files/mappingRulesResponse.json")));
    when(remoteFileSystemClient.get(fileName)).thenThrow(new RuntimeException());
    when(ruleService.getMarcRules(operationId))
      .thenReturn(new BulkOperationMarcRuleCollection()
        .bulkOperationMarcRules(singletonList(new BulkOperationMarcRule().tag("500"))));

    var res = marcCsvHelper.enrichCsvWithMarcChanges(content, operation);

    var expectedInstanceNotes = "General note;General note text;false";

    assertThat(new String(res)).contains(expectedInstanceNotes);
  }

  @Test
  @SneakyThrows
  void shouldNotEnrichCsvIfMarcCsvIsInvalid() {
    var fileName = "file.csv";
    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .status(REVIEW_CHANGES)
      .entityType(EntityType.INSTANCE_MARC)
      .linkToModifiedRecordsMarcCsvFile(fileName)
      .linkToCommittedRecordsMarcCsvFile(fileName)
      .build();

    var content = new FileInputStream("src/test/resources/files/instance.csv").readAllBytes();

    when(mappingRulesClient.getMarcBibMappingRules())
      .thenReturn(Files.readString(Path.of("src/test/resources/files/mappingRulesResponse.json")));
    when(remoteFileSystemClient.get(fileName))
      .thenReturn(new FileInputStream("src/test/resources/files/invalid_sample_marc_csv.csv"));
    when(ruleService.getMarcRules(operationId))
      .thenReturn(new BulkOperationMarcRuleCollection()
        .bulkOperationMarcRules(singletonList(new BulkOperationMarcRule().tag("500"))));

    var res = marcCsvHelper.enrichCsvWithMarcChanges(content, operation);

    var expectedInstanceNotes = "General note;General note text;false";

    assertThat(new String(res)).contains(expectedInstanceNotes);
  }

  @Test
  @SneakyThrows
  void shouldEnrichCsvWithMarcChangesWhenLinkToCommittedIsNull() {
    var fileName = "file.csv";
    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
            .id(operationId)
            .status(OperationStatusType.COMPLETED)
            .entityType(EntityType.INSTANCE_MARC)
            .linkToModifiedRecordsMarcCsvFile(fileName)
            .linkToCommittedRecordsMarcCsvFile(null)
            .build();

    var content = new FileInputStream("src/test/resources/files/instance.csv").readAllBytes();

    when(mappingRulesClient.getMarcBibMappingRules())
            .thenReturn(Files.readString(Path.of("src/test/resources/files/mappingRulesResponse.json")));
    when(remoteFileSystemClient.get(fileName))
            .thenReturn(new FileInputStream("src/test/resources/files/sample_marc_csv.csv"));
    when(ruleService.getMarcRules(operationId))
            .thenReturn(new BulkOperationMarcRuleCollection()
                    .bulkOperationMarcRules(singletonList(new BulkOperationMarcRule().tag("500"))));

    var res = marcCsvHelper.enrichCsvWithMarcChanges(content, operation);

    var expectedInstanceNotes = "General note;General note text;false";
    assertThat(new String(res)).contains(expectedInstanceNotes);
  }

  @Test
  @SneakyThrows
  void shouldEnrichCsvWithEmptyValue() {
    var fileName = "file.csv";
    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
            .id(operationId)
            .status(REVIEW_CHANGES)
            .entityType(EntityType.INSTANCE_MARC)
            .linkToModifiedRecordsMarcCsvFile(fileName)
            .build();

    var content = new FileInputStream("src/test/resources/files/instance.csv").readAllBytes();

    when(mappingRulesClient.getMarcBibMappingRules())
            .thenReturn(Files.readString(Path.of("src/test/resources/files/mappingRulesResponse.json")));
    when(remoteFileSystemClient.get(fileName))
            .thenReturn(new FileInputStream("src/test/resources/files/marc_csv_empty_notes.csv"));
    when(ruleService.getMarcRules(operationId))
            .thenReturn(new BulkOperationMarcRuleCollection()
                    .bulkOperationMarcRules(singletonList(new BulkOperationMarcRule().tag("500"))));

    var res = marcCsvHelper.enrichCsvWithMarcChanges(content, operation);

    var expectedInstanceNotes = "";
    assertThat(new String(res)).contains(expectedInstanceNotes);
  }
}
