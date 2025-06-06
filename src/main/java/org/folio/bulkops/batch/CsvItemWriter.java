package org.folio.bulkops.batch;

import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.exception.ConverterException;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.util.BulkOperationsEntityCsvWriter;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

@Log4j2
public class CsvItemWriter<T extends BulkOperationsEntity> implements ItemWriter<T>, ItemStream {
  private BufferedWriter writer;
  private BulkOperationsEntityCsvWriter delegate;
  private ErrorService errorService;
  private UUID bulkOperationId;
  private IdentifierType identifierType;

  public CsvItemWriter(String path, Class<T> clazz, ErrorService errorService, String bulkOperationId, String identifierType) throws IOException {
    this.writer = new BufferedWriter(new FileWriter(path));
    if (clazz == ExtendedItem.class) {
      delegate = new BulkOperationsEntityCsvWriter(writer, Item.class);
    } else if (clazz == ExtendedHoldingsRecord.class) {
      delegate = new BulkOperationsEntityCsvWriter(writer, HoldingsRecord.class);
    } else if (clazz == ExtendedInstance.class) {
      delegate = new BulkOperationsEntityCsvWriter(writer, Instance.class);
    } else if (clazz == User.class){
      delegate = new BulkOperationsEntityCsvWriter(writer, clazz);
    } else {
      throw new IllegalArgumentException("Class " + clazz.getName() + " is not supported for writing");
    }
    this.errorService = errorService;
    this.bulkOperationId = UUID.fromString(bulkOperationId);
    this.identifierType = IdentifierType.fromValue(identifierType);
  }

  @Override
  public void write(Chunk<? extends T> chunk) throws Exception {
    for (T entity: chunk) {
      try {
        delegate.write(entity.getRecordBulkOperationEntity());
      } catch (ConverterException converterException) {
        saveError(entity, converterException);
        delegate.write(entity.getRecordBulkOperationEntity());
      }
    }
  }

  @Override
  public void close() {
    try {
      writer.close();
    } catch (IOException e) {
      log.error(e);
    }
  }

  private void saveError(BulkOperationsEntity entity, ConverterException converterException) {
    errorService.saveError(bulkOperationId, entity.getIdentifier(identifierType), converterException.getMessage(),
      ErrorType.WARNING);
  }
}
