package org.folio.bulkops.processor;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.folio.bulkops.domain.dto.UpdateActionType.MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.INSTANCE_NOTE;
import static org.folio.bulkops.util.Constants.STAFF_ONLY_NOTE_PARAMETER_KEY;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.InstanceNote;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@AllArgsConstructor
public class InstanceNotesUpdaterFactory {

  public static final String INSTANCE_NOTE_TYPE_ID_KEY = "INSTANCE_NOTE_TYPE_ID_KEY";
  private final AdministrativeNotesUpdater administrativeNotesUpdater;

  public Optional<Updater<Instance>> getUpdater(UpdateOptionType option, Action action) {
    return switch (action.getType()) {
      case MARK_AS_STAFF_ONLY, REMOVE_MARK_AS_STAFF_ONLY -> Optional.of(instance -> setStaffOnly(instance, action, option));
      case REMOVE_ALL -> Optional.of(instance -> removeAll(instance, action, option));
      case ADD_TO_EXISTING -> Optional.of(instance -> addToExisting(instance, action, option));
      case FIND_AND_REMOVE_THESE -> Optional.of(instance -> findAndRemove(instance, action, option));
      case FIND_AND_REPLACE -> Optional.of(instance -> findAndReplace(instance, action, option));
      case CHANGE_TYPE -> Optional.of(instance -> changeNoteType(instance, action, option));
      default -> Optional.empty();
    };
  }

  private void setStaffOnly(Instance instance, Action action, UpdateOptionType option) {
    if (INSTANCE_NOTE.equals(option) && isNotEmpty(instance.getInstanceNotes())) {
      getTypeIdOptional(action.getParameters())
        .ifPresent(typeId -> instance.getInstanceNotes().stream()
          .filter(instanceNote -> typeId.equals(instanceNote.getInstanceNoteTypeId()))
          .forEach(instanceNote -> instanceNote.setStaffOnly(MARK_AS_STAFF_ONLY.equals(action.getType()))));
    }
  }

  private void removeAll(Instance instance, Action action, UpdateOptionType option) {
    if (ADMINISTRATIVE_NOTE.equals(option)) {
      instance.setAdministrativeNotes(Collections.emptyList());
    } else if (INSTANCE_NOTE.equals(option)) {
      instance.setInstanceNotes(removeNotesByTypeId(instance.getInstanceNotes(), action.getParameters()));
    }
  }

  private void addToExisting(Instance instance, Action action, UpdateOptionType option) {
    if (ADMINISTRATIVE_NOTE.equals(option)) {
      instance.setAdministrativeNotes(administrativeNotesUpdater.addToAdministrativeNotes(action.getUpdated(), instance.getAdministrativeNotes()));
    } else if (INSTANCE_NOTE.equals(option)) {
      instance.setInstanceNotes(addToNotesByTypeId(instance.getInstanceNotes(), action.getParameters(), action.getUpdated()));
    }
  }

  private void findAndRemove(Instance instance, Action action, UpdateOptionType option) {
    if (ADMINISTRATIVE_NOTE.equals(option)) {
      instance.setAdministrativeNotes(administrativeNotesUpdater.findAndRemoveAdministrativeNote(action.getInitial(), instance.getAdministrativeNotes()));
    } else if (INSTANCE_NOTE.equals(option)) {
      instance.setInstanceNotes(findAndRemoveNoteByValueAndTypeId(action.getInitial(), instance.getInstanceNotes(), action.getParameters()));
    }
  }

  private void findAndReplace(Instance instance, Action action, UpdateOptionType option) {
    if (ADMINISTRATIVE_NOTE.equals(option)) {
      instance.setAdministrativeNotes(administrativeNotesUpdater.findAndReplaceAdministrativeNote(action, instance.getAdministrativeNotes()));
    } else if (INSTANCE_NOTE.equals(option)) {
      findAndReplaceNoteByValueAndTypeId(action, instance.getInstanceNotes());
    }
  }

