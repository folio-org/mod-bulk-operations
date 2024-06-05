package org.folio.bulkops.service;

import static com.opencsv.ICSVWriter.NO_QUOTE_CHARACTER;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.folio.bulkops.util.Constants.COMMA_DELIMETER;
import static org.folio.bulkops.util.Constants.HOLDINGS_NOTE_POSITION;

import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.RFC4180ParserBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.RemoteFileSystemClient;
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

  private final RemoteFileSystemClient remoteFileSystemClient;
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
      .map(s -> s.contains(COMMA_DELIMETER) || s.contains(LF) ? "\"" + s + "\"" : s)
      .toList();

    try (var reader = new CSVReaderBuilder(new InputStreamReader(new ByteArrayInputStream(input)))
          .withCSVParser(new RFC4180ParserBuilder().build()).build();
         var stringWriter = new StringWriter();
         var writer = new CSVWriterBuilder(stringWriter)
           .withSeparator(',')
           .withQuoteChar(NO_QUOTE_CHARACTER)
           .build()) {
      String[] line;
      while ((line = reader.readNext()) != null) {
        if (reader.getRecordsRead() == FIRST_LINE) {
          var headers = new ArrayList<>(Arrays.asList(line));
          headers.remove(HOLDINGS_NOTE_POSITION);
          headers.addAll(HOLDINGS_NOTE_POSITION, noteTypeHeaders);
          line = headers.toArray(new String[0]);
        } else {
          line = processNotesData(line, noteTypeNames);
        }
        writer.writeNext(line);
      }
      return stringWriter.toString().getBytes();
    } catch (Exception e) {
      log.error(e.getMessage());
      return new byte[0];
    }
  }

  private String[] processNotesData(String[] line, List<String> noteTypeNames) {
    return noteTableUpdater.enrichWithNotesByType(new ArrayList<>(Arrays.asList(line)), HOLDINGS_NOTE_POSITION, noteTypeNames)
      .toArray(String[]::new);
  }
}
