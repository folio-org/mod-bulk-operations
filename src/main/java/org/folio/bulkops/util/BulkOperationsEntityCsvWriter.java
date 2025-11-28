package org.folio.bulkops.util;

import static com.opencsv.ICSVWriter.DEFAULT_SEPARATOR;
import static org.folio.bulkops.processor.folio.ItemCirculationNoteProcessor.splitCirculationNotes;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.io.Writer;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.exception.ConverterException;

public class BulkOperationsEntityCsvWriter {

  private final StatefulBeanToCsv<BulkOperationsEntity> delegate;

  public BulkOperationsEntityCsvWriter(Writer writer, Class<? extends BulkOperationsEntity> clazz) {
    CustomMappingStrategy<BulkOperationsEntity> strategy = new CustomMappingStrategy<>();
    strategy.setType(clazz);
    delegate =
        new StatefulBeanToCsvBuilder<BulkOperationsEntity>(writer)
            .withSeparator(DEFAULT_SEPARATOR)
            .withApplyQuotesToAll(false)
            .withMappingStrategy(strategy)
            .build();
  }

  public void write(BulkOperationsEntity entity)
      throws ConverterException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
    delegate.write(processNotes(entity));
  }

  private BulkOperationsEntity processNotes(BulkOperationsEntity entity) {
    if (entity instanceof Item item) {
      return splitCirculationNotes(item);
    }
    return entity;
  }
}
