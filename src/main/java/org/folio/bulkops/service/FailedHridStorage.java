package org.folio.bulkops.service;

import static java.util.Optional.ofNullable;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class FailedHridStorage {
  private Map<UUID, Set<String>> storage = new HashMap<>();

  public void addFailedHrid(UUID bulkOperationId, String hrid) {
    var idSet = storage.getOrDefault(bulkOperationId, new HashSet<>());
    idSet.add(hrid);
    storage.put(bulkOperationId, idSet);
  }

  public Set<String> fetchFailedHrids(UUID bulkOperationId) {
    return ofNullable(storage.remove(bulkOperationId)).orElse(new HashSet<>());
  }
}
