package org.folio.bulkops.domain.bean;

import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.Date;
import java.util.List;

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
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.converter.BooleanConverter;
import org.folio.bulkops.domain.converter.BoundWithTitlesConverter;
import org.folio.bulkops.domain.converter.CallNumberTypeConverter;
import org.folio.bulkops.domain.converter.CirculationNoteListConverter;
import org.folio.bulkops.domain.converter.DamagedStatusConverter;
import org.folio.bulkops.domain.converter.DateWithoutTimeConverter;
import org.folio.bulkops.domain.converter.EffectiveCallNumberComponentsConverter;
import org.folio.bulkops.domain.converter.ItemElectronicAccessListConverter;
import org.folio.bulkops.domain.converter.ItemLocationConverter;
import org.folio.bulkops.domain.converter.ItemNoteListConverter;
import org.folio.bulkops.domain.converter.ItemStatisticalCodeListConverter;
import org.folio.bulkops.domain.converter.ItemStatusConverter;
import org.folio.bulkops.domain.converter.LoanTypeConverter;
import org.folio.bulkops.domain.converter.MaterialTypeConverter;
import org.folio.bulkops.domain.converter.StringConverter;
import org.folio.bulkops.domain.converter.StringListConverter;
import org.folio.bulkops.domain.converter.TagsConverter;
import org.folio.bulkops.domain.dto.DataType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.TenantNotePair;
import org.folio.bulkops.service.ItemReferenceHelper;

@Log4j2
@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("item")
@EqualsAndHashCode(exclude = {"effectiveCallNumberComponents", "effectiveLocation", "boundWithTitles", "holdingsData", "tenantId"})
public class Item implements BulkOperationsEntity, ElectronicAccessEntity {

  public Item(@JsonProperty("tenantId") String tenantId) {
    this.tenantId = tenantId;
  }

