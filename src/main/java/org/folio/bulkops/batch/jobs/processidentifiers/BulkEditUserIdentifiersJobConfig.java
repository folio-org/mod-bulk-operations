package org.folio.bulkops.batch.jobs.processidentifiers;

import static org.folio.bulkops.domain.bean.JobParameterNames.BULK_OPERATION_ID;
import static org.folio.bulkops.domain.bean.JobParameterNames.IDENTIFIER_TYPE;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_OUTPUT_CSV_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_OUTPUT_JSON_PATH;
import static org.folio.bulkops.util.Constants.BULK_EDIT_IDENTIFIERS;
import static org.folio.bulkops.util.Constants.HYPHEN;
import static org.folio.bulkops.util.Constants.IDENTIFIERS_FILE_NAME;

import java.net.SocketException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.folio.bulkops.batch.BulkEditSkipListener;
import org.folio.bulkops.batch.CsvItemWriter;
import org.folio.bulkops.batch.JobCompletionNotificationListener;
import org.folio.bulkops.batch.JsonFileWriter;
import org.folio.bulkops.batch.jobs.BulkEditUserProcessor;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.exception.BulkEditException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BulkEditUserIdentifiersJobConfig {
  private final BulkEditUserProcessor bulkEditUserProcessor;
  private final BulkEditSkipListener bulkEditSkipListener;
  private final RemoteFileSystemClient remoteFileSystemClient;

  @Value("${application.batch.chunk-size}")
  private int chunkSize;

  @Value("${application.batch.num-partitions}")
  private int numPartitions;

  @Bean
  public Job bulkEditProcessUserIdentifiersJob(
      JobCompletionNotificationListener listener,
      Step userPartitionStep,
      JobRepository jobRepository) {
    return new JobBuilder(BULK_EDIT_IDENTIFIERS + HYPHEN + EntityType.USER, jobRepository)
        .incrementer(new RunIdIncrementer())
        .listener(listener)
        .flow(userPartitionStep)
        .end()
        .build();
  }

  @Bean
  public Step userPartitionStep(
      FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
      CompositeItemWriter<User> compositeUserListWriter,
      IdentifiersWriteListener<User> identifiersWriteListener,
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      @Qualifier("asyncTaskExecutorBulkEdit") TaskExecutor taskExecutor,
      Partitioner bulkEditUserPartitioner,
      BulkEditFileAssembler bulkEditFileAssembler) {
    return new StepBuilder("userPartitionStep", jobRepository)
        .partitioner("bulkEditUserStep", bulkEditUserPartitioner)
        .gridSize(numPartitions)
        .step(
            bulkEditUserStep(
                csvItemIdentifierReader,
                compositeUserListWriter,
                identifiersWriteListener,
                jobRepository,
                transactionManager))
        .taskExecutor(taskExecutor)
        .aggregator(bulkEditFileAssembler)
        .build();
  }

  @Bean
  @StepScope
  public Partitioner bulkEditUserPartitioner(
      @Value("#{jobParameters['" + TEMP_LOCAL_FILE_PATH + "']}") String outputCsvJsonFilePath,
      @Value("#{jobParameters['" + IDENTIFIERS_FILE_NAME + "']}") String uploadedFileName) {
    return new BulkEditPartitioner(
        outputCsvJsonFilePath,
        outputCsvJsonFilePath,
        null,
        remoteFileSystemClient.getNumOfLines(uploadedFileName));
  }

  @Bean
  public Step bulkEditUserStep(
      FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
      CompositeItemWriter<User> compositeItemWriter,
      IdentifiersWriteListener<User> identifiersWriteListener,
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {
    return new StepBuilder("bulkEditUserStep", jobRepository)
        .<ItemIdentifier, User>chunk(chunkSize, transactionManager)
        .reader(csvItemIdentifierReader)
        .processor(bulkEditUserProcessor)
        .faultTolerant()
        .retry(SocketException.class)
        .retryLimit(3)
        .skip(BulkEditException.class)
        .skipLimit(1_000_000)
        // Required to avoid repeating BulkEditItemProcessor#process after skip.
        .processorNonTransactional()
        .skip(BulkEditException.class)
        .listener(bulkEditSkipListener)
        .writer(compositeItemWriter)
        .listener(identifiersWriteListener)
        .build();
  }

  @Bean
  @StepScope
  @SneakyThrows
  public CompositeItemWriter<User> compositeItemWriter(
      @Value("#{stepExecutionContext['" + TEMP_OUTPUT_CSV_PATH + "']}") String csvPath,
      @Value("#{stepExecutionContext['" + TEMP_OUTPUT_JSON_PATH + "']}") String jsonPath,
      @Value("#{jobParameters['" + BULK_OPERATION_ID + "']}") String bulkOperationId,
      @Value("#{jobParameters['" + IDENTIFIER_TYPE + "']}") String identifierType) {
    var writer = new CompositeItemWriter<User>();
    writer.setDelegates(
        Arrays.asList(
            new CsvItemWriter<>(csvPath, User.class, bulkOperationId, identifierType),
            new JsonFileWriter<>(new FileSystemResource(jsonPath))));
    return writer;
  }
}
