package org.folio.bulkops.adapters.impl.items;

import org.folio.bulkops.adapters.ElectronicAccessStringMapper;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.domain.dto.CirculationNote;
import org.folio.bulkops.domain.dto.ContributorName;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.InventoryItemStatus;
import org.folio.bulkops.domain.dto.Item;
import org.folio.bulkops.domain.dto.ItemCollection;
import org.folio.bulkops.domain.dto.ItemNote;
import org.folio.bulkops.domain.dto.LastCheckIn;
import org.folio.bulkops.domain.dto.MaterialType;
import org.folio.bulkops.domain.dto.Personal;
import org.folio.bulkops.domain.dto.Source;
import org.folio.bulkops.domain.dto.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.folio.bulkops.adapters.BulkEditAdapterHelper.getValueFromTable;
import static org.folio.bulkops.adapters.Constants.ITEM_BARCODE;
import static org.folio.bulkops.adapters.Constants.ITEM_CIRCULATION_NOTES;
import static org.folio.bulkops.adapters.Constants.ITEM_CONTRIBUTORS_NAMES;
import static org.folio.bulkops.adapters.Constants.ITEM_ID;
import static org.folio.bulkops.adapters.Constants.ITEM_LEVEL_CALL_NUMBER_TYPE;
import static org.folio.bulkops.adapters.Constants.ITEM_MATERIAL_TYPE;
import static org.folio.bulkops.adapters.Constants.ITEM_NOTES;
import static org.folio.bulkops.adapters.Constants.ITEM_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ItemModClientAdapterTest {

  @Mock
  private ItemReferenceResolver itemReferenceResolver;
  @Mock
  private ElectronicAccessStringMapper electronicAccessStringMapper;
  @Mock
  private ItemClient itemClient;

  @InjectMocks
  private ItemModClientAdapter itemModClientAdapter;

  @Test
  void convertEntityToUnifiedTableTest() {
    var item = new Item().id("id").barcode("barcode")
      .status(new InventoryItemStatus().name(InventoryItemStatus.NameEnum.AGED_TO_LOST))
      .materialType(new MaterialType().id("id").name("materialTypeName"))
      .itemLevelCallNumberTypeId("callNumberTypeId")
      .contributorNames(List.of(new ContributorName().name("contributor1"), new ContributorName().name("contributor2")))
      .notes(List.of(new ItemNote().note("note1"), new ItemNote().note("note2")))
      .circulationNotes(new ArrayList<>(List.of(
        new CirculationNote().source(new Source().id("id1").personal(new Personal().lastName("last")
          .firstName("first"))).staffOnly(true).noteType(CirculationNote.NoteTypeEnum.IN).note("circNote1"),
        new CirculationNote().source(new Source().id("id2").personal(new Personal().lastName("last")
          .firstName("first"))).staffOnly(false).noteType(CirculationNote.NoteTypeEnum.IN).note("circNote2"))))
      .statisticalCodeIds(List.of("statisticalCodeId"))
      .lastCheckIn(new LastCheckIn().servicePointId("servicePointId").staffMemberId("staffMemberId"))
      .inTransitDestinationServicePointId("servicePointId")
      .isBoundWith(true)
      .tags(new Tags());

    when(itemReferenceResolver.getCallNumberTypeNameById(isA(String.class), isA(UUID.class), eq("id"))).thenReturn("callNumberType");
    when(itemReferenceResolver.getStatisticalCodeById(eq("statisticalCodeId"), isA(UUID.class), eq("id"))).thenReturn("statisticalCode");
    when(itemReferenceResolver.getServicePointNameById(eq("servicePointId"), isA(UUID.class), eq("id"))).thenReturn("servicePoint");
    when(itemReferenceResolver.getUserNameById(eq("staffMemberId"), isA(UUID.class), eq("id"))).thenReturn("staffMember");

    var actual = itemModClientAdapter.convertEntityToUnifiedTable(item, UUID.randomUUID(), IdentifierType.ID);

    var header = actual.getHeader();
    var rows = actual.getRows();

    assertEquals(48, header.size());
    assertEquals(1, rows.size());
    assertEquals(48, rows.get(0). getRow().size());

    assertEquals("id",  getValueFromTable(ITEM_ID, actual));
    assertEquals("contributor1;contributor2",  getValueFromTable(ITEM_CONTRIBUTORS_NAMES, actual));
    assertEquals("barcode",  getValueFromTable(ITEM_BARCODE, actual));
    assertEquals("callNumberType",  getValueFromTable(ITEM_LEVEL_CALL_NUMBER_TYPE, actual));
    assertEquals("null;note1;false|null;note2;false",  getValueFromTable(ITEM_NOTES, actual));
    assertEquals("null;Check in;circNote1;true;id1;last;first;|null;Check in;circNote2;false;id2;last;first;", getValueFromTable(ITEM_CIRCULATION_NOTES, actual));
    assertEquals("Aged to lost;",  getValueFromTable(ITEM_STATUS, actual));
    assertEquals("materialTypeName",  getValueFromTable(ITEM_MATERIAL_TYPE, actual));
  }

  @Test
  void getUnifiedRepresentationByQueryTest() {
    var item1 = new Item().id("id1").barcode("barcode1")
      .status(new InventoryItemStatus().name(InventoryItemStatus.NameEnum.AGED_TO_LOST))
      .materialType(new MaterialType().name("materialTypeName"))
      .isBoundWith(true)
      .tags(new Tags());
   var item2 = new Item().id("id2").barcode("barcode2")
      .status(new InventoryItemStatus().name(InventoryItemStatus.NameEnum.AGED_TO_LOST))
      .materialType(new MaterialType().name("materialTypeName"))
      .isBoundWith(true)
      .tags(new Tags());
    var itemCollection = new ItemCollection();
    itemCollection.setItems(List.of(item1, item2));
    when(itemClient.getItemByQuery("query", 1, 2)).thenReturn(itemCollection);

    var unifiedTable  = itemModClientAdapter.getUnifiedRepresentationByQuery("query", 1, 2);

    assertEquals(48,unifiedTable.getHeader().size());
    assertEquals(2, unifiedTable.getRows().size());
  }

  @Test
  void getUnifiedRepresentationByQueryIfEmptyResponseTest() {
    var itemCollection = new ItemCollection();
    itemCollection.setItems(new ArrayList<>());
    when(itemClient.getItemByQuery("query", 1, 2)).thenReturn(itemCollection);
    var unifiedTable  = itemModClientAdapter.getUnifiedRepresentationByQuery("query", 1, 2);

    assertEquals(48,unifiedTable.getHeader().size());
    assertEquals(0, unifiedTable.getRows().size());
  }
}
