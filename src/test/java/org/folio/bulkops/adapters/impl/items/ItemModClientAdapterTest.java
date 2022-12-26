package org.folio.bulkops.adapters.impl.items;

import org.folio.bulkops.adapters.ElectronicAccessStringMapper;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.domain.bean.CirculationNote;
import org.folio.bulkops.domain.bean.ContributorName;
import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemCollection;
import org.folio.bulkops.domain.bean.ItemNote;
import org.folio.bulkops.domain.bean.LastCheckIn;
import org.folio.bulkops.domain.bean.MaterialType;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.Source;
import org.folio.bulkops.domain.bean.Tags;
import org.folio.bulkops.domain.dto.IdentifierType;
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
    var item = new Item().withId("id").withBarcode("barcode")
      .withStatus(new InventoryItemStatus().withName(InventoryItemStatus.NameEnum.AGED_TO_LOST))
      .withMaterialType(new MaterialType().withId("id").withName("materialTypeName"))
      .withItemLevelCallNumberTypeId("callNumberTypeId")
      .withContributorNames(List.of(new ContributorName().withName("contributor1"), new ContributorName().withName("contributor2")))
      .withNotes(List.of(new ItemNote().withNote("note1"), new ItemNote().withNote("note2")))
      .withCirculationNotes(new ArrayList<>(List.of(
        new CirculationNote().withSource(new Source().withId("id1").withPersonal(new Personal().withLastName("last")
          .withFirstName("first"))).withStaffOnly(true).withNoteType(CirculationNote.NoteTypeEnum.IN).withNote("circNote1"),
        new CirculationNote().withSource(new Source().withId("id2").withPersonal(new Personal().withLastName("last")
          .withFirstName("first"))).withStaffOnly(false).withNoteType(CirculationNote.NoteTypeEnum.IN).withNote("circNote2"))))
      .withStatisticalCodeIds(List.of("statisticalCodeId"))
      .withLastCheckIn(new LastCheckIn().withServicePointId("servicePointId").withStaffMemberId("staffMemberId"))
      .withInTransitDestinationServicePointId("servicePointId")
      .withIsBoundWith(true)
      .withTags(new Tags());

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
    var item1 = new Item().withId("id1").withBarcode("barcode1")
      .withStatus(new InventoryItemStatus().withName(InventoryItemStatus.NameEnum.AGED_TO_LOST))
      .withMaterialType(new MaterialType().withName("materialTypeName"))
      .withIsBoundWith(true)
      .withTags(new Tags());
   var item2 = new Item().withId("id2").withBarcode("barcode2")
      .withStatus(new InventoryItemStatus().withName(InventoryItemStatus.NameEnum.AGED_TO_LOST))
      .withMaterialType(new MaterialType().withName("materialTypeName"))
      .withIsBoundWith(true)
      .withTags(new Tags());
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
