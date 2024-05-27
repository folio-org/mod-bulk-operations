package org.folio.bulkops.service;

import static java.lang.String.format;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.dto.Users;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.mapper.UserToUserDtoMapper;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListUsersService {

  private static final String QUERY = "id==%s";

  private final JpaCqlRepository<BulkOperation, UUID> bulkOperationCqlRepository;
  private final UserClient userClient;
  private final UserToUserDtoMapper userToUserDtoMapper;

  public Users getListUsers(String query, Integer offset, Integer limit) {
    var allBulkOperations = bulkOperationCqlRepository.findByCql(query, OffsetRequest.of(Objects.isNull(offset) ? 0 : offset,
      Objects.isNull(limit) ? Integer.MAX_VALUE : limit));
    var distinctUsers = allBulkOperations.stream().map(op -> userClient.getByQuery(format(QUERY, op.getUserId().toString()), 1).getUsers())
      .filter(users -> !users.isEmpty()).map(users -> users.get(0)).collect(Collectors.toSet());
    var usersToReturn = distinctUsers.stream().map(userToUserDtoMapper::mapUserToUserDto).toList();
    return new Users().users(usersToReturn).totalRecords(distinctUsers.size());
  }
}
