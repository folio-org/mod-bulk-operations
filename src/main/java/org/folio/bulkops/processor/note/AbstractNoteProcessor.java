package org.folio.bulkops.processor.note;

import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180ParserBuilder;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.service.NoteTableUpdater;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Log4j2
public abstract class AbstractNoteProcessor {

  private static final int FIRST_LINE = 1;

  private NoteTableUpdater noteTableUpdater;

  @Autowired
  private void setNoteTableUpdater(NoteTableUpdater noteTableUpdater) {
    this.noteTableUpdater = noteTableUpdater;
  }

  public byte[] processNotes(byte[] input) {
    List<String> noteTypeNames = getNoteTypeNames();
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
          headers.remove(getNotePosition());
          headers.addAll(getNotePosition(), noteTypeHeaders);
          line = headers.stream()
            .map(this::processSpecialCharacters)
            .toArray(String[]::new);
        } else {
          line = processNotesData(line, noteTypeNames);
        }
        stringWriter.write(String.join(",", line) + "\n");
      }
      stringWriter.flush();
      return stringWriter.toString().getBytes();
    } catch (Exception e) {
      log.error(e.getMessage());
      return new byte[0];
    }
  }

  protected abstract List<String> getNoteTypeNames();

  protected abstract int getNotePosition();

  private String[] processNotesData(String[] line, List<String> noteTypeNames) {
    return noteTableUpdater.enrichWithNotesByType(new ArrayList<>(Arrays.asList(line)), getNotePosition(), noteTypeNames).stream()
      .map(this::processSpecialCharacters)
      .toArray(String[]::new);
  }

  private String processSpecialCharacters(String line) {
    if (isNotEmpty(line)) {
      line = line.contains("\"") ? line.replace("\"", "\"\"") : line;
      return line.contains(",") || line.contains("\n") ? "\"" + line + "\"" : line;
    }
    return EMPTY;
  }
}
