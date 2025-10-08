package org.folio.bulkops.processor.folio;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StatisticalCodesUpdater {

  public List<String> addToStatisticalCodeIds(String statisticalCodeIdsFromAction,
                                              List<String> existingStatisticalCodeIds,
                                              boolean forPreview) {
    var newStatisticalCodeIds = new ArrayList<>(Arrays.asList(
            statisticalCodeIdsFromAction.split(",")));
    if (isNull(existingStatisticalCodeIds)) {
      return forPreview ? newStatisticalCodeIds : newStatisticalCodeIds.stream().distinct()
              .toList();
    }
    if (forPreview) {
      existingStatisticalCodeIds.addAll(newStatisticalCodeIds);
    } else {
      newStatisticalCodeIds.stream().distinct().filter(
              newCode -> !existingStatisticalCodeIds.contains(newCode))
        .forEach(existingStatisticalCodeIds::add);
    }
    return existingStatisticalCodeIds;
  }

  public List<String> removeSomeStatisticalCodeIds(String statisticalCodeIdsFromAction,
                                                   List<String> existingStatisticalCodeIds) {
    var statisticalCodeIdsToRemove = new ArrayList<>(Arrays.asList(
            statisticalCodeIdsFromAction.split(",")));
    if (isNull(existingStatisticalCodeIds)) {
      return Collections.emptyList();
    }
    existingStatisticalCodeIds.removeAll(statisticalCodeIdsToRemove);
    return existingStatisticalCodeIds;
  }
}
