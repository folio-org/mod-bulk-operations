package org.folio.bulkops.util;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.dto.FileContentType.PROPOSED_CHANGES_FILE;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.MappingRulesClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.Marc21ReferenceProvider;
import org.folio.bulkops.service.RuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class MarcCsvHelperOptionNotFoundTest extends BaseTest {

  @Autowired private MarcCsvHelper marcCsvHelper;

  @MockitoBean private Marc21ReferenceProvider marc21ReferenceProvider;
  @MockitoBean private MappingRulesClient mappingRulesClient;
  @MockitoBean private RemoteFileSystemClient remoteFileSystemClient;
  @MockitoBean private RuleService ruleService;

  @Test
  @SneakyThrows
  void shouldEnrichCsvWithEmptyValueBecauseOfOptionNotFound() {
    var fileName = "file.csv";
    var operationId = UUID.randomUUID();
    var operation =
        BulkOperation.builder()
            .id(operationId)
            .status(REVIEW_CHANGES)
            .entityType(org.folio.bulkops.domain.dto.EntityType.INSTANCE_MARC)
            .linkToModifiedRecordsMarcCsvFile(fileName)
            .build();

    var content = new FileInputStream("src/test/resources/files/instance.csv").readAllBytes();

    when(mappingRulesClient.getMarcBibMappingRules())
        .thenReturn(
            Files.readString(Path.of("src/test/resources/files/mappingRulesResponse.json")));
    when(remoteFileSystemClient.get(fileName))
        .thenReturn(new FileInputStream("src/test/resources/files/marc_csv_empty_notes.csv"));
    when(ruleService.getMarcRules(operationId))
        .thenReturn(
            new org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection()
                .bulkOperationMarcRules(
                    singletonList(
                        new org.folio.bulkops.domain.dto.BulkOperationMarcRule().tag("500"))));
    when(marc21ReferenceProvider.getChangedOptionsSetForCsv(
            any(org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection.class)))
        .thenReturn(Set.of("Not notes"));

    var res = marcCsvHelper.enrichCsvWithMarcChanges(content, operation, PROPOSED_CHANGES_FILE);

    var expectedInstanceNotes = "";
    assertThat(new String(res)).contains(expectedInstanceNotes);
  }
}
