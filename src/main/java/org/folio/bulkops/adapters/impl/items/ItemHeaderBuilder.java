package org.folio.bulkops.adapters.impl.items;

import static org.folio.bulkops.domain.dto.DataType.STRING;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.DataType;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ItemHeaderBuilder {
  ID("Item id", STRING, false),
  VERSION("Version", STRING, false),
  HRID("Item HRID", STRING, false),
  HOLDING_RECORD_ID ("Holdings Record Id", STRING, false),
  FORMER_IDS ("Former Ids", STRING, false),
  DISCOVERY_SUPRESS ("Discovery Suppress", STRING, false),
  TITLE ("Title", STRING, false),
  CONTRIBUTORS_NAMES("Contributor Names", STRING, false),
  CALL_NUMBER("Call Number", STRING, false),
  BARCODE("Barcode", STRING, false),
  EFFECTIVE_SHELVING_ORDER("Effective Shelving Order", STRING, false),
  ACCESSION_NUMBER("Accession Number", STRING, false),
  LEVEL_CALL_NUMBER("Item Level Call Number", STRING, false),
  LEVEL_CALL_NUMBER_PREFIX("Item Level Call Number Prefix", STRING, false),
  LEVEL_CALL_NUMBER_SUFFIX("Item Level Call Number Suffix", STRING, false),
  LEVEL_CALL_NUMBER_TYPE("Item Level Call Number Type", STRING, false),
  EFFECTIVE_CALL_NUMBER_COMPONENTS("Effective Call Number Components", STRING, false),
  VOLUME("Volume", STRING, false),
  ENUMERATION("Enumeration", STRING, false),
  CHRONOLOGY("Chronology", STRING, false),
  YEAR_CAPTION("Year Caption", STRING, false),
  IDENTIFIER("Item Identifier", STRING, false),
  COPY_NUMBER("Copy Number", STRING, false),
  NUMBER_OF_PIECES("Number Of Pieces", STRING, false),
  DESCRIPTION_OF_PIECES("Description Of Pieces", STRING, false),
  NUMBER_OF_MISSING_PIECES("Number Of Missing Pieces", STRING, false),
  MISSING_PIECES("Missing Peaces", STRING, false),
  MISSING_PIECES_DATE("Missing Pieces Date", STRING, false),
  DAMAGED_STATUS("Item Damaged Status", STRING, false),
  DAMAGED_STATUS_DATE("Item Damaged Status Date", STRING, false),
  ADMINISTRATIVE_NOTES("Administrative Notes", STRING, false),
  NOTES("Notes", STRING, false),
  CIRCULATION_NOTES("Circulation Notes", STRING, false),
  STATUS("Status", STRING, false),
  MATERIAL_TYPE("Material Type", STRING, false),
  IS_BOUND_WITH("Is Bound With", STRING, false),
  BOUND_WITH_TITLES("Bound With Titles", STRING, false),
  PERMANENT_LOAN_TYPE("Permanent Loan Type", STRING, false),
  TEMPORARY_LOAN_TYPE("Temporary Loan Type", STRING, false),
  PERMANENT_LOCATION("Permanent Location", STRING, false),
  TEMPORARY_LOCATION("Temporary Location", STRING, false),
  EFFECTIVE_LOCATION("Effective Location", STRING, false),
  IN_TRANSIT_DESTINATION_SERVICE_POINT("In Transit Destination Service Point", STRING, false),
  STATISTICAL_CODES("Statistical Codes", STRING, false),
  PURCHASE_ORDER_LINE_IDENTIFIER("Purchase Order Line Identifier", STRING, false),
  TAGS("Tags", STRING, false),
  LAST_CHECK_IN("Last CheckIn", STRING, false),
  ELECTRONIC_ACCESS("Electronic Access", STRING, false);

  private String value;
  private DataType dataType;
  private boolean visible;

  public static List<Cell> getHeaders() {
    return Arrays.stream(values())
      .map(v -> new Cell().value(v.value).dataType(v.dataType).visible(v.visible))
      .collect(Collectors.toList());
  }
}
