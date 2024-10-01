package org.folio.bulkops.processor.note;

public interface NoteProcessorFactory {
  CsvDownloadPreProcessor getNoteProcessor(String entityType);
}
