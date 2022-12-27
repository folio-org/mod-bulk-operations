package org.folio.bulkops.adapters.impl.items;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.adapters.BulkEditAdapterHelper.dateToString;
import static org.folio.bulkops.adapters.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.adapters.Constants.ITEM_DELIMITER;

import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import org.folio.bulkops.adapters.ElectronicAccessStringMapper;
import org.folio.bulkops.adapters.ModClient;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.domain.bean.CirculationNote;
import org.folio.bulkops.domain.bean.ContributorName;
import org.folio.bulkops.domain.bean.EffectiveCallNumberComponents;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.Title;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class ItemModClientAdapter implements ModClient<Item> {

  private final ItemReferenceResolver itemReferenceResolver;
  private final ElectronicAccessStringMapper electronicAccessStringMapper;
  private final ItemClient itemClient;

  @Override
  public UnifiedTable convertEntityToUnifiedTable(Item item, UUID bulkOperationId, IdentifierType identifierType) {
    var identifier = fetchIdentifier(item, identifierType);
    return new UnifiedTable().header(ItemHeaderBuilder.getHeaders())
      .addRowsItem(convertToUnifiedTableRow(item, bulkOperationId, identifier));
  }

  @Override
  public UnifiedTable getUnifiedRepresentationByQuery(String query, long offset, long limit) {
    var items = itemClient.getItemByQuery(query, offset, limit)
      .getItems();
    return new UnifiedTable().header(ItemHeaderBuilder.getHeaders())
      .rows(items.isEmpty() ? Collections.emptyList()
          : items.stream()
            .map(i -> convertToUnifiedTableRow(i, null, null))
            .collect(Collectors.toList()));
  }

  private Row convertToUnifiedTableRow(Item item, UUID bulkOperationId, String identifier) {
    return new Row().addRowItem(item.getId())
      .addRowItem(isEmpty(item.getVersion()) ? EMPTY : Integer.toString(item.getVersion()))
      .addRowItem(item.getHrid())
      .addRowItem(item.getHoldingsRecordId())
      .addRowItem(isEmpty(item.getFormerIds()) ? EMPTY : String.join(ARRAY_DELIMITER, item.getFormerIds()))
      .addRowItem(isEmpty(item.getDiscoverySuppress()) ? EMPTY
          : item.getDiscoverySuppress()
            .toString())
      .addRowItem(item.getTitle())
      .addRowItem(fetchContributorNames(item))
      .addRowItem(item.getCallNumber())
      .addRowItem(item.getBarcode())
      .addRowItem(item.getEffectiveShelvingOrder())
      .addRowItem(item.getAccessionNumber())
      .addRowItem(item.getItemLevelCallNumber())
      .addRowItem(item.getItemLevelCallNumberPrefix())
      .addRowItem(item.getItemLevelCallNumberSuffix())
      .addRowItem(itemReferenceResolver.getCallNumberTypeNameById(item.getItemLevelCallNumberTypeId(), bulkOperationId, identifier))
      .addRowItem(effectiveCallNumberComponentsToString(item.getEffectiveCallNumberComponents(), bulkOperationId, identifier))
      .addRowItem(item.getVolume())
      .addRowItem(item.getEnumeration())
      .addRowItem(item.getChronology())
      .addRowItem(isEmpty(item.getYearCaption()) ? EMPTY : String.join(ARRAY_DELIMITER, item.getYearCaption()))
      .addRowItem(item.getItemIdentifier())
      .addRowItem(item.getCopyNumber())
      .addRowItem(item.getNumberOfPieces())
      .addRowItem(item.getDescriptionOfPieces())
      .addRowItem(item.getNumberOfMissingPieces())
      .addRowItem(item.getMissingPieces())
      .addRowItem(item.getMissingPiecesDate())
      .addRowItem(itemReferenceResolver.getDamagedStatusNameById(item.getItemDamagedStatusId(), bulkOperationId, identifier))
      .addRowItem(item.getItemDamagedStatusDate())
      .addRowItem(isEmpty(item.getAdministrativeNotes()) ? EMPTY : String.join(ARRAY_DELIMITER, item.getAdministrativeNotes()))
      .addRowItem(fetchNotes(item, bulkOperationId, identifier))
      .addRowItem(fetchCirculationNotes(item))
      .addRowItem(isNull(item.getStatus()) ? EMPTY
          : String.join(ARRAY_DELIMITER, item.getStatus()
            .getName()
            .getValue(),
              dateToString(item.getStatus()
                .getDate())))
      .addRowItem(isNull(item.getMaterialType()) ? EMPTY
          : item.getMaterialType()
            .getName())
      .addRowItem(isNull(item.getIsBoundWith()) ? EMPTY
          : item.getIsBoundWith()
            .toString())
      .addRowItem(fetchBoundWithTitles(item))
      .addRowItem(isEmpty(item.getPermanentLoanType()) ? EMPTY
          : item.getPermanentLoanType()
            .getName())
      .addRowItem(isEmpty(item.getTemporaryLoanType()) ? EMPTY
          : item.getTemporaryLoanType()
            .getName())
      .addRowItem(isEmpty(item.getPermanentLocation()) ? EMPTY
          : item.getPermanentLocation()
            .getName())
      .addRowItem(isEmpty(item.getTemporaryLocation()) ? EMPTY
          : item.getTemporaryLocation()
            .getName())
      .addRowItem(isEmpty(item.getEffectiveLocation()) ? EMPTY
          : item.getEffectiveLocation()
            .getName())
      .addRowItem(
          itemReferenceResolver.getServicePointNameById(item.getInTransitDestinationServicePointId(), bulkOperationId, identifier))
      .addRowItem(fetchStatisticalCodes(item, bulkOperationId, identifier))
      .addRowItem(item.getPurchaseOrderLineIdentifier())
      .addRowItem(isEmpty(item.getTags()
        .getTagList()) ? EMPTY
            : String.join(ARRAY_DELIMITER, item.getTags()
              .getTagList()))
      .addRowItem(lastCheckInToString(item, bulkOperationId, identifier))
      .addRowItem(
          electronicAccessStringMapper.getElectronicAccessesToString(item.getElectronicAccess(), bulkOperationId, identifier));
  }

  private String fetchContributorNames(Item item) {
    return isEmpty(item.getContributorNames()) ? EMPTY
        : item.getContributorNames()
          .stream()
          .map(ContributorName::getName)
          .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  private String effectiveCallNumberComponentsToString(EffectiveCallNumberComponents components, UUID bulkOperationId,
      String identifier) {
    if (isEmpty(components)) {
      return EMPTY;
    }
    return String.join(ARRAY_DELIMITER, isEmpty(components.getCallNumber()) ? EMPTY : components.getCallNumber(),
        isEmpty(components.getPrefix()) ? EMPTY : components.getPrefix(),
        isEmpty(components.getSuffix()) ? EMPTY : components.getSuffix(),
        itemReferenceResolver.getCallNumberTypeNameById(components.getTypeId(), bulkOperationId, identifier));
  }

  private String fetchNotes(Item item, UUID bulkOperationId, String identifier) {
    return isEmpty(item.getNotes()) ? EMPTY
        : item.getNotes()
          .stream()
          .map(itemNote -> String.join(ARRAY_DELIMITER,
              itemReferenceResolver.getNoteTypeNameById(itemNote.getItemNoteTypeId(), bulkOperationId, identifier),
              itemNote.getNote(), itemNote.getStaffOnly()
                .toString()))
          .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String fetchCirculationNotes(Item item) {
    return isEmpty(item.getCirculationNotes()) ? EMPTY
        : item.getCirculationNotes()
          .stream()
          .map(this::circulationNotesToString)
          .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String circulationNotesToString(CirculationNote note) {
    return String.join(ARRAY_DELIMITER, note.getId(), note.getNoteType()
      .getValue(), note.getNote(),
        note.getStaffOnly()
          .toString(),
        isEmpty(note.getSource()
          .getId()) ? EMPTY
              : note.getSource()
                .getId(),
        isEmpty(note.getSource()
          .getPersonal()
          .getLastName()) ? EMPTY
              : note.getSource()
                .getPersonal()
                .getLastName(),
        isEmpty(note.getSource()
          .getPersonal()
          .getFirstName()) ? EMPTY
              : note.getSource()
                .getPersonal()
                .getFirstName(),
        dateToString(note.getDate()));
  }

  private String fetchBoundWithTitles(Item item) {
    return isEmpty(item.getBoundWithTitles()) ? EMPTY
        : item.getBoundWithTitles()
          .stream()
          .map(this::titleToString)
          .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String titleToString(Title title) {
    return String.join(ARRAY_DELIMITER, title.getBriefHoldingsRecord()
      .getHrid(),
        title.getBriefInstance()
          .getHrid(),
        title.getBriefInstance()
          .getTitle());
  }

  private String fetchStatisticalCodes(Item item, UUID bulkOperationId, String identifier) {
    return isEmpty(item.getStatisticalCodeIds()) ? EMPTY
        : item.getStatisticalCodeIds()
          .stream()
          .map(id -> itemReferenceResolver.getStatisticalCodeById(id, bulkOperationId, identifier))

          .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  private String lastCheckInToString(Item item, UUID bulkOperationId, String identifier) {
    var lastCheckIn = item.getLastCheckIn();
    if (isEmpty(lastCheckIn)) {
      return EMPTY;
    }
    return String.join(ARRAY_DELIMITER,
        itemReferenceResolver.getServicePointNameById(lastCheckIn.getServicePointId(), bulkOperationId, identifier),
        itemReferenceResolver.getUserNameById(lastCheckIn.getStaffMemberId(), bulkOperationId, identifier),
        lastCheckIn.getDateTime());
  }

  private String fetchIdentifier(Item item, IdentifierType identifierType) {
    switch (identifierType) {
    case BARCODE:
      return item.getBarcode();
    case HOLDINGS_RECORD_ID:
      return item.getHoldingsRecordId();
    case HRID:
      return item.getHrid();
    case FORMER_IDS:
      return String.join(ARRAY_DELIMITER, item.getFormerIds());
    case ACCESSION_NUMBER:
      return item.getAccessionNumber();
    default:
      return item.getId();
    }
  }
}
