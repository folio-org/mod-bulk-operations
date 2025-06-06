package org.folio.bulkops.batch;

import static java.util.Objects.nonNull;
import static org.folio.bulkops.util.Constants.MARC;
import static org.folio.bulkops.util.Constants.NO_MARC_CONTENT;

import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.converter.JsonToMarcConverter;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.exception.BulkEditException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.util.Assert;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@StepScope
public class MarcAsListStringsWriter<T extends BulkOperationsEntity> extends FlatFileItemWriter<List<T>> {

  private SrsClient srsClient;
  private MarcAsStringWriter<String> delegateToStringWriter;
  private JsonToMarcConverter jsonToMarcConverter;

  public MarcAsListStringsWriter(String outputFileName, SrsClient srsClient, JsonToMarcConverter jsonToMarcConverter) {
    super();
    this.srsClient = srsClient;
    this.jsonToMarcConverter = jsonToMarcConverter;
    delegateToStringWriter = new MarcAsStringWriter<>(outputFileName);
  }

  @Override
  public void write(Chunk<? extends List<T>> entities) throws Exception {
    delegateToStringWriter.write(new Chunk<>(entities.getItems().stream().flatMap(List::stream)
      .map(this::getIdIfExtendedInstanceMarc)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .map(id -> {
        try {
          return getMarcContent(id);
        } catch (Exception e) {
          log.error(e);
          throw new BulkEditException(NO_MARC_CONTENT.formatted(id, e.getMessage()), ErrorType.ERROR);
        }
      })
      .flatMap(List::stream)
      .filter(Objects::nonNull)
      .toList()));
  }

  private Optional<String> getIdIfExtendedInstanceMarc(T entity) {
    return entity instanceof ExtendedInstance ei && MARC.equals(ei.getEntity().getSource()) ?
      Optional.of(ei.getEntity().getId()) :
      Optional.empty();
  }

  @Override
  public void afterPropertiesSet() {
    Assert.notNull(delegateToStringWriter, "Delegate was not set");
  }

  @Override
  public void open(ExecutionContext executionContext) {
    if (nonNull(delegateToStringWriter)) {
      (delegateToStringWriter).open(executionContext);
    }
  }

  @Override
  public void update(ExecutionContext executionContext) {
    if (nonNull(delegateToStringWriter)) {
      (delegateToStringWriter).update(executionContext);
    }
  }

  @Override
  public void close() {
    if (nonNull(delegateToStringWriter)) {
      (delegateToStringWriter).close();
    }
  }

  private List<String> getMarcContent(String id) throws Exception {
    List<String> mrcRecords = new ArrayList<>();
    var srsRecords = srsClient.getMarc(id, "INSTANCE", true).get("sourceRecords");
    if (srsRecords.isEmpty()) {
      log.warn("No SRS records found by instanceId = {}", id);
      return mrcRecords;
    }
    for (var jsonNodeIterator = srsRecords.elements(); jsonNodeIterator.hasNext();) {
      var srsRec = jsonNodeIterator.next();
      var parsedRec = srsRec.get("parsedRecord");
      var content = parsedRec.get("content").toString();
      mrcRecords.add(jsonToMarcConverter.convertJsonRecordToMarcRecord(content));
    }
    return mrcRecords;
  }
}
