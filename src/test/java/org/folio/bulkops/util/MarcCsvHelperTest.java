package org.folio.bulkops.util;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;
import static org.folio.bulkops.util.MarcCsvHelper.ENRICHED_PREFIX;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

class MarcCsvHelperTest extends BaseTest {
  @Autowired
  private MarcCsvHelper marcCsvHelper;

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

    assertThat(res[11]).isEqualTo("Sample title");
    assertThat(res[23]).isEqualTo("General note;General note;false");
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
  void shouldEnrichCommittedCsvWithUpdatedMarcRecords() {
    var pathToCommittedCsv = "somedir/committed.csv";
    var bulkOperation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .linkToTriggeringCsvFile("instances.csv")
      .linkToCommittedRecordsCsvFile(pathToCommittedCsv)
      .linkToCommittedRecordsMarcFile("committed.mrc")
      .linkToMatchedRecordsJsonFile("matched.json")
      .build();

    when(remoteFileSystemClient.get(bulkOperation.getLinkToCommittedRecordsCsvFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/committed_marc_instance.csv"));
    when(remoteFileSystemClient.get(bulkOperation.getLinkToCommittedRecordsMarcFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/committed_marc_record.mrc"));
    when(remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsJsonFile()))
    .thenReturn(new FileInputStream("src/test/resources/files/matched_marc_instances.json"));

    var csvHrids = marcCsvHelper.getUpdatedInventoryInstanceHrids(bulkOperation);
    var marcHrids = marcCsvHelper.getUpdatedMarcInstanceHrids(bulkOperation);
    marcCsvHelper.enrichCommittedCsvWithUpdatedMarcRecords(bulkOperation, csvHrids, marcHrids);

    var contentCaptor = ArgumentCaptor.forClass(InputStream.class);
    var pathCaptor = ArgumentCaptor.forClass(String.class);

    verify(remoteFileSystemClient).remove(pathToCommittedCsv);
    verify(remoteFileSystemClient).put(contentCaptor.capture(), pathCaptor.capture());
    assertThat(contentCaptor.getValue()).isInstanceOf(SequenceInputStream.class);
    assertThat(pathCaptor.getValue()).startsWith(ENRICHED_PREFIX);
    assertThat(bulkOperation.getLinkToCommittedRecordsCsvFile()).startsWith(ENRICHED_PREFIX);
  }

  @Test
  @SneakyThrows
  void shouldCreateCommittedCsvWithUpdatedMarcRecordsIfNoLink() {
    var bulkOperation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .linkToTriggeringCsvFile("instances.csv")
      .linkToCommittedRecordsMarcFile("committed.mrc")
      .linkToMatchedRecordsJsonFile("matched.json")
      .build();

    var writer = new StringWriter();

    when(remoteFileSystemClient.get(bulkOperation.getLinkToCommittedRecordsMarcFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/committed_marc_record.mrc"));
    when(remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsJsonFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/matched_marc_instances.json"));
    when(remoteFileSystemClient.writer(anyString()))
      .thenReturn(writer);

    marcCsvHelper.enrichCommittedCsvWithUpdatedMarcRecords(bulkOperation, singletonList("hrid1"), singletonList("hrid3"));

    assertThat(bulkOperation.getLinkToCommittedRecordsCsvFile()).isNotNull();
    var expectedCsvContent = """
      Instance UUID,Suppress from discovery,Staff suppress,Previously held,Instance HRID,Source,Cataloged date,Instance status term,Mode of issuance,Statistical code,Administrative note,Resource title,Index title,Series statements,Contributors,Edition,Physical description,Resource type,Nature of content,Formats,Languages,Publication frequency,Publication range,Notes
      043c77e9-3653-43d6-a064-5d99b9e5adb4,,,,hrid3,MARC,,,,,,,,,,,,,,,,,,
      """;
    assertThat(writer).hasToString(expectedCsvContent);
  }

  @Test
  @SneakyThrows
  void shouldEnrichCommittedMarcWithUpdatedInventoryRecords() {
    var pathToCommittedMarc = "somedir/committed.mrc";
    var bulkOperation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .linkToTriggeringCsvFile("instances.csv")
      .linkToCommittedRecordsCsvFile("committed.csv")
      .linkToCommittedRecordsMarcFile(pathToCommittedMarc)
      .linkToMatchedRecordsMarcFile("matched.json")
      .build();

    when(remoteFileSystemClient.get(bulkOperation.getLinkToCommittedRecordsCsvFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/committed_marc_instance.csv"));
    when(remoteFileSystemClient.get(bulkOperation.getLinkToCommittedRecordsMarcFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/committed_marc_record.mrc"));
    when(remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsMarcFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/matched_marc_records.mrc"));

    marcCsvHelper.enrichCommittedMarcWithUpdatedInventoryRecords(bulkOperation, singletonList("hrid1"), singletonList("hrid3"));

    var contentCaptor = ArgumentCaptor.forClass(InputStream.class);
    var pathCaptor = ArgumentCaptor.forClass(String.class);

    verify(remoteFileSystemClient).remove(pathToCommittedMarc);
    verify(remoteFileSystemClient).put(contentCaptor.capture(), pathCaptor.capture());
    assertThat(contentCaptor.getValue()).isInstanceOf(SequenceInputStream.class);
    assertThat(pathCaptor.getValue()).startsWith(ENRICHED_PREFIX);
    assertThat(bulkOperation.getLinkToCommittedRecordsMarcFile()).startsWith(ENRICHED_PREFIX);
  }

  @Test
  @SneakyThrows
  void shouldCreateCommittedMarcWithUpdatedInventoryRecordsIfNoLink() {
    var bulkOperation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .linkToTriggeringCsvFile("instances.csv")
      .linkToCommittedRecordsCsvFile("committed.csv")
      .linkToMatchedRecordsMarcFile("matched.json")
      .build();

    when(remoteFileSystemClient.get(bulkOperation.getLinkToCommittedRecordsCsvFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/committed_marc_instance.csv"));
    when(remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsMarcFile()))
      .thenReturn(new FileInputStream("src/test/resources/files/matched_marc_records.mrc"));

    var csvHrids = marcCsvHelper.getUpdatedInventoryInstanceHrids(bulkOperation);
    var marcHrids = marcCsvHelper.getUpdatedMarcInstanceHrids(bulkOperation);
    marcCsvHelper.enrichCommittedMarcWithUpdatedInventoryRecords(bulkOperation, csvHrids, marcHrids);

    assertThat(bulkOperation.getLinkToCommittedRecordsMarcFile()).isNotNull();

    var expectedMarcContent = "00044nam a2200037Ia 4500001000600000\u001Ehrid1\u001E\u001D";
    var contentCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(remoteFileSystemClient).put(contentCaptor.capture(), anyString());
    assertThat(new String(contentCaptor.getValue().readAllBytes())).isEqualTo(expectedMarcContent);
  }
}
