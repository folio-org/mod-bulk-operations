package org.folio.bulkops.batch.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.folio.bulkops.batch.jobs.processidentifiers.DuplicationCheckerFactory;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.client.SearchClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordCollection;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.ConsortiumHolding;
import org.folio.bulkops.domain.dto.ConsortiumHoldingCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
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

class BulkEditHoldingsProcessorTest {

  @Mock
  private HoldingsClient holdingsClient;
  @Mock
  private HoldingsReferenceService holdingsReferenceService;
  @Mock
  private SearchClient searchClient;
  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private FolioModuleMetadata folioModuleMetadata;
  @Mock
  private UserClient userClient;
  @Mock
  private PermissionsValidator permissionsValidator;
  @Mock
  private TenantResolver tenantResolver;
  @Mock
  private DuplicationCheckerFactory duplicationCheckerFactory;

  @InjectMocks
  private BulkEditHoldingsProcessor processor;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    ReflectionTestUtils.setField(processor, "identifierType", IdentifierType.ID.getValue());
    ReflectionTestUtils.setField(processor, "jobId", "jobId");
    ReflectionTestUtils.setField(processor, "fileName", "file.csv");
    JobExecution jobExecution = Mockito.mock(JobExecution.class, Mockito.RETURNS_DEEP_STUBS);
    ReflectionTestUtils.setField(processor, "jobExecution", jobExecution);
    when(consortiaService.getCentralTenantId(anyString())).thenReturn("centralTenant");
    when(folioExecutionContext.getTenantId()).thenReturn("centralTenant");
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.of(TENANT, List.of("diku")));
  }

  @Test
  void returnsExtendedHoldingsRecordsForCentralTenantAndPermittedAffiliation() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("holdingsId");
    HoldingsRecord holdingsRecord = new HoldingsRecord().withId("holdingsId").withInstanceId("instanceId");
    HoldingsRecordCollection holdingsRecordCollection = HoldingsRecordCollection.builder()
      .holdingsRecords(List.of(holdingsRecord)).totalRecords(1).build();

    ConsortiumHolding consortiumHolding = new ConsortiumHolding().id("holdingsId").tenantId("tenant1");
    ConsortiumHoldingCollection consortiumHoldingCollection = new ConsortiumHoldingCollection()
      .holdings(List.of(consortiumHolding)).totalRecords(1);

    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(new HashSet<>());
    when(duplicationCheckerFactory.getFetchedIds(any())).thenReturn(new HashSet<>());
    when(searchClient.getConsortiumHoldingCollection(any())).thenReturn(consortiumHoldingCollection);
    when(tenantResolver.getAffiliatedPermittedTenantIds(eq(EntityType.HOLDINGS_RECORD), any(), anyString(), anySet(), eq(itemIdentifier)))
      .thenReturn(Set.of("tenant1"));
    when(holdingsClient.getByQuery(anyString())).thenReturn(holdingsRecordCollection);
    when(holdingsReferenceService.getInstanceTitleById(anyString(), anyString())).thenReturn("Instance Title");

    List<ExtendedHoldingsRecord> result = processor.process(itemIdentifier);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getEntity().getId()).isEqualTo("holdingsId");
    assertThat(result.getFirst().getTenantId()).isEqualTo("tenant1");
    assertThat(result.getFirst().getEntity().getInstanceTitle()).isEqualTo("Instance Title");
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
    ConsortiumHoldingCollection emptyConsortium = new ConsortiumHoldingCollection().holdings(Collections.emptyList()).totalRecords(0);

    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(new HashSet<>());
    when(searchClient.getConsortiumHoldingCollection(any())).thenReturn(emptyConsortium);

    assertThatThrownBy(() -> processor.process(itemIdentifier))
      .isInstanceOf(BulkEditException.class)
      .hasMessageContaining("No match found");
  }

  @Test
  void throwsWhenMultipleMatchesFoundForLocalTenant() {
    when(folioExecutionContext.getTenantId()).thenReturn("localTenant");
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("multi");
    HoldingsRecord record1 = new HoldingsRecord().withId("1");
    HoldingsRecord record2 = new HoldingsRecord().withId("2");
    HoldingsRecordCollection collection = HoldingsRecordCollection.builder()
      .holdingsRecords(List.of(record1, record2)).totalRecords(2).build();

    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(new HashSet<>());
    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), any())).thenReturn(true);
    when(holdingsClient.getByQuery(anyString())).thenReturn(collection);

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
  void throwsWhenUnsupportedIdentifierType() {
    ReflectionTestUtils.setField(processor, "identifierType", "UNSUPPORTED");
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("id");
    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(new HashSet<>());

    assertThatThrownBy(() -> processor.process(itemIdentifier))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Unexpected value 'UNSUPPORTED'");
  }

  @Test
  void returnsExtendedHoldingsRecordsWithInstanceTitleAndTenantId() {
    HoldingsRecord holdingsRecord = new HoldingsRecord().withId("holdingsId").withInstanceId("instanceId");
    HoldingsRecordCollection holdingsRecordCollection = HoldingsRecordCollection.builder()
            .holdingsRecords(List.of(holdingsRecord)).totalRecords(1).build();

    when(holdingsReferenceService.getInstanceTitleById("instanceId", "tenantId")).thenReturn("Instance Title");

    List<ExtendedHoldingsRecord> result = holdingsRecordCollection.getHoldingsRecords().stream()
            .map(holdRec -> holdRec.withInstanceTitle(holdingsReferenceService.getInstanceTitleById(holdRec.getInstanceId(), "tenantId")))
            .map(holdRec -> new ExtendedHoldingsRecord().withTenantId("tenantId").withEntity(holdRec))
            .toList();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getEntity().getInstanceTitle()).isEqualTo("Instance Title");
    assertThat(result.getFirst().getTenantId()).isEqualTo("tenantId");
  }

  @Test
  void returnsEmptyListWhenNoHoldingsRecordsExist() {
    HoldingsRecordCollection holdingsRecordCollection = HoldingsRecordCollection.builder()
            .holdingsRecords(Collections.emptyList()).totalRecords(0).build();

    List<ExtendedHoldingsRecord> result = holdingsRecordCollection.getHoldingsRecords().stream()
            .map(holdRec -> holdRec.withInstanceTitle(holdingsReferenceService.getInstanceTitleById(holdRec.getInstanceId(), "tenantId")))
            .map(holdRec -> new ExtendedHoldingsRecord().withTenantId("tenantId").withEntity(holdRec))
            .toList();

    assertThat(result).isEmpty();
  }
}
