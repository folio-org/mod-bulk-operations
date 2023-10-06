package org.folio.bulkops.service;

import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE_INCLUDING_ITEMS;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE_INCLUDING_ITEMS;
import static org.folio.bulkops.util.Constants.MSG_HOLDING_NO_CHANGE_REQUIRED_SUPPRESSED_ITEMS_UPDATED;
import static org.folio.bulkops.util.Constants.MSG_HOLDING_NO_CHANGE_REQUIRED_UNSUPPRESSED_ITEMS_UPDATED;
import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.domain.dto.UpdateActionType;

@RequiredArgsConstructor
public enum DiscoverySuppressTestData {
  SET_TRUE_HOLDINGS_SUPPRESSED_ITEMS_SUPPRESSED(SET_TO_TRUE_INCLUDING_ITEMS, true, true,  true, true, 0, MSG_NO_CHANGE_REQUIRED),
  SET_FALSE_HOLDINGS_SUPPRESSED_ITEMS_SUPPRESSED(SET_TO_FALSE_INCLUDING_ITEMS, true, false, true, true, 2, null),
  SET_TRUE_HOLDINGS_UNSUPPRESSED_ITEMS_UNSUPPRESSED(SET_TO_TRUE_INCLUDING_ITEMS, false, true,  false, false, 2, null),
  SET_FALSE_HOLDINGS_UNSUPPRESSED_ITEMS_UNSUPPRESSED(SET_TO_FALSE_INCLUDING_ITEMS, false, false,  false, false, 0, MSG_NO_CHANGE_REQUIRED),
  SET_TRUE_HOLDINGS_SUPPRESSED_ONE_ITEM_SUPPRESSED(SET_TO_TRUE_INCLUDING_ITEMS, true, true, true, false, 1, MSG_HOLDING_NO_CHANGE_REQUIRED_UNSUPPRESSED_ITEMS_UPDATED),
  SET_FALSE_HOLDINGS_SUPPRESSED_ONE_ITEM_SUPPRESSED(SET_TO_FALSE_INCLUDING_ITEMS, true, false, true, false, 1, null),
  SET_TRUE_HOLDINGS_UNSUPPRESSED_ONE_ITEM_UNSUPPRESSED(SET_TO_TRUE_INCLUDING_ITEMS, false, true, true, false, 1, null),
  SET_FALSE_HOLDINGS_UNSUPPRESSED_ONE_ITEM_UNSUPPRESSED(SET_TO_FALSE_INCLUDING_ITEMS, false, false, true, false, 1, MSG_HOLDING_NO_CHANGE_REQUIRED_SUPPRESSED_ITEMS_UPDATED);

  final UpdateActionType actionType;
  final boolean originalHoldingsDiscoverySuppress;
  final boolean modifiedHoldingsDiscoverySuppress;
  final boolean item1DiscoverySuppress;
  final boolean item2DiscoverySuppress;
  final int expectedNumOfItemUpdates;
  final String expectedErrorMessage;
}
