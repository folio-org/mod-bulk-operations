package org.folio.bulkops.adapters.impl.holdings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.CallNumberTypeClient;
import org.folio.bulkops.client.HoldingsNoteTypeClient;
import org.folio.bulkops.client.HoldingsTypeClient;
import org.folio.bulkops.client.IllPolicyClient;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.domain.bean.BriefInstance;
import org.folio.bulkops.domain.bean.CallNumberType;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.HoldingsType;
import org.folio.bulkops.domain.bean.IllPolicy;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.error.BulkOperationException;
import org.folio.bulkops.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HoldingsReferenceResolverTest {

  @Mock
  private InstanceClient instanceClient;
  @Mock
  private HoldingsTypeClient holdingsTypeClient;
  @Mock
  private LocationClient locationClient;
  @Mock
  private CallNumberTypeClient callNumberTypeClient;
  @Mock
  private HoldingsNoteTypeClient holdingsNoteTypeClient;
  @Mock
  private IllPolicyClient illPolicyClient;
  @InjectMocks
  private HoldingsReferenceResolver holdingsReferenceResolver;

  @Test
  void getInstanceTitleByIdTest() {
    when(instanceClient.getById("id")).thenReturn(new BriefInstance().withTitle("title"));
    var actual = holdingsReferenceResolver.getInstanceTitleById("id");
    verify(instanceClient).getById("id");
    assertEquals("title", actual);

    actual = holdingsReferenceResolver.getInstanceTitleById("");
    assertTrue(StringUtils.isEmpty(actual));

    when(instanceClient.getById("id")).thenThrow(NotFoundException.class);
    assertThrows(BulkOperationException.class, () -> holdingsReferenceResolver.getInstanceTitleById("id"));
  }

  @Test
  void getHoldingsTypeNameByIdTest() {
    when(holdingsTypeClient.getById("id")).thenReturn(new HoldingsType().withName("name"));
    var actual = holdingsReferenceResolver.getHoldingsTypeNameById("id", null, null);
    verify(holdingsTypeClient).getById("id");
    assertEquals("name", actual);

    actual = holdingsReferenceResolver.getHoldingsTypeNameById("",null, null);
    assertTrue(StringUtils.isEmpty(actual));

    when(holdingsTypeClient.getById("id")).thenThrow(NotFoundException.class);
    actual = holdingsReferenceResolver.getHoldingsTypeNameById("id", null, null);
    assertEquals("id", actual);
  }

  @Test
  void getLocationNameByIdTest() {
    when(locationClient.getLocationById("id")).thenReturn(new ItemLocation().withName("name"));
    var actual = holdingsReferenceResolver.getLocationNameById("id");
    verify(locationClient).getLocationById("id");
    assertEquals("name", actual);

    actual = holdingsReferenceResolver.getLocationNameById("");
    assertTrue(StringUtils.isEmpty(actual));

    when(locationClient.getLocationById("id")).thenThrow(NotFoundException.class);
    assertThrows(BulkOperationException.class, () -> holdingsReferenceResolver.getLocationNameById("id"));
  }

  @Test
  void getCallNumberTypeNameByIdTest() {
    when(callNumberTypeClient.getById("id")).thenReturn(new CallNumberType().withName("name"));
    var actual = holdingsReferenceResolver.getCallNumberTypeNameById("id", null, null);
    verify(callNumberTypeClient).getById("id");
    assertEquals("name", actual);

    actual = holdingsReferenceResolver.getCallNumberTypeNameById("", null, null);
    assertTrue(StringUtils.isEmpty(actual));

    when(callNumberTypeClient.getById("id")).thenThrow(NotFoundException.class);
    actual = holdingsReferenceResolver.getCallNumberTypeNameById("id", null, null);
    assertEquals("id", actual);
  }

  @Test
  void getNoteTypeNameByIdTest() {
    when(holdingsNoteTypeClient.getById("id")).thenReturn(new HoldingsNoteType().withName("name"));
    var actual = holdingsReferenceResolver.getNoteTypeNameById("id", null, null);
    verify(holdingsNoteTypeClient).getById("id");
    assertEquals("name", actual);

    actual = holdingsReferenceResolver.getNoteTypeNameById("", null, null);
    assertTrue(StringUtils.isEmpty(actual));

    when(holdingsNoteTypeClient.getById("id")).thenThrow(NotFoundException.class);
    actual = holdingsReferenceResolver.getNoteTypeNameById("id", null, null);
    assertEquals("id", actual);
  }

  @Test
  void getIllPolicyNameByIdTest() {
    when(illPolicyClient.getById("id")).thenReturn(new IllPolicy().withName("name"));
    var actual = holdingsReferenceResolver.getIllPolicyNameById("id", null, null);
    verify(illPolicyClient).getById("id");
    assertEquals("name", actual);

    actual = holdingsReferenceResolver.getIllPolicyNameById("", null, null);
    assertTrue(StringUtils.isEmpty(actual));

    when(illPolicyClient.getById("id")).thenThrow(NotFoundException.class);
    actual = holdingsReferenceResolver.getIllPolicyNameById("id", null, null);
    assertEquals("id", actual);
  }

}
