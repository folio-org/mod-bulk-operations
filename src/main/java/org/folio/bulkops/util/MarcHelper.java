package org.folio.bulkops.util;

import static org.folio.bulkops.util.Constants.FIELD_999;
import static org.folio.bulkops.util.Constants.INDICATOR_F;

import lombok.experimental.UtilityClass;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

@UtilityClass
public class MarcHelper {
  public static String fetchInstanceUuidOrElseHrid(Record marcRecord) {
    return marcRecord.getDataFields().stream()
      .filter(f -> FIELD_999.equals(f.getTag()) && INDICATOR_F == f.getIndicator1() && INDICATOR_F == f.getIndicator2())
      .findFirst()
      .map(f -> f.getSubfield('i'))
      .map(Subfield::getData)
      .orElse(marcRecord.getControlNumber());
  }
}
