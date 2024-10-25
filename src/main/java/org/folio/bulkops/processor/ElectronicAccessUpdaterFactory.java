package org.folio.bulkops.processor;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.util.RuleUtils;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Log4j2
@AllArgsConstructor
public class ElectronicAccessUpdaterFactory {

  private final FolioExecutionContext folioExecutionContext;

  public Updater<ExtendedHoldingsRecord> updater(UpdateOptionType option, Action action, boolean forPreview) {
    return switch (option) {
      case ELECTRONIC_ACCESS_URL_RELATIONSHIP -> updateUrlRelationship(option, action, forPreview);
      case ELECTRONIC_ACCESS_URI -> updateUri(option, action);
      case ELECTRONIC_ACCESS_LINK_TEXT -> updateLinkText(option, action);
      case ELECTRONIC_ACCESS_MATERIALS_SPECIFIED -> updateMaterialsSpecified(option, action);
      case ELECTRONIC_ACCESS_URL_PUBLIC_NOTE -> updatePublicNote(option, action);
      default -> notSupported(option, action);
    };
  }

  private Updater<ExtendedHoldingsRecord> updateUrlRelationship(UpdateOptionType option, Action action, boolean forPreview) {
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
          .forEach(electronicAccess -> electronicAccess.setRelationshipId(getRelationshipId(action, forPreview))));
      case REPLACE_WITH -> electronicAccessEntity -> ofNullable(electronicAccessEntity.getElectronicAccess())
        .ifPresent(list -> list.forEach(electronicAccess -> electronicAccess.setRelationshipId(getRelationshipId(action, forPreview))));
      default -> notSupported(option, action);
    };
  }

  private String getRelationshipId(Action action, boolean forPreview) {
    var id = action.getUpdated();
    if (forPreview) {
      id += ARRAY_DELIMITER + RuleUtils.getTenantFromAction(action, folioExecutionContext);
    }
    return id;
  }

  private Updater<ExtendedHoldingsRecord> updateUri(UpdateOptionType option, Action action) {
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

  private Updater<ExtendedHoldingsRecord> updateLinkText(UpdateOptionType option, Action action) {
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

  private Updater<ExtendedHoldingsRecord> updateMaterialsSpecified(UpdateOptionType option, Action action) {
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

  private Updater<ExtendedHoldingsRecord> updatePublicNote(UpdateOptionType option, Action action) {
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

  private Updater<ExtendedHoldingsRecord> notSupported(UpdateOptionType option, Action action) {
    return electronicAccessEntity -> {
      throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
    };
  }
}
