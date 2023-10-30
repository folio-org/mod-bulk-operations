package org.folio.bulkops.processor;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.HoldingsNote;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toCollection;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADD_TO_EXISTING;
import static org.folio.bulkops.domain.dto.UpdateActionType.CHANGE_TYPE;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REMOVE_THESE;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REPLACE;
import static org.folio.bulkops.domain.dto.UpdateActionType.MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_ALL;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.HOLDINGS_NOTE;

@Component
@AllArgsConstructor
public class HoldingsNotesUpdater {

  public static final String HOLDINGS_NOTE_TYPE_ID_KEY = "HOLDINGS_NOTE_TYPE_ID_KEY";
  private final AdministrativeNotesUpdater administrativeNotesUpdater;

  public Optional<Updater<HoldingsRecord>> updateNotes( Action action, UpdateOptionType option) {
    if ((MARK_AS_STAFF_ONLY == action.getType() || REMOVE_MARK_AS_STAFF_ONLY == action.getType()) && option == HOLDINGS_NOTE){
      var markAsStaffValue = action.getType() == MARK_AS_STAFF_ONLY;
      return Optional.of(holding -> setMarkAsStaffForNotesByTypeId(holding.getNotes(), action.getParameters(), markAsStaffValue));
    } else if (REMOVE_ALL == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return Optional.of(holding -> holding.setAdministrativeNotes(administrativeNotesUpdater.removeAdministrativeNotes()));
      } else if (option == HOLDINGS_NOTE) {
        return Optional.of(holding -> holding.setNotes(removeNotesByTypeId(holding.getNotes(), action.getParameters())));
      }
    } else if (ADD_TO_EXISTING == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return Optional.of(holding -> holding.setAdministrativeNotes(administrativeNotesUpdater.addToAdministrativeNotes(action.getUpdated(), holding.getAdministrativeNotes())));
      } else if (option == HOLDINGS_NOTE) {
        return Optional.of(holding -> holding.setNotes(addToNotesByTypeId(holding.getNotes(), action.getParameters(), action.getUpdated())));
      }
    } else if (FIND_AND_REMOVE_THESE == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return Optional.of(holding -> holding.setAdministrativeNotes(administrativeNotesUpdater.findAndRemoveAdministrativeNote(action.getInitial(), holding.getAdministrativeNotes())));
      } else if (option == HOLDINGS_NOTE) {
        return Optional.of(holding -> holding.setNotes(findAndRemoveNoteByValueAndTypeId(action.getInitial(), holding.getNotes(), action.getParameters())));
      }
    } else if (FIND_AND_REPLACE == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return Optional.of(holding -> holding.setAdministrativeNotes(administrativeNotesUpdater.findAndReplaceAdministrativeNote(action, holding.getAdministrativeNotes())));
      } else if (option == HOLDINGS_NOTE) {
        return Optional.of(holding -> findAndReplaceNoteByValueAndTypeId(action, holding.getNotes()));
      }
    } else if (CHANGE_TYPE == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return Optional.of(holding -> administrativeNotesUpdater.changeNoteTypeForAdministrativeNotes(holding, action));
      } else if (option == HOLDINGS_NOTE) {
        return Optional.of(holding -> changeNoteTypeForHoldingsNote(holding, action));
      }
    }
    return Optional.empty();
  }

  private void setMarkAsStaffForNotesByTypeId(List<HoldingsNote> notes, List<Parameter> parameters, boolean markAsStaffValue) {
    parameters.stream().filter(p -> StringUtils.equals(p.getKey(), HOLDINGS_NOTE_TYPE_ID_KEY)).findFirst()
      .ifPresent(parameter -> {
        var typeId = parameter.getValue();
        if (notes != null) {
          notes.forEach(note -> {
            if (StringUtils.equals(typeId, note.getHoldingsNoteTypeId())) {
              note.setStaffOnly(markAsStaffValue);
            }
          });
        }});
  }

  private List<HoldingsNote> removeNotesByTypeId(List<HoldingsNote> notes, List<Parameter> parameters) {
    var typeIdParameterOptional = getTypeIdParameterOptional(parameters);
    if (typeIdParameterOptional.isPresent()) {
      var typeId = typeIdParameterOptional.get().getValue();
      if (notes != null) {
        var notesWithoutRemoved = notes.stream().filter(note -> !StringUtils.equals(note.getHoldingsNoteTypeId(), typeId)).toList();
        return new ArrayList<>(notesWithoutRemoved);
      }}
    return notes;
  }

  private List<HoldingsNote> addToNotesByTypeId(List<HoldingsNote> notes, List<Parameter> parameters, String noteValue) {
    var typeIdParameterOptional = getTypeIdParameterOptional(parameters);
    if (typeIdParameterOptional.isPresent()) {
      var note = new HoldingsNote().withHoldingsNoteTypeId(typeIdParameterOptional.get().getValue())
        .withNote(noteValue).withStaffOnly(false);
      if (notes == null) {
        notes = new ArrayList<>();
      }
      notes.add(note);
    }
    return notes;
  }

  private List<HoldingsNote> findAndRemoveNoteByValueAndTypeId(String noteToRemove, List<HoldingsNote> notes, List<Parameter> parameters) {
    var typeIdParameterOptional = getTypeIdParameterOptional(parameters);
    if (typeIdParameterOptional.isPresent() && notes != null) {
      notes = notes.stream().filter(note -> !(StringUtils.equals(note.getHoldingsNoteTypeId(), typeIdParameterOptional.get().getValue())
              && StringUtils.equals(note.getNote(), noteToRemove))).collect(toCollection(ArrayList::new));
    }
    return notes;
  }

  private void findAndReplaceNoteByValueAndTypeId(Action action, List<HoldingsNote> notes) {
    var typeIdParameterOptional = getTypeIdParameterOptional(action.getParameters());
    if (typeIdParameterOptional.isPresent() && notes != null) {
      notes.forEach(note -> {
        if (StringUtils.equals(note.getHoldingsNoteTypeId(), typeIdParameterOptional.get().getValue())
              && StringUtils.equals(note.getNote(), action.getInitial())) note.setNote(action.getUpdated());
      });
    }
  }

  private void changeNoteTypeForHoldingsNote(HoldingsRecord holding, Action action) {
    var typeIdParameterOptional = getTypeIdParameterOptional(action.getParameters());
    if (typeIdParameterOptional.isPresent()) {
      var typeId  = typeIdParameterOptional.get().getValue();
      var notesWithTypeForChange = holding.getNotes().stream()
        .filter(note -> StringUtils.equals(note.getHoldingsNoteTypeId(), typeId)).toList();
      if (notesWithTypeForChange.isEmpty()) return;
      if (ADMINISTRATIVE_NOTE.getValue().equals(action.getUpdated())) {
        if (holding.getAdministrativeNotes() == null) holding.setAdministrativeNotes(new ArrayList<>());
        var notesWithoutTypeForChange = holding.getNotes().stream().filter(note -> !StringUtils.equals(note.getHoldingsNoteTypeId(), typeId)).collect(toCollection(ArrayList::new));
        notesWithTypeForChange.forEach(note -> holding.getAdministrativeNotes().add(note.getNote()));
        holding.setNotes(notesWithoutTypeForChange);
      } else {
        notesWithTypeForChange.forEach(note -> note.setHoldingsNoteTypeId(action.getUpdated()));
      }
    }
  }

  private Optional<Parameter> getTypeIdParameterOptional(List<Parameter> parameters) {
    return parameters.stream().filter(parameter -> StringUtils.equals(parameter.getKey(), HOLDINGS_NOTE_TYPE_ID_KEY)).findFirst();
  }

}
