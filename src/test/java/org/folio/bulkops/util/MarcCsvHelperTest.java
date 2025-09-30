package org.folio.bulkops.util;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_HRID;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.MappingRulesClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.InstanceNoteType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.InstanceReferenceService;
import org.folio.bulkops.service.Marc21ReferenceProvider;
import org.folio.bulkops.service.MarcToUnifiedTableRowMapper;
import org.folio.bulkops.service.NoteTableUpdater;
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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
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
  void getChangedMarcData_shouldPutEmptyStringValue() throws Exception {
    Marc21ReferenceProvider marc21ReferenceProvider = mock(Marc21ReferenceProvider.class);
    var marcCsvHelper = new MarcCsvHelper(
            mock(NoteTableUpdater.class),
            mock(MarcToUnifiedTableRowMapper.class),
            remoteFileSystemClient,
            ruleService,
            marc21ReferenceProvider,
            objectMapper
    );

    var instanceHeaderNames = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Instance.class)
            .getHeader().stream().map(org.folio.bulkops.domain.dto.Cell::getValue).toList();
    var hridIndex = instanceHeaderNames.indexOf(INSTANCE_HRID);
    var option = instanceHeaderNames.get(hridIndex + 1); // pick a column after hrid

    var fileName = "test.csv";
    var bulkOperation = BulkOperation.builder()
            .id(UUID.randomUUID())
            .status(OperationStatusType.REVIEW_CHANGES)
            .entityType(EntityType.INSTANCE_MARC)
            .linkToModifiedRecordsMarcCsvFile(fileName)
            .build();

    // Prepare CSV: hrid, option, ...; row: hridValue, "", ...
    String[] header = instanceHeaderNames.toArray(new String[0]);
    String[] row = new String[header.length];
    row[hridIndex] = "hridValue";
    row[instanceHeaderNames.indexOf(option)] = ""; // empty string

    var csvContent = String.join(",", header) + "\n" + String.join(",", row) + "\n";

    // Mock ObjectReader and all relevant ObjectMapper methods
    ObjectReader objectReader = mock(ObjectReader.class);
    when(objectReader.forType(any(Class.class))).thenReturn(objectReader);
    when(remoteFileSystemClient.get(fileName)).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));
    when(ruleService.getMarcRules(bulkOperation.getId()))
            .thenReturn(new BulkOperationMarcRuleCollection()
                    .bulkOperationMarcRules(singletonList(new BulkOperationMarcRule().tag(option))));
    when(marc21ReferenceProvider.getChangedOptionsSetForCsv(any())).thenReturn(Set.of(option));
    doNothing().when(marc21ReferenceProvider).updateMappingRules();

    // Use reflection to call private method
    var method = MarcCsvHelper.class.getDeclaredMethod("getChangedMarcData", BulkOperation.class);
    method.setAccessible(true);
    var result = method.invoke(marcCsvHelper, bulkOperation);

    assertThat(result).isInstanceOf(Map.class);
    var resultMap = (Map<String, Map<String, String>>) result;
    assertThat(resultMap).containsKey("hridValue");
    assertThat(resultMap.get("hridValue")).containsEntry(option, "");
  }

  @Test
  void getChangedMarcData_shouldSkipWhenValueIsNull() throws Exception {
    Marc21ReferenceProvider marc21ReferenceProvider = mock(Marc21ReferenceProvider.class);
    var marcCsvHelper = new MarcCsvHelper(
            mock(NoteTableUpdater.class),
            mock(MarcToUnifiedTableRowMapper.class),
            remoteFileSystemClient,
            ruleService,
            marc21ReferenceProvider,
            objectMapper
    );

    var instanceHeaderNames = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Instance.class)
            .getHeader().stream().map(org.folio.bulkops.domain.dto.Cell::getValue).toList();
    var hridIndex = instanceHeaderNames.indexOf(INSTANCE_HRID);
    var option = instanceHeaderNames.get(hridIndex + 1); // pick a column after hrid

    var fileName = "test_null_value.csv";
    var bulkOperation = BulkOperation.builder()
            .id(UUID.randomUUID())
            .status(OperationStatusType.REVIEW_CHANGES)
            .entityType(EntityType.INSTANCE_MARC)
            .linkToModifiedRecordsMarcCsvFile(fileName)
            .build();

    // Prepare CSV: hrid, option, ...; row: hridValue, null, ...
    String[] header = instanceHeaderNames.toArray(new String[0]);
    String[] row = new String[header.length];
    row[hridIndex] = "hridValue";
    // Explicitly set the option column to null
    row[instanceHeaderNames.indexOf(option)] = null;

    var csvContent = String.join(",", header) + "\n" + String.join(",", row) + "\n";

    ObjectReader objectReader = mock(ObjectReader.class);
    when(objectReader.forType(any(Class.class))).thenReturn(objectReader);
    when(remoteFileSystemClient.get(fileName)).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));
    when(ruleService.getMarcRules(bulkOperation.getId()))
            .thenReturn(new BulkOperationMarcRuleCollection()
                    .bulkOperationMarcRules(singletonList(new BulkOperationMarcRule().tag(option))));
    when(marc21ReferenceProvider.getChangedOptionsSetForCsv(any())).thenReturn(Set.of(option));
    doNothing().when(marc21ReferenceProvider).updateMappingRules();

    var method = MarcCsvHelper.class.getDeclaredMethod("getChangedMarcData", BulkOperation.class);
    method.setAccessible(true);
    var result = method.invoke(marcCsvHelper, bulkOperation);

    assertThat(result).isInstanceOf(Map.class);
    var resultMap = (Map<String, Map<String, String>>) result;
    assertThat(resultMap).containsKey("hridValue");
    // Should not contain the option key since line[index] == null
    assertThat(resultMap.get("hridValue").get("Source")).isEqualTo("null");
  }
}
