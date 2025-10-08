package org.folio.bulkops.processor.folio;

import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.replace;
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
import static org.folio.bulkops.util.Constants.STAFF_ONLY_NOTE_PARAMETER_KEY;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.CirculationNote;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemNote;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.processor.Updater;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ItemsNotesUpdater {

  public static final String ITEM_NOTE_TYPE_ID_KEY = "ITEM_NOTE_TYPE_ID_KEY";
  public static final String ADMINISTRATIVE_NOTE_TYPE = ADMINISTRATIVE_NOTE.getValue();
  public static final String CHECK_IN_NOTE_TYPE = CHECK_IN_NOTE.getValue();
  public static final String CHECK_OUT_NOTE_TYPE = CHECK_OUT_NOTE.getValue();
  public static final List<String> NOTES_TYPES_TO_UPDATE = List.of(ADMINISTRATIVE_NOTE_TYPE,
          CHECK_IN_NOTE_TYPE, CHECK_OUT_NOTE_TYPE);

  private final AdministrativeNotesUpdater administrativeNotesUpdater;

  public Optional<Updater<ExtendedItem>> updateNotes(Action action, UpdateOptionType option) {
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

  private Optional<Updater<ExtendedItem>> setStaffOnly(Action action, UpdateOptionType option) {
    var markAsStaffValue = action.getType() == MARK_AS_STAFF_ONLY;
    if (option == CHECK_IN_NOTE) {
      return Optional.of(extendedItem -> {
        if (extendedItem.getEntity().getCirculationNotes() != null) {
          extendedItem.getEntity().getCirculationNotes().forEach(circulationNote -> {
            if (circulationNote.getNoteType() == CirculationNote.NoteTypeEnum.IN) {
              circulationNote.setStaffOnly(markAsStaffValue);
            }
          });
        }
      });
    } else if (option == CHECK_OUT_NOTE) {
      return Optional.of(extendedItem -> {
        if (extendedItem.getEntity().getCirculationNotes() != null) {
          extendedItem.getEntity().getCirculationNotes().forEach(circulationNote -> {
            if (circulationNote.getNoteType() == CirculationNote.NoteTypeEnum.OUT) {
              circulationNote.setStaffOnly(markAsStaffValue);
            }
          });
        }
      });
    } else if (option == ITEM_NOTE) {
      return Optional.of(extendedItem -> action.getParameters()
              .stream().filter(p -> StringUtils.equals(p.getKey(), ITEM_NOTE_TYPE_ID_KEY))
              .findFirst().ifPresent(parameter -> {
                var itemNoteTypeId = parameter.getValue();
                if (extendedItem.getEntity().getNotes() != null) {
                  extendedItem.getEntity().getNotes().forEach(itemNote -> {
                    if (StringUtils.equals(itemNoteTypeId, itemNote.getItemNoteTypeId())) {
                      itemNote.setStaffOnly(markAsStaffValue);
                    }
                  });
                }
              }));
    }
    return Optional.empty();
  }

  private Optional<Updater<ExtendedItem>> removeAll(Action action, UpdateOptionType option) {
    if (option == ADMINISTRATIVE_NOTE) {
      return Optional.of(extendedItem -> extendedItem.getEntity()
              .setAdministrativeNotes(administrativeNotesUpdater.removeAdministrativeNotes()));
    } else if (option == CHECK_IN_NOTE) {
      return Optional.of(extendedItem -> {
        if (extendedItem.getEntity().getCirculationNotes() != null) {
          var circulationNotes = extendedItem.getEntity().getCirculationNotes().stream()
                  .filter(circulationNote -> circulationNote.getNoteType()
                          != CirculationNote.NoteTypeEnum.IN).toList();
          extendedItem.getEntity().setCirculationNotes(circulationNotes);
        }
      });
    } else if (option == CHECK_OUT_NOTE) {
      return Optional.of(extendedItem -> {
        if (extendedItem.getEntity().getCirculationNotes() != null) {
          var circulationNotes = extendedItem.getEntity().getCirculationNotes().stream()
                  .filter(circulationNote -> circulationNote.getNoteType()
                          != CirculationNote.NoteTypeEnum.OUT).toList();
          extendedItem.getEntity().setCirculationNotes(circulationNotes);
        }
      });
    } else if (option == ITEM_NOTE) {
      return Optional.of(extendedItem -> action.getParameters()
              .stream().filter(parameter -> StringUtils.equals(parameter.getKey(),
                      ITEM_NOTE_TYPE_ID_KEY))
              .findFirst().ifPresent(parameter -> {
                var itemNoteTypeId = parameter.getValue();
                if (extendedItem.getEntity().getNotes() != null) {
                  var notes = extendedItem.getEntity().getNotes().stream()
                          .filter(note -> !StringUtils.equals(
                                  note.getItemNoteTypeId(), itemNoteTypeId)).toList();
                  extendedItem.getEntity().setNotes(notes);
                }
              }));
    }
    return Optional.empty();
  }

  private Optional<Updater<ExtendedItem>> addToExisting(Action action, UpdateOptionType option) {
    if (option == ADMINISTRATIVE_NOTE) {
      return Optional.of(extendedItem -> extendedItem.getEntity().setAdministrativeNotes(
              administrativeNotesUpdater.addToAdministrativeNotes(action.getUpdated(),
                      extendedItem.getEntity().getAdministrativeNotes())));
    } else if (option == CHECK_IN_NOTE || option == CHECK_OUT_NOTE) {
      var type = option == CHECK_IN_NOTE ? CirculationNote.NoteTypeEnum.IN
              : CirculationNote.NoteTypeEnum.OUT;
      var staffOnly = extractStaffOnlyParamValue(action);
      return Optional.of(extendedItem -> {
        var circulationNotes = extendedItem.getEntity().getCirculationNotes();
        var circulationNote = new CirculationNote().withNoteType(type)
                .withNote(action.getUpdated()).withStaffOnly(staffOnly);
        if (circulationNotes == null) {
          circulationNotes = new ArrayList<>();
        } else {
          circulationNotes = new ArrayList<>(circulationNotes);
        }
        extendedItem.getEntity().setCirculationNotes(circulationNotes);
        circulationNotes.add(circulationNote);
      });
    } else if (option == ITEM_NOTE) {
      var staffOnly = extractStaffOnlyParamValue(action);
      return Optional.of(extendedItem -> action.getParameters()
        .stream().filter(parameter -> StringUtils.equals(parameter.getKey(),
                      ITEM_NOTE_TYPE_ID_KEY))
        .findFirst().ifPresent(parameter -> {
          var note = new ItemNote()
                  .withItemNoteTypeId(parameter.getValue())
                  .withNote(action.getUpdated())
                  .withStaffOnly(staffOnly);
          var notes = extendedItem.getEntity().getNotes();
          if (notes == null) {
            notes = new ArrayList<>();
          } else {
            notes = new ArrayList<>(notes);
          }
          extendedItem.getEntity().setNotes(notes);
          notes.add(note);
        }));
    }
    return Optional.empty();
  }

  private Boolean extractStaffOnlyParamValue(org.folio.bulkops.domain.dto.Action action) {
    if (CollectionUtils.isEmpty(action.getParameters())) {
      return false;
    }
    return action.getParameters()
            .stream()
            .filter(p -> STAFF_ONLY_NOTE_PARAMETER_KEY.equalsIgnoreCase(p.getKey()))
            .map(p -> Boolean.valueOf(p.getValue()))
            .findFirst()
            .orElse(false);
  }

  private Optional<Updater<ExtendedItem>> changeType(Action action, UpdateOptionType option) {
    if (option == ADMINISTRATIVE_NOTE) {
      return Optional.of(extendedItem -> administrativeNotesUpdater
              .changeNoteTypeForAdministrativeNotes(extendedItem.getEntity(), action));
    } else if (option == CHECK_IN_NOTE || option == CHECK_OUT_NOTE) {
      return Optional.of(extendedItem -> {
        var noteTypeToUse = action.getUpdated();
        changeNoteTypeForCirculationNotes(extendedItem.getEntity(), noteTypeToUse, option);
      });
    } else if (option == ITEM_NOTE) {
      return Optional.of(extendedItem ->
        action.getParameters()
          .stream().filter(parameter -> StringUtils.equals(parameter.getKey(),
                        ITEM_NOTE_TYPE_ID_KEY))
          .findFirst().ifPresent(parameter -> {
            var noteTypeToUse = action.getUpdated();
            changeNoteTypeForItemNotes(extendedItem.getEntity(), noteTypeToUse,
                    parameter.getValue());
          }));
    }
    return Optional.empty();
  }

  private Optional<Updater<ExtendedItem>> findAndRemoveThese(Action action,
                                                             UpdateOptionType option) {
    return switch (option) {
      case ADMINISTRATIVE_NOTE -> Optional.of(
              extendedItem -> extendedItem.getEntity().setAdministrativeNotes(
                      administrativeNotesUpdater.findAndRemoveAdministrativeNote(
                              action.getInitial(),
                              extendedItem.getEntity().getAdministrativeNotes())));
      case CHECK_IN_NOTE, CHECK_OUT_NOTE -> {
        var type = CHECK_IN_NOTE.equals(option) ? CirculationNote.NoteTypeEnum.IN
                : CirculationNote.NoteTypeEnum.OUT;
        yield Optional.of(extendedItem -> {
          if (extendedItem.getEntity().getCirculationNotes() != null) {
            extendedItem.getEntity().getCirculationNotes().stream()
                    .filter(circulationNote -> circulationNote.getNoteType() == type)
                    .forEach(note -> note.setNote(note.getNote()
                            .replace(action.getInitial(), EMPTY)));
          }
        });
      }
      case ITEM_NOTE -> Optional.of(extendedItem -> action.getParameters()
              .stream().filter(parameter -> StringUtils.equals(parameter.getKey(),
                      ITEM_NOTE_TYPE_ID_KEY))
              .findFirst().ifPresent(parameter -> {
                if (extendedItem.getEntity().getNotes() != null) {
                  extendedItem.getEntity().getNotes().stream()
                          .filter(note -> StringUtils.equals(note.getItemNoteTypeId(),
                                  parameter.getValue()))
                          .forEach(note -> note.setNote(note.getNote()
                                  .replace(action.getInitial(), EMPTY)));
                }
              }));
      default -> Optional.empty();
    };
  }

  private Optional<Updater<ExtendedItem>> findAndReplace(Action action, UpdateOptionType option) {
    return switch (option) {
      case ADMINISTRATIVE_NOTE -> Optional.of(
              extendedItem -> extendedItem.getEntity().setAdministrativeNotes(
                      administrativeNotesUpdater.findAndReplaceAdministrativeNote(action,
                              extendedItem.getEntity().getAdministrativeNotes())));
      case CHECK_IN_NOTE, CHECK_OUT_NOTE -> {
        var type = CHECK_IN_NOTE.equals(option) ? CirculationNote.NoteTypeEnum.IN
                : CirculationNote.NoteTypeEnum.OUT;
        yield Optional.of(extendedItem -> {
          if (extendedItem.getEntity().getCirculationNotes() != null) {
            extendedItem.getEntity().getCirculationNotes().forEach(circulationNote -> {
              if (contains(circulationNote.getNote(), action.getInitial())
                      && type == circulationNote.getNoteType()) {
                circulationNote.setNote(replace(circulationNote.getNote(),
                        action.getInitial(), action.getUpdated()));
              }
            });
          }
        });
      }
      case ITEM_NOTE -> Optional.of(extendedItem -> action.getParameters()
              .stream().filter(parameter -> StringUtils.equals(parameter.getKey(),
                      ITEM_NOTE_TYPE_ID_KEY))
              .findFirst().ifPresent(parameter -> {
                if (extendedItem.getEntity().getNotes() != null) {
                  extendedItem.getEntity().getNotes().forEach(itemNote -> {
                    if (StringUtils.equals(itemNote.getItemNoteTypeId(), parameter.getValue())
                            && contains(itemNote.getNote(), action.getInitial())) {
                      itemNote.setNote(replace(itemNote.getNote(), action.getInitial(),
                              action.getUpdated()));
                    }
                  });
                }
              }));
      default -> Optional.empty();
    };
  }

  private Optional<Updater<ExtendedItem>> duplicate(Action action, UpdateOptionType option) {
    if (option == CHECK_IN_NOTE || option == CHECK_OUT_NOTE) {
      return Optional.of(extendedItem -> {
        var noteTypeToUse = action.getUpdated();
        if (extendedItem.getEntity().getCirculationNotes() != null) {
          var circNoteTypeToDuplicate = CHECK_OUT_NOTE_TYPE.equals(noteTypeToUse)
                  ? CirculationNote.NoteTypeEnum.OUT : CirculationNote.NoteTypeEnum.IN;
          var circNotesToDuplicate = extendedItem.getEntity().getCirculationNotes().stream()
                  .filter(circNote -> circNote.getNoteType() != circNoteTypeToDuplicate).toList();
          circNotesToDuplicate.forEach(circNote -> {
            var createdNote = circNote.toBuilder().build();
            if (StringUtils.isNotEmpty(createdNote.getId())) {
              createdNote.setId(null);
            }
            createdNote.setNoteType(circNoteTypeToDuplicate);
            extendedItem.getEntity().getCirculationNotes().add(createdNote);
          });
        }
      });
    }
    return Optional.empty();
  }

  private void changeNoteTypeForCirculationNotes(Item item, String noteTypeToUse,
                                                 UpdateOptionType optionType) {
    if (item.getCirculationNotes() != null) {
      CirculationNote.NoteTypeEnum typeForChange = optionType == CHECK_IN_NOTE
              ? CirculationNote.NoteTypeEnum.IN : CirculationNote.NoteTypeEnum.OUT;
      if (CHECK_OUT_NOTE_TYPE.equals(noteTypeToUse) || CHECK_IN_NOTE_TYPE.equals(noteTypeToUse)) {
        var useType = typeForChange == CirculationNote.NoteTypeEnum.IN
                ? CirculationNote.NoteTypeEnum.OUT : CirculationNote.NoteTypeEnum.IN;
        item.getCirculationNotes().forEach(note -> note.setNoteType(useType));
      } else {
        var circNotesWithoutTypeForChange = item.getCirculationNotes().stream()
                .filter(circulationNote -> circulationNote.getNoteType() != typeForChange)
                .collect(toCollection(ArrayList::new));
        var circNotesWithTypeForChange = item.getCirculationNotes().stream()
                .filter(circulationNote -> circulationNote.getNoteType() == typeForChange)
                .toList();
        if (!circNotesWithTypeForChange.isEmpty()) {
          if (item.getAdministrativeNotes() == null) {
            item.setAdministrativeNotes(new ArrayList<>());
          }
          if (item.getNotes() == null) {
            item.setNotes(new ArrayList<>());
          }
          circNotesWithTypeForChange.forEach(note -> {
            if (ADMINISTRATIVE_NOTE_TYPE.equals(noteTypeToUse)) {
              item.getAdministrativeNotes().add(note.getNote());
            } else {
              item.getNotes().add(new ItemNote().withItemNoteTypeId(noteTypeToUse)
                      .withNote(note.getNote()).withStaffOnly(note.getStaffOnly()));
            }
          });
          item.setCirculationNotes(circNotesWithoutTypeForChange);
        }
      }
    }
  }

  private void changeNoteTypeForItemNotes(Item item, String noteTypeToUse, String noteTypeId) {
    if (item.getNotes() != null) {
      var notesWithTypeForChange = item.getNotes().stream()
              .filter(note -> StringUtils.equals(note.getItemNoteTypeId(), noteTypeId)).toList();
      if (NOTES_TYPES_TO_UPDATE.contains(noteTypeToUse)) {
        if (!notesWithTypeForChange.isEmpty()) {
          var notesWithoutTypeForChange = item.getNotes().stream()
                  .filter(note -> !StringUtils.equals(note.getItemNoteTypeId(),
                          noteTypeId)).collect(toCollection(ArrayList::new));
          if (item.getAdministrativeNotes() == null) {
            item.setAdministrativeNotes(new ArrayList<>());
          }
          if (item.getCirculationNotes() == null) {
            item.setCirculationNotes(new ArrayList<>());
          }
          notesWithTypeForChange.forEach(note -> {
            if (CHECK_IN_NOTE_TYPE.equals(noteTypeToUse)) {
              item.getCirculationNotes().add(new CirculationNote().withNoteType(
                      CirculationNote.NoteTypeEnum.IN)
                      .withNote(note.getNote()).withStaffOnly(note.getStaffOnly()));
            }
            if (CHECK_OUT_NOTE_TYPE.equals(noteTypeToUse)) {
              item.getCirculationNotes().add(new CirculationNote().withNoteType(
                      CirculationNote.NoteTypeEnum.OUT)
                      .withNote(note.getNote()).withStaffOnly(note.getStaffOnly()));
            }
            if (ADMINISTRATIVE_NOTE_TYPE.equals(noteTypeToUse)) {
              item.getAdministrativeNotes().add(note.getNote());
            }
          });
          item.setNotes(notesWithoutTypeForChange);
        }
      } else {
        notesWithTypeForChange.forEach(note -> note.setItemNoteTypeId(noteTypeToUse));
      }
    }
  }
}
