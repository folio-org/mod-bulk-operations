package org.folio.bulkops.util;

import com.opencsv.bean.BeanField;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvCustomBindByName;

public class CustomMappingStrategy<T> extends ColumnPositionMappingStrategy<T> {

  @Override
  public String[] generateHeader(T bean) {

    var headers = getFieldMap().values().stream().map(BeanField::getField)
      .map(field -> field.getDeclaredAnnotation(CsvCustomBindByName.class))
      .map(CsvCustomBindByName::column).toArray(String[]::new);

    super.setColumnMapping(headers);

    return headers;
  }
}
