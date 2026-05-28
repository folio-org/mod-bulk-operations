package org.folio.bulkops.batch.jobs.processidentifiers;

import static org.folio.bulkops.domain.bean.JobParameterNames.BULK_OPERATION_ID;
import static org.folio.bulkops.domain.bean.JobParameterNames.IDENTIFIER_TYPE;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_OUTPUT_CSV_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_OUTPUT_JSON_PATH;
import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.util.Constants.BULK_EDIT_IDENTIFIERS;
import static org.folio.bulkops.util.Constants.HYPHEN;
import static org.folio.bulkops.util.Constants.TOTAL_CSV_LINES;

import java.net.SocketException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.folio.bulkops.batch.BulkEditHoldingsSkipListener;
import org.folio.bulkops.batch.CsvListItemWriter;
import org.folio.bulkops.batch.JobCompletionNotificationListener;
import org.folio.bulkops.batch.JsonListFileWriter;
import org.folio.bulkops.batch.jobs.BulkEditHoldingsProcessor;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.exception.BulkEditException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
@RequiredArgsConstructor
public class BulkEditHoldingsIdentifiersJobConfig {
  private final BulkEditHoldingsProcessor bulkEditHoldingsProcessor;
  private final BulkEditHoldingsSkipListener bulkEditHoldingsSkipListener;

  @Value("${application.batch.chunk-size}")
  private int chunkSize;

  @Value("${application.batch.num-partitions}")
  private int numPartitions;

  @Value("${application.batch.max-retries-on-connection-reset}")
  private int maxRetriesOnConnectionReset;

  @Bean
  public Job bulkEditProcessHoldingsIdentifiersJob(
      JobCompletionNotificationListener listener,
      JobRepository jobRepository,
      Step holdingsPartitionStep) {
    return new JobBuilder(BULK_EDIT_IDENTIFIERS + HYPHEN + HOLDINGS_RECORD, jobRepository)
        .listener(listener)
        .flow(holdingsPartitionStep)
        .end()
        .build();
  }

  @Bean
  public Step holdingsPartitionStep(
      JobRepository jobRepository,
      @Qualifier("bulkEditHoldingsStep") Step bulkEditHoldingsStep,
      Partitioner bulkEditHoldingsPartitioner,
      BulkEditFileAssembler bulkEditFileAssembler) {

    var partitionHandler =
        new PerJobPartitionHandler(bulkEditHoldingsStep, numPartitions);

    return new StepBuilder("holdingsPartitionStep", jobRepository)
        .partitioner("bulkEditHoldingsStep", bulkEditHoldingsPartitioner)
        .gridSize(numPartitions)
        .partitionHandler(partitionHandler)
        .aggregator(bulkEditFileAssembler)
        .build();
  }

  @Bean
  @StepScope
  public Partitioner bulkEditHoldingsPartitioner(
      @Value("#{jobParameters['" + TEMP_LOCAL_FILE_PATH + "']}") String outputCsvJsonFilePath,
      @Value("#{jobParameters['" + TOTAL_CSV_LINES + "']}") long numOfLines) {
    return new BulkEditPartitioner(outputCsvJsonFilePath, outputCsvJsonFilePath, null, numOfLines);
  }

  @Bean
  public Step bulkEditHoldingsStep(
      FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
      CompositeItemWriter<List<ExtendedHoldingsRecord>> writer,
      ListIdentifiersWriteListener<ExtendedHoldingsRecord> listIdentifiersWriteListener,
      JobRepository jobRepository) {

    return new StepBuilder("bulkEditHoldingsStep", jobRepository)
        .<ItemIdentifier, List<ExtendedHoldingsRecord>>chunk(chunkSize)
        .reader(csvItemIdentifierReader)
        .processor(bulkEditHoldingsProcessor)
        .faultTolerant()
        .retry(SocketException.class)
        .retryLimit(maxRetriesOnConnectionReset)
        .skip(BulkEditException.class)
        .skipLimit(1_000_000)
        .skipListener(bulkEditHoldingsSkipListener)
        .writer(writer)
        .listener(listIdentifiersWriteListener)
        .build();
  }

  @Bean
  @StepScope
  @SneakyThrows
  public CompositeItemWriter<List<ExtendedHoldingsRecord>> compositeHoldingsListWriter(
      @Value("#{stepExecutionContext['" + TEMP_OUTPUT_CSV_PATH + "']}") String csvPath,
      @Value("#{stepExecutionContext['" + TEMP_OUTPUT_JSON_PATH + "']}") String jsonPath,
      @Value("#{jobParameters['" + BULK_OPERATION_ID + "']}") String bulkOperationId,
      @Value("#{jobParameters['" + IDENTIFIER_TYPE + "']}") String identifierType) {

    var writer = new CompositeItemWriter<List<ExtendedHoldingsRecord>>();
    var csvWriter =
        new CsvListItemWriter<>(
            csvPath, ExtendedHoldingsRecord.class, bulkOperationId, identifierType);
    var jsonWriter =
        new JsonListFileWriter<ExtendedHoldingsRecord>(new FileSystemResource(jsonPath));

    List<ItemWriter<? super List<ExtendedHoldingsRecord>>> delegates = new java.util.ArrayList<>();
    delegates.add(csvWriter);
    delegates.add(jsonWriter);
    writer.setDelegates(delegates);
    return writer;
  }
}