  @JsonProperty("id")
  @CsvCustomBindByName(column = "Item UUID", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 0, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String id;

  @JsonProperty("title")
  @CsvCustomBindByName(column = "Instance (Title, Publisher, Publication date)", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 1, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String title;

  @JsonProperty("holdingsData")
  @CsvCustomBindByName(column = "Holdings (Location, Call number)", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 2, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String holdingsData;

  @JsonIgnore
  @CsvCustomBindByName(column = "Item effective location", converter = ItemLocationConverter.class)
  @CsvCustomBindByPosition(position = 3, converter = ItemLocationConverter.class)
  @UnifiedTableCell
  private ItemLocation effectiveLocation;

  @JsonProperty("effectiveLocationId")
  public void setEffectiveLocationId(String id) {
    this.effectiveLocation = new ItemLocation().withId(id).withName(ItemReferenceHelper.service().getLocationById(id, tenantId).getName());
  }

  @JsonProperty("effectiveLocationId")
  public String getEffectiveLocationId() {
    return effectiveLocation != null ? effectiveLocation.getId() : null;
  }

  @JsonProperty("effectiveLocation")
  public ItemLocation getEffectiveLocation() {
    return effectiveLocation;
  }

  @JsonProperty("effectiveCallNumberComponents")
  @CsvCustomBindByName(column = "Effective call number", converter = EffectiveCallNumberComponentsConverter.class)
  @CsvCustomBindByPosition(position = 4, converter = EffectiveCallNumberComponentsConverter.class)
  @UnifiedTableCell
  private EffectiveCallNumberComponents effectiveCallNumberComponents;

  @JsonProperty("discoverySuppress")
  @CsvCustomBindByName(column = "Suppress from discovery", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 5, converter = BooleanConverter.class)
  @UnifiedTableCell(visible = false)
  private Boolean discoverySuppress;

  @JsonProperty("hrid")
  @CsvCustomBindByName(column = "Item HRID", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 6, converter = StringConverter.class)
  @UnifiedTableCell
  private String hrid;

  @JsonProperty("barcode")
  @CsvCustomBindByName(column = "Barcode", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 7, converter = StringConverter.class)
  @UnifiedTableCell
  private String barcode;

  @JsonProperty("accessionNumber")
  @CsvCustomBindByName(column = "Accession number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 8, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String accessionNumber;

  @JsonProperty("itemIdentifier")
  @CsvCustomBindByName(column = "Item identifier", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 9, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemIdentifier;

  @JsonProperty("formerIds")
  @Valid
  @CsvCustomBindByName(column = "Former identifier", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 10, converter = StringListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> formerIds;

  @JsonProperty("statisticalCodeIds")
  @Valid
  @CsvCustomBindByName(column = "Statistical codes", converter = ItemStatisticalCodeListConverter.class)
  @CsvCustomBindByPosition(position = 11, converter = ItemStatisticalCodeListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> statisticalCodes;

  @JsonProperty("administrativeNotes")
  @Valid
  @CsvCustomBindByName(column = "Administrative note", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 12, converter = StringListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> administrativeNotes;

  @JsonIgnore
  @CsvCustomBindByName(column = "Material type", converter = MaterialTypeConverter.class)
  @CsvCustomBindByPosition(position = 13, converter = MaterialTypeConverter.class)
  @UnifiedTableCell
  private MaterialType materialType;

  @JsonProperty("materialTypeId")
  public void setMaterialTypeId(String id) {
    log.info("tenant bean: {}", tenantId);
    this.materialType = new MaterialType().withId(id).withName(ItemReferenceHelper.service().getMaterialTypeById(id, tenantId).getName());
  }

  @JsonProperty("materialTypeId")
  public String getMaterialTypeId() {
    return materialType != null ? materialType.getId() : null;
  }

  @JsonProperty("materialType")
  public MaterialType getMaterialType() {
    return materialType;
  }

  @JsonProperty("copyNumber")
  @CsvCustomBindByName(column = "Copy number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 14, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String copyNumber;

  @JsonProperty("effectiveShelvingOrder")
  @CsvCustomBindByName(column = "Shelving order", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 15, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String effectiveShelvingOrder;

  @JsonProperty("itemLevelCallNumberTypeId")
  @CsvCustomBindByName(column = "Item level call number type", converter = CallNumberTypeConverter.class)
  @CsvCustomBindByPosition(position = 16, converter = CallNumberTypeConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemLevelCallNumberType;

  @JsonProperty("itemLevelCallNumberPrefix")
  @CsvCustomBindByName(column = "Item level call number prefix", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 17, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemLevelCallNumberPrefix;

  @JsonProperty("itemLevelCallNumber")
  @CsvCustomBindByName(column = "Item level call number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 18, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemLevelCallNumber;

  @JsonProperty("itemLevelCallNumberSuffix")
  @CsvCustomBindByName(column = "Item level call number suffix", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 19, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemLevelCallNumberSuffix;

  @JsonProperty("numberOfPieces")
  @CsvCustomBindByName(column = "Number of pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 20, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String numberOfPieces;

  @JsonProperty("descriptionOfPieces")
  @CsvCustomBindByName(column = "Description of pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 21, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String descriptionOfPieces;

  @JsonProperty("enumeration")
  @CsvCustomBindByName(column = "Enumeration", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 22, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String enumeration;

  @JsonProperty("chronology")
  @CsvCustomBindByName(column = "Chronology", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 23, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String chronology;

  @JsonProperty("volume")
  @CsvCustomBindByName(column = "Volume", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 24, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String volume;

  @JsonProperty("yearCaption")
  @Valid
  @CsvCustomBindByName(column = "Year, caption", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 25, converter = StringListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> yearCaption;

  @JsonProperty("numberOfMissingPieces")
  @CsvCustomBindByName(column = "Number of missing pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 26, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String numberOfMissingPieces;

  @JsonProperty("missingPieces")
  @CsvCustomBindByName(column = "Missing pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 27, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String missingPieces;

  @JsonProperty("missingPiecesDate")
  @CsvCustomBindByName(column = "Missing pieces date", converter = DateWithoutTimeConverter.class)
  @CsvCustomBindByPosition(position = 28, converter = DateWithoutTimeConverter.class)
  @UnifiedTableCell(dataType = DataType.DATE_TIME, visible = false)
  private Date missingPiecesDate;

  @JsonProperty("itemDamagedStatusId")
  @CsvCustomBindByName(column = "Item damaged status", converter = DamagedStatusConverter.class)
  @CsvCustomBindByPosition(position = 29, converter = DamagedStatusConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemDamagedStatus;

  @JsonProperty("itemDamagedStatusDate")
  @CsvCustomBindByName(column = "Item damaged status date", converter = DateWithoutTimeConverter.class)
  @CsvCustomBindByPosition(position = 30, converter = DateWithoutTimeConverter.class)
  @UnifiedTableCell(dataType = DataType.DATE_TIME, visible = false)
  private Date itemDamagedStatusDate;

  @JsonProperty("notes")
  @Valid
  @CsvCustomBindByName(column = "Notes", converter = ItemNoteListConverter.class)
  @CsvCustomBindByPosition(position = 31, converter = ItemNoteListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<ItemNote> notes;

  @JsonIgnore
  @CsvCustomBindByName(column = "Permanent loan type", converter = LoanTypeConverter.class)
  @CsvCustomBindByPosition(position = 32, converter = LoanTypeConverter.class)
  @UnifiedTableCell
  private LoanType permanentLoanType;

  @JsonProperty("permanentLoanTypeId")
  public void setPermanentLoanTypeId(String id) {
    this.permanentLoanType = new LoanType().withId(id).withName(ItemReferenceHelper.service().getLoanTypeById(id, tenantId).getName());
  }

  @JsonProperty("permanentLoanTypeId")
  public String getPermanentLoanTypeId() {
    return permanentLoanType != null ? permanentLoanType.getId() : null;
  }

  @JsonProperty("permanentLoanType")
  public LoanType getPermanentLoanType() {
    return permanentLoanType;
  }

  @JsonIgnore
  @CsvCustomBindByName(column = "Temporary loan type", converter = LoanTypeConverter.class)
  @CsvCustomBindByPosition(position = 33, converter = LoanTypeConverter.class)
  @UnifiedTableCell
  private LoanType temporaryLoanType;

  @JsonProperty("temporaryLoanTypeId")
  public void setTemporaryLoanTypeId(String id) {
    this.temporaryLoanType = new LoanType().withId(id).withName(ItemReferenceHelper.service().getLoanTypeById(id, tenantId).getName());
  }

  @JsonProperty("temporaryLoanTypeId")
  public String getTemporaryLoanTypeId() {
    return temporaryLoanType != null ? temporaryLoanType.getId() : null;
  }

  @JsonProperty("temporaryLoanType")
  public LoanType getTemporaryLoanType() {
    return temporaryLoanType;
  }

  @JsonProperty("status")
  @CsvCustomBindByName(column = "Status", converter = ItemStatusConverter.class)
  @CsvCustomBindByPosition(position = 34, converter = ItemStatusConverter.class)
  @UnifiedTableCell
  private InventoryItemStatus status;

  @JsonIgnore
  @CsvCustomBindByName(column = "Check in note", converter = CirculationNoteListConverter.class)
  @CsvCustomBindByPosition(position = 35, converter = CirculationNoteListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<CirculationNote> checkInNotes;

  @JsonIgnore
  @CsvCustomBindByName(column = "Check out note", converter = CirculationNoteListConverter.class)
  @CsvCustomBindByPosition(position = 36, converter = CirculationNoteListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<CirculationNote> checkOutNotes;

  @JsonIgnore
  @CsvCustomBindByName(column = "Item permanent location", converter = ItemLocationConverter.class)
  @CsvCustomBindByPosition(position = 37, converter = ItemLocationConverter.class)
  @UnifiedTableCell(visible = false)
  private ItemLocation permanentLocation;

  @JsonProperty("permanentLocationId")
  public void setPermanentLocationId(String id) {
    this.permanentLocation = new ItemLocation().withId(id).withName(ItemReferenceHelper.service().getLocationById(id, tenantId).getName());
  }

  @JsonProperty("permanentLocationId")
  public String getPermanentLocationId() {
    return permanentLocation != null ? permanentLocation.getId() : null;
  }

  @JsonProperty("permanentLocation")
  public ItemLocation getPermanentLocation() {
    return permanentLocation;
  }

  @JsonIgnore
  @CsvCustomBindByName(column = "Item temporary location", converter = ItemLocationConverter.class)
  @CsvCustomBindByPosition(position = 38, converter = ItemLocationConverter.class)
  @UnifiedTableCell(visible = false)
  private ItemLocation temporaryLocation;

  @JsonProperty("temporaryLocationId")
  public void setTemporaryLocationId(String id) {
    this.temporaryLocation = new ItemLocation().withId(id).withName(ItemReferenceHelper.service().getLocationById(id, tenantId).getName());
  }

  @JsonProperty("temporaryLocationId")
  public String getTemporaryLocationId() {
    return temporaryLocation != null ? temporaryLocation.getId() : null;
  }

  @JsonProperty("temporaryLocation")
  public ItemLocation getTemporaryLocation() {
    return temporaryLocation;
  }


  @JsonProperty("electronicAccess")
  @Valid
  @CsvCustomBindByName(column = "Electronic access", converter = ItemElectronicAccessListConverter.class)
  @CsvCustomBindByPosition(position = 39, converter = ItemElectronicAccessListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<ElectronicAccess> electronicAccess;

  @JsonProperty("isBoundWith")
  @CsvCustomBindByName(column = "Is bound with", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 40, converter = BooleanConverter.class)
  @UnifiedTableCell(visible = false)
  private Boolean isBoundWith;

  @JsonProperty("boundWithTitles")
  @Valid
  @CsvCustomBindByName(column = "Bound with titles", converter = BoundWithTitlesConverter.class)
  @CsvCustomBindByPosition(position = 41, converter = BoundWithTitlesConverter.class)
  @UnifiedTableCell(visible = false)
  private List<Title> boundWithTitles = emptyList();

  @JsonProperty("tags")
  @CsvCustomBindByName(column = "Tags", converter = TagsConverter.class)
  @CsvCustomBindByPosition(position = 42, converter = TagsConverter.class)
  @UnifiedTableCell(visible = false)
  private Tags tags;

  @JsonProperty("holdingsRecordId")
  @CsvCustomBindByName(column = "Holdings UUID", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 43, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String holdingsRecordId;

  @JsonProperty("tenantId")
  @CsvCustomBindByName(column = "Tenant", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 44, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String tenantId;

  @JsonProperty("_version")
  private Integer version;

  @JsonProperty("circulationNotes")
  @Valid
  private List<CirculationNote> circulationNotes;

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

  @Override
  public Integer _version() {
    return version;
  }

  public Boolean getDiscoverySuppress() {
    return isNull(discoverySuppress) ? FALSE : discoverySuppress;
  }

  @Override
  public void setTenantToNotes(List<TenantNotePair> tenantNotePairs) {
    getNotes().forEach(note -> note.setTenantId(
      tenantNotePairs.stream()
        .filter(pair -> pair.getNoteTypeId().equals(note.getItemNoteTypeId()))
        .map(TenantNotePair::getTenantId).findFirst().orElseGet(() -> tenantId)
    ));
  }

  @Override
  public void setTenant(String tenantId) {
    this.tenantId = tenantId;
  }
}
