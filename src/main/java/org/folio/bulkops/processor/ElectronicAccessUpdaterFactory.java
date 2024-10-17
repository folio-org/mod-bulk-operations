package org.folio.bulkops.processor;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import org.folio.bulkops.domain.bean.ElectronicAccessEntity;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ElectronicAccessUpdaterFactory {

  public Updater<? extends ElectronicAccessEntity> updater(UpdateOptionType option, Action action) {
    return switch (option) {
      case ELECTRONIC_ACCESS_URL_RELATIONSHIP -> updateUrlRelationship(option, action);
      case ELECTRONIC_ACCESS_URI -> updateUri(option, action);
      case ELECTRONIC_ACCESS_LINK_TEXT -> updateLinkText(option, action);
      case ELECTRONIC_ACCESS_MATERIALS_SPECIFIED -> updateMaterialsSpecified(option, action);
      case ELECTRONIC_ACCESS_URL_PUBLIC_NOTE -> updatePublicNote(option, action);
      default -> notSupported(option, action);
    };
  }

  private Updater<? extends ElectronicAccessEntity> updateUrlRelationship(UpdateOptionType option, Action action) {
    return switch (action.getType()) {
      case CLEAR_FIELD -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.forEach(electronicAccess -> electronicAccess.setRelationshipId(null)));
      case FIND_AND_REMOVE_THESE -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.stream()
          .filter(electronicAccess -> equalsIgnoreCase(electronicAccess.getRelationshipId(), action.getInitial()))
          .forEach(electronicAccess -> electronicAccess.setRelationshipId(null)));
      case FIND_AND_REPLACE -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.stream()
          .filter(electronicAccess -> equalsIgnoreCase(electronicAccess.getRelationshipId(), action.getInitial()))
          .forEach(electronicAccess -> electronicAccess.setRelationshipId(action.getUpdated())));
      case REPLACE_WITH -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.forEach(electronicAccess -> electronicAccess.setRelationshipId(action.getUpdated())));
      default -> notSupported(option, action);
    };
  }

  private Updater<? extends ElectronicAccessEntity> updateUri(UpdateOptionType option, Action action) {
    return switch (action.getType()) {
      case CLEAR_FIELD -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.forEach(electronicAccess -> electronicAccess.setUri(EMPTY)));
      case FIND_AND_REMOVE_THESE -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.stream()
          .filter(electronicAccess -> Objects.equals(electronicAccess.getUri(), action.getInitial()))
          .forEach(electronicAccess -> electronicAccess.setUri(EMPTY)));
      case FIND_AND_REPLACE -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.stream()
          .filter(electronicAccess -> Objects.equals(electronicAccess.getUri(), action.getInitial()))
          .forEach(electronicAccess -> electronicAccess.setUri(isNull(action.getUpdated()) ? EMPTY : action.getUpdated())));
      case REPLACE_WITH -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.forEach(electronicAccess -> electronicAccess.setUri(isNull(action.getUpdated()) ? EMPTY : action.getUpdated())));
      default -> notSupported(option, action);
    };
  }

  private Updater<? extends ElectronicAccessEntity> updateLinkText(UpdateOptionType option, Action action) {
    return switch (action.getType()) {
      case CLEAR_FIELD -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.forEach(electronicAccess -> electronicAccess.setLinkText(null)));
      case FIND_AND_REMOVE_THESE -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.stream()
          .filter(electronicAccess -> Objects.equals(electronicAccess.getLinkText(), action.getInitial()))
          .forEach(electronicAccess -> electronicAccess.setLinkText(null)));
      case FIND_AND_REPLACE -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.stream()
          .filter(electronicAccess -> Objects.equals(electronicAccess.getLinkText(), action.getInitial()))
          .forEach(electronicAccess -> electronicAccess.setLinkText(action.getUpdated())));
      case REPLACE_WITH -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.forEach(electronicAccess -> electronicAccess.setLinkText(action.getUpdated())));
      default -> notSupported(option, action);
    };
  }

  private Updater<? extends ElectronicAccessEntity> updateMaterialsSpecified(UpdateOptionType option, Action action) {
    return switch (action.getType()) {
      case CLEAR_FIELD -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.forEach(electronicAccess -> electronicAccess.setMaterialsSpecification(null)));
      case FIND_AND_REMOVE_THESE -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.stream()
          .filter(electronicAccess -> Objects.equals(electronicAccess.getMaterialsSpecification(), action.getInitial()))
          .forEach(electronicAccess -> electronicAccess.setMaterialsSpecification(null)));
      case FIND_AND_REPLACE -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.stream()
          .filter(electronicAccess -> Objects.equals(electronicAccess.getMaterialsSpecification(), action.getInitial()))
          .forEach(electronicAccess -> electronicAccess.setMaterialsSpecification(action.getUpdated())));
      case REPLACE_WITH -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.forEach(electronicAccess -> electronicAccess.setMaterialsSpecification(action.getUpdated())));
      default -> notSupported(option, action);
    };
  }

  private Updater<? extends ElectronicAccessEntity> updatePublicNote(UpdateOptionType option, Action action) {
    return switch (action.getType()) {
      case CLEAR_FIELD -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.forEach(electronicAccess -> electronicAccess.setPublicNote(null)));
      case FIND_AND_REMOVE_THESE -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.stream()
          .filter(electronicAccess -> Objects.equals(electronicAccess.getPublicNote(), action.getInitial()))
          .forEach(electronicAccess -> electronicAccess.setPublicNote(null)));
      case FIND_AND_REPLACE -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.stream()
          .filter(electronicAccess -> Objects.equals(electronicAccess.getPublicNote(), action.getInitial()))
          .forEach(electronicAccess -> electronicAccess.setPublicNote(action.getUpdated())));
      case REPLACE_WITH -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.forEach(electronicAccess -> electronicAccess.setPublicNote(action.getUpdated())));
      default -> notSupported(option, action);
    };
  }

  private Updater<? extends ElectronicAccessEntity> notSupported(UpdateOptionType option, Action action) {
    return electronicAccessEntity -> {
      throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
    };
  }
}
