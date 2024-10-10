package org.folio.bulkops.domain.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.bean.CirculationNote.NoteTypeEnum.IN;
import static org.folio.bulkops.domain.bean.CirculationNote.NoteTypeEnum.OUT;

import lombok.SneakyThrows;
import org.folio.bulkops.domain.bean.CirculationNote;
import org.folio.bulkops.domain.bean.Item;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.StringWriter;
import java.util.List;

class BulkOperationsEntityCsvWriterTest {
  @Test
  @SneakyThrows
  void shouldSplitCirculationNotesForItem() {
    var pathToExpectedCsv = "src/test/resources/files/item_with_split_circ_notes.csv";
    var item = Item.builder()
      .tenantId("tenantId")
      .circulationNotes(List.of(
        CirculationNote.builder().noteType(IN).note("Check in note 1").staffOnly(true).build(),
        CirculationNote.builder().noteType(OUT).note("Check out note 1").staffOnly(true).build(),
        CirculationNote.builder().noteType(OUT).note("Check out note 2").staffOnly(false).build(),
        CirculationNote.builder().noteType(IN).note("Check in note 2").build()))
      .build();

    try (var writer = new StringWriter();
      var expectedStream = new FileInputStream(pathToExpectedCsv)) {
      var csvWriter = new BulkOperationsEntityCsvWriter(writer, Item.class);

      csvWriter.write(item);

      var expectedCsv = new String(expectedStream.readAllBytes());
      var actualCsv = writer.toString();

      assertThat(actualCsv).isEqualTo(expectedCsv);
    }
  }
}
