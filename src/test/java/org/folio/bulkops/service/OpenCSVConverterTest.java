package org.folio.bulkops.service;

import static com.opencsv.ICSVWriter.DEFAULT_SEPARATOR;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.TestEntity;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.Tags;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.converter.CustomMappingStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class OpenCSVConverterTest extends BaseTest {

  private static class BulkOperationEntityClassProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
        Arguments.of(User.class, Item.class, HoldingsRecord.class)
      );
    }
  }

  //TODO TechDebt - Large test to cover object -> json -> object -> csv -> object transformation to prevent all the possible convertation issues
  @ParameterizedTest
  @Disabled
  @EnumSource(value = TestEntity.class, names = {"USER"}, mode = EnumSource.Mode.INCLUDE)
  void shouldConvertEntity(TestEntity entity) {
    Assertions.assertTrue(true);
  }

  @ParameterizedTest
  @ArgumentsSource(BulkOperationEntityClassProvider.class)
  void shouldConvertEmptyEntity(Class<BulkOperationsEntity> clazz) {

    BulkOperationsEntity bean;
    if (clazz.equals(User.class)) {
      bean = new User();
    } else if (clazz.equals(Item.class)) {
      bean = new Item().withVersion(1);
    } else {
      bean = new HoldingsRecord().withVersion(2);
    }

    var strategy = new CustomMappingStrategy<BulkOperationsEntity>();
    String csv = null;

    strategy.setType(clazz);

    try (Writer writer  = new StringWriter()) {

      StatefulBeanToCsv<BulkOperationsEntity> sbc = new StatefulBeanToCsvBuilder<BulkOperationsEntity>(writer)
        .withSeparator(DEFAULT_SEPARATOR)
        .withApplyQuotesToAll(false)
        .withMappingStrategy(strategy)
        .build();

      sbc.write(bean);
      csv = writer.toString();
    } catch (Exception e) {
      Assertions.fail("Error parsing bean to CSV");
    }

    List<BulkOperationsEntity> list = new ArrayList<>();

    try (Reader reader = new StringReader(csv)) {
      CsvToBean<BulkOperationsEntity> cb = new CsvToBeanBuilder<BulkOperationsEntity>(reader)
        .withType(clazz)
        .withSkipLines(1)
        .build();
      list = cb.parse().stream().toList();
    } catch (IOException e) {
      Assertions.fail("Error parsing CSV to bean");
    }

    assertThat(list, hasSize(1));
  }

  @Test
  @SneakyThrows
  void shouldConvertItemWithNullElementsInLists() {
    var itemString = "{\"id\":null,\"_version\":null,\"hrid\":null,\"holdingsRecordId\":null,\"formerIds\":[ null ],\"discoverySuppress\":false,\"title\":null,\"contributorNames\":[ null ],\"callNumber\":null,\"barcode\":null,\"effectiveShelvingOrder\":null,\"accessionNumber\":null,\"itemLevelCallNumber\":null,\"itemLevelCallNumberPrefix\":null,\"itemLevelCallNumberSuffix\":null,\"itemLevelCallNumberTypeId\":null,\"effectiveCallNumberComponents\":null,\"volume\":null,\"enumeration\":null,\"chronology\":null,\"yearCaption\":[ null ],\"itemIdentifier\":null,\"copyNumber\":null,\"numberOfPieces\":null,\"descriptionOfPieces\":null,\"numberOfMissingPieces\":null,\"missingPieces\":null,\"missingPiecesDate\":null,\"itemDamagedStatusId\":null,\"itemDamagedStatusDate\":null,\"administrativeNotes\":[ null ],\"notes\":[ null ],\"circulationNotes\":[ null ],\"status\":{\"name\":null,\"date\":null},\"materialType\":null,\"isBoundWith\":false,\"boundWithTitles\":[ null ],\"permanentLoanType\":null,\"temporaryLoanType\":null,\"permanentLocation\":null,\"temporaryLocation\":null,\"effectiveLocation\":null,\"electronicAccess\":[ null ],\"inTransitDestinationServicePointId\":null,\"statisticalCodeIds\":[ null ],\"purchaseOrderLineIdentifier\":null,\"metadata\":null,\"tags\":{\"tagList\":[ null ]},\"lastCheckIn\":null}";
    var itemFromJson = objectMapper.readValue(itemString, Item.class);

    String itemCsv;
    try (var writer = new StringWriter()) {
      var strategy = new CustomMappingStrategy<BulkOperationsEntity>();
      strategy.setType(Item.class);
      StatefulBeanToCsv<BulkOperationsEntity> sbc = new StatefulBeanToCsvBuilder<BulkOperationsEntity>(writer)
        .withSeparator(DEFAULT_SEPARATOR)
        .withApplyQuotesToAll(false)
        .withMappingStrategy(strategy)
        .build();
      sbc.write(itemFromJson);
      itemCsv = writer.toString();
    }

    assertNotNull(itemCsv);

    var expectedItemFromCsv = itemFromJson
      .withFormerIds(emptyList())
      .withDiscoverySuppress(false)
      .withContributorNames(emptyList())
      .withYearCaption(emptyList())
      .withAdministrativeNotes(emptyList())
      .withNotes(emptyList())
      .withCirculationNotes(emptyList())
      .withStatus(new InventoryItemStatus())
      .withIsBoundWith(false)
      .withBoundWithTitles(emptyList())
      .withElectronicAccess(emptyList())
      .withStatisticalCodeIds(emptyList())
      .withTags(new Tags());

    try (var reader = new StringReader(itemCsv)) {
      var iterator = new CsvToBeanBuilder<BulkOperationsEntity>(reader)
        .withType(Item.class)
        .withSkipLines(1)
        .build()
        .iterator();
      assertTrue(iterator.hasNext());
      assertEquals(expectedItemFromCsv, iterator.next());
    }
  }

  @Test
  @SneakyThrows
  void shouldConvertUserWithNullElementsInLists() {
    var userString = "{\"username\":null,\"id\":null,\"externalSystemId\":null,\"barcode\":null,\"active\":false,\"type\":null,\"patronGroup\":null,\"departments\":[ null ],\"meta\":null,\"proxyFor\":[ null ],\"personal\":{\"lastName\":null,\"firstName\":null,\"middleName\":null,\"preferredFirstName\":null,\"email\":null,\"phone\":null,\"mobilePhone\":null,\"dateOfBirth\":null,\"addresses\":[ null ],\"preferredContactTypeId\":null},\"enrollmentDate\":null,\"expirationDate\":null,\"createdDate\":null,\"updatedDate\":null,\"metadata\":null,\"tags\":{\"tagList\":null},\"customFields\":null}";
    var userFromJson = objectMapper.readValue(userString, User.class);

    String userCsv;
    try (var writer = new StringWriter()) {
      var strategy = new CustomMappingStrategy<BulkOperationsEntity>();
      strategy.setType(User.class);
      StatefulBeanToCsv<BulkOperationsEntity> sbc = new StatefulBeanToCsvBuilder<BulkOperationsEntity>(writer)
        .withSeparator(DEFAULT_SEPARATOR)
        .withApplyQuotesToAll(false)
        .withMappingStrategy(strategy)
        .build();
      sbc.write(userFromJson);
      userCsv = writer.toString();
    }

    assertNotNull(userCsv);

    var expectedUserFromCsv = userFromJson
      .withActive(false)
      .withDepartments(emptySet())
      .withProxyFor(emptyList())
      .withPersonal(Personal.builder().addresses(emptyList()).build())
      .withTags(new Tags());

    try (var reader = new StringReader(userCsv)) {
      var iterator = new CsvToBeanBuilder<BulkOperationsEntity>(reader)
        .withType(User.class)
        .withSkipLines(1)
        .build()
        .iterator();
      assertTrue(iterator.hasNext());
      assertEquals(objectMapper.writeValueAsString(expectedUserFromCsv), objectMapper.writeValueAsString(iterator.next()));
    }
  }

  @Test
  @SneakyThrows
  void shouldConvertHoldingsWithNullValuesInLists() {
    var holdingsString = "{\"id\":null,\"_version\":null,\"hrid\":null,\"holdingsTypeId\":null,\"formerIds\":[ null ],\"instanceId\":null,\"permanentLocationId\":null,\"permanentLocation\":null,\"temporaryLocationId\":null,\"effectiveLocationId\":null,\"electronicAccess\":[ null ],\"callNumberTypeId\":null,\"callNumberPrefix\":null,\"callNumber\":null,\"callNumberSuffix\":null,\"shelvingTitle\":null,\"acquisitionFormat\":null,\"acquisitionMethod\":null,\"receiptStatus\":null,\"administrativeNotes\":[ null ],\"notes\":[ null ],\"illPolicyId\":null,\"illPolicy\":null,\"retentionPolicy\":null,\"digitizationPolicy\":null,\"holdingsStatements\":[ null ],\"holdingsStatementsForIndexes\":[ null ],\"holdingsStatementsForSupplements\":[ null ],\"copyNumber\":null,\"numberOfItems\":null,\"receivingHistory\":null,\"discoverySuppress\":false,\"statisticalCodeIds\":[ null ],\"tags\":{\"tagList\":null},\"metadata\":null,\"sourceId\":null,\"instanceHrid\":null,\"itemBarcode\":null}";
    var holdingsFromJson = objectMapper.readValue(holdingsString, HoldingsRecord.class);

    String holdingsCsv;
    try (var writer = new StringWriter()) {
      var strategy = new CustomMappingStrategy<BulkOperationsEntity>();
      strategy.setType(HoldingsRecord.class);
      StatefulBeanToCsv<BulkOperationsEntity> sbc = new StatefulBeanToCsvBuilder<BulkOperationsEntity>(writer)
        .withSeparator(DEFAULT_SEPARATOR)
        .withApplyQuotesToAll(false)
        .withMappingStrategy(strategy)
        .build();
      sbc.write(holdingsFromJson);
      holdingsCsv = writer.toString();
    }

    assertNotNull(holdingsCsv);

    var expectedHoldingsFromCsv = holdingsFromJson
      .withFormerIds(emptyList())
      .withElectronicAccess(emptyList())
      .withAdministrativeNotes(emptyList())
      .withNotes(emptyList())
      .withHoldingsStatements(emptyList())
      .withHoldingsStatementsForIndexes(emptyList())
      .withHoldingsStatementsForSupplements(emptyList())
      .withDiscoverySuppress(false)
      .withStatisticalCodeIds(emptyList())
      .withTags(new Tags());

    try (var reader = new StringReader(holdingsCsv)) {
      var iterator = new CsvToBeanBuilder<BulkOperationsEntity>(reader)
        .withType(HoldingsRecord.class)
        .withSkipLines(1)
        .build()
        .iterator();
      assertTrue(iterator.hasNext());
      assertEquals(objectMapper.writeValueAsString(expectedHoldingsFromCsv), objectMapper.writeValueAsString(iterator.next()));
    }
  }
}
