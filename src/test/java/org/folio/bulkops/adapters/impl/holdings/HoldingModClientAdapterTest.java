package org.folio.bulkops.adapters.impl.holdings;

import org.folio.bulkops.adapters.ElectronicAccessStringMapper;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.domain.dto.HoldingsNote;
import org.folio.bulkops.domain.dto.HoldingsRecord;
import org.folio.bulkops.domain.dto.HoldingsRecordCollection;
import org.folio.bulkops.domain.dto.HoldingsStatement;
import org.folio.bulkops.domain.dto.ItemLocation;
import org.folio.bulkops.domain.dto.ReceivingHistoryEntries;
import org.folio.bulkops.domain.dto.ReceivingHistoryEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.folio.bulkops.adapters.BulkEditAdapterHelper.getValueFromTable;
import static org.folio.bulkops.adapters.Constants.HOLDING_CALL_NUMBER;
import static org.folio.bulkops.adapters.Constants.HOLDING_ID;
import static org.folio.bulkops.adapters.Constants.HOLDING_INSTANCE;
import static org.folio.bulkops.adapters.Constants.HOLDING_NOTES;
import static org.folio.bulkops.adapters.Constants.HOLDING_PERMANENT_LOCATION;
import static org.folio.bulkops.adapters.Constants.HOLDING_RECEIVING_HISTORY;
import static org.folio.bulkops.adapters.Constants.HOLDING_STATEMENTS;
import static org.folio.bulkops.adapters.Constants.HOLDING_STATISTICAL_CODES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoldingModClientAdapterTest {
  @Mock
  private HoldingsReferenceResolver holdingsReferenceResolver;
  @Mock
  private ElectronicAccessStringMapper electronicAccessStringMapper;
  @Mock
  private HoldingsClient holdingClient;

  @InjectMocks
  private HoldingModClientAdapter holdingModClientAdapter;

  @Test
  void convertEntityToUnifiedTableTest() {
    var permanentLocation = new ItemLocation().id("permanentLocationId").name("permanentLocation");
    var holdingsRecord = new HoldingsRecord().id("id")
      .callNumber("callNumber").instanceId("instanceId")
      .notes(List.of(new HoldingsNote().note("note").staffOnly(true)
        .holdingsNoteTypeId("noteTypeId")))
      .permanentLocation(permanentLocation)
      .permanentLocationId("permanentLocationId")
      .holdingsStatements(List.of(new HoldingsStatement().note("statementNote")))
      .receivingHistory(new ReceivingHistoryEntries().displayType("type")
        .entries(List.of(new ReceivingHistoryEntry().publicDisplay(true).chronology("chronology").enumeration("enumeration"))))
      .statisticalCodeIds(List.of("statisticalCodeId"));

    when(holdingsReferenceResolver.getLocationNameById("permanentLocationId")).thenReturn("permanentLocation");
    when(holdingsReferenceResolver.getNoteTypeNameById(eq("noteTypeId"), isNull(), isNull())).thenReturn("noteType");
    when(holdingsReferenceResolver.getStatisticalCodeNameById(eq("statisticalCodeId"), isNull(), isNull())).thenReturn("statisticalCode");

    var unifiedTable = holdingModClientAdapter.convertEntityToUnifiedTable(holdingsRecord, null, null);

    var header = unifiedTable.getHeader();
    var rows = unifiedTable.getRows();

    assertEquals(33, header.size());
    assertEquals(1, rows.size());
    assertEquals(33, rows.get(0). getRow().size());

    assertEquals("id", getValueFromTable(HOLDING_ID, unifiedTable));
    assertEquals("callNumber", getValueFromTable(HOLDING_CALL_NUMBER, unifiedTable));
    assertEquals("null;instanceId", getValueFromTable(HOLDING_INSTANCE, unifiedTable));
    assertEquals("permanentLocation", getValueFromTable(HOLDING_PERMANENT_LOCATION, unifiedTable));
    assertEquals("noteType;note;true", getValueFromTable(HOLDING_NOTES, unifiedTable));
    assertEquals("null;statementNote;null", getValueFromTable(HOLDING_STATEMENTS, unifiedTable));
    assertEquals("statisticalCode", getValueFromTable(HOLDING_STATISTICAL_CODES, unifiedTable));
    assertEquals("type|true;enumeration;chronology", getValueFromTable(HOLDING_RECEIVING_HISTORY, unifiedTable));
  }

  @Test
  void getUnifiedRepresentationByQueryTest() {
    var holdingsRecord1 = new HoldingsRecord().id("id1").callNumber("callNumber1");
    var holdingsRecord2 = new HoldingsRecord().id("id2").callNumber("callNumber2");
    var holdingsCollection = new HoldingsRecordCollection();
    holdingsCollection.setHoldingsRecords(List.of(holdingsRecord1, holdingsRecord2));

    when(holdingClient.getHoldingsByQuery("query", 1, 2)).thenReturn(holdingsCollection);

    var unifiedTable  = holdingModClientAdapter.getUnifiedRepresentationByQuery("query", 1, 2);

    assertEquals(33,unifiedTable.getHeader().size());
    assertEquals(2, unifiedTable.getRows().size());
  }

  @Test
  void getUnifiedRepresentationByQueryIfEmptyResponseTest() {
    var holdingCollection = new HoldingsRecordCollection();
    holdingCollection.setHoldingsRecords(new ArrayList<>());
    when(holdingClient.getHoldingsByQuery("query", 1, 2)).thenReturn(holdingCollection);

    var unifiedTable  = holdingModClientAdapter.getUnifiedRepresentationByQuery("query", 1, 2);

    assertEquals(33,unifiedTable.getHeader().size());
    assertEquals(0, unifiedTable.getRows().size());
  }
}
