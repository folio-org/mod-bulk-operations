package org.folio.bulkops.domain.bean;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.List;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;
import org.folio.bulkops.domain.converter.BooleanConverter;
import org.folio.bulkops.domain.converter.BoundWithTitlesConverter;
import org.folio.bulkops.domain.converter.CallNumberTypeConverter;
import org.folio.bulkops.domain.converter.CirculationNoteListConverter;
import org.folio.bulkops.domain.converter.ContributorListConverter;
import org.folio.bulkops.domain.converter.DamagedStatusConverter;
import org.folio.bulkops.domain.converter.EffectiveCallNumberComponentsConverter;
import org.folio.bulkops.domain.converter.ElectronicAccessListConverter;
import org.folio.bulkops.domain.converter.IntegerConverter;
import org.folio.bulkops.domain.converter.InventoryItemStatusConverter;
import org.folio.bulkops.domain.converter.ItemLocationConverter;
import org.folio.bulkops.domain.converter.ItemNoteListConverter;
import org.folio.bulkops.domain.converter.ItemStatisticalCodeListConverter;
import org.folio.bulkops.domain.converter.LastCheckInConverter;
import org.folio.bulkops.domain.converter.LoanTypeConverter;
import org.folio.bulkops.domain.converter.MaterialTypeConverter;
import org.folio.bulkops.domain.converter.ServicePointConverter;
import org.folio.bulkops.domain.converter.StringConverter;
import org.folio.bulkops.domain.converter.StringListConverter;
import org.folio.bulkops.domain.converter.TagsConverter;
import org.folio.bulkops.domain.dto.IdentifierType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("item")
public class Item extends BulkOperationsEntity {
  @JsonProperty("id")
  @CsvCustomBindByName(column = "Item id", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 0, converter = StringConverter.class)
  private String id;

  @JsonProperty("_version")
  @CsvCustomBindByName(column = "Version", converter = IntegerConverter.class)
  @CsvCustomBindByPosition(position = 1, converter = IntegerConverter.class)
  private Integer version;

  @JsonProperty("hrid")
  @CsvCustomBindByName(column = "Item HRID", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 2, converter = StringConverter.class)
  private String hrid;

  @JsonProperty("holdingsRecordId")
  @CsvCustomBindByName(column = "Holdings Record Id", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 3, converter = StringConverter.class)
  private String holdingsRecordId;

  @JsonProperty("formerIds")
  @Valid
  @CsvCustomBindByName(column = "Former ids", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 4, converter = StringListConverter.class)
  private List<String> formerIds = null;

  @JsonProperty("discoverySuppress")
  @CsvCustomBindByName(column = "Discovery suppress", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 5, converter = BooleanConverter.class)
  private Boolean discoverySuppress;

  @JsonProperty("title")
  @CsvCustomBindByName(column = "Title", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 6, converter = StringConverter.class)
  private String title;

  @JsonProperty("contributorNames")
  @Valid
  @CsvCustomBindByName(column = "Contributor Names", converter = ContributorListConverter.class)
  @CsvCustomBindByPosition(position = 7, converter = ContributorListConverter.class)
  private List<ContributorName> contributorNames = null;

