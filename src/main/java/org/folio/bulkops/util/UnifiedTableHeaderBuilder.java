package org.folio.bulkops.util;

import static org.folio.bulkops.domain.dto.DataType.STRING;

import com.opencsv.bean.CsvCustomBindByName;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.UnifiedTable;

import java.util.List;

@UtilityClass
public class UnifiedTableHeaderBuilder {
  public static UnifiedTable getEmptyTableWithHeaders(Class<? extends BulkOperationsEntity> clazz) {
    return new UnifiedTable().header(getHeaders(clazz));
  }

  public static List<Cell> getHeaders(Class<? extends BulkOperationsEntity> clazz) {
    return FieldUtils.getFieldsListWithAnnotation(clazz, CsvCustomBindByName.class).stream()
      .map(field -> new Cell().dataType(STRING).value(field.getAnnotation(CsvCustomBindByName.class).column()).visible(true))
      .toList();
  }
}
