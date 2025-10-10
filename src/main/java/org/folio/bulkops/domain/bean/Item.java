package org.folio.bulkops.domain.bean;

import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.ACCESSION_NUMBER;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.ADMINISTRATIVE_NOTES;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.BARCODE;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.BOUND_WITH_TITLES;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.CHRONOLOGY;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.CIRCULATION_NOTES;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.COPY_NUMBER;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.DESCRIPTION_OF_PIECES;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.DISCOVERY_SUPPRESS;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.EFFECTIVE_CALL_NUMBER_COMPONENTS;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.EFFECTIVE_LOCATION;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.EFFECTIVE_SHELVING_ORDER;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.ELECTRONIC_ACCESS;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.ENUMERATION;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.FORMER_IDS;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.HOLDINGS_DATA;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.HOLDINGS_RECORD_ID;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.HRID;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.ID;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.ITEM_DAMAGED_STATUS_DATE;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.ITEM_DAMAGED_STATUS_ID;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.ITEM_IDENTIFIER;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.ITEM_LEVEL_CALL_NUMBER;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.ITEM_LEVEL_CALL_NUMBER_PREFIX;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.ITEM_LEVEL_CALL_NUMBER_SUFFIX;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.ITEM_LEVEL_CALL_NUMBER_TYPE_ID;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.IS_BOUND_WITH;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.MATERIAL_TYPE;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.MISSING_PIECES;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.MISSING_PIECES_DATE;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.NOTES;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.NUMBER_OF_MISSING_PIECES;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.NUMBER_OF_PIECES;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.PERMANENT_LOAN_TYPE;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.PERMANENT_LOCATION;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.STATISTICAL_CODE_IDS;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.STATUS;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.TAGS;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.TEMPORARY_LOAN_TYPE;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.TEMPORARY_LOCATION;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.TENANT_ID;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.TITLE;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.VOLUME;
import static org.folio.bulkops.domain.bean.ItemJsonPropertyNames.YEAR_CAPTION;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.converter.BooleanConverter;
import org.folio.bulkops.domain.converter.BoundWithTitlesConverter;
import org.folio.bulkops.domain.converter.CallNumberTypeConverter;
import org.folio.bulkops.domain.converter.CirculationNoteListConverter;
import org.folio.bulkops.domain.converter.DamagedStatusConverter;
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
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.TenantNotePair;
import org.folio.bulkops.service.ItemReferenceHelper;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("item")
@EqualsAndHashCode(exclude = {"effectiveCallNumberComponents", "effectiveLocation",
    "boundWithTitles", "holdingsData", "tenantId"})
public class Item implements BulkOperationsEntity, ElectronicAccessEntity {

  public Item(@JsonProperty("tenantId") String tenantId) {
    this.tenantId = tenantId;
  }

