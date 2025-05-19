package org.folio.bulkops.processor.permissions.check;

import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_WRITE_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_USERS_VIEW_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_USERS_WRITE_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_ITEMS_ITEM_PUT;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.USERS_ITEM_PUT;
import static org.folio.bulkops.util.Constants.DUPLICATES_ACROSS_TENANTS;
import static org.folio.bulkops.util.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.SneakyThrows;
import org.folio.bulkops.client.SearchConsortium;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.ConsortiumHolding;
import org.folio.bulkops.domain.bean.ConsortiumHoldingCollection;
import org.folio.bulkops.domain.bean.ConsortiumItem;
import org.folio.bulkops.domain.bean.ConsortiumItemCollection;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.BatchIdsDto;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.ReadPermissionException;
import org.folio.bulkops.exception.UploadFromQueryException;
import org.folio.bulkops.exception.WritePermissionDoesNotExist;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PermissionsValidatorTest {

  @Mock
  private PermissionsProvider permissionsProvider;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private ReadPermissionsValidator readPermissionsValidator;
  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private UserClient userClient;
  @Mock
  private RequiredPermissionResolver requiredPermissionResolver;
  @Mock
  private SearchConsortium searchClient;
  @Mock
  private TenantResolver tenantResolver;

  @InjectMocks
  private PermissionsValidator permissionsValidator;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(readPermissionsValidator, "requiredPermissionResolver", requiredPermissionResolver);
    ReflectionTestUtils.setField(readPermissionsValidator, "folioExecutionContext", folioExecutionContext);
    ReflectionTestUtils.setField(readPermissionsValidator, "permissionsProvider", permissionsProvider);
  }

  @Test
  void testCheckIfBulkEditWritePermissionExistsForInventory() {
    when(permissionsProvider.getUserPermissions(eq("tenant1"), isA(UUID.class))).thenReturn(List.of(INVENTORY_ITEMS_ITEM_PUT.getValue(), "not_write_permission", BULK_EDIT_INVENTORY_WRITE_PERMISSION.getValue()));
    when(permissionsProvider.getUserPermissions(eq("tenant2"), isA(UUID.class))).thenReturn(List.of("not_write_permission"));
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(requiredPermissionResolver.getWritePermission(any(EntityType.class))).thenCallRealMethod();

    assertDoesNotThrow(() -> permissionsValidator.checkIfBulkEditWritePermissionExists("tenant1", EntityType.ITEM, "errorMessage"));
    assertThrows(WritePermissionDoesNotExist.class, () -> permissionsValidator.checkIfBulkEditWritePermissionExists("tenant2", EntityType.ITEM, "errorMessage"));
  }

  @Test
  void testCheckIfBulkEditWritePermissionExistsForUsers() {
    when(permissionsProvider.getUserPermissions(eq("tenant1"),  isA(UUID.class))).thenReturn(List.of(USERS_ITEM_PUT.getValue(), "not_write_permission", BULK_EDIT_USERS_WRITE_PERMISSION.getValue()));
    when(permissionsProvider.getUserPermissions(eq("tenant2"), isA(UUID.class))).thenReturn(List.of("not_write_permission"));
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(requiredPermissionResolver.getWritePermission(any(EntityType.class))).thenCallRealMethod();

    assertDoesNotThrow(() -> permissionsValidator.checkIfBulkEditWritePermissionExists("tenant1", EntityType.USER, "errorMessage"));
    assertThrows(WritePermissionDoesNotExist.class, () -> permissionsValidator.checkIfBulkEditWritePermissionExists("tenant2", EntityType.USER, "errorMessage"));
  }

  @ParameterizedTest
  @EnumSource(value = EntityType.class, names = {"HOLDINGS_RECORD","INSTANCE","ITEM","USER","INSTANCE_MARC"})
  void shouldThrowExceptionIfBulkEditReadPermissionNotExistsForCurrentTenant(EntityType entityType) {
    var operation = BulkOperation.builder().entityType(entityType).build();
    var entityWithNotAllowedTenant = new ExtendedHoldingsRecord().withTenantId("tenant1").withEntity(
            HoldingsRecord.builder().id(UUID.randomUUID().toString()).build());
    var userId = UUID.randomUUID();

    when(permissionsProvider.getUserPermissions(eq("tenant1"),  isA(UUID.class))).thenReturn(List.of(BULK_EDIT_USERS_VIEW_PERMISSION.getValue()));
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn("tenant1");
    if (entityType == EntityType.ITEM || entityType == EntityType.HOLDINGS_RECORD) {
      when(consortiaService.getCentralTenantId("tenant1")).thenReturn("central");
    }
    when(userClient.getUserById(userId.toString())).thenReturn(User.builder().id(userId.toString()).username("username").build());
    when(requiredPermissionResolver.getReadPermission(any(EntityType.class))).thenCallRealMethod();
    when(readPermissionsValidator.isBulkEditReadPermissionExists("tenant1", entityType)).thenCallRealMethod();

    assertThrows(ReadPermissionException.class, () -> permissionsValidator.checkPermissions(operation, entityWithNotAllowedTenant));
  }

  @ParameterizedTest
  @CsvSource(value = {
          "HOLDINGS_RECORD,INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION,BULK_EDIT_INVENTORY_VIEW_PERMISSION",
          "INSTANCE,INVENTORY_INSTANCES_ITEM_GET_PERMISSION,BULK_EDIT_INVENTORY_VIEW_PERMISSION",
          "ITEM,INVENTORY_ITEMS_ITEM_GET_PERMISSION,BULK_EDIT_INVENTORY_VIEW_PERMISSION",
          "USER,USER_ITEM_GET_PERMISSION,BULK_EDIT_USERS_VIEW_PERMISSION",
          "INSTANCE_MARC,INVENTORY_INSTANCES_ITEM_GET_PERMISSION,BULK_EDIT_INVENTORY_VIEW_PERMISSION"})
  void shouldNotThrowExceptionIfBulkEditReadPermissionExistsForCurrentTenant(EntityType entityType, PermissionEnum inventoryOrUsersPermission, PermissionEnum bulkEditPermission) {
    var operation = BulkOperation.builder().entityType(entityType).build();
    var entityWithAllowedTenant = new ExtendedHoldingsRecord().withTenantId("tenant1").withEntity(
            HoldingsRecord.builder().id(UUID.randomUUID().toString()).build());
    var userId = UUID.randomUUID();

    when(permissionsProvider.getUserPermissions(eq("tenant1"),  isA(UUID.class))).thenReturn(List.of(bulkEditPermission.getValue(), inventoryOrUsersPermission.getValue()));
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn("tenant1");
    if (entityType == EntityType.ITEM || entityType == EntityType.HOLDINGS_RECORD) {
      when(consortiaService.getCentralTenantId("tenant1")).thenReturn("central");
    }
    when(requiredPermissionResolver.getReadPermission(any(EntityType.class))).thenCallRealMethod();
    when(readPermissionsValidator.isBulkEditReadPermissionExists("tenant1", entityType)).thenCallRealMethod();

    assertDoesNotThrow( () -> permissionsValidator.checkPermissions(operation, entityWithAllowedTenant));
  }

  @Test
  void shouldThrowDuplicatesAcrossTenantsForHoldingsIfCurrentTenantIsCentral() {
    var operation = BulkOperation.builder().entityType(EntityType.HOLDINGS_RECORD).build();
    var entityWithAllowedTenant = new ExtendedHoldingsRecord().withTenantId("tenant1").withEntity(
            HoldingsRecord.builder().id(UUID.randomUUID().toString()).build());
    var holdingsCollection = new ConsortiumHoldingCollection();
    var consHold = new ConsortiumHolding();
    consHold.setTenantId("tenant2");
    var consHold2 = new ConsortiumHolding();
    consHold2.setTenantId("tenant1");
    holdingsCollection.setHoldings(List.of(consHold, consHold2));

    when(consortiaService.getCentralTenantId("tenant1")).thenReturn("tenant1");
    when(folioExecutionContext.getTenantId()).thenReturn("tenant1");
    when(searchClient.getConsortiumHoldingCollection(any(BatchIdsDto.class)))
            .thenReturn(holdingsCollection);

    assertThrows(UploadFromQueryException.class, () -> permissionsValidator.checkPermissions(operation, entityWithAllowedTenant),
            DUPLICATES_ACROSS_TENANTS);
  }

  @Test
  void shouldThrowNoMatchFoundForHoldingsIfCurrentTenantIsCentral() {
    var operation = BulkOperation.builder().entityType(EntityType.HOLDINGS_RECORD).build();
    var entityWithAllowedTenant = new ExtendedHoldingsRecord().withTenantId("tenant1").withEntity(
            HoldingsRecord.builder().id(UUID.randomUUID().toString()).build());

    when(consortiaService.getCentralTenantId("tenant1")).thenReturn("tenant1");
    when(folioExecutionContext.getTenantId()).thenReturn("tenant1");
    when(searchClient.getConsortiumHoldingCollection(any(BatchIdsDto.class)))
            .thenReturn(new ConsortiumHoldingCollection());

    assertThrows(UploadFromQueryException.class, () -> permissionsValidator.checkPermissions(operation, entityWithAllowedTenant),
            NO_MATCH_FOUND_MESSAGE);
  }

  @Test
  @SneakyThrows
  void shouldCallTenantResolverForHoldingsIfCurrentTenantIsCentral() {
    var operation = BulkOperation.builder().entityType(EntityType.HOLDINGS_RECORD).build();
    var entityWithAllowedTenant = new ExtendedHoldingsRecord().withTenantId("tenant1").withEntity(
            HoldingsRecord.builder().id(UUID.randomUUID().toString()).build());
    var holdingsCollection = new ConsortiumHoldingCollection();
    var consHold = new ConsortiumHolding();
    consHold.setTenantId("tenant2");
    holdingsCollection.setHoldings(List.of(consHold));

    when(consortiaService.getCentralTenantId("tenant1")).thenReturn("tenant1");
    when(folioExecutionContext.getTenantId()).thenReturn("tenant1");
    when(searchClient.getConsortiumHoldingCollection(any(BatchIdsDto.class)))
            .thenReturn(holdingsCollection);

    permissionsValidator.checkPermissions(operation, entityWithAllowedTenant);

    verify(tenantResolver).checkAffiliatedPermittedTenantIds(any(EntityType.class), any(String.class), any(Set.class),
            any(String.class));
  }

  @Test
  void shouldThrowDuplicatesAcrossTenantsForItemIfCurrentTenantIsCentral() {
    var operation = BulkOperation.builder().entityType(EntityType.ITEM).build();
    var entityWithAllowedTenant = new ExtendedItem().withTenantId("tenant1").withEntity(
            Item.builder().id(UUID.randomUUID().toString()).build());
    var itemsCollection = new ConsortiumItemCollection();
    var consItem = new ConsortiumItem();
    consItem.setTenantId("tenant2");
    var consItem2 = new ConsortiumItem();
    consItem2.setTenantId("tenant1");
    itemsCollection.setItems(List.of(consItem, consItem2));

    when(consortiaService.getCentralTenantId("tenant1")).thenReturn("tenant1");
    when(folioExecutionContext.getTenantId()).thenReturn("tenant1");
    when(searchClient.getConsortiumItemCollection(any(BatchIdsDto.class)))
            .thenReturn(itemsCollection);

    assertThrows(UploadFromQueryException.class, () -> permissionsValidator.checkPermissions(operation, entityWithAllowedTenant),
            DUPLICATES_ACROSS_TENANTS);
  }

  @Test
  void shouldThrowNoMatchFoundForItemIfCurrentTenantIsCentral() {
    var operation = BulkOperation.builder().entityType(EntityType.ITEM).build();
    var entityWithAllowedTenant = new ExtendedItem().withTenantId("tenant1").withEntity(
            Item.builder().id(UUID.randomUUID().toString()).build());

    when(consortiaService.getCentralTenantId("tenant1")).thenReturn("tenant1");
    when(folioExecutionContext.getTenantId()).thenReturn("tenant1");
    when(searchClient.getConsortiumItemCollection(any(BatchIdsDto.class)))
            .thenReturn(new ConsortiumItemCollection());

    assertThrows(UploadFromQueryException.class, () -> permissionsValidator.checkPermissions(operation, entityWithAllowedTenant),
            NO_MATCH_FOUND_MESSAGE);
  }

  @Test
  @SneakyThrows
  void shouldCallTenantResolverForItemIfCurrentTenantIsCentral() {
    var operation = BulkOperation.builder().entityType(EntityType.ITEM).build();
    var entityWithAllowedTenant = new ExtendedItem().withTenantId("tenant1").withEntity(
            Item.builder().id(UUID.randomUUID().toString()).build());
    var itemCollection = new ConsortiumItemCollection();
    var consItem = new ConsortiumItem();
    consItem.setTenantId("tenant2");
    itemCollection.setItems(List.of(consItem));

    when(consortiaService.getCentralTenantId("tenant1")).thenReturn("tenant1");
    when(folioExecutionContext.getTenantId()).thenReturn("tenant1");
    when(searchClient.getConsortiumItemCollection(any(BatchIdsDto.class)))
            .thenReturn(itemCollection);

    permissionsValidator.checkPermissions(operation, entityWithAllowedTenant);

    verify(tenantResolver).checkAffiliatedPermittedTenantIds(any(EntityType.class), any(String.class), any(Set.class),
            any(String.class));
  }
}
