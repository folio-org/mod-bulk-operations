package org.folio.bulkops.service;

import static io.swagger.v3.core.util.Constants.COMMA;

import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.RFC4180ParserBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Log4j2
public class HoldingsNotesProcessor {
  private static final int FIRST_LINE = 1;
  private static final int NOTES_POSITION = 24;

  private final RemoteFileSystemClient remoteFileSystemClient;
  private final HoldingsReferenceService holdingsReferenceService;
  private final NoteTableUpdater noteTableUpdater;

  public void processHoldingsNotes(String pathToInput, String pathToOutput) {
    var noteTypeNames = holdingsReferenceService.getAllHoldingsNoteTypes().stream()
      .map(HoldingsNoteType::getName)
      .filter(Objects::nonNull)
      .sorted()
      .toList();
    var noteTypeHeaders = noteTypeNames.stream()
      .map(s -> s.contains(COMMA) ? "\"" + s + "\"" : s)
      .toList();

    try (var reader = new CSVReaderBuilder(new InputStreamReader(remoteFileSystemClient.get(pathToInput)))
          .withCSVParser(new RFC4180ParserBuilder().build()).build();
         var writer = new CSVWriterBuilder(remoteFileSystemClient.writer(pathToOutput)).withSeparator(',').build()) {
      String[] line;
      while ((line = reader.readNext()) != null) {
        if (reader.getRecordsRead() == FIRST_LINE) {
          var headers = new ArrayList<>(Arrays.asList(line));
          headers.remove(NOTES_POSITION);
          headers.addAll(NOTES_POSITION, noteTypeHeaders);
          line = headers.toArray(new String[0]);
        } else {
          line = processNotesData(line, noteTypeNames);
        }
        writer.writeNext(line);
      }
      remoteFileSystemClient.remove(pathToInput);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  private String[] processNotesData(String[] line, List<String> noteTypeNames) {
    return noteTableUpdater.enrichWithNotesByType(new ArrayList<>(Arrays.asList(line)), NOTES_POSITION, noteTypeNames)
      .toArray(String[]::new);
  }
}
