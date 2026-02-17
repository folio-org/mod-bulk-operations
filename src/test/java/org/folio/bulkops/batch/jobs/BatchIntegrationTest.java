package org.folio.bulkops.batch.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_VIEW_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_USERS_VIEW_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_INSTANCES_ITEM_GET_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.USER_ITEM_GET_PERMISSION;
import static org.folio.bulkops.util.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.bulkops.util.BulkEditProcessorHelper.resolveIdentifier;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.batch.JobCommandHelper;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.SearchClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.InstanceCollection;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.UserPermissionsService;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class BatchIntegrationTest extends BaseTest {
  @MockitoBean private UserPermissionsService userPermissionsService;
  @MockitoBean private RemoteFileSystemClient remoteFileSystemClient;
  @MockitoBean private BulkOperationRepository bulkOperationRepository;
  @MockitoBean private SrsClient srsClient;
  @MockitoBean private ErrorService errorService;
  @MockitoBean private ConsortiaService consortiaService;
  @MockitoBean private SearchClient searchClient;

  @Autowired private JobOperator jobOperator;
  @Autowired private JobRepository jobRepository;
  @Autowired private Job bulkEditProcessInstanceIdentifiersJob;
  @Autowired private Job bulkEditProcessUserIdentifiersJob;

  private final IdentifierType identifierType = IdentifierType.ID;
  private final String identifier = "111";
  private final String linkToTriggeringCsvFile = "path/to/identifiers.csv";

  private UUID bulkOperationId;

  @BeforeEach
  void initializeVariables() {
    bulkOperationId = UUID.randomUUID();
  }

  @Test
  @SneakyThrows
  void shouldFetchAndWriteFolioInstance() {
    var bulkOperation =
        BulkOperation.builder()
            .id(bulkOperationId)
            .entityType(EntityType.INSTANCE)
            .identifierType(identifierType)
            .linkToTriggeringCsvFile(linkToTriggeringCsvFile)
            .build();

    when(bulkOperationRepository.findById(bulkOperationId)).thenReturn(Optional.of(bulkOperation));
    when(bulkOperationRepository.save(any(BulkOperation.class))).thenReturn(bulkOperation);
    when(remoteFileSystemClient.get(linkToTriggeringCsvFile))
        .thenReturn(new ByteArrayInputStream(identifier.getBytes()));
    when(remoteFileSystemClient.getNumOfLines(linkToTriggeringCsvFile)).thenReturn(1);
    when(userPermissionsService.getPermissions())
        .thenReturn(
            List.of(
                BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue(),
                INVENTORY_INSTANCES_ITEM_GET_PERMISSION.getValue()));
    var instanceQuery =
        getMatchPattern(identifierType.getValue())
            .formatted(resolveIdentifier(identifierType.getValue()), identifier);
    when(instanceClient.getInstanceByQuery(instanceQuery, 1))
        .thenReturn(
            InstanceCollection.builder()
                .instances(
                    Collections.singletonList(
                        Instance.builder()
                            .id(UUID.randomUUID().toString())
                            .source("FOLIO")
                            .title("Sample title")
                            .build()))
                .totalRecords(1)
                .build());

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      JobOperatorTestUtils testLauncher = createTestLauncher(bulkEditProcessInstanceIdentifiersJob);
      var jobParameters = JobCommandHelper.prepareJobParameters(bulkOperation, 1);

      JobExecution jobExecution = testLauncher.startJob(jobParameters);

      verify(remoteFileSystemClient, times(2)).put(any(InputStream.class), any(String.class));

      assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }
  }

  @Test
  @SneakyThrows
  void shouldFetchAndWriteUser() {
    var bulkOperation =
        BulkOperation.builder()
            .id(bulkOperationId)
            .entityType(EntityType.USER)
            .identifierType(identifierType)
            .linkToTriggeringCsvFile(linkToTriggeringCsvFile)
            .build();

    when(bulkOperationRepository.findById(bulkOperationId)).thenReturn(Optional.of(bulkOperation));
    when(bulkOperationRepository.save(any(BulkOperation.class))).thenReturn(bulkOperation);
    when(remoteFileSystemClient.get(linkToTriggeringCsvFile))
        .thenReturn(new ByteArrayInputStream(identifier.getBytes()));
    when(remoteFileSystemClient.getNumOfLines(linkToTriggeringCsvFile)).thenReturn(1);
    when(userPermissionsService.getPermissions())
        .thenReturn(
            List.of(
                USER_ITEM_GET_PERMISSION.getValue(), BULK_EDIT_USERS_VIEW_PERMISSION.getValue()));
    when(userClient.getByQuery(anyString(), anyLong()))
        .thenReturn(
            UserCollection.builder()
                .users(
                    List.of(
                        User.builder()
                            .id(UUID.randomUUID().toString())
                            .personal(Personal.builder().dateOfBirth(new Date()).build())
                            .build()))
                .totalRecords(1)
                .build());

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      JobOperatorTestUtils testLauncher = createTestLauncher(bulkEditProcessUserIdentifiersJob);
      var jobParameters = JobCommandHelper.prepareJobParameters(bulkOperation, 1);

      JobExecution jobExecution = testLauncher.startJob(jobParameters);

      verify(remoteFileSystemClient, times(2)).put(any(InputStream.class), any(String.class));

      assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }
  }

  private JobOperatorTestUtils createTestLauncher(Job job) {
    JobOperatorTestUtils testLauncher = new JobOperatorTestUtils(jobOperator, jobRepository);
    testLauncher.setJob(job);
    return testLauncher;
  }
}
