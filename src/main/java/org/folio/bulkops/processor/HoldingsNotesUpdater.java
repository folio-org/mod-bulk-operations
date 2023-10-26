package org.folio.bulkops.processor;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.HoldingsNote;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.Parameter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toCollection;


@Component
public class HoldingsNotesUpdater {
  public static final String HOLDINGS_NOTE_TYPE_ID_KEY = "HOLDINGS_NOTE_TYPE_ID_KEY";

  public void setMarkAsStaffForNotesByTypeId(List<HoldingsNote> notes, List<Parameter> parameters, boolean markAsStaffValue) {
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

  public List<String> removeAdministrativeNotes() {
    return new ArrayList<>();
  }

  public List<HoldingsNote> removeNotesByTypeId(List<HoldingsNote> notes, List<Parameter> parameters) {
    var typeIdParameterOptional = getTypeIdParameterOptional(parameters);
    if (typeIdParameterOptional.isPresent()) {
      var typeId = typeIdParameterOptional.get().getValue();
      if (notes != null) {
        var notesWithoutRemoved = notes.stream().filter(note -> !StringUtils.equals(note.getHoldingsNoteTypeId(), typeId)).toList();
        return new ArrayList<>(notesWithoutRemoved);
      }}
    return notes;
  }

  public List<String> addToAdministrativeNotes(String administrativeNote, List<String> administrativeNotes) {
    if (administrativeNotes == null) {
      administrativeNotes = new ArrayList<>();
    }
    administrativeNotes.add(administrativeNote);
    return administrativeNotes;
  }

  public List<HoldingsNote> addToNotesByTypeId(List<HoldingsNote> notes, List<Parameter> parameters, String noteValue) {
    var typeIdParameterOptional = getTypeIdParameterOptional(parameters);
    if (typeIdParameterOptional.isPresent()) {
      var note = new HoldingsNote().withHoldingsNoteTypeId(typeIdParameterOptional.get().getValue()).withNote(noteValue);
      if (notes == null) {
        notes = new ArrayList<>();
      }
      notes.add(note);
    }
    return notes;
  }

  public List<String> findAndRemoveAdministrativeNote(String administrativeNoteToRemove, List<String> administrativeNotes) {
    if (administrativeNotes != null) {
      administrativeNotes = administrativeNotes.stream()
        .filter(administrativeNote -> !StringUtils.equals(administrativeNote, administrativeNoteToRemove))
        .collect(toCollection(ArrayList::new));
    }
    return administrativeNotes;
  }

  public List<HoldingsNote> findAndRemoveNoteByValueAndTypeId(String noteToRemove, List<HoldingsNote> notes, List<Parameter> parameters) {
    var typeIdParameterOptional = getTypeIdParameterOptional(parameters);
    if (typeIdParameterOptional.isPresent() && notes != null) {
      notes = notes.stream().filter(note -> !(StringUtils.equals(note.getHoldingsNoteTypeId(), typeIdParameterOptional.get().getValue())
              && StringUtils.equals(note.getNote(), noteToRemove))).collect(toCollection(ArrayList::new));
    }
    return notes;
  }

  public List<String> findAndReplaceAdministrativeNote(Action action, List<String> administrativeNotes) {
    if (administrativeNotes != null) {
      administrativeNotes = administrativeNotes.stream().map(administrativeNote -> {
        if (StringUtils.equals(administrativeNote, action.getInitial())) {
          return action.getUpdated();
        }
        return administrativeNote;
      }).collect(toCollection(ArrayList::new));
    }
    return administrativeNotes;
  }

  public void findAndReplaceNoteByValueAndTypeId(Action action, List<HoldingsNote> notes) {
    var typeIdParameterOptional = getTypeIdParameterOptional(action.getParameters());
    if (typeIdParameterOptional.isPresent() && notes != null) {
      notes.forEach(note -> {
        if (StringUtils.equals(note.getHoldingsNoteTypeId(), typeIdParameterOptional.get().getValue())
              && StringUtils.equals(note.getNote(), action.getInitial())) note.setNote(action.getUpdated());
      });
    }
  }

  private Optional<Parameter> getTypeIdParameterOptional(List<Parameter> parameters) {
    return parameters.stream().filter(parameter -> StringUtils.equals(parameter.getKey(), HOLDINGS_NOTE_TYPE_ID_KEY)).findFirst();
  }

}
