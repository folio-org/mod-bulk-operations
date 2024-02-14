package org.folio.bulkops.domain.bean;

import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.List;

import org.folio.bulkops.domain.converter.BooleanConverter;
import org.folio.bulkops.domain.converter.BoundWithTitlesConverter;
import org.folio.bulkops.domain.converter.CallNumberTypeConverter;
import org.folio.bulkops.domain.converter.CirculationNoteListConverter;
import org.folio.bulkops.domain.converter.ContributorListConverter;
import org.folio.bulkops.domain.converter.DamagedStatusConverter;
import org.folio.bulkops.domain.converter.EffectiveCallNumberComponentsConverter;
import org.folio.bulkops.domain.converter.ElectronicAccessListConverter;
import org.folio.bulkops.domain.converter.IntegerConverter;
import org.folio.bulkops.domain.converter.ItemLocationConverter;
import org.folio.bulkops.domain.converter.ItemNoteListConverter;
import org.folio.bulkops.domain.converter.ItemStatisticalCodeListConverter;
import org.folio.bulkops.domain.converter.ItemStatusConverter;
import org.folio.bulkops.domain.converter.LastCheckInConverter;
import org.folio.bulkops.domain.converter.LoanTypeConverter;
import org.folio.bulkops.domain.converter.MaterialTypeConverter;
import org.folio.bulkops.domain.converter.ServicePointConverter;
import org.folio.bulkops.domain.converter.StringConverter;
import org.folio.bulkops.domain.converter.StringListConverter;
import org.folio.bulkops.domain.converter.TagsConverter;
import org.folio.bulkops.domain.dto.IdentifierType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("item")
@EqualsAndHashCode(exclude = {"metadata", "effectiveCallNumberComponents", "effectiveLocation", "boundWithTitles", "effectiveLocationCallNumber"})
public class Item implements BulkOperationsEntity, ElectronicAccessEntity {

