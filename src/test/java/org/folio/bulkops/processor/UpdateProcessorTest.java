package org.folio.bulkops.processor;

import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UpdateProcessorTest extends BaseTest {
  @Autowired
  private HoldingsUpdateProcessor holdingsUpdateProcessor;
  @Autowired
  private ItemUpdateProcessor itemUpdateProcessor;
  @Autowired
  private UserUpdateProcessor userUpdateProcessor;

  @Test
  void shouldUpdateHoldingsRecord() {
    var holdingsRecord = new HoldingsRecord()
        .withId(UUID.randomUUID().toString())
        .withInstanceId(UUID.randomUUID().toString())
        .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()));

    holdingsUpdateProcessor.updateRecord(holdingsRecord);

    verify(holdingsClient).updateHoldingsRecord(holdingsRecord, holdingsRecord.getId());
  }

  @Test
  void shouldUpdateItem() {
    var item = new Item()
      .withId(UUID.randomUUID().toString())
      .withHoldingsRecordId(UUID.randomUUID().toString());

    itemUpdateProcessor.updateRecord(item);

    verify(itemClient).updateItem(item, item.getId());
  }

  @Test
  void shouldUpdateUser() {
    var user = new User()
        .withId(UUID.randomUUID().toString())
        .withPatronGroup(UUID.randomUUID().toString())
        .withUsername("sample_user");

    userUpdateProcessor.updateRecord(user);

    verify(userClient).updateUser(user, user.getId());
  }
}
