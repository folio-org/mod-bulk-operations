package org.folio.bulkops.batch.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_VIEW_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_INSTANCES_ITEM_GET_PERMISSION;
import static org.folio.bulkops.util.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.bulkops.util.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.bulkops.util.Constants.LINKED_DATA_SOURCE_IS_NOT_SUPPORTED;
import static org.folio.bulkops.util.Constants.SRS_MISSING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.batch.JobCommandHelper;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.SearchClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.InstanceCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.UserPermissionsService;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class ProcessInstancesBatchTest extends BaseTest {
  @MockitoBean
  private UserPermissionsService userPermissionsService;
  @MockitoBean
  private RemoteFileSystemClient remoteFileSystemClient;
  @MockitoBean
  private BulkOperationRepository bulkOperationRepository;
  @MockitoBean
  private SrsClient srsClient;
  @MockitoBean
  private ErrorService errorService;
  @MockitoBean
  private ConsortiaService consortiaService;
  @MockitoBean
  private SearchClient searchClient;

  @Autowired
  private JobLauncher jobLauncher;
  @Autowired
  private JobRepository jobRepository;
  @Autowired
  private Job bulkEditProcessInstanceIdentifiersJob;

  private final IdentifierType identifierType = IdentifierType.ID;
  private final String identifier = "111";
  private final String linkToTriggeringCsvFile = "path/to/identifiers.csv";

  private UUID bulkOperationId;
  private String recordId;

  @BeforeEach
  void initializeVariables() {
    bulkOperationId = UUID.randomUUID();
    recordId = UUID.randomUUID().toString();
  }

  @Test
  @SneakyThrows
  void instance_shouldRetrieveFolioInstance() {
    var bulkOperation = BulkOperation.builder()
      .id(bulkOperationId)
      .entityType(EntityType.INSTANCE)
      .identifierType(identifierType)
      .linkToTriggeringCsvFile(linkToTriggeringCsvFile)
      .build();

    when(bulkOperationRepository.findById(bulkOperationId))
      .thenReturn(Optional.of(bulkOperation));
    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(bulkOperation);
    when(remoteFileSystemClient.get(linkToTriggeringCsvFile))
      .thenReturn(new ByteArrayInputStream(identifier.getBytes()));
    when(remoteFileSystemClient.getNumOfLines(linkToTriggeringCsvFile))
      .thenReturn(1);
    when(userPermissionsService.getPermissions())
      .thenReturn(List.of(BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue(), INVENTORY_INSTANCES_ITEM_GET_PERMISSION.getValue()));
    var instanceQuery = getMatchPattern(identifierType.getValue()).formatted(resolveIdentifier(identifierType.getValue()), identifier);
    when(instanceClient.getInstanceByQuery(instanceQuery, 1))
      .thenReturn(InstanceCollection.builder()
        .instances(Collections.singletonList(
          Instance.builder()
            .id(UUID.randomUUID().toString())
            .source("FOLIO")
            .title("Sample title")
            .build()))
        .totalRecords(1)
        .build());

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessInstanceIdentifiersJob);
      var jobParameters = JobCommandHelper.prepareJobParameters(bulkOperation, 1);

      JobExecution jobExecution = testLauncher.launchJob(jobParameters);

      verify(remoteFileSystemClient, times(2)).put(any(InputStream.class), any(String.class));

      assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }
  }

  @Test
  @SneakyThrows
  void instance_shouldRetrieveMarcInstance() {
    var bulkOperation = BulkOperation.builder()
      .id(bulkOperationId)
      .entityType(EntityType.INSTANCE)
      .identifierType(identifierType)
      .linkToTriggeringCsvFile(linkToTriggeringCsvFile)
      .build();
    String srsJson = """
              {
                "sourceRecords": [
                    { "recordId": "22240328-788e-43fc-9c3c-af39e243f3b7" }
                  ]
              }
              """;

    when(bulkOperationRepository.findById(bulkOperationId))
      .thenReturn(Optional.of(bulkOperation));
    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(bulkOperation);
    when(remoteFileSystemClient.get(linkToTriggeringCsvFile))
      .thenReturn(new ByteArrayInputStream(identifier.getBytes()));
    when(remoteFileSystemClient.getNumOfLines(linkToTriggeringCsvFile))
      .thenReturn(1);
    when(userPermissionsService.getPermissions())
      .thenReturn(List.of(BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue(), INVENTORY_INSTANCES_ITEM_GET_PERMISSION.getValue()));
    var instanceQuery = getMatchPattern(identifierType.getValue()).formatted(resolveIdentifier(identifierType.getValue()), identifier);
    when(instanceClient.getInstanceByQuery(instanceQuery, 1))
      .thenReturn(InstanceCollection.builder()
        .instances(Collections.singletonList(
          Instance.builder()
            .id(recordId)
            .source("MARC")
            .title("Sample title")
            .build()))
        .totalRecords(1)
        .build());
    when(srsClient.getMarc(recordId, "INSTANCE", true))
      .thenReturn(objectMapper.readTree(srsJson));

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessInstanceIdentifiersJob);
      var jobParameters = JobCommandHelper.prepareJobParameters(bulkOperation, 1);

      JobExecution jobExecution = testLauncher.launchJob(jobParameters);

      verify(remoteFileSystemClient, times(3)).put(any(InputStream.class), any(String.class));

      assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }
  }

  @Test
  @SneakyThrows
  void instances_shouldSaveErrorWhenSourceIsLinkedData() {
    var bulkOperation = BulkOperation.builder()
      .id(bulkOperationId)
      .entityType(EntityType.INSTANCE)
      .identifierType(identifierType)
      .linkToTriggeringCsvFile(linkToTriggeringCsvFile)
      .build();

    when(bulkOperationRepository.findById(bulkOperationId))
      .thenReturn(Optional.of(bulkOperation));
    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(bulkOperation);
    when(remoteFileSystemClient.get(linkToTriggeringCsvFile))
      .thenReturn(new ByteArrayInputStream(identifier.getBytes()));
    when(remoteFileSystemClient.getNumOfLines(linkToTriggeringCsvFile))
      .thenReturn(1);
    when(userPermissionsService.getPermissions())
      .thenReturn(List.of(BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue(), INVENTORY_INSTANCES_ITEM_GET_PERMISSION.getValue()));
    var instanceQuery = getMatchPattern(identifierType.getValue()).formatted(resolveIdentifier(identifierType.getValue()), identifier);
    when(instanceClient.getInstanceByQuery(instanceQuery, 1))
      .thenReturn(InstanceCollection.builder()
        .instances(Collections.singletonList(
          Instance.builder()
            .id(recordId)
            .source("LINKED_DATA")
            .title("Sample title")
            .build()))
        .totalRecords(1)
        .build());

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessInstanceIdentifiersJob);
      var jobParameters = JobCommandHelper.prepareJobParameters(bulkOperation, 1);

      testLauncher.launchJob(jobParameters);

      verify(errorService).saveError(bulkOperationId, identifier, LINKED_DATA_SOURCE_IS_NOT_SUPPORTED, ErrorType.ERROR);
    }
  }

  @Test
  @SneakyThrows
  void instances_shouldSaveErrorWhenSrsIsMissingForMarc() {
    var bulkOperation = BulkOperation.builder()
      .id(bulkOperationId)
      .entityType(EntityType.INSTANCE)
      .identifierType(identifierType)
      .linkToTriggeringCsvFile(linkToTriggeringCsvFile)
      .build();

    when(bulkOperationRepository.findById(bulkOperationId))
      .thenReturn(Optional.of(bulkOperation));
    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(bulkOperation);
    when(remoteFileSystemClient.get(linkToTriggeringCsvFile))
      .thenReturn(new ByteArrayInputStream(identifier.getBytes()));
    when(remoteFileSystemClient.getNumOfLines(linkToTriggeringCsvFile))
      .thenReturn(1);
    when(userPermissionsService.getPermissions())
      .thenReturn(List.of(BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue(), INVENTORY_INSTANCES_ITEM_GET_PERMISSION.getValue()));
    var instanceQuery = getMatchPattern(identifierType.getValue()).formatted(resolveIdentifier(identifierType.getValue()), identifier);
    when(instanceClient.getInstanceByQuery(instanceQuery, 1))
      .thenReturn(InstanceCollection.builder()
        .instances(Collections.singletonList(
          Instance.builder()
            .id(recordId)
            .source("MARC")
            .title("Sample title")
            .build()))
        .totalRecords(1)
        .build());
    String srsJson = """
              {
                "sourceRecords": []
              }
              """;
    when(srsClient.getMarc(recordId, "INSTANCE", true))
      .thenReturn(objectMapper.readTree(srsJson));

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessInstanceIdentifiersJob);
      var jobParameters = JobCommandHelper.prepareJobParameters(bulkOperation, 1);

      testLauncher.launchJob(jobParameters);

      verify(errorService).saveError(bulkOperationId, identifier, SRS_MISSING, ErrorType.ERROR);
    }
  }

  @Test
  @SneakyThrows
  void instances_shouldSaveErrorWhenMultipleSrsForMarc() {
    var bulkOperation = BulkOperation.builder()
      .id(bulkOperationId)
      .entityType(EntityType.INSTANCE)
      .identifierType(identifierType)
      .linkToTriggeringCsvFile(linkToTriggeringCsvFile)
      .build();

    when(bulkOperationRepository.findById(bulkOperationId))
      .thenReturn(Optional.of(bulkOperation));
    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(bulkOperation);
    when(remoteFileSystemClient.get(linkToTriggeringCsvFile))
      .thenReturn(new ByteArrayInputStream(identifier.getBytes()));
    when(remoteFileSystemClient.getNumOfLines(linkToTriggeringCsvFile))
      .thenReturn(1);
    when(userPermissionsService.getPermissions())
      .thenReturn(List.of(BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue(), INVENTORY_INSTANCES_ITEM_GET_PERMISSION.getValue()));
    var instanceQuery = getMatchPattern(identifierType.getValue()).formatted(resolveIdentifier(identifierType.getValue()), identifier);
    when(instanceClient.getInstanceByQuery(instanceQuery, 1))
      .thenReturn(InstanceCollection.builder()
        .instances(Collections.singletonList(
          Instance.builder()
            .id(recordId)
            .source("MARC")
            .title("Sample title")
            .build()))
        .totalRecords(1)
        .build());
    String srsJson = """
              {
                "sourceRecords": [
                  { "recordId": "22240328-788e-43fc-9c3c-af39e243f3b7" },
                  { "recordId": "33340328-788e-43fc-9c3c-af39e243f3b7" }
                ]
              }
              """;
    when(srsClient.getMarc(recordId, "INSTANCE", true))
      .thenReturn(objectMapper.readTree(srsJson));

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessInstanceIdentifiersJob);
      var jobParameters = JobCommandHelper.prepareJobParameters(bulkOperation, 1);

      testLauncher.launchJob(jobParameters);

      var errorMessageCaptor = ArgumentCaptor.forClass(String.class);

      verify(errorService).saveError(eq(bulkOperationId), eq(identifier), errorMessageCaptor.capture(), eq(ErrorType.ERROR));

      assertThat(errorMessageCaptor.getValue()).contains(List.of("22240328-788e-43fc-9c3c-af39e243f3b7", "33340328-788e-43fc-9c3c-af39e243f3b7"));
    }
  }

  private JobLauncherTestUtils createTestLauncher(Job job) {
    JobLauncherTestUtils testLauncher = new JobLauncherTestUtils();
    testLauncher.setJob(job);
    testLauncher.setJobLauncher(jobLauncher);
    testLauncher.setJobRepository(jobRepository);
    return testLauncher;
  }
}
