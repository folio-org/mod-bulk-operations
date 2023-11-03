package org.folio.bulkops.processor;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.CirculationNote;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemNote;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toCollection;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADD_TO_EXISTING;
import static org.folio.bulkops.domain.dto.UpdateActionType.CHANGE_TYPE;
import static org.folio.bulkops.domain.dto.UpdateActionType.DUPLICATE;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REMOVE_THESE;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REPLACE;
import static org.folio.bulkops.domain.dto.UpdateActionType.MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_ALL;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.CHECK_IN_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.CHECK_OUT_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ITEM_NOTE;

@Component
@AllArgsConstructor
public class ItemsNotesUpdater {

  public static final String ITEM_NOTE_TYPE_ID_KEY = "ITEM_NOTE_TYPE_ID_KEY";
  public static final String ADMINISTRATIVE_NOTE_TYPE = ADMINISTRATIVE_NOTE.getValue();
  public static final String CHECK_IN_NOTE_TYPE = CHECK_IN_NOTE.getValue();
  public static final String CHECK_OUT_NOTE_TYPE = CHECK_OUT_NOTE.getValue();
  public static final List<String> NOTES_TYPES_TO_UPDATE = List.of(ADMINISTRATIVE_NOTE_TYPE, CHECK_IN_NOTE_TYPE, CHECK_OUT_NOTE_TYPE);

  private final AdministrativeNotesUpdater administrativeNotesUpdater;

  public Optional<Updater<Item>> updateNotes(Action action, UpdateOptionType option) {
    if (MARK_AS_STAFF_ONLY == action.getType() || REMOVE_MARK_AS_STAFF_ONLY == action.getType()) {
      return setStaffOnly(action, option);
    } else if (REMOVE_ALL == action.getType()) {
      return removeAll(action, option);
    } else if (ADD_TO_EXISTING == action.getType()) {
      return addToExisting(action, option);
    } else if (FIND_AND_REMOVE_THESE == action.getType()) {
      return findAndRemoveThese(action, option);
    } else if (FIND_AND_REPLACE == action.getType()) {
      return findAndReplace(action, option);
    } else if (CHANGE_TYPE == action.getType()) {
      return changeType(action, option);
    } else if (DUPLICATE == action.getType()) {
      return duplicate(action, option);
    }
    return Optional.empty();
  }

  private Optional<Updater<Item>> setStaffOnly(Action action, UpdateOptionType option) {
    var markAsStaffValue = action.getType() == MARK_AS_STAFF_ONLY;
    if (option == CHECK_IN_NOTE) {
      return Optional.of(item -> {
        if (item.getCirculationNotes() != null) {
          item.getCirculationNotes().forEach(circulationNote -> {
            if (circulationNote.getNoteType() == CirculationNote.NoteTypeEnum.IN)
              circulationNote.setStaffOnly(markAsStaffValue);
          });
        }});
    } else if (option == CHECK_OUT_NOTE) {
      return Optional.of(item -> {
        if (item.getCirculationNotes() != null) {
          item.getCirculationNotes().forEach(circulationNote -> {
            if (circulationNote.getNoteType() == CirculationNote.NoteTypeEnum.OUT)
              circulationNote.setStaffOnly(markAsStaffValue);
          });
        }});
    } else if (option == ITEM_NOTE) {
      return Optional.of(item -> action.getParameters()
        .stream().filter(p -> StringUtils.equals(p.getKey(), ITEM_NOTE_TYPE_ID_KEY))
        .findFirst().ifPresent(parameter -> {
          var itemNoteTypeId = parameter.getValue();
          if (item.getNotes() != null) {
            item.getNotes().forEach(itemNote -> {
              if (StringUtils.equals(itemNoteTypeId, itemNote.getItemNoteTypeId())) {
                itemNote.setStaffOnly(markAsStaffValue);
              }
            });
          }}));
    }
    return Optional.empty();
  }

