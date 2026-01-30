package org.folio.bulkops.processor.folio;

import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADD_TO_EXISTING;
import static org.folio.bulkops.domain.dto.UpdateActionType.CHANGE_TYPE;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REMOVE_THESE;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REPLACE;
import static org.folio.bulkops.domain.dto.UpdateActionType.MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_ALL;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.HOLDINGS_NOTE;
import static org.folio.bulkops.util.Constants.STAFF_ONLY_NOTE_PARAMETER_KEY;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsNote;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.processor.Updater;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@AllArgsConstructor
public class HoldingsNotesUpdater {

  public static final String HOLDINGS_NOTE_TYPE_ID_KEY = "HOLDINGS_NOTE_TYPE_ID_KEY";
  private final AdministrativeNotesUpdater administrativeNotesUpdater;

  public Optional<Updater<ExtendedHoldingsRecord>> updateNotes(
      Action action, UpdateOptionType option) {
    if ((MARK_AS_STAFF_ONLY == action.getType() || REMOVE_MARK_AS_STAFF_ONLY == action.getType())
        && option == HOLDINGS_NOTE) {
      var markAsStaffValue = action.getType() == MARK_AS_STAFF_ONLY;
      return Optional.of(
          extendedHoldingsRecord ->
              setMarkAsStaffForNotesByTypeId(
                  extendedHoldingsRecord.getEntity().getNotes(),
                  action.getParameters(),
                  markAsStaffValue));
    } else if (REMOVE_ALL == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return Optional.of(
            extendedHoldingsRecord ->
                extendedHoldingsRecord
                    .getEntity()
                    .setAdministrativeNotes(
                        administrativeNotesUpdater.removeAdministrativeNotes()));
      } else if (option == HOLDINGS_NOTE) {
        return Optional.of(
            extendedHoldingsRecord ->
                extendedHoldingsRecord
                    .getEntity()
                    .setNotes(
                        removeNotesByTypeId(
                            extendedHoldingsRecord.getEntity().getNotes(),
                            action.getParameters())));
      }
    } else if (ADD_TO_EXISTING == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return Optional.of(
            extendedHoldingsRecord ->
                extendedHoldingsRecord
                    .getEntity()
                    .setAdministrativeNotes(
                        administrativeNotesUpdater.addToAdministrativeNotes(
                            action.getUpdated(),
                            extendedHoldingsRecord.getEntity().getAdministrativeNotes())));
      } else if (option == HOLDINGS_NOTE) {
        return Optional.of(
            extendedHoldingsRecord ->
                extendedHoldingsRecord
                    .getEntity()
                    .setNotes(
                        addToNotesByTypeId(
                            extendedHoldingsRecord.getEntity().getNotes(),
                            action.getParameters(),
                            action.getUpdated())));
      }
    } else if (FIND_AND_REMOVE_THESE == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return Optional.of(
            extendedHoldingsRecord ->
                extendedHoldingsRecord
                    .getEntity()
                    .setAdministrativeNotes(
                        administrativeNotesUpdater.findAndRemoveAdministrativeNote(
                            action.getInitial(),
                            extendedHoldingsRecord.getEntity().getAdministrativeNotes())));
      } else if (option == HOLDINGS_NOTE) {
        return Optional.of(
            extendedHoldingsRecord ->
                extendedHoldingsRecord
                    .getEntity()
                    .setNotes(
                        findAndRemoveNoteByValueAndTypeId(
                            action.getInitial(),
                            extendedHoldingsRecord.getEntity().getNotes(),
                            action.getParameters())));
      }
    } else if (FIND_AND_REPLACE == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return Optional.of(
            extendedHoldingsRecord ->
                extendedHoldingsRecord
                    .getEntity()
                    .setAdministrativeNotes(
                        administrativeNotesUpdater.findAndReplaceAdministrativeNote(
                            action, extendedHoldingsRecord.getEntity().getAdministrativeNotes())));
      } else if (option == HOLDINGS_NOTE) {
        return Optional.of(
            extendedHoldingsRecord -> {
              if (extendedHoldingsRecord.getEntity().getNotes() != null) {
                var notes = new ArrayList<>(extendedHoldingsRecord.getEntity().getNotes());
                findAndReplaceNoteByValueAndTypeId(action, notes);
                extendedHoldingsRecord.getEntity().setNotes(notes);
              }
            });
      }
    } else if (CHANGE_TYPE == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return Optional.of(
            extendedHoldingsRecord ->
                administrativeNotesUpdater.changeNoteTypeForAdministrativeNotes(
                    extendedHoldingsRecord.getEntity(), action));
      } else if (option == HOLDINGS_NOTE) {
        return Optional.of(
            extendedHoldingsRecord ->
                changeNoteTypeForHoldingsNote(extendedHoldingsRecord.getEntity(), action));
      }
    }
    return Optional.empty();
  }

  private Boolean extractStaffOnlyParamValue(List<Parameter> parameters) {
    if (CollectionUtils.isEmpty(parameters)) {
      return false;
    }

    return parameters.stream()
        .filter(p -> STAFF_ONLY_NOTE_PARAMETER_KEY.equalsIgnoreCase(p.getKey()))
        .map(p -> Boolean.valueOf(p.getValue()))
        .findFirst()
        .orElse(false);
  }

  private void setMarkAsStaffForNotesByTypeId(
      List<HoldingsNote> notes, List<Parameter> parameters, boolean markAsStaffValue) {
    parameters.stream()
        .filter(p -> StringUtils.equals(p.getKey(), HOLDINGS_NOTE_TYPE_ID_KEY))
        .findFirst()
        .ifPresent(
            parameter -> {
              var typeId = parameter.getValue();
              if (notes != null) {
                notes.forEach(
                    note -> {
                      if (StringUtils.equals(typeId, note.getHoldingsNoteTypeId())) {
                        note.setStaffOnly(markAsStaffValue);
                      }
                    });
              }
            });
  }

  private List<HoldingsNote> removeNotesByTypeId(
      List<HoldingsNote> notes, List<Parameter> parameters) {
    var typeIdParameterOptional = getTypeIdParameterOptional(parameters);
    if (typeIdParameterOptional.isPresent()) {
      var typeId = typeIdParameterOptional.get().getValue();
      if (notes != null) {
        var notesWithoutRemoved =
            notes.stream()
                .filter(note -> !StringUtils.equals(note.getHoldingsNoteTypeId(), typeId))
                .toList();
        return new ArrayList<>(notesWithoutRemoved);
      }
    }
    return notes;
  }

  private List<HoldingsNote> addToNotesByTypeId(
      List<HoldingsNote> notes, List<Parameter> parameters, String noteValue) {
    var staffOnly = extractStaffOnlyParamValue(parameters);
    var typeIdParameterOptional = getTypeIdParameterOptional(parameters);
    if (typeIdParameterOptional.isPresent()) {
      var note =
          new HoldingsNote()
              .withHoldingsNoteTypeId(typeIdParameterOptional.get().getValue())
              .withNote(noteValue)
              .withStaffOnly(staffOnly);
      if (notes == null) {
        notes = new ArrayList<>();
      }
      notes.add(note);
    }
    return notes;
  }

  private List<HoldingsNote> findAndRemoveNoteByValueAndTypeId(
      String valueToRemove, List<HoldingsNote> notes, List<Parameter> parameters) {
    var typeIdParameterOptional = getTypeIdParameterOptional(parameters);
    if (typeIdParameterOptional.isPresent() && notes != null) {
      notes = new ArrayList<>(notes);
      var notesToRemove = new ArrayList<HoldingsNote>();
      notes.stream()
          .filter(
              note ->
                  StringUtils.equals(
                      note.getHoldingsNoteTypeId(), typeIdParameterOptional.get().getValue()))
          .forEach(
              note -> {
                String updatedNote = note.getNote().replace(valueToRemove, StringUtils.EMPTY);
                if (updatedNote.trim().isEmpty()) {
                  notesToRemove.add(note);
                } else {
                  note.setNote(updatedNote);
                }
              });
      notes.removeAll(notesToRemove);
    }
    return notes;
  }

  private void findAndReplaceNoteByValueAndTypeId(Action action, List<HoldingsNote> notes) {
    var typeIdParameterOptional = getTypeIdParameterOptional(action.getParameters());
    if (typeIdParameterOptional.isPresent() && notes != null) {
      var notesToRemove = new ArrayList<HoldingsNote>();
      notes.forEach(
          note -> {
            if (StringUtils.equals(
                    note.getHoldingsNoteTypeId(), typeIdParameterOptional.get().getValue())
                && contains(note.getNote(), action.getInitial())) {
              String replacedNote =
                  replace(note.getNote(), action.getInitial(), action.getUpdated());
              if (replacedNote.trim().isEmpty()) {
                notesToRemove.add(note);
              } else {
                note.setNote(replacedNote);
              }
            }
          });
      notes.removeAll(notesToRemove);
    }
  }

  private void changeNoteTypeForHoldingsNote(HoldingsRecord holding, Action action) {
    var typeIdParameterOptional = getTypeIdParameterOptional(action.getParameters());
    if (typeIdParameterOptional.isPresent()) {
      var typeId = typeIdParameterOptional.get().getValue();
      var notesWithTypeForChange =
          holding.getNotes().stream()
              .filter(note -> StringUtils.equals(note.getHoldingsNoteTypeId(), typeId))
              .toList();
      if (notesWithTypeForChange.isEmpty()) {
        return;
      }
      if (ADMINISTRATIVE_NOTE.getValue().equals(action.getUpdated())) {
        if (holding.getAdministrativeNotes() == null) {
          holding.setAdministrativeNotes(new ArrayList<>());
        }
        var notesWithoutTypeForChange =
            holding.getNotes().stream()
                .filter(note -> !StringUtils.equals(note.getHoldingsNoteTypeId(), typeId))
                .collect(toCollection(ArrayList::new));
        notesWithTypeForChange.forEach(
            note -> holding.getAdministrativeNotes().add(note.getNote()));
        holding.setNotes(notesWithoutTypeForChange);
      } else {
        notesWithTypeForChange.forEach(note -> note.setHoldingsNoteTypeId(action.getUpdated()));
      }
    }
  }

  private Optional<Parameter> getTypeIdParameterOptional(List<Parameter> parameters) {
    return parameters.stream()
        .filter(parameter -> StringUtils.equals(parameter.getKey(), HOLDINGS_NOTE_TYPE_ID_KEY))
        .findFirst();
  }
}
