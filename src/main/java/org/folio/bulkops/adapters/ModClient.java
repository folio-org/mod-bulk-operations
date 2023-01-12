package org.folio.bulkops.adapters;

import static java.util.Objects.isNull;

import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.domain.bean.IdentifierType;
import org.folio.bulkops.domain.dto.UnifiedTable;

import java.util.UUID;
import java.util.stream.Collectors;

public interface ModClient<T> {

  UnifiedTable convertEntityToUnifiedTable(T entity, UUID bulkOperationId, IdentifierType identifierType);
  Row convertEntityToUnifiedTableRow(T entity);
  UnifiedTable getUnifiedRepresentationByQuery(String query, long offset, long limit);
  UnifiedTable getEmptyTableWithHeaders();
  Class<T> getProcessedType();

  default String rowToCsvLine(Row row) {
    return row.getRow().stream()
      .map(this::prepareForCsv)
      .collect(Collectors.joining(",", "", "\n"));
  }

  private String prepareForCsv(String s) {
    if (isNull(s)) {
      return "";
    }

    if (s.contains("\"")) {
      s = s.replace("\"", "\"\"");
    }

    if (s.contains("\n")) {
      s = s.replace("\n", "\\n");
    }

    if (s.contains(",")) {
      s = "\"" + s + "\"";
    }

    return s;
  }
}
