//package org.folio.bulkops.processor;
//
//
//import org.folio.bulkops.domain.bean.HoldingsRecord;
//import org.folio.bulkops.domain.bean.Item;
//import org.folio.bulkops.domain.bean.User;
//import org.springframework.beans.factory.annotation.Autowired;
//
//public class DataProcessorFactory<T> {
//
//  private UserDataProcessor userDataProcessor;
//  private ItemDataProcessor itemDataProcessor;
//  private HoldingDataProcessor holdingDataProcessor;
//
//  public DataProcessor<T> getProcessor(Class<T> clazz) {
//    if (clazz == User.class) {
//      return userDataProcessor;
//    } else if (clazz == Item.class) {
//      return ValidHandler.ITEM.make();
//    } else if (clazz == HoldingsRecord.class) {
//      return ValidHandler.HOLDING.make();
//    }
//
//    return null;
//  }
//
//  enum ValidHandler {
//    USER {
//      @Override
//      DataProcessor<User> make() {
//        return new UserDataProcessor();
//      }
//    },
//    ITEM {
//      @Override
//      DataProcessor<User> make() {
//        return new ItemDataProcessor();
//      }
//    },
//    HOLDING {
//      @Override
//      DataProcessor<User> make() {
//        return new HoldingDataProcessor();
//      }
//    };
//    abstract <T> DataProcessor<T> make();
//  }
//}
