package org.folio.bulkops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ListUsersServiceTest extends BaseTest {

  @MockitoBean private JpaCqlRepository<BulkOperation, UUID> bulkOperationCqlRepository;

  @Autowired private ListUsersService listUsersService;

  @Test
  void shouldReturnListOfDistinctUsers() {
    var firstUserOperationId = UUID.randomUUID();
    var secondUserFirstOperationId = UUID.randomUUID();
    var secondUserSecondOperationId = UUID.randomUUID();
    var firstUserId = UUID.randomUUID();
    var secondUserId = UUID.randomUUID();

    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {

      when(userClient.getByQuery("id==(" + firstUserId + " or " + secondUserId + ")", 2))
          .thenReturn(
              new UserCollection()
                  .withUsers(
                      List.of(
                          new User()
                              .withId(firstUserId.toString())
                              .withPersonal(
                                  new Personal()
                                      .withFirstName("Test unique")
                                      .withLastName("Test last name unique")
                                      .withPreferredFirstName("Test preferred first name unique")
                                      .withMiddleName("Test middle name unique")),
                          new User()
                              .withId(secondUserId.toString())
                              .withPersonal(
                                  new Personal()
                                      .withFirstName("Test repeated")
                                      .withLastName("Test last name repeated")
                                      .withPreferredFirstName("Test preferred first name repeated")
                                      .withMiddleName("Test middle name repeated")))));

      final Page<BulkOperation> page =
          new PageImpl<>(
              List.of(
                  BulkOperation.builder()
                      .id(firstUserOperationId)
                      .status(OperationStatusType.COMPLETED)
                      .totalNumOfRecords(10)
                      .processedNumOfRecords(10)
                      .entityType(EntityType.USER)
                      .userId(firstUserId)
                      .build(),
                  BulkOperation.builder()
                      .id(secondUserFirstOperationId)
                      .status(OperationStatusType.COMPLETED)
                      .totalNumOfRecords(10)
                      .processedNumOfRecords(10)
                      .entityType(EntityType.USER)
                      .userId(secondUserId)
                      .build(),
                  BulkOperation.builder()
                      .id(secondUserSecondOperationId)
                      .status(OperationStatusType.COMPLETED)
                      .totalNumOfRecords(10)
                      .processedNumOfRecords(10)
                      .entityType(EntityType.USER)
                      .userId(secondUserId)
                      .build()));

      when(bulkOperationCqlRepository.findByCql("(entityType==\"USER\")", OffsetRequest.of(0, 100)))
          .thenReturn(page);
      when(bulkOperationCqlRepository.findByCql(
              "(entityType==\"USER\")", OffsetRequest.of(0, Integer.MAX_VALUE)))
          .thenReturn(page);

      var query = "(entityType==\"USER\")";
      Integer limit = 100;
      Integer offset = 0;

      var listUsers = listUsersService.getListUsers(query, offset, limit);
      assertEquals(2, listUsers.getUsers().size());
      assertEquals(2, listUsers.getTotalRecords());
      assertNotEquals(listUsers.getUsers().get(0).getId(), listUsers.getUsers().get(1).getId());

      listUsers = listUsersService.getListUsers(query, null, null);
      assertEquals(2, listUsers.getUsers().size());
      assertEquals(2, listUsers.getTotalRecords());
      assertNotEquals(listUsers.getUsers().get(0).getId(), listUsers.getUsers().get(1).getId());
    }
  }
}
