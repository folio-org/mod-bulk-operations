package org.folio.bulkops.processor;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOAN_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOCATION;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STATUS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOAN_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOCATION;
import static org.folio.bulkops.processor.ItemDataProcessor.BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE;
import static org.folio.bulkops.processor.ItemDataProcessor.MODULE_NAME;
import static org.folio.bulkops.processor.ItemDataProcessor.STATUSES_CONFIG_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.ConfigurationCollection;
import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.domain.bean.ModelConfiguration;
import org.folio.bulkops.domain.bean.ResultInfo;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationCollection;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ItemDataProcessorTest extends BaseTest {

  @Autowired
  DataProcessorFactory factory;

  private DataProcessor<Item> processor;

  @MockBean
  private BulkOperationExecutionContentRepository bulkOperationExecutionContentRepository;

  public static final String IDENTIFIER = "123";

  @BeforeEach
  void setUp() {
    if (isNull(processor)) {
      processor = factory.getProcessorFromFactory(Item.class);
    }
    when(configurationClient.getConfigurations(String.format(BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE, MODULE_NAME, STATUSES_CONFIG_NAME)))
      .thenReturn(
        new ConfigurationCollection()
          .withConfigs(List.of(new ModelConfiguration()
            .withId("6e2fcd41-3d6e-40e7-871d-4ae2bd494a59")
            .withModule("BULKEDIT")
            .withConfigName("statuses")
            .with_default(true)
            .withEnabled(true)
            .withValue("{\"Available\":[\"Missing\"],\"Missing\":[\"Withdrawn\"]}")))
          .withTotalRecords(1)
          .withResultInfo(new ResultInfo()
            .withTotalRecords(1)));
  }

  @Test
  void testClearItemStatus() {
    assertNull(processor.process(IDENTIFIER, new Item(), rules(rule(STATUS, CLEAR_FIELD, null))));
  }

  @Test
  void testClearItemLocationAndLoanType() {
    var item = new Item()
      .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Permanent location"))
      .withTemporaryLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Temporary location"))
      .withPermanentLoanType(new LoanType().withId(UUID.randomUUID().toString()).withName("Permanent loan type"));

    var rules = rules(rule(PERMANENT_LOCATION, CLEAR_FIELD, null),
      rule(TEMPORARY_LOCATION, CLEAR_FIELD, null), rule(TEMPORARY_LOAN_TYPE, CLEAR_FIELD, null));

    var result = processor.process(IDENTIFIER, item, rules);
    assertNotNull(result);
    assertNull(result.getEntity().getPermanentLocation());
    assertNull(result.getEntity().getTemporaryLocation());
    assertNull(result.getEntity().getTemporaryLoanType());
  }

  @Test
  void testUpdateItemAndLoanTypeLocation() {
    var updatedLocationId = "dc3868f6-6169-47b2-88a7-71c2e9e4e924";
    var updatedLocation = new ItemLocation()
      .withId(updatedLocationId)
      .withName("New location");

    var updatedLoanTypeId = "2c2857f6-381a-4782-9385-3524ebef8b69";
    var updatedLoanType = new LoanType()
      .withId(updatedLoanTypeId)
      .withName("New loan type");

    when(locationClient.getLocationById(updatedLocationId)).thenReturn(updatedLocation);
    when(loanTypeClient.getLoanTypeById(updatedLoanTypeId)).thenReturn(updatedLoanType);

    var item = new Item()
      .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Permanent location"))
      .withTemporaryLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Temporary location"))
      .withPermanentLoanType(new LoanType().withId(UUID.randomUUID().toString()).withName("Permanent loan type"))
      .withTemporaryLoanType(new LoanType().withId(UUID.randomUUID().toString()).withName("Temporary loan type"));

    var rules = rules(rule(PERMANENT_LOCATION, REPLACE_WITH, updatedLocationId),
      rule(TEMPORARY_LOCATION, REPLACE_WITH, updatedLocationId), rule(PERMANENT_LOAN_TYPE, REPLACE_WITH, updatedLoanTypeId),
      rule(TEMPORARY_LOAN_TYPE, REPLACE_WITH, updatedLoanTypeId));

    var result = processor.process(IDENTIFIER, item, rules);

    assertNotNull(result);
    assertEquals(updatedLocationId, result.getEntity().getPermanentLocation().getId());
    assertEquals(updatedLocationId, result.getEntity().getTemporaryLocation().getId());
    assertEquals(updatedLoanTypeId, result.getEntity().getPermanentLoanType().getId());
    assertEquals(updatedLoanTypeId, result.getEntity().getTemporaryLoanType().getId());
  }

  @Test
  void testClearPermanentLoanType() {
    assertNull(processor.process(IDENTIFIER, new Item(), rules(rule(PERMANENT_LOAN_TYPE, CLEAR_FIELD, null))));
  }

  @Test
  void testReplaceLoanTypeWithEmptyValue() {
    assertNull(processor.process(IDENTIFIER, new Item(), rules(rule(PERMANENT_LOAN_TYPE, REPLACE_WITH, null))));
  }

  @Test
  void testUpdateAllowedItemStatus() {
    var item = new Item()
      .withStatus(new InventoryItemStatus().withName(InventoryItemStatus.NameEnum.AVAILABLE));

    var rules = rules(rule(STATUS, REPLACE_WITH, InventoryItemStatus.NameEnum.MISSING.getValue()));
    var result = processor.process(IDENTIFIER, item, rules);

    assertNotNull(result);
    assertEquals(InventoryItemStatus.NameEnum.MISSING, result.getEntity().getStatus().getName());
    assertNotNull(result.getEntity().getStatus().getDate());
  }

  @Test
  void testUpdateRestrictedItemStatus() {
    assertNull(processor.process(IDENTIFIER, new Item()
      .withStatus(new InventoryItemStatus().withName(InventoryItemStatus.NameEnum.AGED_TO_LOST)), rules(rule(STATUS, REPLACE_WITH, InventoryItemStatus.NameEnum.MISSING.getValue()))));

    assertNull(processor.process(IDENTIFIER, new Item()
      .withStatus(new InventoryItemStatus().withName(InventoryItemStatus.NameEnum.AVAILABLE)), rules(rule(STATUS, REPLACE_WITH, InventoryItemStatus.NameEnum.IN_TRANSIT.getValue()))));
  }
}
