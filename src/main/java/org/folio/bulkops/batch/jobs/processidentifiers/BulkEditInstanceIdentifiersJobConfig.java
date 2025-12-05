package org.folio.bulkops.batch.jobs.processidentifiers;

import static org.folio.bulkops.domain.bean.JobParameterNames.BULK_OPERATION_ID;
import static org.folio.bulkops.domain.bean.JobParameterNames.IDENTIFIER_TYPE;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_LOCAL_MARC_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_OUTPUT_CSV_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_OUTPUT_JSON_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_OUTPUT_MARC_PATH;
import static org.folio.bulkops.util.Constants.BULK_EDIT_IDENTIFIERS;
import static org.folio.bulkops.util.Constants.HYPHEN;
import static org.folio.bulkops.util.Constants.IDENTIFIERS_FILE_NAME;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.batch.BulkEditSkipListener;
import org.folio.bulkops.batch.CsvListItemWriter;
import org.folio.bulkops.batch.JobCompletionNotificationListener;
import org.folio.bulkops.batch.JsonListFileWriter;
import org.folio.bulkops.batch.MarcAsListStringsWriter;
import org.folio.bulkops.batch.jobs.BulkEditInstanceProcessor;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.domain.converter.JsonToMarcConverter;
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
@Log4j2
@RequiredArgsConstructor
public class BulkEditInstanceIdentifiersJobConfig {
  private final BulkEditInstanceProcessor bulkEditInstanceProcessor;
  private final BulkEditSkipListener bulkEditSkipListener;
  private final SrsClient srsClient;
  private final JsonToMarcConverter jsonToMarcConverter;
  private final RemoteFileSystemClient remoteFileSystemClient;

  @Value("${application.batch.chunk-size}")
  private int chunkSize;

  @Value("${application.batch.num-partitions}")
  private int numPartitions;

  @Value("${application.batch.max-retries-on-connection-reset}")
  private int maxRetriesOnConnectionReset;

  @Bean
  public Job bulkEditProcessInstanceIdentifiersJob(
      JobCompletionNotificationListener listener,
      Step instancePartitionStep,
      JobRepository jobRepository) {
    return new JobBuilder(BULK_EDIT_IDENTIFIERS + HYPHEN + EntityType.INSTANCE, jobRepository)
        .incrementer(new RunIdIncrementer())
        .listener(listener)
        .flow(instancePartitionStep)
        .end()
        .build();
  }

  @Bean
  public Step instancePartitionStep(
      FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
      CompositeItemWriter<List<ExtendedInstance>> compositeInstanceListWriter,
      ListIdentifiersWriteListener<ExtendedInstance> listIdentifiersWriteListener,
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      @Qualifier("asyncTaskExecutorBulkEdit") TaskExecutor taskExecutor,
      Partitioner bulkEditInstancePartitioner,
      BulkEditFileAssembler bulkEditFileAssembler) {

    return new StepBuilder("instancePartitionStep", jobRepository)
        .partitioner("bulkEditInstanceStep", bulkEditInstancePartitioner)
        .gridSize(numPartitions)
        .step(
            bulkEditInstanceStep(
                csvItemIdentifierReader,
                compositeInstanceListWriter,
                listIdentifiersWriteListener,
                jobRepository,
                transactionManager))
        .taskExecutor(taskExecutor)
        .aggregator(bulkEditFileAssembler)
        .build();
  }

  @Bean
  @StepScope
  public Partitioner bulkEditInstancePartitioner(
      @Value("#{jobParameters['" + TEMP_LOCAL_FILE_PATH + "']}") String outputCsvJsonFilePath,
      @Value("#{jobParameters['" + TEMP_LOCAL_MARC_PATH + "']}") String outputMarcName,
      @Value("#{jobParameters['" + IDENTIFIERS_FILE_NAME + "']}") String uploadedFileName) {

    var numOfLines = remoteFileSystemClient.getNumOfLines(uploadedFileName);
    return new BulkEditPartitioner(
        outputCsvJsonFilePath, outputCsvJsonFilePath, outputMarcName, numOfLines);
  }

  @Bean
  public Step bulkEditInstanceStep(
      FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
      CompositeItemWriter<List<ExtendedInstance>> compositeInstanceListWriter,
      ListIdentifiersWriteListener<ExtendedInstance> listIdentifiersWriteListener,
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {

    return new StepBuilder("bulkEditInstanceStep", jobRepository)
        .<ItemIdentifier, List<ExtendedInstance>>chunk(chunkSize, transactionManager)
        .reader(csvItemIdentifierReader)
        .processor(bulkEditInstanceProcessor)
        .faultTolerant()
        .retry(SocketException.class)
        .retryLimit(maxRetriesOnConnectionReset)
        .skip(BulkEditException.class)
        .skipLimit(1_000_000)
        // Required to avoid repeating BulkEditItemProcessor#process after skip.
        .processorNonTransactional()
        .skip(BulkEditException.class)
        .listener(bulkEditSkipListener)
        .writer(compositeInstanceListWriter)
        .listener(listIdentifiersWriteListener)
        .build();
  }

  @Bean
  @StepScope
  @SneakyThrows
  public CompositeItemWriter<List<ExtendedInstance>> compositeInstanceListWriter(
      @Value("#{stepExecutionContext['" + TEMP_OUTPUT_CSV_PATH + "']}") String csvPath,
      @Value("#{stepExecutionContext['" + TEMP_OUTPUT_JSON_PATH + "']}") String jsonPath,
      @Value("#{stepExecutionContext['" + TEMP_OUTPUT_MARC_PATH + "']}") String marcPath,
      @Value("#{jobParameters['" + BULK_OPERATION_ID + "']}") String bulkOperationId,
      @Value("#{jobParameters['" + IDENTIFIER_TYPE + "']}") String identifierType) {

    var csvWriter =
        new CsvListItemWriter<>(csvPath, ExtendedInstance.class, bulkOperationId, identifierType);

    var jsonWriter = new JsonListFileWriter<ExtendedInstance>(new FileSystemResource(jsonPath));

    var marcWriter =
        new MarcAsListStringsWriter<ExtendedInstance>(marcPath, srsClient, jsonToMarcConverter);

    List<org.springframework.batch.item.ItemWriter<? super List<ExtendedInstance>>> delegates =
        new ArrayList<>();
    delegates.add(csvWriter);
    delegates.add(jsonWriter);
    delegates.add(marcWriter);

    var writer = new CompositeItemWriter<List<ExtendedInstance>>();
    writer.setDelegates(delegates);
    return writer;
  }
}
