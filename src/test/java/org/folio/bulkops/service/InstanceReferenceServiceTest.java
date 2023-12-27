package org.folio.bulkops.service;

import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.Utils.encode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.folio.bulkops.client.InstanceFormatsClient;
import org.folio.bulkops.client.InstanceStatusesClient;
import org.folio.bulkops.client.InstanceTypesClient;
import org.folio.bulkops.client.ModesOfIssuanceClient;
import org.folio.bulkops.client.NatureOfContentTermsClient;
import org.folio.bulkops.domain.bean.InstanceFormats;
import org.folio.bulkops.domain.bean.InstanceStatuses;
import org.folio.bulkops.domain.bean.InstanceTypes;
import org.folio.bulkops.domain.bean.ModesOfIssuance;
import org.folio.bulkops.domain.bean.NatureOfContentTerms;
import org.folio.bulkops.exception.NotFoundException;
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
  @InjectMocks
  private InstanceReferenceService instanceReferenceService;

  @Test
  void shouldThrowNotFoundExceptionIfInstanceStatusNotFound() {
    var id = UUID.randomUUID().toString();
    doThrow(new NotFoundException("not found")).when(instanceStatusesClient).getById(id);
    assertThrows(NotFoundException.class, () -> instanceReferenceService.getInstanceStatusNameById(id));

    var name = "name";
    when(instanceStatusesClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1)).thenReturn(InstanceStatuses.builder()
      .statuses(Collections.emptyList())
      .totalRecords(0)
      .build());
    assertThrows(NotFoundException.class, () -> instanceReferenceService.getInstanceStatusIdByName(name));
  }

  @Test
  void shouldThrowNotFoundExceptionIfModeOfIssuanceNotFound() {
    var id = UUID.randomUUID().toString();
    doThrow(new NotFoundException("not found")).when(modesOfIssuanceClient).getById(id);
    assertThrows(NotFoundException.class, () -> instanceReferenceService.getModeOfIssuanceNameById(id));

    var name = "name";
    when(modesOfIssuanceClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1)).thenReturn(ModesOfIssuance.builder()
      .modes(Collections.emptyList())
      .totalRecords(0)
      .build());
    assertThrows(NotFoundException.class, () -> instanceReferenceService.getModeOfIssuanceIdByName(name));
  }

  @Test
  void shouldThrowNotFoundExceptionIfInstanceTypeNotFound() {
    var id = UUID.randomUUID().toString();
    doThrow(new NotFoundException("not found")).when(instanceTypesClient).getById(id);
    assertThrows(NotFoundException.class, () -> instanceReferenceService.getInstanceTypeNameById(id));

    var name = "name";
    when(instanceTypesClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1)).thenReturn(InstanceTypes.builder()
      .types(Collections.emptyList())
      .totalRecords(0)
      .build());
    assertThrows(NotFoundException.class, () -> instanceReferenceService.getInstanceTypeIdByName(name));
  }

  @Test
  void shouldThrowNotFoundExceptionIfNatureOfContentTermNotFound() {
    var id = UUID.randomUUID().toString();
    doThrow(new NotFoundException("not found")).when(natureOfContentTermsClient).getById(id);
    assertThrows(NotFoundException.class, () -> instanceReferenceService.getNatureOfContentTermNameById(id));

    var name = "name";
    when(natureOfContentTermsClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1)).thenReturn(NatureOfContentTerms.builder()
      .terms(Collections.emptyList())
      .totalRecords(0)
      .build());
    assertThrows(NotFoundException.class, () -> instanceReferenceService.getNatureOfContentTermIdByName(name));
  }

  @Test
  void shouldThrowNotFoundExceptionIfInstanceFormatNotFound() {
    var id = UUID.randomUUID().toString();
    doThrow(new NotFoundException("not found")).when(instanceFormatsClient).getById(id);
    assertThrows(NotFoundException.class, () -> instanceReferenceService.getInstanceFormatNameById(id));

    var name = "name";
    when(instanceFormatsClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1)).thenReturn(InstanceFormats.builder()
      .formats(Collections.emptyList())
      .totalRecords(0)
      .build());
    assertThrows(NotFoundException.class, () -> instanceReferenceService.getInstanceFormatIdByName(name));
  }
}
