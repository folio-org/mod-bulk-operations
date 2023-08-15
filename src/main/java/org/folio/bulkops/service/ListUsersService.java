package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.dto.Users;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListUsersService {

  private final JpaCqlRepository<BulkOperation, UUID> bulkOperationCqlRepository;
  private final UserClient userClient;

  public Users getListUsers(String query, Integer offset, Integer limit) {
    var allBulkOperations = bulkOperationCqlRepository.findByCQL(query, OffsetRequest.of(Objects.isNull(offset) ? 0 : offset,
      Objects.isNull(limit) ? Integer.MAX_VALUE : limit));
    var distinctUsers = allBulkOperations.stream().map(op -> userClient.getUserById(op.getUserId().toString())).collect(Collectors.toSet());
    var usersToReturn = distinctUsers.stream().map(user -> new org.folio.bulkops.domain.dto.User().id(UUID.fromString(user.getId()))
      .firstName(user.getPersonal().getFirstName()).lastName(user.getPersonal().getLastName())).toList();
    return new Users().users(usersToReturn).totalRecords(distinctUsers.size());
  }
}
