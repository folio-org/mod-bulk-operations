package org.folio.bulkops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.IntStream;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.processor.UserDeleteProcessor;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class UserDeleteServiceTest {

  @Mock private UserDeleteProcessor userDeleteProcessor;
  @Mock private RemoteFileSystemClient remoteFileSystemClient;
  @Mock private ErrorService errorService;
  @Mock private BulkOperationRepository bulkOperationRepository;
  @Mock private ObjectMapper objectMapper;
  @Mock private BulkOperationServiceHelper bulkOperationServiceHelper;

  @InjectMocks private UserDeleteService userDeleteService;

  @Test
  void deleteUsers_shouldFailOperationWhenMatchedRecordsFileIsMissing() {
    var operation = BulkOperation.builder().id(UUID.randomUUID()).build();

    userDeleteService.deleteUsers(operation);

    verify(bulkOperationServiceHelper)
        .failBulkOperation(operation, "File with matched records does not exist.");
    verify(bulkOperationServiceHelper, never()).completeBulkOperation(any(BulkOperation.class));
  }

  @Test
  void deleteUsers_shouldDeleteUsersSaveErrorsAndCompleteOperation() throws Exception {
    var operationId = UUID.randomUUID();
    var user1 = new User().withId("user-1");
    var user2 = new User().withId("user-2");
    var expectedQueryPath = operationId + "/users.fql";
    var queryWriter = new StringWriter();

    @SuppressWarnings("unchecked")
    var iterator = (MappingIterator<User>) mock(MappingIterator.class);
    when(iterator.hasNext()).thenReturn(true, true, false);
    when(iterator.next()).thenReturn(user1, user2);

    when(remoteFileSystemClient.get("tmp/users.json"))
        .thenReturn(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
    when(objectMapper.createParser(any(java.io.Reader.class))).thenReturn(mock(JsonParser.class));
    when(objectMapper.readValues(any(JsonParser.class), eq(User.class))).thenReturn(iterator);
    when(remoteFileSystemClient.writer(expectedQueryPath)).thenReturn(queryWriter);
    org.mockito.Mockito.doThrow(new RuntimeException("cannot delete user"))
        .when(userDeleteProcessor)
        .delete(user2);

    var operation =
        BulkOperation.builder()
            .id(operationId)
            .linkToMatchedRecordsJsonFile("tmp/users.json")
            .linkToTriggeringCsvFile("tmp/users.csv")
            .fqlQuery("query=true")
            .userFriendlyQuery("username==test")
            .build();

    userDeleteService.deleteUsers(operation);

    verify(userDeleteProcessor).delete(user1);
    verify(userDeleteProcessor).delete(user2);
    verify(errorService).saveError(operationId, "user-2", "cannot delete user", ErrorType.ERROR);
    assertEquals(2, operation.getCommittedNumOfRecords());
    assertEquals(expectedQueryPath, operation.getLinkToTriggeringQueryFile());
    assertEquals("username==test", queryWriter.toString());
    verify(bulkOperationServiceHelper).completeBulkOperation(operation);
    verify(bulkOperationServiceHelper, never())
        .failBulkOperation(any(BulkOperation.class), any(String.class));
  }

  @Test
  void deleteUsers_shouldPersistProgressEvery100Records() throws Exception {
    var hasNextSequence = new Boolean[101];
    Arrays.fill(hasNextSequence, Boolean.TRUE);
    hasNextSequence[100] = Boolean.FALSE;

    @SuppressWarnings("unchecked")
    var iterator = (MappingIterator<User>) mock(MappingIterator.class);
    var users =
        IntStream.rangeClosed(1, 100)
            .mapToObj(i -> new User().withId("user-" + i))
            .toArray(User[]::new);
    when(iterator.hasNext())
        .thenReturn(
            hasNextSequence[0], Arrays.copyOfRange(hasNextSequence, 1, hasNextSequence.length));
    when(iterator.next()).thenReturn(users[0], Arrays.copyOfRange(users, 1, users.length));

    when(remoteFileSystemClient.get("tmp/users.json"))
        .thenReturn(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
    when(objectMapper.createParser(any(java.io.Reader.class))).thenReturn(mock(JsonParser.class));
    when(objectMapper.readValues(any(JsonParser.class), eq(User.class))).thenReturn(iterator);

    var operation =
        BulkOperation.builder()
            .id(UUID.randomUUID())
            .linkToMatchedRecordsJsonFile("tmp/users.json")
            .build();

    userDeleteService.deleteUsers(operation);

    verify(bulkOperationRepository, times(1)).save(operation);
    assertEquals(100, operation.getCommittedNumOfRecords());
    verify(bulkOperationServiceHelper).completeBulkOperation(operation);
  }

  @Test
  void deleteUsers_shouldFailOperationWhenUnexpectedExceptionOccurs() throws Exception {
    var operation =
        BulkOperation.builder()
            .id(UUID.randomUUID())
            .linkToMatchedRecordsJsonFile("tmp/users.json")
            .build();

    when(remoteFileSystemClient.get("tmp/users.json"))
        .thenReturn(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
    when(objectMapper.createParser(any(java.io.Reader.class)))
        .thenThrow(new RuntimeException("read error"));

    userDeleteService.deleteUsers(operation);

    verify(bulkOperationServiceHelper).failBulkOperation(operation, "read error");
    verify(bulkOperationServiceHelper, never()).completeBulkOperation(any(BulkOperation.class));
  }
}
