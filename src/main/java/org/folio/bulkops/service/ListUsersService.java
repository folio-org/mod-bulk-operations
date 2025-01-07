package org.folio.bulkops.service;

import static java.lang.String.format;
import static java.lang.String.join;

import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.Users;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListUsersService {

  private static final String QUERY = "id==(%s)";
  private static final String OR_DELIMETER = " or ";

  private final JpaCqlRepository<BulkOperation, UUID> bulkOperationCqlRepository;
  private final UserClient userClient;

  public Users getListUsers(String query, Integer offset, Integer limit) {
    var ids = bulkOperationCqlRepository.findByCql(query, OffsetRequest.of(Objects.isNull(offset) ? 0 : offset,
      Objects.isNull(limit) ? Integer.MAX_VALUE : limit))
      .stream()
      .map(BulkOperation::getUserId)
      .distinct()
      .map(UUID::toString)
      .toList();
    var users = userClient.getByQuery(format(QUERY, join(OR_DELIMETER, ids)), ids.size()).getUsers()
      .stream()
      .map(this::mapUserToUserDto)
      .toList();
    return new Users().users(users).totalRecords(users.size());
  }

  private org.folio.bulkops.domain.dto.User mapUserToUserDto(User user) {
    Personal userPersonal = user.getPersonal();
    return new org.folio.bulkops.domain.dto.User()
      .id(UUID.fromString(user.getId()))
      .firstName(userPersonal.getFirstName())
      .lastName(userPersonal.getLastName())
      .preferredFirstName(userPersonal.getPreferredFirstName())
      .middleName(userPersonal.getMiddleName());
  }
}
