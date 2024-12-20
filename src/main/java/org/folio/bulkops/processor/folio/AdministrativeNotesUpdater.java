package org.folio.bulkops.processor.folio;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.CirculationNote;
import org.folio.bulkops.domain.bean.HoldingsNote;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.InstanceNote;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemNote;
import org.folio.bulkops.domain.dto.Action;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toCollection;
import static org.folio.bulkops.processor.folio.ItemsNotesUpdater.CHECK_IN_NOTE_TYPE;
import static org.folio.bulkops.processor.folio.ItemsNotesUpdater.CHECK_OUT_NOTE_TYPE;
import static org.folio.bulkops.processor.folio.ItemsNotesUpdater.NOTES_TYPES_TO_UPDATE;

@Component
public class AdministrativeNotesUpdater {

  public List<String> removeAdministrativeNotes() {
    return new ArrayList<>();
  }

  public List<String> addToAdministrativeNotes(String administrativeNote, List<String> administrativeNotes) {
    List<String> notes = isNull(administrativeNotes) ? new ArrayList<>() : new ArrayList<>(administrativeNotes);
    notes.add(administrativeNote);
    return notes;
  }

  public List<String> findAndRemoveAdministrativeNote(String administrativeNoteToRemove, List<String> administrativeNotes) {
    if (administrativeNotes != null) {
      administrativeNotes = administrativeNotes.stream()
        .filter(administrativeNote -> !StringUtils.contains(administrativeNote, administrativeNoteToRemove))
        .collect(toCollection(ArrayList::new));
    }
    return administrativeNotes;
  }

  public List<String> findAndReplaceAdministrativeNote(Action action, List<String> administrativeNotes) {
    if (administrativeNotes != null) {
      administrativeNotes = administrativeNotes.stream().map(administrativeNote -> {
        if (StringUtils.contains(administrativeNote, action.getInitial())) {
          return StringUtils.replace(administrativeNote, action.getInitial(), action.getUpdated());
        }
        return administrativeNote;
      }).collect(toCollection(ArrayList::new));
    }
    return administrativeNotes;
  }

  public void changeNoteTypeForAdministrativeNotes(HoldingsRecord holding, Action action) {
    if (holding.getAdministrativeNotes() != null) {
      if (holding.getNotes() == null) holding.setNotes(new ArrayList<>());
      holding.getAdministrativeNotes().forEach(administrativeNote ->
        holding.getNotes().add(new HoldingsNote().withHoldingsNoteTypeId(action.getUpdated()).withNote(administrativeNote).withStaffOnly(false)));
      holding.setAdministrativeNotes(new ArrayList<>());
    }
  }

  public void changeNoteTypeForAdministrativeNotes(Item item, Action action) {
    if (item.getAdministrativeNotes() != null) {
      if (item.getCirculationNotes() == null) item.setCirculationNotes(new ArrayList<>());
      if (item.getNotes() == null) item.setNotes(new ArrayList<>());
      var noteTypeToUse = action.getUpdated();
      item.getAdministrativeNotes().forEach(administrativeNote -> {
        if (NOTES_TYPES_TO_UPDATE.contains(noteTypeToUse)) {
          if (CHECK_IN_NOTE_TYPE.equals(noteTypeToUse)) item.getCirculationNotes().add(new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.IN).withNote(administrativeNote).withStaffOnly(false));
          if (CHECK_OUT_NOTE_TYPE.equals(noteTypeToUse)) item.getCirculationNotes().add(new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.OUT).withNote(administrativeNote).withStaffOnly(false));
        } else {
          item.getNotes().add(new ItemNote().withItemNoteTypeId(noteTypeToUse).withNote(administrativeNote));
        }
      });
      item.setAdministrativeNotes(new ArrayList<>());
    }
  }

  public void changeNoteTypeForAdministrativeNotes(Instance instance, Action action) {
    if (nonNull(instance.getAdministrativeNotes())) {
      List<InstanceNote> instanceNotes = isNull(instance.getInstanceNotes()) ?
        new ArrayList<>() :
        new ArrayList<>(instance.getInstanceNotes());
      var notes = instance.getAdministrativeNotes().stream()
        .map(administrativeNote -> InstanceNote.builder()
          .instanceNoteTypeId(action.getUpdated())
          .note(administrativeNote)
          .staffOnly(false).build())
        .toList();
      instanceNotes.addAll(notes);
      instance.setInstanceNotes(instanceNotes);
      instance.setAdministrativeNotes(new ArrayList<>());
    }
  }
}
