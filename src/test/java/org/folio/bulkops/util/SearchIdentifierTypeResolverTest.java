package org.folio.bulkops.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.bulkops.domain.dto.BatchIdsDto;
import org.folio.bulkops.domain.dto.BatchIdsDto.IdentifierTypeEnum;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.junit.jupiter.api.Test;

class SearchIdentifierTypeResolverTest {

  @Test
  void returnsCorrectEnumForEachIdentifierType() {
    assertThat(SearchIdentifierTypeResolver.getSearchIdentifierType(IdentifierType.ID))
        .isEqualTo(BatchIdsDto.IdentifierTypeEnum.ID);
    assertThat(SearchIdentifierTypeResolver.getSearchIdentifierType(IdentifierType.HRID))
        .isEqualTo(BatchIdsDto.IdentifierTypeEnum.HRID);
    assertThat(SearchIdentifierTypeResolver.getSearchIdentifierType(IdentifierType.BARCODE))
        .isEqualTo(BatchIdsDto.IdentifierTypeEnum.BARCODE);
    assertThat(
            SearchIdentifierTypeResolver.getSearchIdentifierType(IdentifierType.HOLDINGS_RECORD_ID))
        .isEqualTo(IdentifierTypeEnum.HOLDINGS_RECORD_ID);
    assertThat(
            SearchIdentifierTypeResolver.getSearchIdentifierType(IdentifierType.ACCESSION_NUMBER))
        .isEqualTo(IdentifierTypeEnum.ACCESSION_NUMBER);
    assertThat(SearchIdentifierTypeResolver.getSearchIdentifierType(IdentifierType.FORMER_IDS))
        .isEqualTo(IdentifierTypeEnum.FORMER_IDS);
    assertThat(SearchIdentifierTypeResolver.getSearchIdentifierType(IdentifierType.INSTANCE_HRID))
        .isEqualTo(IdentifierTypeEnum.INSTANCE_HRID);
    assertThat(SearchIdentifierTypeResolver.getSearchIdentifierType(IdentifierType.ITEM_BARCODE))
        .isEqualTo(IdentifierTypeEnum.ITEM_BARCODE);
  }
}
