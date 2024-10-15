package org.folio.bulkops.service;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.UserTenant;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantTableUpdaterTest {

  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private ConsortiaService consortiaService;

  @InjectMocks
  private TenantTableUpdater tableUpdater;

  @ParameterizedTest
  @MethodSource("getEntityClassesWithTenant")
  void updateTenantInHeadersAndRowsForNotConsortiaTest(Class<? extends BulkOperationsEntity> entityClass) {
    var table = new UnifiedTable();
    var cell = new Cell();
    cell.setValue("Tenant");
    var headers = new ArrayList<>(List.of(cell));
    table.setHeader(headers);
    var row = new Row();
    row.setRow(new ArrayList<>(List.of("value")));
    table.setRows(List.of(row));

    when(folioExecutionContext.getTenantId()).thenReturn(UUID.randomUUID().toString());
    when(consortiaService.isCurrentTenantInConsortia(anyString())).thenReturn(false);

    tableUpdater.updateTenantInHeadersAndRows(table, entityClass);

    assertEquals(0, table.getHeader().size());
    assertEquals(0, table.getRows().get(0).getRow().size());
  }

  @ParameterizedTest
  @MethodSource("getEntityClassesWithTenant")
  void updateTenantInHeadersAndRowsForConsortiaTest(Class<? extends BulkOperationsEntity> entityClass) {
    Map<String, UserTenant> userTenants = new HashMap<>();
    var userTenant = new UserTenant();
    userTenant.setTenantId("tenantId");
    userTenant.setTenantName("tenantName");
    userTenants.put("tenantId", userTenant);

    var table = new UnifiedTable();
    var cell = new Cell();
    cell.setValue("Tenant");
    var headers = List.of(cell);
    table.setHeader(headers);
    var row = new Row();
    row.setRow(new ArrayList<>(List.of("tenantId")));
    table.setRows(List.of(row));

    when(folioExecutionContext.getTenantId()).thenReturn(UUID.randomUUID().toString());
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(consortiaService.isCurrentTenantInConsortia(anyString())).thenReturn(true);
    when(consortiaService.getUserTenantsPerId(anyString(), anyString())).thenReturn(userTenants);

    tableUpdater.updateTenantInHeadersAndRows(table, entityClass);

    var actualTenantHeaderValue = table.getHeader().get(0).getValue();
    assertEquals("Member", actualTenantHeaderValue);
    var actualTenantRowValue = table.getRows().get(0).getRow().get(0);
    assertEquals("tenantName", actualTenantRowValue);
  }

  @Test
  void updateTenantInHeadersAndRowsForConsortiaIfTypeDoesNotHaveTenantTest() {
    var table = new UnifiedTable();
    var cell = new Cell();
    cell.setValue("Header");
    var headers = List.of(cell);
    table.setHeader(headers);
    var row = new Row();
    row.setRow(new ArrayList<>(List.of("value")));
    table.setRows(List.of(row));

    tableUpdater.updateTenantInHeadersAndRows(table, Instance.class);

    verify(folioExecutionContext, never()).getTenantId();
    verify(consortiaService, never()).isCurrentTenantInConsortia(anyString());
  }

  private static Stream<Arguments> getEntityClassesWithTenant() {
    return Stream.of(Arguments.of(HoldingsRecord.class), Arguments.of(Item.class));
  }
}
