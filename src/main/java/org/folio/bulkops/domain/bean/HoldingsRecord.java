package org.folio.bulkops.domain.bean;

import static java.lang.Boolean.FALSE;
import static java.util.Objects.isNull;

import java.util.List;

import org.folio.bulkops.domain.converter.BooleanConverter;
import org.folio.bulkops.domain.converter.CallNumberTypeConverter;
import org.folio.bulkops.domain.converter.ElectronicAccessListConverter;
import org.folio.bulkops.domain.converter.HoldingsLocationConverter;
import org.folio.bulkops.domain.converter.HoldingsNoteListConverter;
import org.folio.bulkops.domain.converter.HoldingsStatementListConverter;
import org.folio.bulkops.domain.converter.HoldingsStatisticalCodeListConverter;
import org.folio.bulkops.domain.converter.HoldingsTypeConverter;
import org.folio.bulkops.domain.converter.IllPolicyConverter;
import org.folio.bulkops.domain.converter.SourceConverter;
import org.folio.bulkops.domain.converter.StringConverter;
import org.folio.bulkops.domain.converter.StringListConverter;
import org.folio.bulkops.domain.converter.TagsConverter;
import org.folio.bulkops.domain.dto.IdentifierType;

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
@JsonTypeName("holdingsRecord")
@EqualsAndHashCode(exclude = {"metadata", "instanceId", "permanentLocation", "effectiveLocationId", "illPolicy", "instanceHrid", "itemBarcode"})
public class HoldingsRecord implements BulkOperationsEntity {

