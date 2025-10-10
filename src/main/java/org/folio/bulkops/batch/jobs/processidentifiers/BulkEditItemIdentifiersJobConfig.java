package org.folio.bulkops.batch.jobs.processidentifiers;

import static org.folio.bulkops.domain.bean.JobParameterNames.BULK_OPERATION_ID;
import static org.folio.bulkops.domain.bean.JobParameterNames.IDENTIFIER_TYPE;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_OUTPUT_CSV_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_OUTPUT_JSON_PATH;
import static org.folio.bulkops.domain.dto.EntityType.ITEM;
import static org.folio.bulkops.util.Constants.BULK_EDIT_IDENTIFIERS;
import static org.folio.bulkops.util.Constants.HYPHEN;
import static org.folio.bulkops.util.Constants.IDENTIFIERS_FILE_NAME;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.folio.bulkops.batch.BulkEditSkipListener;
import org.folio.bulkops.batch.CsvListItemWriter;
import org.folio.bulkops.batch.JobCompletionNotificationListener;
import org.folio.bulkops.batch.JsonListFileWriter;
import org.folio.bulkops.batch.jobs.BulkEditItemListProcessor;
import org.folio.bulkops.batch.jobs.BulkEditItemProcessor;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.ItemIdentifier;
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
import org.springframework.batch.item.support.CompositeItemProcessor;
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
public class BulkEditItemIdentifiersJobConfig {
  private final BulkEditItemListProcessor bulkEditItemListProcessor;
  private final BulkEditItemProcessor bulkEditItemProcessor;
  private final BulkEditSkipListener bulkEditSkipListener;
  private final RemoteFileSystemClient remoteFileSystemClient;

  @Value("${application.batch.chunk-size}")
  private int chunkSize;

  @Value("${application.batch.num-partitions}")
  private int numPartitions;

  @Bean
  public Job bulkEditProcessItemIdentifiersJob(
      JobCompletionNotificationListener listener,
      Step itemPartitionStep,
      JobRepository jobRepository) {
    return new JobBuilder(
            BULK_EDIT_IDENTIFIERS + HYPHEN + ITEM,
            jobRepository)
        .incrementer(new RunIdIncrementer())
        .listener(listener)
        .flow(itemPartitionStep)
        .end()
        .build();
  }

  @Bean
  public Step itemPartitionStep(
      FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
      CompositeItemWriter<List<ExtendedItem>> compositeItemListWriter,
      ListIdentifiersWriteListener<ExtendedItem> listIdentifiersWriteListener,
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      @Qualifier("asyncTaskExecutorBulkEdit") TaskExecutor taskExecutor,
      Partitioner bulkEditItemPartitioner,
      BulkEditFileAssembler bulkEditFileAssembler) {

    return new StepBuilder("itemPartitionStep", jobRepository)
        .partitioner("bulkEditItemStep", bulkEditItemPartitioner)
        .gridSize(numPartitions)
        .step(
            bulkEditItemStep(
                csvItemIdentifierReader,
                compositeItemListWriter,
                listIdentifiersWriteListener,
                jobRepository,
                transactionManager))
        .taskExecutor(taskExecutor)
        .aggregator(bulkEditFileAssembler)
        .build();
  }

  @Bean
  @StepScope
  public Partitioner bulkEditItemPartitioner(
      @Value("#{jobParameters['" + TEMP_LOCAL_FILE_PATH + "']}") String outputCsvJsonFilePath,
      @Value("#{jobParameters['" + IDENTIFIERS_FILE_NAME + "']}") String uploadedFileName) {
    var numOfLines = remoteFileSystemClient.getNumOfLines(uploadedFileName);
    return new BulkEditPartitioner(
        outputCsvJsonFilePath,
        outputCsvJsonFilePath,
        null,
        numOfLines);
  }

  @Bean
  public Step bulkEditItemStep(
      FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
      CompositeItemWriter<List<ExtendedItem>> compositeItemListWriter,
      ListIdentifiersWriteListener<ExtendedItem> listIdentifiersWriteListener,
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {

    return new StepBuilder("bulkEditItemStep", jobRepository)
        .<ItemIdentifier, List<ExtendedItem>>chunk(
            chunkSize,
            transactionManager)
        .reader(csvItemIdentifierReader)
        .processor(identifierItemProcessor())
        .faultTolerant()
        .skipLimit(1_000_000)
        // Required to avoid repeating BulkEditItemProcessor#process after skip.
        .processorNonTransactional()
        .skip(BulkEditException.class)
        .listener(bulkEditSkipListener)
        .writer(compositeItemListWriter)
        .listener(listIdentifiersWriteListener)
        .build();
  }

  @Bean
  public CompositeItemProcessor<ItemIdentifier, List<ExtendedItem>> identifierItemProcessor() {
    var processor = new CompositeItemProcessor<ItemIdentifier, List<ExtendedItem>>();
    processor.setDelegates(Arrays.asList(bulkEditItemProcessor, bulkEditItemListProcessor));
    return processor;
  }

  @Bean
  @StepScope
  @SneakyThrows
  public CompositeItemWriter<List<ExtendedItem>> compositeItemListWriter(
          @Value("#{stepExecutionContext['" + TEMP_OUTPUT_CSV_PATH + "']}") String csvPath,
          @Value("#{stepExecutionContext['" + TEMP_OUTPUT_JSON_PATH + "']}") String jsonPath,
          @Value("#{jobParameters['" + BULK_OPERATION_ID + "']}") String bulkOperationId,
          @Value("#{jobParameters['" + IDENTIFIER_TYPE + "']}") String identifierType) {
    var writer = new CompositeItemWriter<List<ExtendedItem>>();
    writer.setDelegates(Arrays.asList(
            new CsvListItemWriter<>(csvPath, ExtendedItem.class, bulkOperationId, identifierType),
            new JsonListFileWriter<>(new FileSystemResource(jsonPath))));
    return writer;
  }
}
