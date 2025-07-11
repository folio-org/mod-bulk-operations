package org.folio.bulkops.batch;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.core.io.FileSystemResource;

@Log4j2
public class MarcAsStringWriter<T> extends FlatFileItemWriter<T> {

  public MarcAsStringWriter(String outputFileName) {
    super();
    setResource(new FileSystemResource(outputFileName));
    setLineSeparator(EMPTY);
    setLineAggregator(new PassThroughLineAggregator<>());
    setShouldDeleteIfEmpty(true);
    setName("marcItemWriter");
  }
}
