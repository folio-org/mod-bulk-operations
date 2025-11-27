package org.folio.bulkops.batch.jobs.processidentifiers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

@Component
public class DuplicationCheckerFactory {

  @SuppressWarnings("unchecked")
  public Set<ItemIdentifier> getIdentifiersToCheckDuplication(JobExecution jobExecution) {
    final String key = "identifiersToCheckDuplication";
    ExecutionContext context = jobExecution.getExecutionContext();

    synchronized (context) {
      if (!context.containsKey(key)) {
        context.put(key, Collections.synchronizedSet(new HashSet<>()));
      }
      return Collections.synchronizedSet(
          (Set<ItemIdentifier>)
              Optional.ofNullable(context.get(key))
                  .orElse(Collections.synchronizedSet(new HashSet<>())));
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
      return Collections.synchronizedSet(
          (Set<String>)
              Optional.ofNullable(context.get(key))
                  .orElse(Collections.synchronizedSet(new HashSet<>())));
    }
  }
}
