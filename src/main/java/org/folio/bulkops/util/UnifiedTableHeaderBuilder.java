package org.folio.bulkops.util;

import static org.folio.bulkops.domain.dto.DataType.STRING;

import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;
import com.opencsv.bean.CsvRecurse;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.UnifiedTable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class UnifiedTableHeaderBuilder {
  public static UnifiedTable getEmptyTableWithHeaders(Class<? extends BulkOperationsEntity> clazz) {
    return new UnifiedTable().header(getHeaders(clazz));
  }

  public static List<Cell> getHeaders(Class<? extends BulkOperationsEntity> clazz) {
    return Stream.concat(
        FieldUtils.getFieldsListWithAnnotation(clazz, CsvRecurse.class).stream()
          .map(Field::getType)
          .map(aClass -> FieldUtils.getFieldsListWithAnnotation(aClass, CsvCustomBindByName.class))
          .flatMap(List::stream),
        FieldUtils.getFieldsListWithAnnotation(clazz, CsvCustomBindByName.class).stream())
      .collect(Collectors.toMap(field -> field.getAnnotation(CsvCustomBindByPosition.class).position(),
        field -> field.getAnnotation(CsvCustomBindByName.class).column(),
        (key1, key2) -> key1,
        TreeMap::new))
      .values().stream()
        .map(val -> new Cell().dataType(STRING).value(val).visible(true))
        .toList();
  }
}