  private void changeNoteType(Instance instance, Action action, UpdateOptionType option) {
    if (INSTANCE_NOTE.equals(option) && isNotEmpty(instance.getInstanceNotes())) {
      var typeIdOptional = getTypeIdOptional(action.getParameters());
      if (typeIdOptional.isPresent()) {
        var typeId = typeIdOptional.get();
        if (ADMINISTRATIVE_NOTE.getValue().equals(action.getUpdated())) {
          moveInstanceNotesToAdministrativeNotesByTypeId(instance, typeId);
        } else {
          instance.getInstanceNotes().forEach(instanceNote -> {
            if (typeId.equals(instanceNote.getInstanceNoteTypeId())) {
              instanceNote.setInstanceNoteTypeId(action.getUpdated());
            }
          });
        }
      }
    } else if (ADMINISTRATIVE_NOTE.equals(option)) {
      administrativeNotesUpdater.changeNoteTypeForAdministrativeNotes(instance, action);
    }
  }

  private void moveInstanceNotesToAdministrativeNotesByTypeId(Instance instance, String typeId) {
    List<String> administrativeNotes = isNull(instance.getAdministrativeNotes()) ?
      new ArrayList<>() :
      new ArrayList<>(instance.getAdministrativeNotes());
    instance.getInstanceNotes().stream()
      .filter(instanceNote -> typeId.equals(instanceNote.getInstanceNoteTypeId()))
      .map(InstanceNote::getNote)
      .forEach(administrativeNotes::add);
    instance.setAdministrativeNotes(administrativeNotes);
    instance.setInstanceNotes(instance.getInstanceNotes().stream()
      .filter(instanceNote -> !typeId.equals(instanceNote.getInstanceNoteTypeId()))
      .toList());
  }

  private List<InstanceNote> removeNotesByTypeId(List<InstanceNote> notes, List<Parameter> parameters) {
    var typeIdOptional = getTypeIdOptional(parameters);
    if (typeIdOptional.isPresent()) {
      var typeId = typeIdOptional.get();
      if (notes != null) {
        var notesWithoutRemoved = notes.stream().filter(note -> !StringUtils.equals(note.getInstanceNoteTypeId(), typeId)).toList();
        return new ArrayList<>(notesWithoutRemoved);
      }}
    return notes;
  }

  private List<InstanceNote> addToNotesByTypeId(List<InstanceNote> notes, List<Parameter> parameters, String noteValue) {
    var staffOnly = extractStaffOnlyParamValue(parameters);
    var typeIdParameterOptional = getTypeIdOptional(parameters);
    if (typeIdParameterOptional.isPresent()) {
      var note = new InstanceNote().withInstanceNoteTypeId(typeIdParameterOptional.get())
        .withNote(noteValue).withStaffOnly(staffOnly);
      notes = isNull(notes) ? new ArrayList<>() : new ArrayList<>(notes);
      notes.add(note);
    }
    return notes;
  }

  private Boolean extractStaffOnlyParamValue(List<Parameter> parameters) {
    return isNotEmpty(parameters) && parameters.stream()
      .filter(p -> STAFF_ONLY_NOTE_PARAMETER_KEY.equalsIgnoreCase(p.getKey()))
      .map(p -> Boolean.valueOf(p.getValue()))
      .findFirst()
      .orElse(false);
  }

  private List<InstanceNote> findAndRemoveNoteByValueAndTypeId(String noteToRemove, List<InstanceNote> notes, List<Parameter> parameters) {
    var typeIdOptional = getTypeIdOptional(parameters);
    if (typeIdOptional.isPresent() && notes != null) {
      notes = notes.stream().filter(note -> !(StringUtils.equals(note.getInstanceNoteTypeId(), typeIdOptional.get())
              && StringUtils.equals(note.getNote(), noteToRemove))).collect(toCollection(ArrayList::new));
    }
    return notes;
  }

  private void findAndReplaceNoteByValueAndTypeId(Action action, List<InstanceNote> notes) {
    var typeIdOptional = getTypeIdOptional(action.getParameters());
    if (typeIdOptional.isPresent() && notes != null) {
      notes.forEach(note -> {
        if (StringUtils.equals(note.getInstanceNoteTypeId(), typeIdOptional.get())
              && StringUtils.equals(note.getNote(), action.getInitial())) note.setNote(action.getUpdated());
      });
    }
  }

  private Optional<String> getTypeIdOptional(List<Parameter> parameters) {
    return parameters.stream()
      .filter(parameter -> INSTANCE_NOTE_TYPE_ID_KEY.equals(parameter.getKey()))
      .findFirst()
      .map(Parameter::getValue);
  }

}
