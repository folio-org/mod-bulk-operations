package org.folio.bulkops.batch.jobs.processidentifiers;

import static org.folio.bulkops.util.Constants.IDENTIFIERS_FILE_NAME;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.exception.BulkEditException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;

@Configuration
@RequiredArgsConstructor
public class IdentifiersConfig {
  private final RemoteFileSystemClient remoteFileSystemClient;

  @Bean
  @StepScope
  public FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader(
      @Value("#{jobParameters['" + IDENTIFIERS_FILE_NAME + "']}") String uploadedFileName,
      @Value("#{stepExecutionContext['offset']}") Long offset,
      @Value("#{stepExecutionContext['limit']}") Long limit)
      throws BulkEditException {
    try (var is = remoteFileSystemClient.get(uploadedFileName)) {
      var builder =
          new FlatFileItemReaderBuilder<ItemIdentifier>()
              .name("csvItemIdentifierReader")
              .resource(new ByteArrayResource(is.readAllBytes()))
              .linesToSkip(Math.toIntExact(offset))
              .lineMapper(lineMapper());
      if (limit != null && limit > 0) {
        builder.maxItemCount(Math.toIntExact(limit));
      }
      return builder.build();
    } catch (IOException e) {
      throw new BulkEditException("Failed to read file " + IDENTIFIERS_FILE_NAME);
    }
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
