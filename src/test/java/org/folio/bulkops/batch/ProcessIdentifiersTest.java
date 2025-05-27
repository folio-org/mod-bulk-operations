package org.folio.bulkops.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_VIEW_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_INSTANCES_ITEM_GET_PERMISSION;
import static org.folio.bulkops.util.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.bulkops.util.BulkEditProcessorHelper.resolveIdentifier;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.InstanceCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.ReferenceDataNotFoundException;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.InstanceReferenceService;
import org.folio.bulkops.service.UserPermissionsService;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

class ProcessIdentifiersTest extends BaseTest {
  @MockitoBean
  private UserPermissionsService userPermissionsService;
  @MockitoBean
  private RemoteFileSystemClient remoteFileSystemClient;
  @MockitoBean
  private BulkOperationRepository bulkOperationRepository;
//  @MockitoBean
//  private InstanceReferenceService instanceReferenceService;

  @Autowired
  private JobLauncher jobLauncher;
  @Autowired
  private JobRepository jobRepository;
  @Autowired
  private Job bulkEditProcessInstanceIdentifiersJob;

  @ParameterizedTest
  @EnumSource(value = IdentifierType.class, names = {"ID", "HRID"}, mode = EnumSource.Mode.INCLUDE)
  @SneakyThrows
  void shouldRetrieveInstances(IdentifierType identifierType) {
    var bulkOperationId = UUID.randomUUID();
    var linkToTriggeringCsvFile = "path/to/identifiers.csv";
    var identifier = "111";
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
//    doThrow(new ReferenceDataNotFoundException("Not found"))
//      .when(instanceReferenceService).getInstanceFormatNameById("abc");
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

//    when(instanceClient.getInstanceByQuery("id==111", 1))
//      .thenReturn(InstanceCollection.builder().instances(Collections.singletonList(generateInstances(1))).totalRecords(1).build());
//    when(instanceClient.getInstanceByQuery("id==222", 1))
//      .thenReturn(InstanceCollection.builder().instances(Collections.singletonList(generateInstances(2))).totalRecords(1).build());
//    when(instanceClient.getInstanceByQuery("id==333", 1))
//      .thenReturn(InstanceCollection.builder().instances(Collections.singletonList(generateInstances(3))).totalRecords(1).build());
//    when(instanceClient.getInstanceByQuery("id==444", 1))
//      .thenReturn(InstanceCollection.builder().instances(Collections.singletonList(generateInstances(4))).totalRecords(1).build());
//    when(instanceClient.getInstanceByQuery("id==555", 1))
//      .thenReturn(InstanceCollection.builder().instances(Collections.singletonList(generateInstances(5))).totalRecords(1).build());

    var inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);
    var fileNameCaptor = ArgumentCaptor.forClass(String.class);

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessInstanceIdentifiersJob);
      var jobParameters = JobCommandHelper.prepareJobParameters(bulkOperation, 1);

      JobExecution jobExecution = testLauncher.launchJob(jobParameters);

      verify(remoteFileSystemClient, times(2)).put(inputStreamCaptor.capture(), fileNameCaptor.capture());

      assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }
  }

  private JobLauncherTestUtils createTestLauncher(Job job) {
    JobLauncherTestUtils testLauncher = new JobLauncherTestUtils();
    testLauncher.setJob(job);
    testLauncher.setJobLauncher(jobLauncher);
    testLauncher.setJobRepository(jobRepository);
    return testLauncher;
  }

  private Instance generateInstances(int number) {
    return Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .title("Sample title " + number)
      .build();
  }
}
