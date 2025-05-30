package org.folio.bulkops.domain.bean;

import static org.folio.bulkops.util.Constants.UTF8_BOM;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import java.io.Serializable;

@Data
@With
@NoArgsConstructor
public class ItemIdentifier implements Serializable {
  private String itemId;

  public ItemIdentifier(String itemId) {
    setItemId(itemId);
  }

  public void setItemId(String itemId) {
    this.itemId = clearBomSymbol(itemId);
  }

  private String clearBomSymbol(String s) {
    return s.startsWith(UTF8_BOM) ? s.substring(1) : s;
  }
}
