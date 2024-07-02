package org.folio.bulkops.processor.note;

public interface NoteProcessorFactory {
  AbstractNoteProcessor getNoteProcessor(String entityType);
}
