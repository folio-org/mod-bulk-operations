package org.folio.bulkops.service;

import static org.folio.bulkops.util.Constants.HOLDINGS_NOTE_POSITION;

import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180ParserBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Log4j2
public class HoldingsNotesProcessor {
  private static final int FIRST_LINE = 1;

  private final HoldingsReferenceService holdingsReferenceService;
  private final NoteTableUpdater noteTableUpdater;

  public byte[] processHoldingsNotes(byte[] input) {
    var noteTypeNames = holdingsReferenceService.getAllHoldingsNoteTypes().stream()
      .map(HoldingsNoteType::getName)
      .filter(Objects::nonNull)
      .sorted()
      .toList();
    var noteTypeHeaders = noteTypeNames.stream()
      .map(noteTableUpdater::concatNotePostfixIfRequired)
      .toList();

    try (var reader = new CSVReaderBuilder(new InputStreamReader(new ByteArrayInputStream(input)))
          .withCSVParser(new RFC4180ParserBuilder().build()).build();
         var stringWriter = new StringWriter()) {
      String[] line;
      while ((line = reader.readNext()) != null) {
        if (reader.getRecordsRead() == FIRST_LINE) {
          var headers = new ArrayList<>(Arrays.asList(line));
          headers.remove(HOLDINGS_NOTE_POSITION);
          headers.addAll(HOLDINGS_NOTE_POSITION, noteTypeHeaders);
          line = headers.stream()
            .map(this::processSpecialCharacters)
            .toArray(String[]::new);
        } else {
          line = processNotesData(line, noteTypeNames);
        }
        stringWriter.write(String.join(",", line) + "\n");
      }
      return stringWriter.toString().getBytes();
    } catch (Exception e) {
      log.error(e.getMessage());
      return new byte[0];
    }
  }

  private String[] processNotesData(String[] line, List<String> noteTypeNames) {
    return noteTableUpdater.enrichWithNotesByType(new ArrayList<>(Arrays.asList(line)), HOLDINGS_NOTE_POSITION, noteTypeNames).stream()
      .map(this::processSpecialCharacters)
      .toArray(String[]::new);
  }

  private String processSpecialCharacters(String line) {
    line = line.contains("\"") ? line.replace("\"", "\"\"") : line;
    return line.contains(",") || line.contains("\n") ? "\"" + line + "\"" : line;
  }
}
