package org.folio.bulkops.util;

import lombok.experimental.UtilityClass;
import org.folio.bulkops.domain.dto.BatchIdsDto;
import org.folio.bulkops.domain.dto.BatchIdsDto.IdentifierTypeEnum;
import org.folio.bulkops.domain.dto.IdentifierType;

@UtilityClass
public class SearchIdentifierTypeResolver {

  public static BatchIdsDto.IdentifierTypeEnum getSearchIdentifierType(
      IdentifierType identifierType) {
    return switch (identifierType) {
      case ID -> BatchIdsDto.IdentifierTypeEnum.ID;
      case HRID -> BatchIdsDto.IdentifierTypeEnum.HRID;
      case BARCODE -> BatchIdsDto.IdentifierTypeEnum.BARCODE;
      case HOLDINGS_RECORD_ID -> IdentifierTypeEnum.HOLDINGS_RECORD_ID;
      case ACCESSION_NUMBER -> IdentifierTypeEnum.ACCESSION_NUMBER;
      case FORMER_IDS -> IdentifierTypeEnum.FORMER_IDS;
      case INSTANCE_HRID -> IdentifierTypeEnum.INSTANCE_HRID;
      case ITEM_BARCODE -> IdentifierTypeEnum.ITEM_BARCODE;
      default -> throw new IllegalArgumentException("Identifier type doesn't supported");
    };
  }
}
