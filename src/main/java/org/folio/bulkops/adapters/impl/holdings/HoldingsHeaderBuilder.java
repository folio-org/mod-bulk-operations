package org.folio.bulkops.adapters.impl.holdings;

import static org.folio.bulkops.domain.dto.DataType.STRING;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.DataType;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum HoldingsHeaderBuilder {
  ID("Holdings record id", STRING, false),
  VERSION("Version", STRING, false),
  HRID("Hrid", STRING, false),
  TYPE("Holdings type", STRING, false),
  FORMER_IDS("Former ids", STRING, false),
  INSTANCE("Instance", STRING, false),
  PERMANENT_LOCATION("Permanent location", STRING, false),
  TEMPORARY_LOCATION("Temporary location", STRING, false),
  EFFECTIVE_LOCATION("Effective location", STRING, false),
  ELECTRONIC_ACCESS("Electronic access", STRING, false),
  CALL_NUMBER_TYPE("Call number type", STRING, false),
  CALL_NUMBER_PREFIX("Call number prefix", STRING, false),
  CALL_NUMBER("Call number", STRING, false),
  CALL_NUMBER_SUFFIX("Call number suffix", STRING, false),
  SHELVING_TITLE("Shelving title", STRING, false),
  ACQUISITION_FORMAT("Acquisition format", STRING, false),
  ACQUISITION_METHOD("Acquisition method", STRING, false),
  RECEIPT_STATUS("Receipt status", STRING, false),
  NOTES("Notes", STRING, false),
  ADMINISTRATIVE_NOTES("Administrative notes", STRING, false),
  ILL_POLICY("Ill policy", STRING, false),
  RETENTION_POLICY("Retention policy", STRING, false),
  DIGITIZATION_POLICY("Digitization policy", STRING, false),
  STATEMENTS("Holdings statements", STRING, false),
  STATEMENTS_FOR_INDEXES("Holdings statements for indexes", STRING, false),
  STATEMENTS_FOR_SUPPLEMENTS("Holdings statements for supplements", STRING, false),
  COPY_NUMBER("Copy number", STRING, false),
  NUMBER_OF_ITEMS("Number of items", STRING, false),
  RECEIVING_HISTORY("Receiving history", STRING, false),
  DISCOVERY_SUPPRESS("Discovery suppress", STRING, false),
  STATISTICAL_CODES("Statistical codes", STRING, false),
  TAGS("Tags", STRING, false),
  SOURCE("Source", STRING, false);

  private String value;
  private DataType dataType;
  private boolean visible;

  public static List<Cell> getHeaders() {
    return Arrays.stream(values())
      .map(v -> new Cell().value(v.value).dataType(v.dataType).visible(v.visible))
      .collect(Collectors.toList());
  }
}
