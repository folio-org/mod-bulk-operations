package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import javax.validation.Valid;
import java.util.List;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("holdingsRecord")
public class HoldingsRecord  {

  @JsonProperty("id")
  private String id;

  @JsonProperty("_version")
  private Integer version;

  @JsonProperty("hrid")
  private String hrid;

  @JsonProperty("holdingsTypeId")
  private String holdingsTypeId;

  @JsonProperty("formerIds")
  @Valid
  private List<String> formerIds = null;

  @JsonProperty("instanceId")
  private String instanceId;

  @JsonProperty("permanentLocationId")
  private String permanentLocationId;

  @JsonProperty("permanentLocation")
  private ItemLocation permanentLocation;

  @JsonProperty("temporaryLocationId")
  private String temporaryLocationId;

  @JsonProperty("effectiveLocationId")
  private String effectiveLocationId;

  @JsonProperty("electronicAccess")
  @Valid
  private List<ElectronicAccess> electronicAccess = null;

  @JsonProperty("callNumberTypeId")
  private String callNumberTypeId;

  @JsonProperty("callNumberPrefix")
  private String callNumberPrefix;

  @JsonProperty("callNumber")
  private String callNumber;

  @JsonProperty("callNumberSuffix")
  private String callNumberSuffix;

  @JsonProperty("shelvingTitle")
  private String shelvingTitle;

  @JsonProperty("acquisitionFormat")
  private String acquisitionFormat;

  @JsonProperty("acquisitionMethod")
  private String acquisitionMethod;

  @JsonProperty("receiptStatus")
  private String receiptStatus;

  @JsonProperty("administrativeNotes")
  @Valid
  private List<String> administrativeNotes = null;

  @JsonProperty("notes")
  @Valid
  private List<HoldingsNote> notes = null;

  @JsonProperty("illPolicyId")
  private String illPolicyId;

  @JsonProperty("illPolicy")
  private IllPolicy illPolicy;

  @JsonProperty("retentionPolicy")
  private String retentionPolicy;

  @JsonProperty("digitizationPolicy")
  private String digitizationPolicy;

  @JsonProperty("holdingsStatements")
  @Valid
  private List<HoldingsStatement> holdingsStatements = null;

  @JsonProperty("holdingsStatementsForIndexes")
  @Valid
  private List<HoldingsStatement> holdingsStatementsForIndexes = null;

  @JsonProperty("holdingsStatementsForSupplements")
  @Valid
  private List<HoldingsStatement> holdingsStatementsForSupplements = null;

  @JsonProperty("copyNumber")
  private String copyNumber;

  @JsonProperty("numberOfItems")
  private String numberOfItems;

  @JsonProperty("receivingHistory")
  private ReceivingHistoryEntries receivingHistory;

  @JsonProperty("discoverySuppress")
  private Boolean discoverySuppress;

  @JsonProperty("statisticalCodeIds")
  @Valid
  private List<String> statisticalCodeIds = null;

  @JsonProperty("tags")
  private Tags tags;

  @JsonProperty("metadata")
  private Object metadata;

  @JsonProperty("sourceId")
  private String sourceId;
}
