package org.folio.bulkops.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.TestEntity;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.converter.CustomMappingStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.opencsv.ICSVWriter.DEFAULT_SEPARATOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;


class OpenCSVConverterTest extends BaseTest {

  private static class BulkOperationEntityClassProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
        Arguments.of(HoldingsRecord.class)
      );
    }
  }

  //TODO TechDebt - Large test to cover object -> json -> object -> csv -> object transformation to prevent all the possible convertation issues
  @ParameterizedTest
  @Disabled
  @EnumSource(value = TestEntity.class, names = {"USER"}, mode = EnumSource.Mode.INCLUDE)
  void shouldConvertEntity(TestEntity entity) {

  }

  @ParameterizedTest
  @ArgumentsSource(BulkOperationEntityClassProvider.class)
  void shouldConvertEmptyEntity(Class<BulkOperationsEntity> clazz) {

    BulkOperationsEntity bean;
    if (clazz.equals(User.class)) {
      bean = new User();
    } else if (clazz.equals(Item.class)) {
      bean = new Item().withVersion(1);
    } else {
      bean = new HoldingsRecord().withVersion(2);
    }

    var strategy = new CustomMappingStrategy<BulkOperationsEntity>();
    String csv = null;

    strategy.setType(clazz);

    try (Writer writer  = new StringWriter()) {

      StatefulBeanToCsv<BulkOperationsEntity> sbc = new StatefulBeanToCsvBuilder<BulkOperationsEntity>(writer)
        .withSeparator(DEFAULT_SEPARATOR)
        .withApplyQuotesToAll(false)
        .withMappingStrategy(strategy)
        .build();

      sbc.write(bean);
      csv = writer.toString();
    } catch (Exception e) {
      Assertions.fail("Error parsing bean to CSV");
    }

    List<BulkOperationsEntity> list = new ArrayList<>();

    try (Reader reader = new StringReader(csv)) {
      CsvToBean<BulkOperationsEntity> cb = new CsvToBeanBuilder<BulkOperationsEntity>(reader)
        .withType(clazz)
        .withSkipLines(1)
        .build();
      list = cb.parse().stream().toList();
    } catch (IOException e) {
      Assertions.fail("Error parsing CSV to bean");
    }

    assertThat(list, hasSize(1));
  }
}