  @JsonProperty(ID)
  @CsvCustomBindByName(column = "Item UUID", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 0, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String id;

  @JsonProperty(TITLE)
  @CsvCustomBindByName(column = "Instance (Title, Publisher, Publication date)",
          converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 1, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String title;

  @JsonProperty(HOLDINGS_DATA)
  @CsvCustomBindByName(column = "Holdings (Location, Call number)",
          converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 2, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String holdingsData;

  @JsonProperty(EFFECTIVE_LOCATION)
  @CsvCustomBindByName(column = "Item effective location", converter = ItemLocationConverter.class)
  @CsvCustomBindByPosition(position = 3, converter = ItemLocationConverter.class)
  @UnifiedTableCell
  private ItemLocation effectiveLocation;

  @JsonProperty("effectiveLocationId")
  public void setEffectiveLocationId(String id) {
    if (nonNull(id)) {
      this.effectiveLocation = new ItemLocation().withId(id)
              .withName(ItemReferenceHelper.service().getLocationById(id, tenantId).getName());
    }
  }

  @JsonProperty("effectiveLocationId")
  public String getEffectiveLocationId() {
    return effectiveLocation != null ? effectiveLocation.getId() : null;
  }

  @JsonProperty("effectiveLocation")
  public ItemLocation getEffectiveLocation() {
    return effectiveLocation;
  }

  @JsonProperty(EFFECTIVE_CALL_NUMBER_COMPONENTS)
  @CsvCustomBindByName(column = "Effective call number",
          converter = EffectiveCallNumberComponentsConverter.class)
  @CsvCustomBindByPosition(position = 4, converter = EffectiveCallNumberComponentsConverter.class)
  @UnifiedTableCell
  private EffectiveCallNumberComponents effectiveCallNumberComponents;

  @JsonProperty(DISCOVERY_SUPPRESS)
  @CsvCustomBindByName(column = "Suppress from discovery", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 5, converter = BooleanConverter.class)
  @UnifiedTableCell(visible = false)
  private Boolean discoverySuppress;

  @JsonProperty(HRID)
  @CsvCustomBindByName(column = "Item HRID", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 6, converter = StringConverter.class)
  @UnifiedTableCell
  private String hrid;

  @JsonProperty(BARCODE)
  @CsvCustomBindByName(column = "Barcode", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 7, converter = StringConverter.class)
  @UnifiedTableCell
  private String barcode;

  @JsonProperty(ACCESSION_NUMBER)
  @CsvCustomBindByName(column = "Accession number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 8, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String accessionNumber;

  @JsonProperty(ITEM_IDENTIFIER)
  @CsvCustomBindByName(column = "Item identifier", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 9, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemIdentifier;

  @JsonProperty(FORMER_IDS)
  @Valid
  @CsvCustomBindByName(column = "Former identifier", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 10, converter = StringListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> formerIds;

  @JsonProperty(STATISTICAL_CODE_IDS)
  @Valid
  @CsvCustomBindByName(column = "Statistical codes",
          converter = ItemStatisticalCodeListConverter.class)
  @CsvCustomBindByPosition(position = 11, converter = ItemStatisticalCodeListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> statisticalCodes;

  @JsonProperty(ADMINISTRATIVE_NOTES)
  @Valid
  @CsvCustomBindByName(column = "Administrative note", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 12, converter = StringListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> administrativeNotes;

  @JsonProperty(MATERIAL_TYPE)
  @CsvCustomBindByName(column = "Material type", converter = MaterialTypeConverter.class)
  @CsvCustomBindByPosition(position = 13, converter = MaterialTypeConverter.class)
  @UnifiedTableCell
  private MaterialType materialType;

  @JsonProperty("materialTypeId")
  public void setMaterialTypeId(String id) {
    if (nonNull(id)) {
      this.materialType = new MaterialType().withId(id)
              .withName(ItemReferenceHelper.service().getMaterialTypeById(id, tenantId).getName());
    }
  }

  @JsonProperty("materialTypeId")
  public String getMaterialTypeId() {
    return materialType != null ? materialType.getId() : null;
  }

  @JsonProperty("materialType")
  public MaterialType getMaterialType() {
    return materialType;
  }

  @JsonProperty(COPY_NUMBER)
  @CsvCustomBindByName(column = "Copy number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 14, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String copyNumber;

  @JsonProperty(EFFECTIVE_SHELVING_ORDER)
  @CsvCustomBindByName(column = "Shelving order", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 15, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String effectiveShelvingOrder;

  @JsonProperty(ITEM_LEVEL_CALL_NUMBER_TYPE_ID)
  @CsvCustomBindByName(column = "Item level call number type",
          converter = CallNumberTypeConverter.class)
  @CsvCustomBindByPosition(position = 16, converter = CallNumberTypeConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemLevelCallNumberType;

  @JsonProperty(ITEM_LEVEL_CALL_NUMBER_PREFIX)
  @CsvCustomBindByName(column = "Item level call number prefix", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 17, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemLevelCallNumberPrefix;

  @JsonProperty(ITEM_LEVEL_CALL_NUMBER)
  @CsvCustomBindByName(column = "Item level call number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 18, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemLevelCallNumber;

  @JsonProperty(ITEM_LEVEL_CALL_NUMBER_SUFFIX)
  @CsvCustomBindByName(column = "Item level call number suffix", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 19, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemLevelCallNumberSuffix;

  @JsonProperty(NUMBER_OF_PIECES)
  @CsvCustomBindByName(column = "Number of pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 20, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String numberOfPieces;

  @JsonProperty(DESCRIPTION_OF_PIECES)
  @CsvCustomBindByName(column = "Description of pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 21, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String descriptionOfPieces;

  @JsonProperty(ENUMERATION)
  @CsvCustomBindByName(column = "Enumeration", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 22, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String enumeration;

  @JsonProperty(CHRONOLOGY)
  @CsvCustomBindByName(column = "Chronology", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 23, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String chronology;

  @JsonProperty(VOLUME)
  @CsvCustomBindByName(column = "Volume", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 24, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String volume;

  @JsonProperty(YEAR_CAPTION)
  @Valid
  @CsvCustomBindByName(column = "Year, caption", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 25, converter = StringListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> yearCaption;

  @JsonProperty(NUMBER_OF_MISSING_PIECES)
  @CsvCustomBindByName(column = "Number of missing pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 26, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String numberOfMissingPieces;

  @JsonProperty(MISSING_PIECES)
  @CsvCustomBindByName(column = "Missing pieces", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 27, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String missingPieces;

  @JsonProperty(MISSING_PIECES_DATE)
  @CsvCustomBindByName(column = "Missing pieces date", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 28, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String missingPiecesDate;

  @JsonProperty(ITEM_DAMAGED_STATUS_ID)
  @CsvCustomBindByName(column = "Item damaged status", converter = DamagedStatusConverter.class)
  @CsvCustomBindByPosition(position = 29, converter = DamagedStatusConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemDamagedStatus;

  @JsonProperty(ITEM_DAMAGED_STATUS_DATE)
  @CsvCustomBindByName(column = "Item damaged status date", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 30, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String itemDamagedStatusDate;

  @JsonProperty(NOTES)
  @Valid
  @CsvCustomBindByName(column = "Notes", converter = ItemNoteListConverter.class)
  @CsvCustomBindByPosition(position = 31, converter = ItemNoteListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<ItemNote> notes;

  @JsonProperty("order")
  private Integer order;

  @JsonProperty("notes")
  public void setNotes(List<ItemNote> notes) {
    if (nonNull(notes)) {
      notes.forEach(note -> note.setTenantId(tenantId));
    }
    this.notes = notes;
  }

  @JsonProperty(PERMANENT_LOAN_TYPE)
  @CsvCustomBindByName(column = "Permanent loan type", converter = LoanTypeConverter.class)
  @CsvCustomBindByPosition(position = 32, converter = LoanTypeConverter.class)
  @UnifiedTableCell
  private LoanType permanentLoanType;

  @JsonProperty("permanentLoanTypeId")
  public void setPermanentLoanTypeId(String id) {
    if (nonNull(id)) {
      this.permanentLoanType = new LoanType().withId(id)
              .withName(ItemReferenceHelper.service().getLoanTypeById(id, tenantId).getName());
    }
  }

  @JsonProperty("permanentLoanTypeId")
  public String getPermanentLoanTypeId() {
    return permanentLoanType != null ? permanentLoanType.getId() : null;
  }

  @JsonProperty("permanentLoanType")
  public LoanType getPermanentLoanType() {
    return permanentLoanType;
  }

  @JsonProperty(TEMPORARY_LOAN_TYPE)
  @CsvCustomBindByName(column = "Temporary loan type", converter = LoanTypeConverter.class)
  @CsvCustomBindByPosition(position = 33, converter = LoanTypeConverter.class)
  @UnifiedTableCell
  private LoanType temporaryLoanType;

  @JsonProperty("temporaryLoanTypeId")
  public void setTemporaryLoanTypeId(String id) {
    if (nonNull(id)) {
      this.temporaryLoanType = new LoanType().withId(id)
              .withName(ItemReferenceHelper.service().getLoanTypeById(id, tenantId).getName());
    }
  }

  @JsonProperty("temporaryLoanTypeId")
  public String getTemporaryLoanTypeId() {
    return temporaryLoanType != null ? temporaryLoanType.getId() : null;
  }

  @JsonProperty("temporaryLoanType")
  public LoanType getTemporaryLoanType() {
    return temporaryLoanType;
  }

  @JsonProperty(STATUS)
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

  @JsonProperty(PERMANENT_LOCATION)
  @CsvCustomBindByName(column = "Item permanent location", converter = ItemLocationConverter.class)
  @CsvCustomBindByPosition(position = 37, converter = ItemLocationConverter.class)
  @UnifiedTableCell(visible = false)
  private ItemLocation permanentLocation;

  @JsonProperty("permanentLocationId")
  public void setPermanentLocationId(String id) {
    if (nonNull(id)) {
      this.permanentLocation = new ItemLocation().withId(id)
              .withName(ItemReferenceHelper.service().getLocationById(id, tenantId).getName());
    }
  }

  @JsonProperty("permanentLocationId")
  public String getPermanentLocationId() {
    return permanentLocation != null ? permanentLocation.getId() : null;
  }

  @JsonProperty("permanentLocation")
  public ItemLocation getPermanentLocation() {
    return permanentLocation;
  }

  @JsonProperty(TEMPORARY_LOCATION)
  @CsvCustomBindByName(column = "Item temporary location", converter = ItemLocationConverter.class)
  @CsvCustomBindByPosition(position = 38, converter = ItemLocationConverter.class)
  @UnifiedTableCell(visible = false)
  private ItemLocation temporaryLocation;

  @JsonProperty("temporaryLocationId")
  public void setTemporaryLocationId(String id) {
    if (nonNull(id)) {
      this.temporaryLocation = new ItemLocation().withId(id)
              .withName(ItemReferenceHelper.service().getLocationById(id, tenantId).getName());
    }
  }

  @JsonProperty("temporaryLocationId")
  public String getTemporaryLocationId() {
    return temporaryLocation != null ? temporaryLocation.getId() : null;
  }

  @JsonProperty("temporaryLocation")
  public ItemLocation getTemporaryLocation() {
    return temporaryLocation;
  }

  @JsonProperty(ELECTRONIC_ACCESS)
  @Valid
  @CsvCustomBindByName(column = "Electronic access",
          converter = ItemElectronicAccessListConverter.class)
  @CsvCustomBindByPosition(position = 39, converter = ItemElectronicAccessListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<ElectronicAccess> electronicAccess;

  @JsonProperty(IS_BOUND_WITH)
  @CsvCustomBindByName(column = "Is bound with", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 40, converter = BooleanConverter.class)
  @UnifiedTableCell(visible = false)
  private Boolean isBoundWith;

  @JsonProperty(BOUND_WITH_TITLES)
  @Valid
  @CsvCustomBindByName(column = "Bound with titles", converter = BoundWithTitlesConverter.class)
  @CsvCustomBindByPosition(position = 41, converter = BoundWithTitlesConverter.class)
  @UnifiedTableCell(visible = false)
  private List<Title> boundWithTitles = emptyList();

  @JsonProperty(TAGS)
  @CsvCustomBindByName(column = "Tags", converter = TagsConverter.class)
  @CsvCustomBindByPosition(position = 42, converter = TagsConverter.class)
  @UnifiedTableCell(visible = false)
  private Tags tags;

  @JsonProperty(HOLDINGS_RECORD_ID)
  @CsvCustomBindByName(column = "Holdings UUID", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 43, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String holdingsRecordId;

  @JsonProperty(TENANT_ID)
  @CsvCustomBindByName(column = "Tenant", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 44, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String tenantId;

  @JsonProperty("_version")
  private Integer version;

  @JsonProperty(CIRCULATION_NOTES)
  @Valid
  private List<CirculationNote> circulationNotes;

  @JsonProperty("displaySummary")
  private String displaySummary;

  @JsonProperty("inTransitDestinationServicePointId")
  private String inTransitDestinationServicePointId;

  @JsonProperty("lastCheckIn")
  private LastCheckIn lastCheckIn;

  @JsonProperty("purchaseOrderLineIdentifier")
  private String purchaseOrderLineIdentifier;

  @JsonProperty("additionalCallNumbers")
  private List<AdditionalCallNumber> additionalCallNumbers;

  @JsonProperty("customFields")
  private Map<String, Object> customFields;

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
  public Integer entityVersion() {
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