  @JsonProperty("id")
  @CsvCustomBindByName(column = "Item id", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 0, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String id;

  @JsonProperty("_version")
  @CsvCustomBindByName(column = "Version", converter = IntegerConverter.class)
  @CsvCustomBindByPosition(position = 1, converter = IntegerConverter.class)
  @UnifiedTableCell(visible = false)
  private Integer version;

  @JsonProperty("hrid")
  @CsvCustomBindByName(column = "Item HRID", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 2, converter = StringConverter.class)
  @UnifiedTableCell
  private String hrid;

  @JsonProperty("holdingsRecordId")
  @CsvCustomBindByName(column = "Holdings Record Id", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 3, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String holdingsRecordId;

  @JsonProperty("formerIds")
  @Valid
  @CsvCustomBindByName(column = "Former Ids", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 4, converter = StringListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> formerIds;

  @JsonProperty("discoverySuppress")
  @CsvCustomBindByName(column = "Discovery Suppress", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 5, converter = BooleanConverter.class)
  @UnifiedTableCell(visible = false)
  private Boolean discoverySuppress;

  @JsonProperty("title")
  @CsvCustomBindByName(column = "Title", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 6, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String title;

  @CsvCustomBindByName(column = "Holdings (Location, Call number)", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 7, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String effectiveLocationCallNumber;

  @JsonProperty("contributorNames")
  @Valid
  @CsvCustomBindByName(column = "Contributor Names", converter = ContributorListConverter.class)
  @CsvCustomBindByPosition(position = 8, converter = ContributorListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<ContributorName> contributorNames;

  @JsonProperty("callNumber")
  @CsvCustomBindByName(column = "Call Number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 9, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String callNumber;

  @JsonProperty("barcode")
  @CsvCustomBindByName(column = "Barcode", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 10, converter = StringConverter.class)
  @UnifiedTableCell
  private String barcode;

  @JsonProperty("effectiveShelvingOrder")
  @CsvCustomBindByName(column = "Effective Shelving Order", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 11, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String effectiveShelvingOrder;

  @JsonProperty("accessionNumber")
  @CsvCustomBindByName(column = "Accession Number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 12, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String accessionNumber;

  @JsonProperty("itemLevelCallNumber")
  @CsvCustomBindByName(column = "Item Level Call Number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 13, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemLevelCallNumber;

  @JsonProperty("itemLevelCallNumberPrefix")
  @CsvCustomBindByName(column = "Item Level Call Number Prefix", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 14, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemLevelCallNumberPrefix;

  @JsonProperty("itemLevelCallNumberSuffix")
  @CsvCustomBindByName(column = "Item Level Call Number Suffix", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 15, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemLevelCallNumberSuffix;

  @JsonProperty("itemLevelCallNumberTypeId")
  @CsvCustomBindByName(column = "Item Level Call Number Type", converter = CallNumberTypeConverter.class)
  @CsvCustomBindByPosition(position = 16, converter = CallNumberTypeConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemLevelCallNumberTypeId;

  @JsonProperty("effectiveCallNumberComponents")
  @CsvCustomBindByName(column = "Effective Call Number Components", converter = EffectiveCallNumberComponentsConverter.class)
  @CsvCustomBindByPosition(position = 17, converter = EffectiveCallNumberComponentsConverter.class)
  @UnifiedTableCell
  private EffectiveCallNumberComponents effectiveCallNumberComponents;

  @JsonProperty("volume")
  @CsvCustomBindByName(column = "Volume", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 18, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String volume;

  @JsonProperty("enumeration")
  @CsvCustomBindByName(column = "Enumeration", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 19, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String enumeration;

  @JsonProperty("chronology")
  @CsvCustomBindByName(column = "Chronology", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 20, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String chronology;

  @JsonProperty("yearCaption")
  @Valid
  @CsvCustomBindByName(column = "Year Caption", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 21, converter = StringListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> yearCaption;

  @JsonProperty("itemIdentifier")
  @CsvCustomBindByName(column = "Item Identifier", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 22, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemIdentifier;

  @JsonProperty("copyNumber")
  @CsvCustomBindByName(column = "Copy Number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 23, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String copyNumber;

  @JsonProperty("numberOfPieces")
  @CsvCustomBindByName(column = "Number Of Pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 24, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String numberOfPieces;

  @JsonProperty("descriptionOfPieces")
  @CsvCustomBindByName(column = "Description Of Pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 25, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String descriptionOfPieces;

  @JsonProperty("numberOfMissingPieces")
  @CsvCustomBindByName(column = "Number Of Missing Pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 26, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String numberOfMissingPieces;

  @JsonProperty("missingPieces")
  @CsvCustomBindByName(column = "Missing Pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 27, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String missingPieces;

  @JsonProperty("missingPiecesDate")
  @CsvCustomBindByName(column = "Missing Pieces Date", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 28, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String missingPiecesDate;

  @JsonProperty("itemDamagedStatusId")
  @CsvCustomBindByName(column = "Item Damaged Status", converter = DamagedStatusConverter.class)
  @CsvCustomBindByPosition(position = 29, converter = DamagedStatusConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemDamagedStatusId;

  @JsonProperty("itemDamagedStatusDate")
  @CsvCustomBindByName(column = "Item Damaged Status Date", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 30, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemDamagedStatusDate;

  @JsonProperty("administrativeNotes")
  @Valid
  @CsvCustomBindByName(column = "Administrative note", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 31, converter = StringListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> administrativeNotes;

  @JsonProperty("notes")
  @Valid
  @CsvCustomBindByName(column = "Notes", converter = ItemNoteListConverter.class)
  @CsvCustomBindByPosition(position = 32, converter = ItemNoteListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<ItemNote> notes;

  @JsonProperty("circulationNotes")
  @Valid
  private List<CirculationNote> circulationNotes;

  @JsonIgnore
  @CsvCustomBindByName(column = "Check In Notes", converter = CirculationNoteListConverter.class)
  @CsvCustomBindByPosition(position = 33, converter = CirculationNoteListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<CirculationNote> checkInNotes;

  @JsonIgnore
  @CsvCustomBindByName(column = "Check Out Notes", converter = CirculationNoteListConverter.class)
  @CsvCustomBindByPosition(position = 34, converter = CirculationNoteListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<CirculationNote> checkOutNotes;

  @JsonProperty("status")
  @CsvCustomBindByName(column = "Status", converter = ItemStatusConverter.class)
  @CsvCustomBindByPosition(position = 35, converter = ItemStatusConverter.class)
  @UnifiedTableCell
  private InventoryItemStatus status;

  @JsonProperty("materialType")
  @CsvCustomBindByName(column = "Material Type", converter = MaterialTypeConverter.class)
  @CsvCustomBindByPosition(position = 36, converter = MaterialTypeConverter.class)
  @UnifiedTableCell
  private MaterialType materialType;

  @JsonProperty("isBoundWith")
  @CsvCustomBindByName(column = "Is Bound With", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 37, converter = BooleanConverter.class)
  @UnifiedTableCell(visible = false)
  private Boolean isBoundWith;

  @JsonProperty("boundWithTitles")
  @Valid
  @CsvCustomBindByName(column = "Bound With Titles", converter = BoundWithTitlesConverter.class)
  @CsvCustomBindByPosition(position = 38, converter = BoundWithTitlesConverter.class)
  @UnifiedTableCell(visible = false)
  private List<Title> boundWithTitles = emptyList();

  @JsonProperty("permanentLoanType")
  @CsvCustomBindByName(column = "Permanent Loan Type", converter = LoanTypeConverter.class)
  @CsvCustomBindByPosition(position = 39, converter = LoanTypeConverter.class)
  @UnifiedTableCell
  private LoanType permanentLoanType;

  @JsonProperty("temporaryLoanType")
  @CsvCustomBindByName(column = "Temporary Loan Type", converter = LoanTypeConverter.class)
  @CsvCustomBindByPosition(position = 40, converter = LoanTypeConverter.class)
  @UnifiedTableCell
  private LoanType temporaryLoanType;

  @JsonProperty("permanentLocation")
  @CsvCustomBindByName(column = "Permanent Location", converter = ItemLocationConverter.class)
  @CsvCustomBindByPosition(position = 41, converter = ItemLocationConverter.class)
  @UnifiedTableCell(visible = false)
  private ItemLocation permanentLocation;

  @JsonProperty("temporaryLocation")
  @CsvCustomBindByName(column = "Temporary Location", converter = ItemLocationConverter.class)
  @CsvCustomBindByPosition(position = 42, converter = ItemLocationConverter.class)
  @UnifiedTableCell(visible = false)
  private ItemLocation temporaryLocation;

  @JsonProperty("effectiveLocation")
  @CsvCustomBindByName(column = "Effective Location", converter = ItemLocationConverter.class)
  @CsvCustomBindByPosition(position = 43, converter = ItemLocationConverter.class)
  @UnifiedTableCell
  private ItemLocation effectiveLocation;

  @JsonProperty("electronicAccess")
  @Valid
  @CsvCustomBindByName(column = "Electronic Access", converter = ElectronicAccessListConverter.class)
  @CsvCustomBindByPosition(position = 44, converter = ElectronicAccessListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<ElectronicAccess> electronicAccess;

  @JsonProperty("inTransitDestinationServicePointId")
  @CsvCustomBindByName(column = "In Transit Destination Service Point", converter = ServicePointConverter.class)
  @CsvCustomBindByPosition(position = 45, converter = ServicePointConverter.class)
  @UnifiedTableCell(visible = false)
  private String inTransitDestinationServicePointId;

  @JsonProperty("statisticalCodeIds")
  @Valid
  @CsvCustomBindByName(column = "Statistical Codes", converter = ItemStatisticalCodeListConverter.class)
  @CsvCustomBindByPosition(position = 46, converter = ItemStatisticalCodeListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> statisticalCodeIds;

  @JsonProperty("purchaseOrderLineIdentifier")
  @CsvCustomBindByName(column = "Purchase Order Line Identifier", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 47, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String purchaseOrderLineIdentifier;

  @JsonProperty("metadata")
  private Metadata metadata;

  @JsonProperty("tags")
  @CsvCustomBindByName(column = "Tags", converter = TagsConverter.class)
  @CsvCustomBindByPosition(position = 48, converter = TagsConverter.class)
  @UnifiedTableCell(visible = false)
  private Tags tags;

  @JsonProperty("lastCheckIn")
  @CsvCustomBindByName(column = "Last CheckIn", converter = LastCheckInConverter.class)
  @CsvCustomBindByPosition(position = 49, converter = LastCheckInConverter.class)
  @UnifiedTableCell(visible = false)
  private LastCheckIn lastCheckIn;

  @JsonProperty("displaySummary")
  private String displaySummary;

  @Override
  public String getIdentifier(IdentifierType identifierType) {
    return switch (identifierType) {
    case BARCODE -> barcode;
    case HOLDINGS_RECORD_ID -> holdingsRecordId;
    case HRID -> hrid;
    case FORMER_IDS -> isNull(formerIds) ? EMPTY : String.join(",", formerIds);
    case ACCESSION_NUMBER -> accessionNumber;
    default -> id;
    };
  }

  public Boolean getDiscoverySuppress() {
    return isNull(discoverySuppress) ? FALSE : discoverySuppress;
  }
}