  private Optional<Updater<Item>> removeAll(Action action, UpdateOptionType option) {
    if (option == ADMINISTRATIVE_NOTE) {
      return Optional.of(item -> item.setAdministrativeNotes(administrativeNotesUpdater.removeAdministrativeNotes()));
    } else if (option == CHECK_IN_NOTE) {
      return Optional.of(item -> {
        if (item.getCirculationNotes() != null) {
          var circulationNotes = item.getCirculationNotes().stream()
            .filter(circulationNote -> circulationNote.getNoteType() != CirculationNote.NoteTypeEnum.IN).toList();
          item.setCirculationNotes(circulationNotes);
        }});
    } else if (option == CHECK_OUT_NOTE) {
      return Optional.of(item -> {
        if (item.getCirculationNotes() != null) {
          var circulationNotes = item.getCirculationNotes().stream()
            .filter(circulationNote -> circulationNote.getNoteType() != CirculationNote.NoteTypeEnum.OUT).toList();
          item.setCirculationNotes(circulationNotes);
        }});
    } else if (option == ITEM_NOTE) {
      return Optional.of(item -> action.getParameters()
        .stream().filter(parameter -> StringUtils.equals(parameter.getKey(), ITEM_NOTE_TYPE_ID_KEY))
        .findFirst().ifPresent(parameter -> {
          var itemNoteTypeId = parameter.getValue();
          if (item.getNotes() != null) {
            var notes = item.getNotes().stream().filter(note -> !StringUtils.equals(note.getItemNoteTypeId(), itemNoteTypeId)).toList();
            item.setNotes(notes);
          }}));
    }
    return Optional.empty();
  }

  private Optional<Updater<Item>> addToExisting(Action action, UpdateOptionType option) {
    if (option == ADMINISTRATIVE_NOTE) {
      return Optional.of(item -> item.setAdministrativeNotes(administrativeNotesUpdater.addToAdministrativeNotes(action.getUpdated(), item.getAdministrativeNotes())));
    } else if (option == CHECK_IN_NOTE || option == CHECK_OUT_NOTE) {
      var type = option == CHECK_IN_NOTE ? CirculationNote.NoteTypeEnum.IN : CirculationNote.NoteTypeEnum.OUT;
      return Optional.of(item -> {
        var circulationNotes = item.getCirculationNotes();
        var circulationNote = new CirculationNote().withNoteType(type)
          .withNote(action.getUpdated()).withStaffOnly(false);
        if (circulationNotes == null) {
          circulationNotes = new ArrayList<>();
        } else {
          circulationNotes = new ArrayList<>(circulationNotes);
        }
        item.setCirculationNotes(circulationNotes);
        circulationNotes.add(circulationNote);
      });
    } else if (option == ITEM_NOTE) {
      return Optional.of(item -> action.getParameters()
        .stream().filter(parameter -> StringUtils.equals(parameter.getKey(), ITEM_NOTE_TYPE_ID_KEY))
        .findFirst().ifPresent(parameter -> {
          var note = new ItemNote().withItemNoteTypeId(parameter.getValue()).withNote(action.getUpdated());
          var notes = item.getNotes();
          if (notes == null) {
            notes = new ArrayList<>();
          } else {
            notes = new ArrayList<>(notes);
          }
          item.setNotes(notes);
          notes.add(note);
        }));
    }
    return Optional.empty();
  }

  private Optional<Updater<Item>> changeType(Action action, UpdateOptionType option) {
    if (option == ADMINISTRATIVE_NOTE) {
      return Optional.of(item -> administrativeNotesUpdater.changeNoteTypeForAdministrativeNotes(item, action));
    } else if (option == CHECK_IN_NOTE || option == CHECK_OUT_NOTE) {
      return Optional.of(item -> {
        var noteTypeToUse = action.getUpdated();
        changeNoteTypeForCirculationNotes(item, noteTypeToUse, option);
      });
    } else if (option == ITEM_NOTE) {
      return Optional.of(item ->
        action.getParameters()
          .stream().filter(parameter -> StringUtils.equals(parameter.getKey(), ITEM_NOTE_TYPE_ID_KEY))
          .findFirst().ifPresent(parameter -> {
            var noteTypeToUse = action.getUpdated();
            changeNoteTypeForItemNotes(item, noteTypeToUse, parameter.getValue());
          }));
    }
    return Optional.empty();
  }

