package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.domain.bean.MaterialType;
import org.folio.bulkops.domain.bean.StatisticalCode;
import org.folio.bulkops.exception.ReferenceDataNotFoundException;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

@ExtendWith(MockitoExtension.class)
class ItemReferenceServiceTest extends BaseTest {
  @Autowired private ItemReferenceService itemReferenceService;
  @Autowired private FolioModuleMetadata folioModuleMetadata;

  @Test
  @SneakyThrows
  void getLocationByIdReturnsCorrectLocation() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(locationClient.getLocationById("valid_id"))
          .thenReturn(new ItemLocation().withName("Valid Location"));
      var actual = itemReferenceService.getLocationById("valid_id", "tenant");
      assertEquals("Valid Location", actual.getName());
    }
  }

  @Test
  @SneakyThrows
  void getLocationByIdUsesDefaultTenantWhenTenantIdIsNull() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(locationClient.getLocationById("valid_id"))
          .thenReturn(new ItemLocation().withName("Default Tenant Location"));
      var actual = itemReferenceService.getLocationById("valid_id", null);
      assertEquals("Default Tenant Location", actual.getName());
    }
  }

  @Test
  @SneakyThrows
  void getLocationByIdThrowsExceptionForInvalidId() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(locationClient.getLocationById("invalid_id"))
          .thenThrow(new ReferenceDataNotFoundException("Not found"));

      assertThrows(
          ReferenceDataNotFoundException.class,
          () -> itemReferenceService.getLocationById("invalid_id", "tenant"));
    }
  }

  @Test
  @SneakyThrows
  void getMaterialTypeByIdReturnsCorrectMaterialType() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(materialTypeClient.getById("valid_id"))
          .thenReturn(new MaterialType().withName("Material Type 1"));
      var actual = itemReferenceService.getMaterialTypeById("valid_id", "tenant");
      assertEquals("Material Type 1", actual.getName());
    }
  }

  @Test
  @SneakyThrows
  void getMaterialTypeByIdUsesDefaultTenantWhenTenantIdIsNull() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(materialTypeClient.getById("valid_id"))
          .thenReturn(new MaterialType().withName("Default Tenant Material Type"));
      var actual = itemReferenceService.getMaterialTypeById("valid_id", null);
      assertEquals("Default Tenant Material Type", actual.getName());
    }
  }

  @Test
  @SneakyThrows
  void getMaterialTypeByIdThrowsExceptionForInvalidId() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(materialTypeClient.getById("invalid_id"))
          .thenThrow(new ReferenceDataNotFoundException("Not found"));

      assertThrows(
          ReferenceDataNotFoundException.class,
          () -> itemReferenceService.getMaterialTypeById("invalid_id", "tenant"));
    }
  }

  @Test
  @SneakyThrows
  void getLoanTypeByIdReturnsCorrectLoanType() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(loanTypeClient.getLoanTypeById("valid_id"))
          .thenReturn(new LoanType().withName("Loan Type 1"));
      var actual = itemReferenceService.getLoanTypeById("valid_id", "tenant");
      assertEquals("Loan Type 1", actual.getName());
    }
  }

  @Test
  @SneakyThrows
  void getLoanTypeByIdUsesDefaultTenantWhenTenantIdIsNull() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(loanTypeClient.getLoanTypeById("valid_id"))
          .thenReturn(new LoanType().withName("Default Tenant Loan Type"));
      var actual = itemReferenceService.getLoanTypeById("valid_id", null);
      assertEquals("Default Tenant Loan Type", actual.getName());
    }
  }

  @Test
  @SneakyThrows
  void getLoanTypeByIdThrowsExceptionForInvalidId() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(loanTypeClient.getLoanTypeById("invalid_id"))
          .thenThrow(new ReferenceDataNotFoundException("Not found"));

      assertThrows(
          ReferenceDataNotFoundException.class,
          () -> itemReferenceService.getLoanTypeById("invalid_id", "tenant"));
    }
  }

  @Test
  @SneakyThrows
  void shouldGetAllowedItemStatuses() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var statuses = itemReferenceService.getAllowedStatuses("Available");
      assertThat(statuses).hasSize(9);
    }
  }

  @Test
  @SneakyThrows
  void shouldGetEmptyAllowedItemStatusesForInvalidStatus() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var statuses = itemReferenceService.getAllowedStatuses("INVALID");
      assertThat(statuses).isEmpty();
    }
  }

  @Test
  @SneakyThrows
  void getStatisticalCodeByIdReturnsCorrectCode() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(statisticalCodeClient.getById("valid_id"))
          .thenReturn(new StatisticalCode().withId("valid_id").withCode("Code 1"));

      var actual = itemReferenceService.getStatisticalCodeById("valid_id", "test");
      assertEquals("Code 1", actual.getCode());
    }
  }

  @Test
  @SneakyThrows
  void getStatisticalCodeByIdThrowsExceptionForInvalidId() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      when(statisticalCodeClient.getById("invalid_id"))
          .thenThrow(new ReferenceDataNotFoundException("Not found"));

      assertThrows(
          ReferenceDataNotFoundException.class,
          () -> itemReferenceService.getStatisticalCodeById("invalid_id", "test"));
    }
  }
}
