package org.folio.bulkops.batch.jobs.processidentifiers;

import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_IDENTIFIERS_FILE_NAME;

import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.LineMapper;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.infrastructure.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class IdentifiersConfig {

  @Bean
  @StepScope
  public FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader(
      @Value("#{jobParameters['" + TEMP_IDENTIFIERS_FILE_NAME + "']}") String tempIdentifiersFilePath,
      @Value("#{stepExecutionContext['offset']}") Long offset,
      @Value("#{stepExecutionContext['limit']}") Long limit) {
    var builder =
        new FlatFileItemReaderBuilder<ItemIdentifier>()
            .name("csvItemIdentifierReader")
            .resource(new FileSystemResource(tempIdentifiersFilePath))
            .linesToSkip(Math.toIntExact(offset))
            .lineMapper(lineMapper());
    if (limit != null && limit > 0) {
      builder.maxItemCount(Math.toIntExact(limit));
    }
    return builder.build();
  }

  @Bean
  public LineMapper<ItemIdentifier> lineMapper() {
    var lineMapper = new DefaultLineMapper<ItemIdentifier>();
    var tokenizer = new DelimitedLineTokenizer();
    tokenizer.setNames("itemId");
    var fieldSetMapper = new BeanWrapperFieldSetMapper<ItemIdentifier>();
    fieldSetMapper.setTargetType(ItemIdentifier.class);
    lineMapper.setLineTokenizer(tokenizer);
    lineMapper.setFieldSetMapper(fieldSetMapper);
    return lineMapper;
  }
}
