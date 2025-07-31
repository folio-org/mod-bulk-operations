package org.folio.bulkops.batch.jobs.processidentifiers;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class DuplicationCheckerFactory {

  @SuppressWarnings("unchecked")
  public KeySetView<Object, Boolean> getIdentifiersToCheckDuplication(JobExecution jobExecution) {
    final String key = "identifiersToCheckDuplication";
    ExecutionContext context = jobExecution.getExecutionContext();

    synchronized (context) {
      if (!context.containsKey(key)) {
        context.put(key, Collections.synchronizedSet(new HashSet<>()));
      }
      var identifiersToCheckDuplication = ConcurrentHashMap.newKeySet();
      identifiersToCheckDuplication.addAll((Set<ItemIdentifier>)
          Optional.ofNullable(context.get(key)).orElse(Collections.emptySet()));
      return identifiersToCheckDuplication;
    }
  }

  @SuppressWarnings("unchecked")
  public Set<String> getFetchedIds(JobExecution jobExecution) {
    final String key = "fetchedIds";
    ExecutionContext context = jobExecution.getExecutionContext();

    synchronized (context) {
      if (!context.containsKey(key)) {
        context.put(key, Collections.synchronizedSet(new HashSet<>()));
      }
      return (Set<String>) context.get(key);
    }
  }
}