  @JsonProperty("callNumber")
  @CsvCustomBindByName(column = "Call Number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 8, converter = StringConverter.class)
  private String callNumber;

  @JsonProperty("barcode")
  @CsvCustomBindByName(column = "Barcode", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 9, converter = StringConverter.class)
  private String barcode;

  @JsonProperty("effectiveShelvingOrder")
  @CsvCustomBindByName(column = "Effective Shelving Order", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 10, converter = StringConverter.class)
  private String effectiveShelvingOrder;

  @JsonProperty("accessionNumber")
  @CsvCustomBindByName(column = "Accession Number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 11, converter = StringConverter.class)
  private String accessionNumber;

  @JsonProperty("itemLevelCallNumber")
  @CsvCustomBindByName(column = "Item Level Call Number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 12, converter = StringConverter.class)
  private String itemLevelCallNumber;

  @JsonProperty("itemLevelCallNumberPrefix")
  @CsvCustomBindByName(column = "Item Level Call Number Prefix", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 13, converter = StringConverter.class)
  private String itemLevelCallNumberPrefix;

  @JsonProperty("itemLevelCallNumberSuffix")
  @CsvCustomBindByName(column = "Item Level Call Number Suffix", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 14, converter = StringConverter.class)
  private String itemLevelCallNumberSuffix;

  @JsonProperty("itemLevelCallNumberTypeId")
  @CsvCustomBindByName(column = "Item Level Call Number Type", converter = CallNumberTypeConverter.class)
  @CsvCustomBindByPosition(position = 15, converter = CallNumberTypeConverter.class)
  private String itemLevelCallNumberTypeId;

  @JsonProperty("effectiveCallNumberComponents")
  @CsvCustomBindByName(column = "Effective Call Number Components", converter = EffectiveCallNumberComponentsConverter.class)
  @CsvCustomBindByPosition(position = 16, converter = EffectiveCallNumberComponentsConverter.class)
  private EffectiveCallNumberComponents effectiveCallNumberComponents;

  @JsonProperty("volume")
  @CsvCustomBindByName(column = "Volume", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 17, converter = StringConverter.class)
  private String volume;

  @JsonProperty("enumeration")
  @CsvCustomBindByName(column = "Enumeration", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 18, converter = StringConverter.class)
  private String enumeration;

  @JsonProperty("chronology")
  @CsvCustomBindByName(column = "Chronology", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 19, converter = StringConverter.class)
  private String chronology;

  @JsonProperty("yearCaption")
  @Valid
  @CsvCustomBindByName(column = "Year Caption", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 20, converter = StringListConverter.class)
  private List<String> yearCaption = null;

  @JsonProperty("itemIdentifier")
  @CsvCustomBindByName(column = "Item Identifier", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 21, converter = StringConverter.class)
  private String itemIdentifier;

  @JsonProperty("copyNumber")
  @CsvCustomBindByName(column = "Copy Number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 22, converter = StringConverter.class)
  private String copyNumber;

  @JsonProperty("numberOfPieces")
  @CsvCustomBindByName(column = "Number Of Pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 23, converter = StringConverter.class)
  private String numberOfPieces;

  @JsonProperty("descriptionOfPieces")
  @CsvCustomBindByName(column = "Description Of Pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 24, converter = StringConverter.class)
  private String descriptionOfPieces;

  @JsonProperty("numberOfMissingPieces")
  @CsvCustomBindByName(column = "Number Of Missing Pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 25, converter = StringConverter.class)
  private String numberOfMissingPieces;

  @JsonProperty("missingPieces")
  @CsvCustomBindByName(column = "Missing Pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 26, converter = StringConverter.class)
  private String missingPieces;

  @JsonProperty("missingPiecesDate")
  @CsvCustomBindByName(column = "Missing Pieces Date", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 27, converter = StringConverter.class)
  private String missingPiecesDate;

  @JsonProperty("itemDamagedStatusId")
  @CsvCustomBindByName(column = "Item Damaged Status", converter = DamagedStatusConverter.class)
  @CsvCustomBindByPosition(position = 28, converter = DamagedStatusConverter.class)
  private String itemDamagedStatusId;

  @JsonProperty("itemDamagedStatusDate")
  @CsvCustomBindByName(column = "Item Damaged Status Date", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 29, converter = StringConverter.class)
  private String itemDamagedStatusDate;

  @JsonProperty("administrativeNotes")
  @Valid
  @CsvCustomBindByName(column = "Administrative Notes", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 30, converter = StringListConverter.class)
  private List<String> administrativeNotes = null;

  @JsonProperty("notes")
  @Valid
  @CsvCustomBindByName(column = "Notes", converter = ItemNoteListConverter.class)
  @CsvCustomBindByPosition(position = 31, converter = ItemNoteListConverter.class)
  private List<ItemNote> notes = null;

  @JsonProperty("circulationNotes")
  @Valid
  @CsvCustomBindByName(column = "Circulation Notes", converter = CirculationNoteListConverter.class)
  @CsvCustomBindByPosition(position = 32, converter = CirculationNoteListConverter.class)
  private List<CirculationNote> circulationNotes = null;

  @JsonProperty("status")
  @CsvCustomBindByName(column = "Status", converter = InventoryItemStatusConverter.class)
  @CsvCustomBindByPosition(position = 33, converter = InventoryItemStatusConverter.class)
  private InventoryItemStatus status;

  @JsonProperty("materialType")
  @CsvCustomBindByName(column = "Material Type", converter = MaterialTypeConverter.class)
  @CsvCustomBindByPosition(position = 34, converter = MaterialTypeConverter.class)
  private MaterialType materialType;

  @JsonProperty("isBoundWith")
  @CsvCustomBindByName(column = "Is Bound With", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 35, converter = BooleanConverter.class)
  private Boolean isBoundWith;

  @JsonProperty("boundWithTitles")
  @Valid
  @CsvCustomBindByName(column = "Bound With Titles", converter = BoundWithTitlesConverter.class)
  @CsvCustomBindByPosition(position = 36, converter = BoundWithTitlesConverter.class)
  private List<Title> boundWithTitles = null;

  @JsonProperty("permanentLoanType")
  @CsvCustomBindByName(column = "Permanent Loan Type", converter = LoanTypeConverter.class)
  @CsvCustomBindByPosition(position = 37, converter = LoanTypeConverter.class)
  private LoanType permanentLoanType;

  @JsonProperty("temporaryLoanType")
  @CsvCustomBindByName(column = "Temporary Loan Type", converter = LoanTypeConverter.class)
  @CsvCustomBindByPosition(position = 38, converter = LoanTypeConverter.class)
  private LoanType temporaryLoanType;

  @JsonProperty("permanentLocation")
  @CsvCustomBindByName(column = "Permanent Location", converter = ItemLocationConverter.class)
  @CsvCustomBindByPosition(position = 39, converter = ItemLocationConverter.class)
  private ItemLocation permanentLocation;

  @JsonProperty("temporaryLocation")
  @CsvCustomBindByName(column = "Temporary Location", converter = ItemLocationConverter.class)
  @CsvCustomBindByPosition(position = 40, converter = ItemLocationConverter.class)
  private ItemLocation temporaryLocation;

  @JsonProperty("effectiveLocation")
  @CsvCustomBindByName(column = "Effective Location", converter = ItemLocationConverter.class)
  @CsvCustomBindByPosition(position = 41, converter = ItemLocationConverter.class)
  private ItemLocation effectiveLocation;

  @JsonProperty("electronicAccess")
  @Valid
  @CsvCustomBindByName(column = "Electronic Access", converter = ElectronicAccessListConverter.class)
  @CsvCustomBindByPosition(position = 42, converter = ElectronicAccessListConverter.class)
  private List<ElectronicAccess> electronicAccess = null;

  @JsonProperty("inTransitDestinationServicePointId")
  @CsvCustomBindByName(column = "In Transit Destination Service Point", converter = ServicePointConverter.class)
  @CsvCustomBindByPosition(position = 43, converter = ServicePointConverter.class)
  private String inTransitDestinationServicePointId;

  @JsonProperty("statisticalCodeIds")
  @Valid
  @CsvCustomBindByName(column = "Statistical Codes", converter = ItemStatisticalCodeListConverter.class)
  @CsvCustomBindByPosition(position = 44, converter = ItemStatisticalCodeListConverter.class)
  private List<String> statisticalCodeIds = null;

  @JsonProperty("purchaseOrderLineIdentifier")
  @CsvCustomBindByName(column = "Purchase Order LineIdentifier", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 45, converter = StringConverter.class)
  private String purchaseOrderLineIdentifier;

  @JsonProperty("metadata")
  private Metadata metadata;

  @JsonProperty("tags")
  @CsvCustomBindByName(column = "Tags", converter = TagsConverter.class)
  @CsvCustomBindByPosition(position = 46, converter = TagsConverter.class)
  private Tags tags;

  @JsonProperty("lastCheckIn")
  @CsvCustomBindByName(column = "Last CheckIn", converter = LastCheckInConverter.class)
  @CsvCustomBindByPosition(position = 47, converter = LastCheckInConverter.class)
  private LastCheckIn lastCheckIn;

  @Override
  public String getIdentifier(IdentifierType identifierType) {
    switch (identifierType) {
    case BARCODE:
      return barcode;
    case HOLDINGS_RECORD_ID:
      return holdingsRecordId;
    case HRID:
      return hrid;
    case FORMER_IDS:
      return isNull(formerIds) ? EMPTY : String.join(",", formerIds);
    case ACCESSION_NUMBER:
      return accessionNumber;
    default:
      return id;
    }
  }
}
