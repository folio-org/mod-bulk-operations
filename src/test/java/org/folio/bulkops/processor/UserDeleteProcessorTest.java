package org.folio.bulkops.processor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.bulkops.client.BlUsersClient;
import org.folio.bulkops.client.UsersKeycloakClient;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.OpenTransactions;
import org.folio.bulkops.exception.RecordDeleteException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDeleteProcessorTest {

  @Mock private BlUsersClient blUsersClient;
  @Mock private UsersKeycloakClient usersKeycloakClient;
  @Mock private OpenTransactions openTransactions;

  @InjectMocks private UserDeleteProcessor userDeleteProcessor;

  @Test
  void deleteShouldDeleteUserWhenNoOpenTransactions() {
    var userId = "user-id";
    var user = new User().withId(userId);
    when(blUsersClient.getOpenTransactions(userId)).thenReturn(openTransactions);
    when(openTransactions.getHasOpenTransactions()).thenReturn(false);

    assertDoesNotThrow(() -> userDeleteProcessor.delete(user));

    verify(usersKeycloakClient).deleteUserById(userId);
  }

  @Test
  void deleteShouldThrowExceptionWhenUserHasOpenTransactions() {
    var userId = "user-id";
    var user = new User().withId(userId);
    when(blUsersClient.getOpenTransactions(userId)).thenReturn(openTransactions);
    when(openTransactions.getHasOpenTransactions()).thenReturn(true);
    when(openTransactions.getLoans()).thenReturn(1);
    when(openTransactions.getRequests()).thenReturn(2);
    when(openTransactions.getFeesFines()).thenReturn(3);
    when(openTransactions.getProxies()).thenReturn(0);
    when(openTransactions.getBlocks()).thenReturn(null);

    var exception =
        assertThrows(RecordDeleteException.class, () -> userDeleteProcessor.delete(user));

    assertEquals("Open loans, requests, fees/fines", exception.getMessage());
    verify(usersKeycloakClient, never()).deleteUserById(userId);
  }
}
