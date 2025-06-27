package org.folio.bulkops.batch.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.bulkops.batch.jobs.processidentifiers.DuplicationCheckerFactory;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.domain.bean.ExtendedItemCollection;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemCollection;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.domain.bean.ItemNote;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.ConsortiumItem;
import org.folio.bulkops.domain.dto.ConsortiumItemCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.client.SearchClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.processor.permissions.check.TenantResolver;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.JobExecution;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

class BulkEditItemProcessorTest {

  @Mock
  private ItemClient itemClient;
  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private SearchClient searchClient;
  @Mock
  private UserClient userClient;
  @Mock
  private PermissionsValidator permissionsValidator;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private FolioModuleMetadata folioModuleMetadata;
  @Mock
  private TenantResolver tenantResolver;
  @Mock
  private DuplicationCheckerFactory duplicationCheckerFactory;
  @Mock
  private HoldingsReferenceService holdingsReferenceService;

  @InjectMocks
  private BulkEditItemProcessor processor;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    ReflectionTestUtils.setField(processor, "identifierType", IdentifierType.BARCODE.getValue());
    JobExecution jobExecution = Mockito.mock(JobExecution.class, Mockito.RETURNS_DEEP_STUBS);
    ReflectionTestUtils.setField(processor, "jobExecution", jobExecution);
    when(consortiaService.getCentralTenantId(anyString())).thenReturn("centralTenant");
    when(folioExecutionContext.getTenantId()).thenReturn("centralTenant");
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.of(TENANT, List.of("diku")));
  }

  @Test
  void returnsExtendedItemCollectionForCentralTenantAndPermittedAffiliation() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("itemId");
    Item item = new Item().withId("itemId").withHoldingsRecordId("holdingsId");
    ItemCollection itemCollection = ItemCollection.builder().items(List.of(item)).totalRecords(1).build();

    ConsortiumItem consortiumItem = new ConsortiumItem().id("itemId").tenantId("tenant1");
    org.folio.bulkops.domain.dto.ConsortiumItemCollection consortiumItemCollection = new ConsortiumItemCollection();
    consortiumItemCollection.setItems(List.of(consortiumItem));
    consortiumItemCollection.setTotalRecords(1);

    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(new HashSet<>());
    when(searchClient.getConsortiumItemCollection(any())).thenReturn(consortiumItemCollection);
    when(tenantResolver.getAffiliatedPermittedTenantIds(eq(EntityType.ITEM), any(), anyString(), anySet(), eq(itemIdentifier)))
      .thenReturn(Set.of("tenant1"));
    when(itemClient.getByQuery(anyString(), anyInt())).thenReturn(itemCollection);
    when(holdingsReferenceService.getHoldingsRecordById(anyString(), anyString())).thenReturn(new HoldingsRecord().withInstanceId("instanceId"));
    when(holdingsReferenceService.getInstanceTitleById(anyString(), anyString())).thenReturn("Instance Title");
    when(holdingsReferenceService.getHoldingsJsonById(anyString(), anyString())).thenReturn(Mockito.mock(JsonNode.class));
    when(holdingsReferenceService.getHoldingsLocationById(any(), anyString())).thenReturn(Mockito.mock(JsonNode.class));

    ExtendedItemCollection result = processor.process(itemIdentifier);

    assertThat(result.getExtendedItems()).hasSize(1);
    assertThat(result.getExtendedItems().getFirst().getEntity().getId()).isEqualTo("itemId");
    assertThat(result.getExtendedItems().getFirst().getTenantId()).isEqualTo("tenant1");
  }

  @Test
  void throwsWhenDuplicateIdentifier() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("dupId");
    Set<ItemIdentifier> set = Mockito.mock(Set.class);
    when(set.add(any())).thenReturn(false);
    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(set);

    assertThatThrownBy(() -> processor.process(itemIdentifier))
      .isInstanceOf(BulkEditException.class)
      .hasMessageContaining("Duplicate entry");
  }

  @Test
  void throwsWhenNoMatchFoundForCentralTenant() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("notfound");
    ConsortiumItemCollection emptyConsortium = new ConsortiumItemCollection();
    emptyConsortium.setItems(Collections.emptyList());
    emptyConsortium.setTotalRecords(0);

    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(new HashSet<>());
    when(searchClient.getConsortiumItemCollection(any())).thenReturn(emptyConsortium);

    assertThatThrownBy(() -> processor.process(itemIdentifier))
      .isInstanceOf(BulkEditException.class)
      .hasMessageContaining("No match found");
  }

  @Test
  void throwsWhenMultipleMatchesFoundForLocalTenant() {
    when(folioExecutionContext.getTenantId()).thenReturn("localTenant");
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("multi");
    Item item1 = new Item().withId("1");
    Item item2 = new Item().withId("2");
    ItemCollection collection = ItemCollection.builder().items(List.of(item1, item2)).totalRecords(2).build();

    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(new HashSet<>());
    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), any())).thenReturn(true);
    when(itemClient.getByQuery(anyString(), anyInt())).thenReturn(collection);

    assertThatThrownBy(() -> processor.process(itemIdentifier))
      .isInstanceOf(BulkEditException.class)
      .hasMessageContaining("Multiple matches");
  }

  @Test
  void throwsWhenNoPermissionForLocalTenant() {
    when(folioExecutionContext.getTenantId()).thenReturn("localTenant");
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("noPerm");
    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(new HashSet<>());
    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), any())).thenReturn(false);
    when(userClient.getUserById(anyString())).thenReturn(new User().withUsername("testuser"));

    assertThatThrownBy(() -> processor.process(itemIdentifier))
      .isInstanceOf(BulkEditException.class)
      .hasMessageContaining("does not have required permission");
  }

  @Test
  void returnsHoldingsDataInExtendedItem() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("itemId");
    Item item = new Item().withId("itemId").withHoldingsRecordId("holdingsId");
    ItemCollection itemCollection = ItemCollection.builder().items(List.of(item)).totalRecords(1).build();

    ConsortiumItem consortiumItem = new ConsortiumItem().id("itemId").tenantId("tenant1");
    ConsortiumItemCollection consortiumItemCollection = new ConsortiumItemCollection();
    consortiumItemCollection.setItems(List.of(consortiumItem));
    consortiumItemCollection.setTotalRecords(1);

    JsonNode holdingsJson = Mockito.mock(JsonNode.class);
    JsonNode locationJson = Mockito.mock(JsonNode.class);
    when(holdingsJson.get(anyString())).thenReturn(null);
    when(locationJson.get(anyString())).thenReturn(null);

    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(new HashSet<>());
    when(searchClient.getConsortiumItemCollection(any())).thenReturn(consortiumItemCollection);
    when(tenantResolver.getAffiliatedPermittedTenantIds(eq(EntityType.ITEM), any(), anyString(), anySet(), eq(itemIdentifier)))
      .thenReturn(Set.of("tenant1"));
    when(itemClient.getByQuery(anyString(), anyInt())).thenReturn(itemCollection);
    when(holdingsReferenceService.getHoldingsRecordById(anyString(), anyString())).thenReturn(new HoldingsRecord().withInstanceId("instanceId"));
    when(holdingsReferenceService.getInstanceTitleById(anyString(), anyString())).thenReturn("Instance Title");
    when(holdingsReferenceService.getHoldingsJsonById(anyString(), anyString())).thenReturn(holdingsJson);
    when(holdingsReferenceService.getHoldingsLocationById(any(), anyString())).thenReturn(locationJson);

    ExtendedItemCollection result = processor.process(itemIdentifier);

    assertThat(result.getExtendedItems()).hasSize(1);
    assertThat(result.getExtendedItems().getFirst().getEntity().getId()).isEqualTo("itemId");
    assertThat(result.getExtendedItems().getFirst().getEntity().getHoldingsData()).isNotNull();
  }

  @Test
  void setsTenantIdOnElectronicAccessAndNotesWhenPresent() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("itemId");
    String tenantId = "tenant1";

    ElectronicAccess ea1 = new ElectronicAccess().withUri("uri1");
    ElectronicAccess ea2 = new ElectronicAccess().withUri("uri2");
    ItemNote note1 = new ItemNote().withNote("note1");
    ItemNote note2 = new ItemNote().withNote("note2");

    Item item = new Item()
            .withId("itemId")
            .withHoldingsRecordId("holdingsId")
            .withElectronicAccess(List.of(ea1, ea2))
            .withNotes(List.of(note1, note2));
    var itemCollection = org.folio.bulkops.domain.bean.ItemCollection.builder()
            .items(List.of(item)).totalRecords(1).build();

    ConsortiumItem consortiumItem = new ConsortiumItem().id("itemId").tenantId(tenantId);
    ConsortiumItemCollection consortiumItemCollection = new ConsortiumItemCollection();
    consortiumItemCollection.setItems(List.of(consortiumItem));
    consortiumItemCollection.setTotalRecords(1);

    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(new HashSet<>());
    when(searchClient.getConsortiumItemCollection(any())).thenReturn(consortiumItemCollection);
    when(tenantResolver.getAffiliatedPermittedTenantIds(eq(EntityType.ITEM), any(), anyString(), anySet(), eq(itemIdentifier)))
            .thenReturn(Set.of(tenantId));
    when(itemClient.getByQuery(anyString(), anyInt())).thenReturn(itemCollection);
    when(holdingsReferenceService.getHoldingsRecordById(anyString(), anyString())).thenReturn(new HoldingsRecord().withInstanceId("instanceId"));
    when(holdingsReferenceService.getInstanceTitleById(anyString(), anyString())).thenReturn("Instance Title");
    when(holdingsReferenceService.getHoldingsJsonById(anyString(), anyString())).thenReturn(Mockito.mock(JsonNode.class));
    when(holdingsReferenceService.getHoldingsLocationById(any(), anyString())).thenReturn(Mockito.mock(JsonNode.class));

    ExtendedItemCollection result = processor.process(itemIdentifier);

    var extendedItem = result.getExtendedItems().getFirst().getEntity();
    assertThat(extendedItem.getElectronicAccess()).allSatisfy(ea -> assertThat(ea.getTenantId()).isEqualTo(tenantId));
    assertThat(extendedItem.getNotes()).allSatisfy(note -> assertThat(note.getTenantId()).isEqualTo(tenantId));
  }

  @Test
  void doesNotFailWhenElectronicAccessIsNull() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("itemId");
    String tenantId = "tenant1";

    ItemNote note1 = new ItemNote().withNote("note1");
    Item item = new Item()
            .withId("itemId")
            .withHoldingsRecordId("holdingsId")
            .withElectronicAccess(null)
            .withNotes(List.of(note1));
    var itemCollection = org.folio.bulkops.domain.bean.ItemCollection.builder()
            .items(List.of(item)).totalRecords(1).build();

    ConsortiumItem consortiumItem = new ConsortiumItem().id("itemId").tenantId(tenantId);
    ConsortiumItemCollection consortiumItemCollection = new ConsortiumItemCollection();
    consortiumItemCollection.setItems(List.of(consortiumItem));
    consortiumItemCollection.setTotalRecords(1);

    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(new HashSet<>());
    when(searchClient.getConsortiumItemCollection(any())).thenReturn(consortiumItemCollection);
    when(tenantResolver.getAffiliatedPermittedTenantIds(eq(EntityType.ITEM), any(), anyString(), anySet(), eq(itemIdentifier)))
            .thenReturn(Set.of(tenantId));
    when(itemClient.getByQuery(anyString(), anyInt())).thenReturn(itemCollection);
    when(holdingsReferenceService.getHoldingsRecordById(anyString(), anyString())).thenReturn(new HoldingsRecord().withInstanceId("instanceId"));
    when(holdingsReferenceService.getInstanceTitleById(anyString(), anyString())).thenReturn("Instance Title");
    when(holdingsReferenceService.getHoldingsJsonById(anyString(), anyString())).thenReturn(Mockito.mock(JsonNode.class));
    when(holdingsReferenceService.getHoldingsLocationById(any(), anyString())).thenReturn(Mockito.mock(JsonNode.class));

    ExtendedItemCollection result = processor.process(itemIdentifier);

    var extendedItem = result.getExtendedItems().getFirst().getEntity();
    assertThat(extendedItem.getElectronicAccess()).isNull();
    assertThat(extendedItem.getNotes()).allSatisfy(note -> assertThat(note.getTenantId()).isEqualTo(tenantId));
  }

  @Test
  void doesNotFailWhenNotesIsNull() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("itemId");
    String tenantId = "tenant1";

    ElectronicAccess ea1 = new ElectronicAccess().withUri("uri1");
    Item item = new Item()
            .withId("itemId")
            .withHoldingsRecordId("holdingsId")
            .withElectronicAccess(List.of(ea1))
            .withNotes(null);
    var itemCollection = ItemCollection.builder()
            .items(List.of(item)).totalRecords(1).build();

    ConsortiumItem consortiumItem = new ConsortiumItem().id("itemId").tenantId(tenantId);
    ConsortiumItemCollection consortiumItemCollection = new ConsortiumItemCollection();
    consortiumItemCollection.setItems(List.of(consortiumItem));
    consortiumItemCollection.setTotalRecords(1);

    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(new HashSet<>());
    when(searchClient.getConsortiumItemCollection(any())).thenReturn(consortiumItemCollection);
    when(tenantResolver.getAffiliatedPermittedTenantIds(eq(EntityType.ITEM), any(), anyString(), anySet(), eq(itemIdentifier)))
            .thenReturn(Set.of(tenantId));
    when(itemClient.getByQuery(anyString(), anyInt())).thenReturn(itemCollection);
    when(holdingsReferenceService.getHoldingsRecordById(anyString(), anyString())).thenReturn(new HoldingsRecord().withInstanceId("instanceId"));
    when(holdingsReferenceService.getInstanceTitleById(anyString(), anyString())).thenReturn("Instance Title");
    when(holdingsReferenceService.getHoldingsJsonById(anyString(), anyString())).thenReturn(Mockito.mock(JsonNode.class));
    when(holdingsReferenceService.getHoldingsLocationById(any(), anyString())).thenReturn(Mockito.mock(JsonNode.class));

    ExtendedItemCollection result = processor.process(itemIdentifier);

    var extendedItem = result.getExtendedItems().getFirst().getEntity();
    assertThat(extendedItem.getNotes()).isNull();
    assertThat(extendedItem.getElectronicAccess()).allSatisfy(ea -> assertThat(ea.getTenantId()).isEqualTo(tenantId));
  }

  @Test
  void doesNotFailWhenBothElectronicAccessAndNotesAreNull() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("itemId");
    String tenantId = "tenant1";

    Item item = new Item()
            .withId("itemId")
            .withHoldingsRecordId("holdingsId")
            .withElectronicAccess(null)
            .withNotes(null);
    var itemCollection = ItemCollection.builder()
            .items(List.of(item)).totalRecords(1).build();

    ConsortiumItem consortiumItem = new ConsortiumItem().id("itemId").tenantId(tenantId);
    ConsortiumItemCollection consortiumItemCollection = new ConsortiumItemCollection();
    consortiumItemCollection.setItems(List.of(consortiumItem));
    consortiumItemCollection.setTotalRecords(1);

    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(new HashSet<>());
    when(searchClient.getConsortiumItemCollection(any())).thenReturn(consortiumItemCollection);
    when(tenantResolver.getAffiliatedPermittedTenantIds(eq(EntityType.ITEM), any(), anyString(), anySet(), eq(itemIdentifier)))
            .thenReturn(Set.of(tenantId));
    when(itemClient.getByQuery(anyString(), anyInt())).thenReturn(itemCollection);
    when(holdingsReferenceService.getHoldingsRecordById(anyString(), anyString())).thenReturn(new HoldingsRecord().withInstanceId("instanceId"));
    when(holdingsReferenceService.getInstanceTitleById(anyString(), anyString())).thenReturn("Instance Title");
    when(holdingsReferenceService.getHoldingsJsonById(anyString(), anyString())).thenReturn(Mockito.mock(JsonNode.class));
    when(holdingsReferenceService.getHoldingsLocationById(any(), anyString())).thenReturn(Mockito.mock(JsonNode.class));

    ExtendedItemCollection result = processor.process(itemIdentifier);

    var extendedItem = result.getExtendedItems().getFirst().getEntity();
    assertThat(extendedItem.getElectronicAccess()).isNull();
    assertThat(extendedItem.getNotes()).isNull();
  }
}
