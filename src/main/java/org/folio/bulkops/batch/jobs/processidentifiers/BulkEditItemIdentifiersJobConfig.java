package org.folio.bulkops.batch.jobs.processidentifiers;

import static org.folio.bulkops.domain.bean.JobParameterNames.BULK_OPERATION_ID;
import static org.folio.bulkops.domain.bean.JobParameterNames.IDENTIFIER_TYPE;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_OUTPUT_CSV_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_OUTPUT_JSON_PATH;
import static org.folio.bulkops.domain.dto.EntityType.ITEM;
import static org.folio.bulkops.util.Constants.BULK_EDIT_IDENTIFIERS;
import static org.folio.bulkops.util.Constants.HYPHEN;
import static org.folio.bulkops.util.Constants.TOTAL_CSV_LINES;

import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.folio.bulkops.batch.BulkEditItemSkipListener;
import org.folio.bulkops.batch.CsvListItemWriter;
import org.folio.bulkops.batch.JobCompletionNotificationListener;
import org.folio.bulkops.batch.JsonListFileWriter;
import org.folio.bulkops.batch.jobs.BulkEditItemListProcessor;
import org.folio.bulkops.batch.jobs.BulkEditItemProcessor;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.exception.BulkEditException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.support.CompositeItemProcessor;
import org.springframework.batch.infrastructure.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
@RequiredArgsConstructor
public class BulkEditItemIdentifiersJobConfig {
  private final BulkEditItemListProcessor bulkEditItemListProcessor;
  private final BulkEditItemProcessor bulkEditItemProcessor;
  private final BulkEditItemSkipListener bulkEditItemSkipListener;

  @Value("${application.batch.chunk-size}")
  private int chunkSize;

  @Value("${application.batch.num-partitions}")
  private int numPartitions;

  @Value("${application.batch.max-retries-on-connection-reset}")
  private int maxRetriesOnConnectionReset;

  @Bean
  public Job bulkEditProcessItemIdentifiersJob(
      JobCompletionNotificationListener listener,
      Step itemPartitionStep,
      JobRepository jobRepository) {
    return new JobBuilder(BULK_EDIT_IDENTIFIERS + HYPHEN + ITEM, jobRepository)
        .listener(listener)
        .flow(itemPartitionStep)
        .end()
        .build();
  }

  @Bean
  public Step itemPartitionStep(
      JobRepository jobRepository,
      @Qualifier("bulkEditItemStep") Step bulkEditItemStep,
      Partitioner bulkEditItemPartitioner,
      BulkEditFileAssembler bulkEditFileAssembler) {

    var partitionHandler =
        new PerJobPartitionHandler(bulkEditItemStep, numPartitions);

    return new StepBuilder("itemPartitionStep", jobRepository)
        .partitioner("bulkEditItemStep", bulkEditItemPartitioner)
        .gridSize(numPartitions)
        .partitionHandler(partitionHandler)
        .aggregator(bulkEditFileAssembler)
        .build();
  }

  @Bean
  @StepScope
  public Partitioner bulkEditItemPartitioner(
      @Value("#{jobParameters['" + TEMP_LOCAL_FILE_PATH + "']}") String outputCsvJsonFilePath,
      @Value("#{jobParameters['" + TOTAL_CSV_LINES + "']}") long numOfLines) {
    return new BulkEditPartitioner(outputCsvJsonFilePath, outputCsvJsonFilePath, null, numOfLines);
  }

  @Bean
  public Step bulkEditItemStep(
      FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
      CompositeItemWriter<List<ExtendedItem>> compositeItemListWriter,
      ListIdentifiersWriteListener<ExtendedItem> listIdentifiersWriteListener,
      JobRepository jobRepository) {

    return new StepBuilder("bulkEditItemStep", jobRepository)
        .<ItemIdentifier, List<ExtendedItem>>chunk(chunkSize)
        .reader(csvItemIdentifierReader)
        .processor(identifierItemProcessor())
        .faultTolerant()
        .retry(SocketException.class)
        .retryLimit(maxRetriesOnConnectionReset)
        .skip(BulkEditException.class)
        .skipLimit(1_000_000)
        .skipListener(bulkEditItemSkipListener)
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
    writer.setDelegates(
        Arrays.asList(
            new CsvListItemWriter<>(csvPath, ExtendedItem.class, bulkOperationId, identifierType),
            new JsonListFileWriter<>(new FileSystemResource(jsonPath))));
    return writer;
  }
}
