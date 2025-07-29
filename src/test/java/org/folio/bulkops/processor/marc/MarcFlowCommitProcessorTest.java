package org.folio.bulkops.processor.marc;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.util.Constants.ENRICHED_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.StringWriter;
import java.util.UUID;

class MarcFlowCommitProcessorTest extends BaseTest {
  @MockitoBean
  private RemoteFileSystemClient remoteFileSystemClient;
  @MockitoBean
  private ConsortiaService consortiaService;
  @Autowired
  private MarcFlowCommitProcessor marcFlowCommitProcessor;

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @SneakyThrows
  void shouldEnrichCommittedCsvWithUpdatedMarcRecords(boolean isCentralTenant) {
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
    when(consortiaService.isTenantCentral(any())).thenReturn(isCentralTenant);

    var csvHrids = marcFlowCommitProcessor.getUpdatedInventoryInstanceHrids(bulkOperation);
    var marcHrids = marcFlowCommitProcessor.getUpdatedMarcInstanceHrids(bulkOperation);

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      marcFlowCommitProcessor.enrichCommittedCsvWithUpdatedMarcRecords(bulkOperation, csvHrids, marcHrids);
    }

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

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      marcFlowCommitProcessor.enrichCommittedCsvWithUpdatedMarcRecords(bulkOperation, singletonList("hrid1"), singletonList("hrid3"));
    }

    assertThat(bulkOperation.getLinkToCommittedRecordsCsvFile()).isNotNull();
    var expectedCsvContent = """
      Instance UUID,Suppress from discovery,Staff suppress,Previously held,Instance HRID,Source,Cataloged date,Instance status term,Mode of issuance,Statistical code,Administrative note,Resource title,Index title,Series statements,Contributors,Publication,Edition,Physical description,Resource type,Nature of content,Formats,Languages,Publication frequency,Publication range,Notes,Electronic access,Subject,Classification
      043c77e9-3653-43d6-a064-5d99b9e5adb4,,,,hrid3,MARC,,,,,,,,,,,,,,,,,,,,,,
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

    marcFlowCommitProcessor.enrichCommittedMarcWithUpdatedInventoryRecords(bulkOperation, singletonList("hrid1"), singletonList("hrid3"));

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

    var csvHrids = marcFlowCommitProcessor.getUpdatedInventoryInstanceHrids(bulkOperation);
    var marcHrids = marcFlowCommitProcessor.getUpdatedMarcInstanceHrids(bulkOperation);
    marcFlowCommitProcessor.enrichCommittedMarcWithUpdatedInventoryRecords(bulkOperation, csvHrids, marcHrids);

    assertThat(bulkOperation.getLinkToCommittedRecordsMarcFile()).isNotNull();

    var expectedMarcContent = "00044nam a2200037Ia 4500001000600000\u001Ehrid1\u001E\u001D";
    var contentCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(remoteFileSystemClient).put(contentCaptor.capture(), anyString());
    assertThat(new String(contentCaptor.getValue().readAllBytes())).isEqualTo(expectedMarcContent);
  }
}