  private Optional<Updater<Item>> findAndRemoveThese(Action action, UpdateOptionType option) {
    if (option == ADMINISTRATIVE_NOTE) {
      return Optional.of(item -> item.setAdministrativeNotes(administrativeNotesUpdater.findAndRemoveAdministrativeNote(action.getInitial(), item.getAdministrativeNotes())));
    } else if (option == CHECK_IN_NOTE || option == CHECK_OUT_NOTE) {
      var circType = option == CHECK_IN_NOTE ? CirculationNote.NoteTypeEnum.IN : CirculationNote.NoteTypeEnum.OUT;
      return Optional.of(item -> {
        if (item.getCirculationNotes() != null) {
          var circulationNotes = item.getCirculationNotes().stream()
            .filter(circulationNote -> !(StringUtils.equals(circulationNote.getNote(), action.getInitial())
              && circulationNote.getNoteType() == circType)).toList();
          item.setCirculationNotes(circulationNotes);
        }});
    } else if (option == ITEM_NOTE) {
      return Optional.of(item -> action.getParameters()
        .stream().filter(parameter -> StringUtils.equals(parameter.getKey(), ITEM_NOTE_TYPE_ID_KEY))
        .findFirst().ifPresent(parameter -> {
          if (item.getNotes() != null) {
            var itemNotes = item.getNotes().stream()
              .filter(note -> !(StringUtils.equals(note.getItemNoteTypeId(), parameter.getValue())
                && StringUtils.equals(note.getNote(), action.getInitial()))).toList();
            item.setNotes(itemNotes);
          }
        }));
    }
    return Optional.empty();
  }

  private Optional<Updater<Item>> findAndReplace(Action action, UpdateOptionType option) {
    if (option == ADMINISTRATIVE_NOTE) {
      return Optional.of(item -> item.setAdministrativeNotes(administrativeNotesUpdater.findAndReplaceAdministrativeNote(action, item.getAdministrativeNotes())));
    } else if (option == CHECK_IN_NOTE || option == CHECK_OUT_NOTE) {
      var type = option == CHECK_IN_NOTE ? CirculationNote.NoteTypeEnum.IN : CirculationNote.NoteTypeEnum.OUT;
      return Optional.of(item -> {
        if (item.getCirculationNotes() != null) {
          item.getCirculationNotes().forEach(circulationNote -> {
            if (StringUtils.equals(circulationNote.getNote(), action.getInitial()) && type == circulationNote.getNoteType())
              circulationNote.setNote(action.getUpdated());
          });
        }
      });
    } else if (option == ITEM_NOTE) {
      return Optional.of(item -> action.getParameters()
        .stream().filter(parameter -> StringUtils.equals(parameter.getKey(), ITEM_NOTE_TYPE_ID_KEY))
        .findFirst().ifPresent(parameter -> {
          if (item.getNotes() != null) {
            item.getNotes().forEach(itemNote -> {
              if (StringUtils.equals(itemNote.getItemNoteTypeId(), parameter.getValue())
                && StringUtils.equals(itemNote.getNote(), action.getInitial())) itemNote.setNote(action.getUpdated());
            });
          }
        }));
    }
    return Optional.empty();
  }

  private Optional<Updater<Item>> duplicate(Action action, UpdateOptionType option) {
    if (option == CHECK_IN_NOTE || option == CHECK_OUT_NOTE) {
      return Optional.of(item -> {
        var noteTypeToUse = action.getUpdated();
        if (item.getCirculationNotes() != null) {
          var circNoteTypeToDuplicate = CHECK_OUT_NOTE_TYPE.equals(noteTypeToUse) ? CirculationNote.NoteTypeEnum.OUT : CirculationNote.NoteTypeEnum.IN;
          var circNotesToDuplicate = item.getCirculationNotes().stream().filter(circNote -> circNote.getNoteType() != circNoteTypeToDuplicate).toList();
          circNotesToDuplicate.forEach(circNote -> {
            var createdNote = circNote.toBuilder().build();
            if (StringUtils.isNotEmpty(createdNote.getId())) createdNote.setId(null);
            createdNote.setNoteType(circNoteTypeToDuplicate);
            item.getCirculationNotes().add(createdNote);
          });
        }
      });
    }
    return Optional.empty();
  }

