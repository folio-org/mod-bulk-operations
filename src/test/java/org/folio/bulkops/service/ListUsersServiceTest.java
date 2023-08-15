package org.folio.bulkops.service;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;

public class ListUsersServiceTest extends BaseTest {

  @MockBean
  private JpaCqlRepository<BulkOperation, UUID> bulkOperationCqlRepository;

  @Autowired
  private ListUsersService listUsersService;

  @Test
  void shouldReturnListOfDistinctUsers() {
    var operationIdUnique = UUID.randomUUID();
    var operationIdRepeated1 = UUID.randomUUID();
    var operationIdRepeated2 = UUID.randomUUID();
    var userIdUnique = UUID.randomUUID();
    var userIdRepeated = UUID.randomUUID();

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {

      when(userClient.getUserById(userIdUnique.toString()))
        .thenReturn(new User().withId(userIdUnique.toString()).withPersonal(new Personal().withFirstName("Test unique")
          .withLastName("Test last name unique")));
      when(userClient.getUserById(userIdRepeated.toString()))
        .thenReturn(new User().withId(userIdRepeated.toString()).withPersonal(new Personal().withFirstName("Test repeated")
          .withLastName("Test last name repeated")));

      when(bulkOperationCqlRepository.findByCQL("(entityType==\"USER\")", OffsetRequest.of(0, 100)))
        .thenReturn(new PageImpl<>(List.of(BulkOperation.builder()
          .id(operationIdUnique)
          .status(OperationStatusType.COMPLETED)
          .totalNumOfRecords(10)
          .processedNumOfRecords(10)
          .entityType(EntityType.USER)
          .userId(userIdUnique)
          .build(),
          BulkOperation.builder()
            .id(operationIdRepeated1)
            .status(OperationStatusType.COMPLETED)
            .totalNumOfRecords(10)
            .processedNumOfRecords(10)
            .entityType(EntityType.USER)
            .userId(userIdRepeated)
            .build(),
          BulkOperation.builder()
            .id(operationIdRepeated2)
            .status(OperationStatusType.COMPLETED)
            .totalNumOfRecords(10)
            .processedNumOfRecords(10)
            .entityType(EntityType.USER)
            .userId(userIdRepeated)
            .build())));

      var query = "(entityType==\"USER\")";
      var limit = 100;
      var offset = 0;

      var listUsers = listUsersService.getListUsers(query, offset, limit);
      assertEquals(listUsers.getUsers().size(), 2);
      assertEquals(listUsers.getTotalRecords(), 2);
      assertNotEquals(listUsers.getUsers().get(0).getId(), listUsers.getUsers().get(1).getId());
    }
  }
}
