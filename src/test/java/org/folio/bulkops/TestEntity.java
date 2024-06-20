package org.folio.bulkops;

import static java.util.UUID.randomUUID;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.CirculationNote;
import org.folio.bulkops.domain.bean.ContributorName;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.domain.bean.HoldingsNote;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsStatement;
import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.ItemNote;
import org.folio.bulkops.domain.bean.LastCheckIn;
import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.domain.bean.MaterialType;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.ReceivingHistoryEntries;
import org.folio.bulkops.domain.bean.Tags;
import org.folio.bulkops.domain.bean.User;

public enum TestEntity {
  USER {
    @Override
    public BulkOperationsEntity entity() {
      return User.builder()
        .id(randomUUID().toString())
        .username("username")
        .externalSystemId("external-system-id")
        .barcode("123456789")
        .active(Boolean.TRUE)
        .type("type")
        .patronGroup("patron-group")
        .departments(Set.of(randomUUID(), randomUUID()))
        .proxyFor(List.of("proxy-for-1", "proxy-for-2"))
        .personal(new Personal())
        .enrollmentDate(new Date())
        .expirationDate(new Date())
        .tags(new Tags())
        .customFields(Map.of())
        .build();
    }

    @Override
    public Class<? extends BulkOperationsEntity> getEntityClass() {
      return User.class;
    }

  },
  ITEM {
    @Override
    public BulkOperationsEntity entity() {
      return Item.builder()
        .id(randomUUID().toString())
        .version(2)
        .hrid(randomUUID().toString())
        .holdingsRecordId(randomUUID().toString())
        .formerIds(List.of(randomUUID().toString(), randomUUID().toString()))
        .discoverySuppress(Boolean.TRUE)
        .title("Title")
//        .contributorNames(List.of(ContributorName.builder().name("Name_1").build(), ContributorName.builder().name("Name_2").build()))
//        .callNumber("+123-456-78-90")
        .barcode("123456789")
        .effectiveShelvingOrder("123")
        .accessionNumber("accession-number")
        .itemLevelCallNumber("+123-456-789")
        .itemLevelCallNumberPrefix("+123")
        .itemLevelCallNumberSuffix("456")
        .itemLevelCallNumberType("ref-id")
        .volume("V123")
        .enumeration("E123")
        .chronology("H123")
        .yearCaption(List.of("1958", "1985"))
        .itemIdentifier("ID-123")
        .copyNumber("5")
        .numberOfPieces("3")
        .descriptionOfPieces("description")
        .numberOfMissingPieces("1")
        .missingPieces("missing")
        .missingPiecesDate("01-01-2000")
        .itemDamagedStatus("damaged-status-id")
        .itemDamagedStatusDate("01-2001")
        .administrativeNotes(List.of("administrative-note-1", "administrative-note-2"))
        .notes(List.of(ItemNote.builder().build()))
//        .circulationNotes(List.of(CirculationNote.builder().build()))
        .status(InventoryItemStatus.builder().build())
        .materialType(MaterialType.builder().build())
        .permanentLoanType(LoanType.builder().build())
        .temporaryLoanType(LoanType.builder().build())
        .permanentLocation(ItemLocation.builder().build())
        .temporaryLocation(ItemLocation.builder().build())
        .effectiveLocation(ItemLocation.builder().build())
        .electronicAccess(List.of(ElectronicAccess.builder().build()))
//        .inTransitDestinationServicePointId(randomUUID().toString())
        .statisticalCodes(List.of(randomUUID().toString(), randomUUID().toString()))
//        .purchaseOrderLineIdentifier("purchase-ol-identifier")
        .tags(Tags.builder().build())
//        .lastCheckIn(LastCheckIn.builder().build())
        .build();
    }

    @Override
    public Class<? extends BulkOperationsEntity> getEntityClass() {
      return Item.class;
    }

  },
  HOLDING {
    @Override
    public BulkOperationsEntity entity() {
      return HoldingsRecord.builder()
        .id(randomUUID().toString())
        .version(3)
        .hrid(randomUUID().toString())
        .holdingsTypeId(randomUUID().toString())
        .formerIds(List.of(randomUUID().toString(), randomUUID().toString()))
        .instanceId(randomUUID().toString())
        .permanentLocationId(randomUUID().toString())
        .temporaryLocationId(randomUUID().toString())
        .effectiveLocationId(randomUUID().toString())
        .electronicAccess(List.of(ElectronicAccess.builder().build(), ElectronicAccess.builder().build()))
        .callNumberTypeId("call-number-type-id")
        .callNumber("+123-456-789")
        .callNumberPrefix("+123")
        .callNumberSuffix("456")
        .shelvingTitle("shelving-title")
        .acquisitionFormat("acquisition-format")
        .acquisitionMethod("acquisition-method")
        .receiptStatus("receipt-status")
        .administrativeNotes(List.of("administrative-note-1", "administrative-note-2"))
        .notes(List.of(HoldingsNote.builder().build()))
        .illPolicyId("ill-policy-id")
        .retentionPolicy("retention-policy")
        .digitizationPolicy("digitization-policy")
        .holdingsStatements(List.of(HoldingsStatement.builder().build(), HoldingsStatement.builder().build()))
        .holdingsStatementsForIndexes(List.of(HoldingsStatement.builder().build(), HoldingsStatement.builder().build()))
        .holdingsStatementsForSupplements(List.of(HoldingsStatement.builder().build(), HoldingsStatement.builder().build()))
        .copyNumber("copy-number")
        .numberOfItems("5")
        .receivingHistory(ReceivingHistoryEntries.builder().build())
        .discoverySuppress(true)
        .statisticalCodeIds(List.of("statistical-code-1", "statistical-code-2"))
        .tags(Tags.builder().build())
        .sourceId("source-id")
        .build();
    }

    @Override
    public Class<? extends BulkOperationsEntity> getEntityClass() {
      return HoldingsRecord.class;
    }

  };
  public abstract BulkOperationsEntity entity();
  public abstract Class<? extends BulkOperationsEntity> getEntityClass();
}
