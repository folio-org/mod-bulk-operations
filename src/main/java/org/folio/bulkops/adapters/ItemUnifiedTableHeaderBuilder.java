package org.folio.bulkops.adapters;

import static org.folio.bulkops.domain.dto.DataType.STRING;

import java.util.Arrays;
import java.util.List;

import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.DataType;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class ItemUnifiedTableHeaderBuilder implements UnifiedTableHeaderBuilder<Item> {
  @Override
  public UnifiedTable getEmptyTableWithHeaders() {
    return new UnifiedTable().header(ItemHeaderBuilder.getHeaders());
  }

  @Override
  public Class<Item> getProcessedType() {
    return Item.class;
  }

  @AllArgsConstructor
  public enum ItemHeaderBuilder {
    ID("Item id", STRING, false),
    VERSION("Version", STRING, false),
    HRID("Item HRID", STRING, true),
    HOLDING_RECORD_ID ("Holdings Record Id", STRING, false),
    FORMER_IDS ("Former Ids", STRING, false),
    DISCOVERY_SUPRESS ("Discovery Suppress", STRING, false),
    TITLE ("Title", STRING, false),
    CONTRIBUTORS_NAMES("Contributor Names", STRING, false),
    CALL_NUMBER("Call Number", STRING, false),
    BARCODE("Barcode", STRING, true),
    EFFECTIVE_SHELVING_ORDER("Effective Shelving Order", STRING, false),
    ACCESSION_NUMBER("Accession Number", STRING, false),
    LEVEL_CALL_NUMBER("Item Level Call Number", STRING, false),
    LEVEL_CALL_NUMBER_PREFIX("Item Level Call Number Prefix", STRING, false),
    LEVEL_CALL_NUMBER_SUFFIX("Item Level Call Number Suffix", STRING, false),
    LEVEL_CALL_NUMBER_TYPE("Item Level Call Number Type", STRING, false),
    EFFECTIVE_CALL_NUMBER_COMPONENTS("Effective Call Number Components", STRING, true),
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
    STATUS("Status", STRING, true),
    MATERIAL_TYPE("Material Type", STRING, true),
    IS_BOUND_WITH("Is Bound With", STRING, false),
    BOUND_WITH_TITLES("Bound With Titles", STRING, false),
    PERMANENT_LOAN_TYPE("Permanent Loan Type", STRING, true),
    TEMPORARY_LOAN_TYPE("Temporary Loan Type", STRING, true),
    PERMANENT_LOCATION("Permanent Location", STRING, false),
    TEMPORARY_LOCATION("Temporary Location", STRING, false),
    EFFECTIVE_LOCATION("Effective Location", STRING, true),
    ELECTRONIC_ACCESS("Electronic Access", STRING, false),
    IN_TRANSIT_DESTINATION_SERVICE_POINT("In Transit Destination Service Point", STRING, false),
    STATISTICAL_CODES("Statistical Codes", STRING, false),
    PURCHASE_ORDER_LINE_IDENTIFIER("Purchase Order Line Identifier", STRING, false),
    TAGS("Tags", STRING, false),
    LAST_CHECK_IN("Last CheckIn", STRING, false);

    private final String value;
    private final DataType dataType;
    private final boolean visible;

    public static List<Cell> getHeaders() {
      return Arrays.stream(values())
        .map(v -> new Cell().value(v.value).dataType(v.dataType).visible(v.visible))
        .toList();
    }
  }

}
