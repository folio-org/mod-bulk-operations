package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_CODE;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.Utils.encode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.bulkops.client.ContributorTypesClient;
import org.folio.bulkops.client.InstanceFormatsClient;
import org.folio.bulkops.client.InstanceStatusesClient;
import org.folio.bulkops.client.InstanceTypesClient;
import org.folio.bulkops.client.ModesOfIssuanceClient;
import org.folio.bulkops.client.NatureOfContentTermsClient;
import org.folio.bulkops.client.StatisticalCodeClient;
import org.folio.bulkops.client.StatisticalCodeTypeClient;
import org.folio.bulkops.domain.bean.InstanceFormats;
import org.folio.bulkops.domain.bean.InstanceStatuses;
import org.folio.bulkops.domain.bean.InstanceTypes;
import org.folio.bulkops.domain.bean.ModesOfIssuance;
import org.folio.bulkops.domain.bean.NatureOfContentTerms;
import org.folio.bulkops.domain.dto.ContributorTypeCollection;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.ReferenceDataNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InstanceReferenceServiceTest {
  @Mock
  private InstanceStatusesClient instanceStatusesClient;
  @Mock
  private ModesOfIssuanceClient modesOfIssuanceClient;
  @Mock
  private InstanceTypesClient instanceTypesClient;
  @Mock
  private NatureOfContentTermsClient natureOfContentTermsClient;
  @Mock
  private InstanceFormatsClient instanceFormatsClient;
  @Mock
  private ContributorTypesClient contributorTypesClient;
  @Mock
  private StatisticalCodeClient statisticalCodeClient;
  @Mock
  private StatisticalCodeTypeClient statisticalCodeTypeClient;
  @InjectMocks
  private InstanceReferenceService instanceReferenceService;

  @Test
  void shouldThrowNotFoundExceptionIfInstanceStatusNotFound() {
    var id = UUID.randomUUID().toString();
    doThrow(new NotFoundException("not found")).when(instanceStatusesClient).getById(id);
    assertThrows(ReferenceDataNotFoundException.class, () -> instanceReferenceService.getInstanceStatusNameById(id));

    var name = "name";
    when(instanceStatusesClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1)).thenReturn(InstanceStatuses.builder()
      .statuses(Collections.emptyList())
      .totalRecords(0)
      .build());
    assertThrows(ReferenceDataNotFoundException.class, () -> instanceReferenceService.getInstanceStatusIdByName(name));
  }

  @Test
  void shouldThrowNotFoundExceptionIfModeOfIssuanceNotFound() {
    var id = UUID.randomUUID().toString();
    doThrow(new NotFoundException("not found")).when(modesOfIssuanceClient).getById(id);
    assertThrows(ReferenceDataNotFoundException.class, () -> instanceReferenceService.getModeOfIssuanceNameById(id));

    var name = "name";
    when(modesOfIssuanceClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1)).thenReturn(ModesOfIssuance.builder()
      .modes(Collections.emptyList())
      .totalRecords(0)
      .build());
    assertThrows(ReferenceDataNotFoundException.class, () -> instanceReferenceService.getModeOfIssuanceIdByName(name));
  }

  @Test
  void shouldThrowNotFoundExceptionIfInstanceTypeNotFound() {
    var id = UUID.randomUUID().toString();
    doThrow(new NotFoundException("not found")).when(instanceTypesClient).getById(id);
    assertThrows(ReferenceDataNotFoundException.class, () -> instanceReferenceService.getInstanceTypeNameById(id));

    var name = "name";
    when(instanceTypesClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1)).thenReturn(InstanceTypes.builder()
      .types(Collections.emptyList())
      .totalRecords(0)
      .build());
    assertThrows(ReferenceDataNotFoundException.class, () -> instanceReferenceService.getInstanceTypeIdByName(name));
  }

  @Test
  void shouldThrowNotFoundExceptionIfNatureOfContentTermNotFound() {
    var id = UUID.randomUUID().toString();
    doThrow(new NotFoundException("not found")).when(natureOfContentTermsClient).getById(id);
    assertThrows(ReferenceDataNotFoundException.class, () -> instanceReferenceService.getNatureOfContentTermNameById(id));

    var name = "name";
    when(natureOfContentTermsClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1)).thenReturn(NatureOfContentTerms.builder()
      .terms(Collections.emptyList())
      .totalRecords(0)
      .build());
    assertThrows(ReferenceDataNotFoundException.class, () -> instanceReferenceService.getNatureOfContentTermIdByName(name));
  }

  @Test
  void shouldThrowNotFoundExceptionIfInstanceFormatNotFound() {
    var id = UUID.randomUUID().toString();
    doThrow(new NotFoundException("not found")).when(instanceFormatsClient).getById(id);
    assertThrows(ReferenceDataNotFoundException.class, () -> instanceReferenceService.getInstanceFormatNameById(id));

    var name = "name";
    when(instanceFormatsClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1)).thenReturn(InstanceFormats.builder()
      .formats(Collections.emptyList())
      .totalRecords(0)
      .build());
    assertThrows(ReferenceDataNotFoundException.class, () -> instanceReferenceService.getInstanceFormatIdByName(name));
  }

  @Test
  void shouldReturnContributorTypesByCodeIfCodeContainsSlashes() {
    when(contributorTypesClient.getByQuery("code==\"http://code/code\"", 1)).thenReturn(new ContributorTypeCollection().totalRecords(1));

    var res = instanceReferenceService.getContributorTypesByCode("http://code/code");
    assertThat(res.getTotalRecords()).isEqualTo(1);
  }

  @Test
  void shouldThrowNotFoundExceptionIfStatisticalCodeNotFound() {
    var id = UUID.randomUUID().toString();
    doThrow(new NotFoundException("not found")).when(statisticalCodeClient).getById(id);
    doThrow(new NotFoundException("not found")).when(statisticalCodeClient).getByQuery("name==\"some name\"");
    assertThrows(ReferenceDataNotFoundException.class, () -> instanceReferenceService.getStatisticalCodeById(id, "tenant"));
    assertThrows(NotFoundException.class, () -> instanceReferenceService.getStatisticalCodeByName("some name", "tenant"));
  }

  @Test
  void shouldThrowNotFoundExceptionIfStatisticalCodeTypeNotFound() {
    var id = UUID.randomUUID().toString();
    doThrow(new NotFoundException("not found")).when(statisticalCodeTypeClient).getById(id);
    assertThrows(ReferenceDataNotFoundException.class, () -> instanceReferenceService.getStatisticalCodeTypeById(id, "tenant"));
  }

  @Test
  void shouldGetInstanceFormatByCode() {
    var code = "code";
    assertThat(instanceReferenceService.getInstanceFormatsByCode(null).getFormats()).isEmpty();

    instanceReferenceService.getInstanceFormatsByCode(code);

    verify(instanceFormatsClient).getByQuery(QUERY_PATTERN_CODE.formatted(encode(code)), 1L);
  }
}
