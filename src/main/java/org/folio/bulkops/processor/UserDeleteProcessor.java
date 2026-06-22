package org.folio.bulkops.processor;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.BlUsersClient;
import org.folio.bulkops.client.UsersKeycloakClient;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.OpenTransactions;
import org.folio.bulkops.exception.RecordDeleteException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDeleteProcessor implements RecordDeleteProcessor<User> {
  private final BlUsersClient blUsersClient;
  private final UsersKeycloakClient usersKeycloakClient;

  @Override
  public void delete(User user) throws RecordDeleteException {
    var openTransactions = blUsersClient.getOpenTransactions(user.getId());
    if (openTransactions.getHasOpenTransactions()) {
      throw new RecordDeleteException(buildErrorMessage(openTransactions));
    }
    usersKeycloakClient.deleteUserById(user.getId());
  }

  private String buildErrorMessage(OpenTransactions openTransactions) {
    List<String> transactionNames = new ArrayList<>();
    if (openTransactions.getLoans() != null && openTransactions.getLoans() != 0) {
      transactionNames.add("loans");
    }
    if (openTransactions.getRequests() != null && openTransactions.getRequests() != 0) {
      transactionNames.add("requests");
    }
    if (openTransactions.getFeesFines() != null && openTransactions.getFeesFines() != 0) {
      transactionNames.add("fees/fines");
    }
    if (openTransactions.getProxies() != null && openTransactions.getProxies() != 0) {
      transactionNames.add("proxies");
    }
    if (openTransactions.getBlocks() != null && openTransactions.getBlocks() != 0) {
      transactionNames.add("blocks");
    }
    return "Open %s".formatted(String.join(", ", transactionNames));
  }
}
