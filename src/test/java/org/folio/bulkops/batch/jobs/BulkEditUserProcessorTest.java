package org.folio.bulkops.batch.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import org.folio.bulkops.batch.jobs.processidentifiers.DuplicationCheckerFactory;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.JobExecution;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

class BulkEditUserProcessorTest {

  @Mock
  private UserClient userClient;
  @Mock
  private DuplicationCheckerFactory duplicationCheckerFactory;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private PermissionsValidator permissionsValidator;

  @InjectMocks
  private BulkEditUserProcessor processor;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    ReflectionTestUtils.setField(processor, "identifierType", IdentifierType.BARCODE.getValue());
    JobExecution jobExecution = Mockito.mock(JobExecution.class, Mockito.RETURNS_DEEP_STUBS);
    ReflectionTestUtils.setField(processor, "jobExecution", jobExecution);
    when(folioExecutionContext.getTenantId()).thenReturn("tenant1");
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
  }

  @Test
  void returnsUserWhenFoundAndPermitted() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("barcode1");
    User user = new User().withId("userId").withUsername("testuser").withPersonal(new Personal().withDateOfBirth(new Date(946684800000L))); // 2000-01-01
    UserCollection userCollection = UserCollection.builder().users(List.of(user)).totalRecords(1).build();

    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.USER))).thenReturn(true);
    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(
        ConcurrentHashMap.newKeySet());
    when(userClient.getByQuery(anyString(), anyLong())).thenReturn(userCollection);

    User result = processor.process(itemIdentifier);

    assertThat(result.getId()).isEqualTo("userId");
    assertThat(result.getUsername()).isEqualTo("testuser");
  }

  @Test
  void throwsWhenNoPermission() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("barcode2");
    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.USER))).thenReturn(false);
    when(userClient.getUserById(anyString())).thenReturn(new User().withUsername("admin"));

    assertThatThrownBy(() -> processor.process(itemIdentifier))
      .isInstanceOf(BulkEditException.class)
      .hasMessageContaining("does not have required permission");
  }

  @Test
  void throwsWhenDuplicateIdentifier() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("dupId");
    var set = Mockito.mock(KeySetView.class);
    when(set.add(any())).thenReturn(false);
    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.USER))).thenReturn(true);
    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(set);

    assertThatThrownBy(() -> processor.process(itemIdentifier))
      .isInstanceOf(BulkEditException.class)
      .hasMessageContaining("Duplicate entry")
      .extracting("errorType").isEqualTo(ErrorType.WARNING);
  }

  @Test
  void throwsWhenNoMatchFound() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("notfound");
    UserCollection emptyCollection = UserCollection.builder().users(Collections.emptyList()).totalRecords(0).build();

    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.USER))).thenReturn(true);
    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(ConcurrentHashMap.newKeySet());
    when(userClient.getByQuery(anyString(), anyLong())).thenReturn(emptyCollection);

    assertThatThrownBy(() -> processor.process(itemIdentifier))
      .isInstanceOf(BulkEditException.class)
      .hasMessageContaining("No match found");
  }

  @Test
  void throwsWhenMultipleMatchesFound() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("multi");
    User user1 = new User().withId("1");
    User user2 = new User().withId("2");
    UserCollection collection = UserCollection.builder().users(List.of(user1, user2)).totalRecords(2).build();

    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.USER))).thenReturn(true);
    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(ConcurrentHashMap.newKeySet());
    when(userClient.getByQuery(anyString(), anyLong())).thenReturn(collection);

    assertThatThrownBy(() -> processor.process(itemIdentifier))
      .isInstanceOf(BulkEditException.class)
      .hasMessageContaining("Multiple matches");
  }

  @Test
  void throwsWhenBirthDateIsTooEarly() {
    ItemIdentifier itemIdentifier = new ItemIdentifier().withItemId("badbirth");
    // 1800-01-01
    User user = new User().withId("userId").withUsername("testuser").withPersonal(new Personal().withDateOfBirth(new Date(-5364662400000L)));
    UserCollection userCollection = UserCollection.builder().users(List.of(user)).totalRecords(1).build();

    when(permissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(EntityType.USER))).thenReturn(true);
    when(duplicationCheckerFactory.getIdentifiersToCheckDuplication(any())).thenReturn(ConcurrentHashMap.newKeySet());
    when(userClient.getByQuery(anyString(), anyLong())).thenReturn(userCollection);

    assertThatThrownBy(() -> processor.process(itemIdentifier))
      .isInstanceOf(BulkEditException.class)
      .hasMessageContaining("Failed to parse Date from value");
  }
}
