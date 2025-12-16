package org.folio.bulkops.batch.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.bulkops.util.Constants.LINKED_DATA_SOURCE_IS_NOT_SUPPORTED;
import static org.folio.bulkops.util.Constants.SRS_MISSING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.bulkops.batch.jobs.processidentifiers.DuplicationCheckerFactory;
import org.folio.bulkops.client.HoldingsStorageClient;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.InstanceCollection;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.domain.converter.JsonToMarcConverter;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.service.SrsService;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.JobExecution;
import org.springframework.test.util.ReflectionTestUtils;

class BulkEditInstanceProcessorTest {

  @Mock private InstanceClient instanceClient;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private PermissionsValidator permissionsValidator;
  @Mock private UserClient userClient;
  @Mock private SrsClient srsClient;
  @Mock private DuplicationCheckerFactory duplicationCheckerFactory;
  @Mock private JobExecution jobExecution;
  @Mock private JsonToMarcConverter jsonToMarcConverter;
  @Mock private HoldingsStorageClient holdingsStorageClient;
  @InjectMocks private SrsService srsService;

  private BulkEditInstanceProcessor processor;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    processor =
        new BulkEditInstanceProcessor(
            instanceClient,
            folioExecutionContext,
            permissionsValidator,
            userClient,
            duplicationCheckerFactory,
            srsService,
            holdingsStorageClient);
    ReflectionTestUtils.setField(processor, "identifierType", IdentifierType.ID.getValue());
    ReflectionTestUtils.setField(processor, "jobExecution", jobExecution);
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any()))
        .thenReturn(new HashSet<>());
    when(duplicationCheckerFactory.getFetchedIds(any())).thenReturn(new HashSet<>());
  }

  @Test
  void shouldReturnFolioInstance() {
    ItemIdentifier identifier = new ItemIdentifier().withItemId("111");
    Instance instance = Instance.builder().id("id1").source("FOLIO").title("Sample title").build();
    when(instanceClient.getInstanceByQuery(anyString(), anyLong()))
        .thenReturn(
            InstanceCollection.builder().instances(List.of(instance)).totalRecords(1).build());
    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.INSTANCE)))
        .thenReturn(true);

    List<ExtendedInstance> result = processor.process(identifier);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getEntity().getSource()).isEqualTo("FOLIO");
  }

  @Test
  @SneakyThrows
  void shouldReturnMarcInstanceWithSrs() {
    ReflectionTestUtils.setField(processor, "identifierType", IdentifierType.ID.getValue());
    ItemIdentifier identifier = new ItemIdentifier().withItemId("222");
    String instanceId = "id2";
    Instance instance =
        Instance.builder().id(instanceId).source("MARC").title("Sample title").build();
    when(instanceClient.getInstanceByQuery(anyString(), anyLong()))
        .thenReturn(
            InstanceCollection.builder().instances(List.of(instance)).totalRecords(1).build());
    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.INSTANCE)))
        .thenReturn(true);

    var srsJson =
        objectMapper.readTree(new File("src/test/resources/files/srs_response_for_validator.json"));
    when(srsClient.getMarc(instanceId, "INSTANCE", true)).thenReturn(srsJson);

    when(jobExecution.getExecutionContext())
        .thenReturn(new org.springframework.batch.item.ExecutionContext());

    List<ExtendedInstance> result = processor.process(identifier);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getEntity().getSource()).isEqualTo("MARC");
  }

  @Test
  void shouldThrowWhenSrsIsMissingForMarc() {
    String instanceId = "id3";
    Instance instance =
        Instance.builder().id(instanceId).source("MARC").title("Sample title").build();
    when(instanceClient.getInstanceByQuery(anyString(), anyLong()))
        .thenReturn(
            InstanceCollection.builder().instances(List.of(instance)).totalRecords(1).build());
    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.INSTANCE)))
        .thenReturn(true);

    ObjectNode srsJson = objectMapper.createObjectNode();
    srsJson.set("sourceRecords", objectMapper.createArrayNode());
    when(srsClient.getMarc(instanceId, "INSTANCE", true)).thenReturn(srsJson);

    when(jobExecution.getExecutionContext())
        .thenReturn(new org.springframework.batch.item.ExecutionContext());
    ItemIdentifier identifier = new ItemIdentifier().withItemId("333");
    assertThatThrownBy(() -> processor.process(identifier))
        .isInstanceOf(BulkEditException.class)
        .hasMessageContaining(SRS_MISSING);
  }

  @Test
  void shouldThrowWhenMultipleSrsForMarc() {
    String instanceId = "id4";
    Instance instance =
        Instance.builder().id(instanceId).source("MARC").title("Sample title").build();
    when(instanceClient.getInstanceByQuery(anyString(), anyLong()))
        .thenReturn(
            InstanceCollection.builder().instances(List.of(instance)).totalRecords(1).build());
    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.INSTANCE)))
        .thenReturn(true);

    ObjectNode srsJson = objectMapper.createObjectNode();
    srsJson.set(
        "sourceRecords",
        objectMapper
            .createArrayNode()
            .add(objectMapper.createObjectNode().put("recordId", "rec1"))
            .add(objectMapper.createObjectNode().put("recordId", "rec2")));
    when(srsClient.getMarc(instanceId, "INSTANCE", true)).thenReturn(srsJson);

    when(jobExecution.getExecutionContext())
        .thenReturn(new org.springframework.batch.item.ExecutionContext());
    ItemIdentifier identifier = new ItemIdentifier().withItemId("444");
    assertThatThrownBy(() -> processor.process(identifier))
        .isInstanceOf(BulkEditException.class)
        .hasMessageContaining("Multiple SRS records");
  }

  @Test
  void shouldThrowWhenSourceIsLinkedData() {
    ItemIdentifier identifier = new ItemIdentifier().withItemId("555");
    Instance instance =
        Instance.builder().id("id5").source("LINKED_DATA").title("Sample title").build();
    when(instanceClient.getInstanceByQuery(anyString(), anyLong()))
        .thenReturn(
            InstanceCollection.builder().instances(List.of(instance)).totalRecords(1).build());
    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.INSTANCE)))
        .thenReturn(true);

    assertThatThrownBy(() -> processor.process(identifier))
        .isInstanceOf(BulkEditException.class)
        .hasMessageContaining(LINKED_DATA_SOURCE_IS_NOT_SUPPORTED);
  }

  @Test
  void shouldThrowWhenNoPermission() {
    ItemIdentifier identifier = new ItemIdentifier().withItemId("666");
    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.INSTANCE)))
        .thenReturn(false);
    when(userClient.getUserById(anyString()))
        .thenReturn(new org.folio.bulkops.domain.bean.User().withUsername("testuser"));

    assertThatThrownBy(() -> processor.process(identifier))
        .isInstanceOf(BulkEditException.class)
        .hasMessageContaining("does not have required permission");
  }

  @Test
  void shouldThrowOnDuplicateIdentifier() {
    ItemIdentifier identifier = new ItemIdentifier().withItemId("777");
    Set<ItemIdentifier> set = new HashSet<>();
    set.add(identifier);
    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(set);
    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.INSTANCE)))
        .thenReturn(true);

    assertThatThrownBy(() -> processor.process(identifier))
        .isInstanceOf(BulkEditException.class)
        .hasMessageContaining("Duplicate entry");
  }

  @Test
  void shouldThrowOnMultipleMatches() {
    ItemIdentifier identifier = new ItemIdentifier().withItemId("888");
    Instance i1 = Instance.builder().id("id1").source("FOLIO").build();
    Instance i2 = Instance.builder().id("id2").source("FOLIO").build();
    when(instanceClient.getInstanceByQuery(anyString(), anyLong()))
        .thenReturn(
            InstanceCollection.builder().instances(List.of(i1, i2)).totalRecords(2).build());
    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.INSTANCE)))
        .thenReturn(true);

    assertThatThrownBy(() -> processor.process(identifier))
        .isInstanceOf(BulkEditException.class)
        .hasMessageContaining("Multiple matches");
  }

  @Test
  void shouldThrowOnNoMatchFound() {
    ItemIdentifier identifier = new ItemIdentifier().withItemId("999");
    when(instanceClient.getInstanceByQuery(anyString(), anyLong()))
        .thenReturn(
            InstanceCollection.builder()
                .instances(Collections.emptyList())
                .totalRecords(0)
                .build());
    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.INSTANCE)))
        .thenReturn(true);

    assertThatThrownBy(() -> processor.process(identifier))
        .isInstanceOf(BulkEditException.class)
        .hasMessageContaining("No match found");
  }
}
