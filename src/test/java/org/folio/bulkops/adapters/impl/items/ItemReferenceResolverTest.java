package org.folio.bulkops.adapters.impl.items;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.CallNumberTypeClient;
import org.folio.bulkops.client.DamagedStatusClient;
import org.folio.bulkops.client.ItemNoteTypeClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.client.ServicePointClient;
import org.folio.bulkops.client.StatisticalCodeClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.dto.CallNumberType;
import org.folio.bulkops.domain.dto.DamagedStatus;
import org.folio.bulkops.domain.dto.NoteType;
import org.folio.bulkops.domain.dto.ServicePoint;
import org.folio.bulkops.domain.dto.StatisticalCode;
import org.folio.bulkops.domain.dto.User;
import org.folio.bulkops.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemReferenceResolverTest {

  @Mock
  private CallNumberTypeClient callNumberTypeClient;
  @Mock
  private DamagedStatusClient damagedStatusClient;
  @Mock
  private ItemNoteTypeClient itemNoteTypeClient;
  @Mock
  private ServicePointClient servicePointClient;
  @Mock
  private StatisticalCodeClient statisticalCodeClient;
  @Mock
  private UserClient userClient;
  @Mock
  private LocationClient locationClient;
  @InjectMocks
  @Spy
  private ItemReferenceResolver itemReferenceResolver;

  @Test
  void getCallNumberTypeNameByIdTest() {
    when(callNumberTypeClient.getById("id")).thenReturn(new CallNumberType().name("name"));
    var actual = itemReferenceResolver.getCallNumberTypeNameById("id", null, null);
    verify(callNumberTypeClient).getById("id");
    assertEquals("name", actual);

    actual = itemReferenceResolver.getCallNumberTypeNameById("", null, null);
    assertTrue(StringUtils.isEmpty(actual));

    when(callNumberTypeClient.getById("id")).thenThrow(NotFoundException.class);
    actual = itemReferenceResolver.getCallNumberTypeNameById("id", null, null);
    assertEquals("id", actual);
  }

  @Test
  void getDamagedStatusNameByIdTest() {
    when(damagedStatusClient.getById("id")).thenReturn(new DamagedStatus().name("name"));
    var actual = itemReferenceResolver.getDamagedStatusNameById("id", null, null);
    verify(damagedStatusClient).getById("id");
    assertEquals("name", actual);

    actual = itemReferenceResolver.getDamagedStatusNameById("", null, null);
    assertTrue(StringUtils.isEmpty(actual));

    when(damagedStatusClient.getById("id")).thenThrow(NotFoundException.class);
    actual = itemReferenceResolver.getDamagedStatusNameById("id", null, null);
    assertEquals("id", actual);
  }

  @Test
  void getNoteTypeNameByIdTest() {
    when(itemNoteTypeClient.getById("id")).thenReturn(new NoteType().name("name"));
    var actual = itemReferenceResolver.getNoteTypeNameById("id", null, null);
    verify(itemNoteTypeClient).getById("id");
    assertEquals("name", actual);

    actual = itemReferenceResolver.getNoteTypeNameById("", null, null);
    assertTrue(StringUtils.isEmpty(actual));

    when(itemNoteTypeClient.getById("id")).thenThrow(NotFoundException.class);
    actual = itemReferenceResolver.getNoteTypeNameById("id", null, null);
    assertEquals("id", actual);
  }

  @Test
  void getServicePointNameByIdTest() {
    when(servicePointClient.getById("id")).thenReturn(new ServicePoint().name("name"));
    var actual = itemReferenceResolver.getServicePointNameById("id", null, null);
    verify(servicePointClient).getById("id");
    assertEquals("name", actual);

    actual = itemReferenceResolver.getServicePointNameById("", null, null);
    assertTrue(StringUtils.isEmpty(actual));

    when(servicePointClient.getById("id")).thenThrow(NotFoundException.class);
    actual = itemReferenceResolver.getServicePointNameById("id", null, null);
    assertEquals("id", actual);
  }

  @Test
  void getStatisticalCodeByIdTest() {
    when(statisticalCodeClient.getById("id")).thenReturn(new StatisticalCode().code("code"));
    var actual = itemReferenceResolver.getStatisticalCodeById("id", null, null);
    verify(statisticalCodeClient).getById("id");
    assertEquals("code", actual);

    actual = itemReferenceResolver.getStatisticalCodeById("", null, null);
    assertTrue(StringUtils.isEmpty(actual));

    when(statisticalCodeClient.getById("id")).thenThrow(NotFoundException.class);
    actual = itemReferenceResolver.getStatisticalCodeById("id", null, null);
    assertEquals("id", actual);
  }

  @Test
  void getUserNameById() {
    when(userClient.getUserById("id")).thenReturn(new User().username("name"));
    var actual = itemReferenceResolver.getUserNameById("id", null, null);
    verify(userClient).getUserById("id");
    assertEquals("name", actual);

    actual =  itemReferenceResolver.getUserNameById("", null, null);
    assertTrue(StringUtils.isEmpty(actual));

    when(userClient.getUserById("id")).thenThrow(NotFoundException.class);
    actual = itemReferenceResolver.getUserNameById("id", null, null);
    assertEquals("id", actual);
  }
}
