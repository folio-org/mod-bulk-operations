package org.folio.bulkops.domain.bean;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.folio.bulkops.domain.converter.BooleanConverter;
import org.folio.bulkops.domain.converter.CallNumberTypeConverter;
import org.folio.bulkops.domain.converter.ElectronicAccessListConverter;
import org.folio.bulkops.domain.converter.HoldingsLocationConverter;
import org.folio.bulkops.domain.converter.HoldingsNoteListConverter;
import org.folio.bulkops.domain.converter.HoldingsStatementListConverter;
import org.folio.bulkops.domain.converter.HoldingsStatisticalCodeListConverter;
import org.folio.bulkops.domain.converter.HoldingsTypeConverter;
import org.folio.bulkops.domain.converter.IllPolicyConverter;
import org.folio.bulkops.domain.converter.InstanceConverter;
import org.folio.bulkops.domain.converter.IntegerConverter;
import org.folio.bulkops.domain.converter.ReceivingHistoryConverter;
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
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("holdingsRecord")
public class HoldingsRecord extends BulkOperationsEntity {

  @JsonProperty("id")
  @CsvCustomBindByName(column = "Holdings record id", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 0, converter = StringConverter.class)
  private String id;

  @JsonProperty("_version")
  @CsvCustomBindByName(column = "Version", converter = IntegerConverter.class)
  @CsvCustomBindByPosition(position = 1, converter = IntegerConverter.class)
  private Integer version;

  @JsonProperty("hrid")
  @CsvCustomBindByName(column = "HRID", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 2, converter = StringConverter.class)
  private String hrid;

  @JsonProperty("holdingsTypeId")
  @CsvCustomBindByName(column = "Holdings type", converter = HoldingsTypeConverter.class)
  @CsvCustomBindByPosition(position = 3, converter = HoldingsTypeConverter.class)
  private String holdingsTypeId;

  @JsonProperty("formerIds")
  @Valid
  @CsvCustomBindByName(column = "Former ids", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 4, converter = StringListConverter.class)
  private List<String> formerIds = null;

  @JsonProperty("instanceId")
  @CsvCustomBindByName(column = "Instance", converter = InstanceConverter.class)
  @CsvCustomBindByPosition(position = 5, converter = InstanceConverter.class)
  private String instanceId;

  @JsonProperty("permanentLocationId")
  @CsvCustomBindByName(column = "Permanent location", converter = HoldingsLocationConverter.class)
  @CsvCustomBindByPosition(position = 6, converter = HoldingsLocationConverter.class)
  private String permanentLocationId;

  @JsonProperty("permanentLocation")
  private ItemLocation permanentLocation;

  @JsonProperty("temporaryLocationId")
  @CsvCustomBindByName(column = "Temporary location", converter = HoldingsLocationConverter.class)
  @CsvCustomBindByPosition(position = 7, converter = HoldingsLocationConverter.class)
  private String temporaryLocationId;

  @JsonProperty("effectiveLocationId")
  @CsvCustomBindByName(column = "Effective location", converter = HoldingsLocationConverter.class)
  @CsvCustomBindByPosition(position = 8, converter = HoldingsLocationConverter.class)
  private String effectiveLocationId;

  @JsonProperty("electronicAccess")
  @Valid
  @CsvCustomBindByName(column = "Electronic access", converter = ElectronicAccessListConverter.class)
  @CsvCustomBindByPosition(position = 9, converter = ElectronicAccessListConverter.class)
  private List<ElectronicAccess> electronicAccess = null;

  @JsonProperty("callNumberTypeId")
  @CsvCustomBindByName(column = "Call number type", converter = CallNumberTypeConverter.class)
  @CsvCustomBindByPosition(position = 10, converter = CallNumberTypeConverter.class)
  private String callNumberTypeId;

  @JsonProperty("callNumberPrefix")
  @CsvCustomBindByName(column = "Call number prefix", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 11, converter = StringConverter.class)
  private String callNumberPrefix;

