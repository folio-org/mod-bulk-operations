package org.folio.bulkops.adapters.impl.holdings;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.adapters.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.adapters.Constants.ITEM_DELIMITER;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.folio.bulkops.adapters.ElectronicAccessStringMapper;
import org.folio.bulkops.adapters.ModClient;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.domain.bean.HoldingsNote;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsStatement;
import org.folio.bulkops.domain.bean.ReceivingHistoryEntries;
import org.folio.bulkops.domain.bean.ReceivingHistoryEntry;
import org.folio.bulkops.domain.bean.Tags;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class HoldingModClientAdapter implements ModClient<HoldingsRecord> {

  private final HoldingsReferenceResolver holdingsReferenceResolver;
  private final ElectronicAccessStringMapper electronicAccessStringMapper;
  private final HoldingsClient holdingClient;

  @Override
  public UnifiedTable convertEntityToUnifiedTable(HoldingsRecord holdingsRecord, UUID bulkOperationId,
      IdentifierType identifierType) {
    var identifier = IdentifierType.ID.equals(identifierType) ? holdingsRecord.getId() : holdingsRecord.getHrid();
    return new UnifiedTable().header(HoldingsHeaderBuilder.getHeaders())
      .addRowsItem(convertToUnifiedTableRow(holdingsRecord, bulkOperationId, identifier));
  }

  @Override
  public UnifiedTable getUnifiedRepresentationByQuery(String query, long offset, long limit) {
    var holdings = holdingClient.getHoldingsByQuery(query, offset, limit)
      .getHoldingsRecords();
    return new UnifiedTable().header(HoldingsHeaderBuilder.getHeaders())
      .rows(holdings.isEmpty() ? Collections.emptyList()
          : holdings.stream()
            .map(h -> convertToUnifiedTableRow(h, null, null))
            .collect(Collectors.toList()));
  }

  private Row convertToUnifiedTableRow(HoldingsRecord holdingsRecord, UUID bulkOperationId, String identifier) {
    return new Row().addRowItem(holdingsRecord.getId())
      .addRowItem(isEmpty(holdingsRecord.getVersion()) ? EMPTY : Integer.toString(holdingsRecord.getVersion()))
      .addRowItem(isEmpty(holdingsRecord.getHrid()) ? EMPTY : holdingsRecord.getHrid())
      .addRowItem(
          holdingsReferenceResolver.getHoldingsTypeNameById(holdingsRecord.getHoldingsTypeId(), bulkOperationId, identifier))
      .addRowItem(isEmpty(holdingsRecord.getFormerIds()) ? EMPTY : String.join(ARRAY_DELIMITER, holdingsRecord.getFormerIds()))
      .addRowItem(isEmpty(holdingsRecord.getInstanceId()) ? EMPTY
          : String.join(ARRAY_DELIMITER, holdingsReferenceResolver.getInstanceTitleById(holdingsRecord.getInstanceId()),
              holdingsRecord.getInstanceId()))
      .addRowItem(holdingsReferenceResolver.getLocationNameById(holdingsRecord.getPermanentLocationId()))
      .addRowItem(holdingsReferenceResolver.getLocationNameById(holdingsRecord.getTemporaryLocationId()))
      .addRowItem(holdingsReferenceResolver.getLocationNameById(holdingsRecord.getEffectiveLocationId()))
      .addRowItem(electronicAccessStringMapper.getElectronicAccessesToString(holdingsRecord.getElectronicAccess(), bulkOperationId,
          identifier))
      .addRowItem(
          holdingsReferenceResolver.getCallNumberTypeNameById(holdingsRecord.getCallNumberTypeId(), bulkOperationId, identifier))
      .addRowItem(isEmpty(holdingsRecord.getCallNumberPrefix()) ? EMPTY : holdingsRecord.getCallNumberPrefix())
      .addRowItem(isEmpty(holdingsRecord.getCallNumber()) ? EMPTY : holdingsRecord.getCallNumber())
      .addRowItem(isEmpty(holdingsRecord.getCallNumberSuffix()) ? EMPTY : holdingsRecord.getCallNumberSuffix())
      .addRowItem(isEmpty(holdingsRecord.getShelvingTitle()) ? EMPTY : holdingsRecord.getShelvingTitle())
      .addRowItem(isEmpty(holdingsRecord.getAcquisitionFormat()) ? EMPTY : holdingsRecord.getAcquisitionFormat())
      .addRowItem(isEmpty(holdingsRecord.getAcquisitionMethod()) ? EMPTY : holdingsRecord.getAcquisitionMethod())
      .addRowItem(isEmpty(holdingsRecord.getReceiptStatus()) ? EMPTY : holdingsRecord.getReceiptStatus())
      .addRowItem(notesToString(holdingsRecord.getNotes(), bulkOperationId, identifier))
      .addRowItem(isEmpty(holdingsRecord.getAdministrativeNotes()) ? EMPTY
          : String.join(ARRAY_DELIMITER, holdingsRecord.getAdministrativeNotes()))
      .addRowItem(holdingsReferenceResolver.getIllPolicyNameById(holdingsRecord.getIllPolicyId(), bulkOperationId, identifier))
      .addRowItem(isEmpty(holdingsRecord.getRetentionPolicy()) ? EMPTY : holdingsRecord.getRetentionPolicy())
      .addRowItem(isEmpty(holdingsRecord.getDigitizationPolicy()) ? EMPTY : holdingsRecord.getDigitizationPolicy())
      .addRowItem(holdingsStatementsToString(holdingsRecord.getHoldingsStatements()))
      .addRowItem(holdingsStatementsToString(holdingsRecord.getHoldingsStatementsForIndexes()))
      .addRowItem(holdingsStatementsToString(holdingsRecord.getHoldingsStatementsForSupplements()))
      .addRowItem(isEmpty(holdingsRecord.getCopyNumber()) ? EMPTY : holdingsRecord.getCopyNumber())
      .addRowItem(isEmpty(holdingsRecord.getNumberOfItems()) ? EMPTY : holdingsRecord.getNumberOfItems())
      .addRowItem(receivingHistoryToString(holdingsRecord.getReceivingHistory()))
      .addRowItem(isEmpty(holdingsRecord.getDiscoverySuppress()) ? EMPTY : Boolean.toString(holdingsRecord.getDiscoverySuppress()))
      .addRowItem(getStatisticalCodeNames(holdingsRecord.getStatisticalCodeIds(), bulkOperationId, identifier))
      .addRowItem(tagsToString(holdingsRecord.getTags()))
      .addRowItem(holdingsReferenceResolver.getSourceNameById(holdingsRecord.getSourceId(), bulkOperationId, identifier));
  }

  private String notesToString(List<HoldingsNote> notes, UUID bulkOperationId, String identifier) {
    return isEmpty(notes) ? EMPTY
        : notes.stream()
          .map(note -> String.join(ARRAY_DELIMITER,
              holdingsReferenceResolver.getNoteTypeNameById(note.getHoldingsNoteTypeId(), bulkOperationId, identifier),
              note.getNote(), Boolean.toString(note.getStaffOnly())))
          .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String holdingsStatementsToString(List<HoldingsStatement> statements) {
    return isEmpty(statements) ? EMPTY
        : statements.stream()
          .map(statement -> String.join(ARRAY_DELIMITER, statement.getStatement(), statement.getNote(), statement.getStaffNote()))
          .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String receivingHistoryToString(ReceivingHistoryEntries receivingHistoryEntries) {
    if (isEmpty(receivingHistoryEntries)) {
      return EMPTY;
    }
    var displayType = isEmpty(receivingHistoryEntries.getDisplayType()) ? EMPTY : receivingHistoryEntries.getDisplayType();
    var entriesString = isEmpty(receivingHistoryEntries.getEntries()) ? EMPTY
        : receivingHistoryEntries.getEntries()
          .stream()
          .map(this::receivingHistoryEntryToString)
          .collect(Collectors.joining(ITEM_DELIMITER));
    return String.join(ITEM_DELIMITER, displayType, entriesString);
  }

  private String receivingHistoryEntryToString(ReceivingHistoryEntry entry) {
    return String.join(ARRAY_DELIMITER, isEmpty(entry.getPublicDisplay()) ? EMPTY : Boolean.toString(entry.getPublicDisplay()),
        isEmpty(entry.getEnumeration()) ? EMPTY : entry.getEnumeration(),
        isEmpty(entry.getChronology()) ? EMPTY : entry.getChronology());
  }

  private String getStatisticalCodeNames(List<String> codeIds, UUID bulkOperationId, String identifier) {
    return isEmpty(codeIds) ? EMPTY
        : codeIds.stream()
          .map(id -> holdingsReferenceResolver.getStatisticalCodeNameById(id, bulkOperationId, identifier))
          .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  private String tagsToString(Tags tags) {
    if (isEmpty(tags)) {
      return EMPTY;
    }
    return isEmpty(tags.getTagList()) ? EMPTY : String.join(ARRAY_DELIMITER, tags.getTagList());
  }
}