  private void changeNoteTypeForCirculationNotes(Item item, String noteTypeToUse, UpdateOptionType optionType) {
    if (item.getCirculationNotes() != null) {
      CirculationNote.NoteTypeEnum typeForChange = optionType == CHECK_IN_NOTE ? CirculationNote.NoteTypeEnum.IN : CirculationNote.NoteTypeEnum.OUT;
      if (CHECK_OUT_NOTE_TYPE.equals(noteTypeToUse) || CHECK_IN_NOTE_TYPE.equals(noteTypeToUse)) {
        var useType = typeForChange == CirculationNote.NoteTypeEnum.IN ? CirculationNote.NoteTypeEnum.OUT : CirculationNote.NoteTypeEnum.IN;
        item.getCirculationNotes().forEach(note -> note.setNoteType(useType));
      } else {
        var circNotesWithoutTypeForChange = item.getCirculationNotes().stream().filter(circulationNote -> circulationNote.getNoteType() != typeForChange).collect(toCollection(ArrayList::new));
        var circNotesWithTypeForChange = item.getCirculationNotes().stream().filter(circulationNote -> circulationNote.getNoteType() == typeForChange).toList();
        if (!circNotesWithTypeForChange.isEmpty()) {
          if (item.getAdministrativeNotes() == null) item.setAdministrativeNotes(new ArrayList<>());
          if (item.getNotes() == null) item.setNotes(new ArrayList<>());
          circNotesWithTypeForChange.forEach(note -> {
            if (ADMINISTRATIVE_NOTE_TYPE.equals(noteTypeToUse)) {
              item.getAdministrativeNotes().add(note.getNote());
            } else {
              item.getNotes().add(new ItemNote().withItemNoteTypeId(noteTypeToUse).withNote(note.getNote()));
            }
          });
          item.setCirculationNotes(circNotesWithoutTypeForChange);
        }
      }
    }
  }

  private void changeNoteTypeForItemNotes(Item item, String noteTypeToUse, String noteTypeId) {
    if (item.getNotes() != null) {
      var notesWithTypeForChange = item.getNotes().stream().filter(note -> StringUtils.equals(note.getItemNoteTypeId(), noteTypeId)).toList();
      if (NOTES_TYPES_TO_UPDATE.contains(noteTypeToUse)) {
        if (!notesWithTypeForChange.isEmpty()) {
          var notesWithoutTypeForChange = item.getNotes().stream().filter(note -> !StringUtils.equals(note.getItemNoteTypeId(), noteTypeId)).collect(toCollection(ArrayList::new));
          if (item.getAdministrativeNotes() == null) item.setAdministrativeNotes(new ArrayList<>());
          if (item.getCirculationNotes() == null) item.setCirculationNotes(new ArrayList<>());
          notesWithTypeForChange.forEach(note -> {
            if (CHECK_IN_NOTE_TYPE.equals(noteTypeToUse))
              item.getCirculationNotes().add(new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.IN).withNote(note.getNote()).withStaffOnly(false));
            if (CHECK_OUT_NOTE_TYPE.equals(noteTypeToUse))
              item.getCirculationNotes().add(new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.OUT).withNote(note.getNote()).withStaffOnly(false));
            if (ADMINISTRATIVE_NOTE_TYPE.equals(noteTypeToUse)) item.getAdministrativeNotes().add(note.getNote());
          });
          item.setNotes(notesWithoutTypeForChange);
        }
      } else {
        notesWithTypeForChange.forEach(note -> note.setItemNoteTypeId(noteTypeToUse));
      }
    }
  }
}