  @JsonProperty("callNumber")
  @CsvCustomBindByName(column = "Call number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 12, converter = StringConverter.class)
  private String callNumber;

  @JsonProperty("callNumberSuffix")
  @CsvCustomBindByName(column = "Call number suffix", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 13, converter = StringConverter.class)
  private String callNumberSuffix;

  @JsonProperty("shelvingTitle")
  @CsvCustomBindByName(column = "Shelving title", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 14, converter = StringConverter.class)
  private String shelvingTitle;

  @JsonProperty("acquisitionFormat")
  @CsvCustomBindByName(column = "Acquisition format", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 15, converter = StringConverter.class)
  private String acquisitionFormat;

  @JsonProperty("acquisitionMethod")
  @CsvCustomBindByName(column = "Acquisition method", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 16, converter = StringConverter.class)
  private String acquisitionMethod;

  @JsonProperty("receiptStatus")
  @CsvCustomBindByName(column = "Receipt status", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 17, converter = StringConverter.class)
  private String receiptStatus;

  @JsonProperty("administrativeNotes")
  @Valid
  @CsvCustomBindByName(column = "Administrative notes", converter = StringListConverter.class)
  @CsvCustomBindByPosition(position = 18, converter = StringListConverter.class)
  private List<String> administrativeNotes = null;

  @JsonProperty("notes")
  @Valid
  @CsvCustomBindByName(column = "Notes", converter = HoldingsNoteListConverter.class)
  @CsvCustomBindByPosition(position = 19, converter = HoldingsNoteListConverter.class)
  private List<HoldingsNote> notes = null;

  @JsonProperty("illPolicyId")
  @CsvCustomBindByName(column = "Ill policy", converter = IllPolicyConverter.class)
  @CsvCustomBindByPosition(position = 20, converter = IllPolicyConverter.class)
  private String illPolicyId;

  @JsonProperty("illPolicy")
  private IllPolicy illPolicy;

  @JsonProperty("retentionPolicy")
  @CsvCustomBindByName(column = "Retention policy", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 21, converter = StringConverter.class)
  private String retentionPolicy;

  @JsonProperty("digitizationPolicy")
  @CsvCustomBindByName(column = "Digitization policy", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 22, converter = StringConverter.class)
  private String digitizationPolicy;

  @JsonProperty("holdingsStatements")
  @Valid
  @CsvCustomBindByName(column = "Holdings statements", converter = HoldingsStatementListConverter.class)
  @CsvCustomBindByPosition(position = 23, converter = HoldingsStatementListConverter.class)
  private List<HoldingsStatement> holdingsStatements = null;

  @JsonProperty("holdingsStatementsForIndexes")
  @Valid
  @CsvCustomBindByName(column = "Holdings statements for indexes", converter = HoldingsStatementListConverter.class)
  @CsvCustomBindByPosition(position = 24, converter = HoldingsStatementListConverter.class)
  private List<HoldingsStatement> holdingsStatementsForIndexes = null;

  @JsonProperty("holdingsStatementsForSupplements")
  @Valid
  @CsvCustomBindByName(column = "Holdings statements for supplements", converter = HoldingsStatementListConverter.class)
  @CsvCustomBindByPosition(position = 25, converter = HoldingsStatementListConverter.class)
  private List<HoldingsStatement> holdingsStatementsForSupplements = null;

  @JsonProperty("copyNumber")
  @CsvCustomBindByName(column = "Copy number", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 26, converter = StringConverter.class)
  private String copyNumber;

  @JsonProperty("numberOfItems")
  @CsvCustomBindByName(column = "Number of items", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 27, converter = StringConverter.class)
  private String numberOfItems;

  @JsonProperty("receivingHistory")
  @CsvCustomBindByName(column = "Receiving history", converter = ReceivingHistoryConverter.class)
  @CsvCustomBindByPosition(position = 28, converter = ReceivingHistoryConverter.class)
  private ReceivingHistoryEntries receivingHistory;

  @JsonProperty("discoverySuppress")
  @CsvCustomBindByName(column = "Discovery suppress", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 29, converter = BooleanConverter.class)
  private Boolean discoverySuppress;

  @JsonProperty("statisticalCodeIds")
  @Valid
  @CsvCustomBindByName(column = "Statistical codes", converter = HoldingsStatisticalCodeListConverter.class)
  @CsvCustomBindByPosition(position = 30, converter = HoldingsStatisticalCodeListConverter.class)
  private List<String> statisticalCodeIds = null;

  @JsonProperty("tags")
  @CsvCustomBindByName(column = "Tags", converter = TagsConverter.class)
  @CsvCustomBindByPosition(position = 31, converter = TagsConverter.class)
  private Tags tags;

  @JsonProperty("metadata")
  private Metadata metadata;

  @JsonProperty("sourceId")
  @CsvCustomBindByName(column = "Source", converter = SourceConverter.class)
  @CsvCustomBindByPosition(position = 32, converter = SourceConverter.class)
  private String sourceId;

  @JsonProperty("instanceHrid")
  @CsvCustomBindByName(column = "Instance HRID", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 33, converter = StringConverter.class)
  private String instanceHrid;

  @JsonProperty("itemBarcode")
  @CsvCustomBindByName(column = "Item barcode", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 34, converter = StringConverter.class)
  private String itemBarcode;

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
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o, true, HoldingsRecord.class, "metadata", "permanentLocation");
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this, "metadata");
  }
}
