package org.folio.bulkops.domain.bean;

import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

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
  private String id;

  @JsonProperty("_version")
  private Integer version;

  @JsonProperty("hrid")
  private String hrid;

  @JsonProperty("holdingsRecordId")
  private String holdingsRecordId;

  @JsonProperty("formerIds")
  @Valid
  private List<String> formerIds = null;

  @JsonProperty("discoverySuppress")
  private Boolean discoverySuppress;

  @JsonProperty("title")
  private String title;

  @JsonProperty("contributorNames")
  @Valid
  private List<ContributorName> contributorNames = null;

  @JsonProperty("callNumber")
  private String callNumber;

  @JsonProperty("barcode")
  private String barcode;

  @JsonProperty("effectiveShelvingOrder")
  private String effectiveShelvingOrder;

  @JsonProperty("accessionNumber")
  private String accessionNumber;

  @JsonProperty("itemLevelCallNumber")
  private String itemLevelCallNumber;

  @JsonProperty("itemLevelCallNumberPrefix")
  private String itemLevelCallNumberPrefix;

  @JsonProperty("itemLevelCallNumberSuffix")
  private String itemLevelCallNumberSuffix;

  @JsonProperty("itemLevelCallNumberTypeId")
  private String itemLevelCallNumberTypeId;

  @JsonProperty("effectiveCallNumberComponents")
  private EffectiveCallNumberComponents effectiveCallNumberComponents;

  @JsonProperty("volume")
  private String volume;

  @JsonProperty("enumeration")
  private String enumeration;

  @JsonProperty("chronology")
  private String chronology;

  @JsonProperty("yearCaption")
  @Valid
  private List<String> yearCaption = null;

  @JsonProperty("itemIdentifier")
  private String itemIdentifier;

  @JsonProperty("copyNumber")
  private String copyNumber;

  @JsonProperty("numberOfPieces")
  private String numberOfPieces;

  @JsonProperty("descriptionOfPieces")
  private String descriptionOfPieces;

  @JsonProperty("numberOfMissingPieces")
  private String numberOfMissingPieces;

  @JsonProperty("missingPieces")
  private String missingPieces;

  @JsonProperty("missingPiecesDate")
  private String missingPiecesDate;

  @JsonProperty("itemDamagedStatusId")
  private String itemDamagedStatusId;

  @JsonProperty("itemDamagedStatusDate")
  private String itemDamagedStatusDate;

  @JsonProperty("administrativeNotes")
  @Valid
  private List<String> administrativeNotes = null;

  @JsonProperty("notes")
  @Valid
  private List<ItemNote> notes = null;

  @JsonProperty("circulationNotes")
  @Valid
  private List<CirculationNote> circulationNotes = null;

  @JsonProperty("status")
  private InventoryItemStatus status;

  @JsonProperty("materialType")
  private MaterialType materialType;

  @JsonProperty("isBoundWith")
  private Boolean isBoundWith;

  @JsonProperty("boundWithTitles")
  @Valid
  private List<Title> boundWithTitles = null;

  @JsonProperty("permanentLoanType")
  private LoanType permanentLoanType;

  @JsonProperty("temporaryLoanType")
  private LoanType temporaryLoanType;

  @JsonProperty("permanentLocation")
  private ItemLocation permanentLocation;

  @JsonProperty("temporaryLocation")
  private ItemLocation temporaryLocation;

  @JsonProperty("effectiveLocation")
  private ItemLocation effectiveLocation;

  @JsonProperty("electronicAccess")
  @Valid
  private List<ElectronicAccess> electronicAccess = null;

  @JsonProperty("inTransitDestinationServicePointId")
  private String inTransitDestinationServicePointId;

  @JsonProperty("statisticalCodeIds")
  @Valid
  private List<String> statisticalCodeIds = null;

  @JsonProperty("purchaseOrderLineIdentifier")
  private String purchaseOrderLineIdentifier;

  @JsonProperty("metadata")
  private Metadata metadata;

  @JsonProperty("tags")
  private Tags tags;

  @JsonProperty("lastCheckIn")
  private LastCheckIn lastCheckIn;
}
