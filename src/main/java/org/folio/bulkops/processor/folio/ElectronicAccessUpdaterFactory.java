package org.folio.bulkops.processor.folio;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.replace;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.processor.Updater;
import org.folio.bulkops.service.LocalReferenceDataService;
import org.folio.bulkops.util.RuleUtils;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;


@Component
@Log4j2
@AllArgsConstructor
public class ElectronicAccessUpdaterFactory {

  private final FolioExecutionContext folioExecutionContext;
  private final LocalReferenceDataService localReferenceDataService;

  public Updater<ExtendedHoldingsRecord> updater(UpdateOptionType option, Action action) {
    return switch (option) {
      case ELECTRONIC_ACCESS_URL_RELATIONSHIP -> updateUrlRelationship(option, action);
      case ELECTRONIC_ACCESS_URI -> updateUri(option, action);
      case ELECTRONIC_ACCESS_LINK_TEXT -> updateLinkText(option, action);
      case ELECTRONIC_ACCESS_MATERIALS_SPECIFIED -> updateMaterialsSpecified(option, action);
      case ELECTRONIC_ACCESS_URL_PUBLIC_NOTE -> updatePublicNote(option, action);
      default -> notSupported(option, action);
    };
  }

  private Updater<ExtendedHoldingsRecord> updateUrlRelationship(
      UpdateOptionType option,
      Action action
  ) {
    return switch (action.getType()) {
      case CLEAR_FIELD -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess ->
                  electronicAccess.setRelationshipId(null)));

      case FIND_AND_REMOVE_THESE -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess -> {
                if (equalsIgnoreCase(electronicAccess.getRelationshipId(), action.getInitial())) {
                  electronicAccess.setRelationshipId(null);
                }
              }));

      case FIND_AND_REPLACE -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess -> {
                if (equalsIgnoreCase(electronicAccess.getRelationshipId(), action.getInitial())) {
                  electronicAccess.setRelationshipId(getRelationshipId(action));
                }
              }));

      case REPLACE_WITH -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess ->
                  electronicAccess.setRelationshipId(getRelationshipId(action))));

      default -> notSupported(option, action);
    };
  }

  private String getRelationshipId(Action action) {
    var id = action.getUpdated();
    localReferenceDataService.updateTenantForUrlRelationship(
        id,
        RuleUtils.getTenantFromAction(action, folioExecutionContext)
    );
    return id;
  }

  private Updater<ExtendedHoldingsRecord> updateUri(
      UpdateOptionType option,
      Action action
  ) {
    return switch (action.getType()) {
      case CLEAR_FIELD -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess ->
                  electronicAccess.setUri(EMPTY)));

      case FIND_AND_REMOVE_THESE -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess -> {
                if (contains(electronicAccess.getUri(), action.getInitial())) {
                  electronicAccess.setUri(electronicAccess.getUri()
                          .replace(action.getInitial(), EMPTY));
                }
              }));

      case FIND_AND_REPLACE -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess -> {
                if (contains(electronicAccess.getUri(), action.getInitial())) {
                  electronicAccess.setUri(isNull(action.getUpdated())
                      ? EMPTY
                      : electronicAccess.getUri().replace(action.getInitial(),
                          action.getUpdated()));
                }
              }));

      case REPLACE_WITH -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess ->
                  electronicAccess.setUri(isNull(action.getUpdated()) ? EMPTY
                          : action.getUpdated())));

      default -> notSupported(option, action);
    };
  }

  private Updater<ExtendedHoldingsRecord> updateLinkText(
      UpdateOptionType option,
      Action action
  ) {
    return switch (action.getType()) {
      case CLEAR_FIELD -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess ->
                  electronicAccess.setLinkText(null)));

      case FIND_AND_REMOVE_THESE -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess -> {
                if (contains(electronicAccess.getLinkText(), action.getInitial())) {
                  electronicAccess.setLinkText(electronicAccess.getLinkText()
                          .replace(action.getInitial(), EMPTY));
                }
              }));

      case FIND_AND_REPLACE -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess -> {
                if (contains(electronicAccess.getLinkText(), action.getInitial())) {
                  electronicAccess.setLinkText(replace(electronicAccess.getLinkText(),
                          action.getInitial(), action.getUpdated()));
                }
              }));

      case REPLACE_WITH -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess ->
                  electronicAccess.setLinkText(action.getUpdated())));

      default -> notSupported(option, action);
    };
  }

  private Updater<ExtendedHoldingsRecord> updateMaterialsSpecified(
      UpdateOptionType option,
      Action action
  ) {
    return switch (action.getType()) {
      case CLEAR_FIELD -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess ->
                  electronicAccess.setMaterialsSpecification(null)));

      case FIND_AND_REMOVE_THESE -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess -> {
                if (contains(electronicAccess.getMaterialsSpecification(), action.getInitial())) {
                  electronicAccess.setMaterialsSpecification(
                      electronicAccess.getMaterialsSpecification()
                              .replace(action.getInitial(), EMPTY));
                }
              }));

      case FIND_AND_REPLACE -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess -> {
                if (contains(electronicAccess.getMaterialsSpecification(), action.getInitial())) {
                  electronicAccess.setMaterialsSpecification(replace(
                      electronicAccess.getMaterialsSpecification(), action.getInitial(),
                          action.getUpdated()));
                }
              }));

      case REPLACE_WITH -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess ->
                  electronicAccess.setMaterialsSpecification(action.getUpdated())));

      default -> notSupported(option, action);
    };
  }

  private Updater<ExtendedHoldingsRecord> updatePublicNote(
      UpdateOptionType option,
      Action action
  ) {
    return switch (action.getType()) {
      case CLEAR_FIELD -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess ->
                  electronicAccess.setPublicNote(null)));

      case FIND_AND_REMOVE_THESE -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess -> {
                if (contains(electronicAccess.getPublicNote(), action.getInitial())) {
                  electronicAccess.setPublicNote(
                      electronicAccess.getPublicNote().replace(action.getInitial(), EMPTY));
                }
              }));

      case FIND_AND_REPLACE -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess -> {
                if (contains(electronicAccess.getPublicNote(), action.getInitial())) {
                  electronicAccess.setPublicNote(replace(
                      electronicAccess.getPublicNote(), action.getInitial(), action.getUpdated()));
                }
              }));

      case REPLACE_WITH -> electronicAccessEntity ->
          ofNullable(electronicAccessEntity.getElectronicAccess())
              .ifPresent(list -> list.forEach(electronicAccess ->
                  electronicAccess.setPublicNote(action.getUpdated())));

      default -> notSupported(option, action);
    };
  }

  private Updater<ExtendedHoldingsRecord> notSupported(UpdateOptionType option, Action action) {
    return electronicAccessEntity -> {
      throw new BulkOperationException(
          format("Combination %s and %s isn't supported yet", option, action.getType())
      );
    };
  }
}
