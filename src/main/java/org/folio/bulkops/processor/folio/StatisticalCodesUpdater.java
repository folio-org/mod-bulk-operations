package org.folio.bulkops.processor.folio;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.isNull;

@Component
public class StatisticalCodesUpdater {

  public List<String> addToStatisticalCodeIds(String statisticalCodeIdsFromAction, List<String> existingStatisticalCodeIds) {
    var newStatisticalCodeIds = new ArrayList<>(Arrays.asList(statisticalCodeIdsFromAction.split(",")));
    if (isNull(existingStatisticalCodeIds)) {
      return newStatisticalCodeIds;
    }
    existingStatisticalCodeIds.addAll(newStatisticalCodeIds);
    return existingStatisticalCodeIds;
  }

  public List<String> removeSomeStatisticalCodeIds(String statisticalCodeIdsFromAction, List<String> existingStatisticalCodeIds) {
    var statisticalCodeIdsToRemove = new ArrayList<>(Arrays.asList(statisticalCodeIdsFromAction.split(",")));
    if (isNull(existingStatisticalCodeIds)) {
      return null;
    }
    existingStatisticalCodeIds.removeAll(statisticalCodeIdsToRemove);
    return existingStatisticalCodeIds;
  }
}