  @JsonProperty("id")
  @CsvCustomBindByName(column = "Holdings UUID", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 0, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String id;

  @JsonProperty("instanceTitle")
  @CsvCustomBindByName(column = "Instance (Title, Publisher, Publication date)", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 1, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String instanceTitle;

  @JsonProperty("discoverySuppress")
  @CsvCustomBindByName(column = "Suppress from discovery", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 2, converter = BooleanConverter.class)
  @UnifiedTableCell
  private Boolean discoverySuppress;

  @JsonProperty("hrid")
  @CsvCustomBindByName(column = "Holdings HRID", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 3, converter = StringConverter.class)
  @UnifiedTableCell
  private String hrid;

  @JsonProperty("sourceId")
  @CsvCustomBindByName(column = "Source", converter = SourceConverter.class)
  @CsvCustomBindByPosition(position = 4, converter = SourceConverter.class)
  @UnifiedTableCell
  private String sourceId;

  @JsonProperty("formerIds")
  @Valid
  @CsvCustomBindByName(column = "Former holdings Id", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 5, converter = StringListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> formerIds = null;

  @JsonProperty("holdingsTypeId")
  @CsvCustomBindByName(column = "Holdings type", converter = HoldingsTypeConverter.class)
  @CsvCustomBindByPosition(position = 6, converter = HoldingsTypeConverter.class)
  @UnifiedTableCell
  private String holdingsTypeId;

  @JsonProperty("statisticalCodeIds")
  @Valid
  @CsvCustomBindByName(column = "Statistical codes", converter = HoldingsStatisticalCodeListConverter.class)
  @CsvCustomBindByPosition(position = 7, converter = HoldingsStatisticalCodeListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> statisticalCodeIds = null;

  @JsonProperty("administrativeNotes")
  @Valid
  @CsvCustomBindByName(column = "Administrative note", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 8, converter = StringListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> administrativeNotes = null;

  @JsonProperty("permanentLocationId")
  @CsvCustomBindByName(column = "Holdings permanent location", converter = HoldingsLocationConverter.class)
  @CsvCustomBindByPosition(position = 9, converter = HoldingsLocationConverter.class)
  @UnifiedTableCell
  private String permanentLocationId;

  @JsonProperty("temporaryLocationId")
  @CsvCustomBindByName(column = "Holdings temporary location", converter = HoldingsLocationConverter.class)
  @CsvCustomBindByPosition(position = 10, converter = HoldingsLocationConverter.class)
  @UnifiedTableCell(visible = false)
  private String temporaryLocationId;

  @JsonProperty("shelvingTitle")
  @CsvCustomBindByName(column = "Shelving title", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 11, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String shelvingTitle;

  @JsonProperty("copyNumber")
  @CsvCustomBindByName(column = "Holdings copy number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 12, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String copyNumber;

  @JsonProperty("callNumberTypeId")
  @CsvCustomBindByName(column = "Holdings level call number type", converter = CallNumberTypeConverter.class)
  @CsvCustomBindByPosition(position = 13, converter = CallNumberTypeConverter.class)
  @UnifiedTableCell(visible = false)
  private String callNumberTypeId;

  @JsonProperty("callNumberPrefix")
  @CsvCustomBindByName(column = "Holdings level call number prefix", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 14, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String callNumberPrefix;

  @JsonProperty("callNumber")
  @CsvCustomBindByName(column = "Holdings level call number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 15, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String callNumber;

  @JsonProperty("callNumberSuffix")
  @CsvCustomBindByName(column = "Holdings level call number suffix", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 16, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String callNumberSuffix;

  @JsonProperty("numberOfItems")
  @CsvCustomBindByName(column = "Number of items", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 17, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String numberOfItems;

  @JsonProperty("holdingsStatements")
  @Valid
  @CsvCustomBindByName(column = "Holdings statement", converter = HoldingsStatementListConverter.class)
  @CsvCustomBindByPosition(position = 18, converter = HoldingsStatementListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<HoldingsStatement> holdingsStatements = null;

  @JsonProperty("holdingsStatementsForSupplements")
  @Valid
  @CsvCustomBindByName(column = "Holdings statement for supplements", converter = HoldingsStatementListConverter.class)
  @CsvCustomBindByPosition(position = 19, converter = HoldingsStatementListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<HoldingsStatement> holdingsStatementsForSupplements = null;

  @JsonProperty("holdingsStatementsForIndexes")
  @Valid
  @CsvCustomBindByName(column = "Holdings statement for indexes", converter = HoldingsStatementListConverter.class)
  @CsvCustomBindByPosition(position = 20, converter = HoldingsStatementListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<HoldingsStatement> holdingsStatementsForIndexes = null;

  @JsonProperty("illPolicyId")
  @CsvCustomBindByName(column = "ILL policy", converter = IllPolicyConverter.class)
  @CsvCustomBindByPosition(position = 21, converter = IllPolicyConverter.class)
  @UnifiedTableCell(visible = false)
  private String illPolicyId;

  @JsonProperty("digitizationPolicy")
  @CsvCustomBindByName(column = "Digitization policy", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 22, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String digitizationPolicy;

  @JsonProperty("retentionPolicy")
  @CsvCustomBindByName(column = "Retention policy", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 23, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String retentionPolicy;

  @JsonProperty("notes")
  @CsvCustomBindByName(column = "Notes", converter = HoldingsNoteListConverter.class)
  @CsvCustomBindByPosition(position = 24, converter = HoldingsNoteListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<HoldingsNote> notes;

  @JsonProperty("electronicAccess")
  @Valid
  @CsvCustomBindByName(column = "Electronic access", converter = ElectronicAccessListConverter.class)
  @CsvCustomBindByPosition(position = 25, converter = ElectronicAccessListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<ElectronicAccess> electronicAccess = null;

  @JsonProperty("acquisitionMethod")
  @CsvCustomBindByName(column = "Acquisition method", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 26, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String acquisitionMethod;

  @JsonProperty("acquisitionFormat")
  @CsvCustomBindByName(column = "Order format", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 27, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String acquisitionFormat;

  @JsonProperty("receiptStatus")
  @CsvCustomBindByName(column = "Receipt status", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 28, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String receiptStatus;

  @JsonProperty("tags")
  @CsvCustomBindByName(column = "Tags", converter = TagsConverter.class)
  @CsvCustomBindByPosition(position = 29, converter = TagsConverter.class)
  @UnifiedTableCell(visible = false)
  private Tags tags;

  @JsonProperty("_version")
  private Integer version;

  @JsonProperty("instanceId")
  private String instanceId;

  @JsonProperty("effectiveLocationId")
  private String effectiveLocationId;

  @JsonProperty("illPolicy")
  private IllPolicy illPolicy;

  @JsonProperty("receivingHistory")
  private ReceivingHistoryEntries receivingHistory;

  @JsonProperty("metadata")
  private Metadata metadata;

  @JsonProperty("instanceHrid")
  private String instanceHrid;

  @JsonProperty("itemBarcode")
  private String itemBarcode;

  @JsonProperty("tenantId")
  private String tenantId;

  @Override
  public String getIdentifier(IdentifierType identifierType) {
    return switch (identifierType) {
    case HRID -> hrid;
    case INSTANCE_HRID -> instanceHrid;
    case ITEM_BARCODE -> itemBarcode;
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
  public void setTenantToNotes() {
    getNotes().forEach(note -> note.setTenantId(tenantId));
  }

  @Override
  public void setTenant(String tenantId) {
    this.tenantId = tenantId;
  }
}
