package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.InstanceFormat;
import org.folio.bulkops.domain.bean.InstanceFormats;
import org.folio.bulkops.domain.bean.InstanceType;
import org.folio.bulkops.domain.bean.InstanceTypes;
import org.folio.bulkops.domain.dto.ContributorType;
import org.folio.bulkops.domain.dto.ContributorTypeCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.marc4j.marc.impl.DataFieldImpl;
import org.marc4j.marc.impl.LeaderImpl;
import org.marc4j.marc.impl.SubfieldImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;

class MarcToUnifiedTableRowMapperHelperTest extends BaseTest {
  @MockBean
  private InstanceReferenceService instanceReferenceService;
  @Autowired
  private MarcToUnifiedTableRowMapperHelper mapperHelper;

  @ParameterizedTest
  @ValueSource(chars = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','1','2','3','4','5','6','7','8','9','0'})
  void shouldResolveModeOfIssuance(char character) {
    var leader = new LeaderImpl();
    leader.setImplDefined1(new char[]{character});

    var res = mapperHelper.resolveModeOfIssuance(leader);

    switch (character) {
      case 'a', 'c', 'd', 'm' -> assertThat(res).isEqualTo("single unit");
      case 'b', 's' -> assertThat(res).isEqualTo("serial");
      case 'i' -> assertThat(res).isEqualTo("integrating resource");
      default -> assertThat(res).isEqualTo("unspecified");
    }
  }

  @Test
  void shouldFetchLanguages() {
    var dataField = new DataFieldImpl("041", ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "eng"));
    dataField.addSubfield(new SubfieldImpl('a', "fre"));

    var res = mapperHelper.fetchLanguages(dataField);

    assertThat(res).isEqualTo("English | French");
  }

  @ParameterizedTest
  @ValueSource(strings = {"100", "110", "111", "700", "710", "711", "720"})
  void shouldFetchContributorName(String tag) {
    var dataField = new DataFieldImpl(tag, ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "John Smith."));

    var res = mapperHelper.fetchContributorName(dataField);

    assertThat(res).isEqualTo("John Smith");
  }

  @ParameterizedTest
  @CsvSource(value = {"100,1,1", "110,1,1", "111,1,1", "700,1,1", "710,1,1", "711,1,1", "720,1,1", "720,2,1"})
  void shouldFetchContributorNameType(String tag, Character ind1, Character ind2) {
    var dataField = new DataFieldImpl(tag, ind1, ind2);

    var res = mapperHelper.fetchNameType(dataField);

    switch (tag) {
      case "100", "700" -> assertThat(res).isEqualTo("Personal name");
      case "110", "710" -> assertThat(res).isEqualTo("Corporate name");
      case "111", "711" -> assertThat(res).isEqualTo("Meeting name");
      case "720" -> assertThat(res).isEqualTo('2' == ind1 ? "Corporate name" : "Personal name");
    }
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
  100 | act | Actor
  100 | abc | Actor
  100 | abc | Text
  110 | act | Actor
  110 | abc | Actor
  110 | abc | Text
  111 | act | Actor
  111 | abc | Actor
  111 | abc | Text
  700 | act | Actor
  700 | abc | Actor
  700 | abc | Text
  710 | act | Actor
  710 | abc | Actor
  710 | abc | Text
  711 | act | Actor
  711 | abc | Actor
  711 | abc | Text
  720 | act | Actor
  720 | abc | Actor
  720 | abc | Text
    """, delimiter = '|')
  void shouldFetchContributorNameType(String tag, String s1, String s2) {
    var dataField = new DataFieldImpl(tag, ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('4', s1));
    dataField.addSubfield(new SubfieldImpl("111".equals(tag) || "711".equals(tag) ? 'j' : 'e', s2));

    when(instanceReferenceService.getContributorTypesByCode("act"))
      .thenReturn(new ContributorTypeCollection()
        .contributorTypes(Collections.singletonList(new ContributorType().name("Actor"))));
    when(instanceReferenceService.getContributorTypesByCode("abc"))
      .thenReturn(new ContributorTypeCollection()
        .contributorTypes(Collections.emptyList()));
    when(instanceReferenceService.getContributorTypesByName("Actor"))
      .thenReturn(new ContributorTypeCollection()
        .contributorTypes(Collections.singletonList(new ContributorType().name("Actor"))));
    when(instanceReferenceService.getContributorTypesByName("Text"))
      .thenReturn(new ContributorTypeCollection()
        .contributorTypes(Collections.emptyList()));

    var res = mapperHelper.fetchContributorType(dataField);

    if ("act".equals(s1) || "Actor".equals(s2)) {
      assertThat(res).isEqualTo("Actor");
    } else {
      assertThat(res).isEqualTo("Text");
    }
  }

  @Test
  void shouldFetchEdition() {
    var dataField = new DataFieldImpl("250", ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "4th ed."));
    dataField.addSubfield(new SubfieldImpl('b', "edited by Paul Watson.="));

    var res = mapperHelper.fetchEdition(dataField);

    assertThat(res).isEqualTo("4th ed. edited by Paul Watson.");
  }

  @Test
  void shouldFetchPhysicalDescription() {
    var dataField = new DataFieldImpl("300", ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "volumes :"));
    dataField.addSubfield(new SubfieldImpl('b', "illustrations (some color) ;"));
    dataField.addSubfield(new SubfieldImpl('c', "25 cm."));

    var res = mapperHelper.fetchPhysicalDescription(dataField);

    assertThat(res).isEqualTo("volumes : illustrations (some color) ; 25 cm.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"310", "321"})
  void shouldFetchPublicationFrequency(String tag) {
    var dataField = new DataFieldImpl(tag, ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "Monthly,"));
    dataField.addSubfield(new SubfieldImpl('b', "Jan. 1984"));

    var res = mapperHelper.fetchPublicationFrequency(dataField);

    assertThat(res).isEqualTo("Monthly, Jan. 1984");
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
  notated movement  | ntv | notated movement  | ntv
                    | ntv | notated movement  | ntv
  notated movement  |     | notated movement  | ntv
  text              |     | unspecified       | zzz
                    | txt | unspecified       | zzz
    """, delimiter = '|')
  void shouldFetchResourceType(String s1, String s2, String expectedName, String expectedCode) {
    var dataField = new DataFieldImpl("336", ' ', ' ');
    if (isNotEmpty(s1)) {
      dataField.addSubfield(new SubfieldImpl('a', s1));
    }
    if (isNotEmpty(s2)) {
      dataField.addSubfield(new SubfieldImpl('b', s2));
    }

    when(instanceReferenceService.getInstanceTypesByName("notated movement"))
      .thenReturn(InstanceTypes.builder()
        .types(Collections.singletonList(InstanceType.builder()
          .name("notated movement")
          .code("ntv")
          .source("rdacontent").build())).build());
    when(instanceReferenceService.getInstanceTypesByCode("ntv"))
      .thenReturn(InstanceTypes.builder()
        .types(Collections.singletonList(InstanceType.builder()
            .name("notated movement")
            .code("ntv")
            .source("rdacontent").build())).build());
    when(instanceReferenceService.getInstanceTypesByName("text"))
      .thenReturn(InstanceTypes.builder()
        .types(Collections.emptyList()).build());
    when(instanceReferenceService.getInstanceTypesByCode("txt"))
      .thenReturn(InstanceTypes.builder()
        .types(Collections.emptyList()).build());

    var res = mapperHelper.fetchResourceType(dataField);

    assertThat(res).isEqualTo(String.join(ARRAY_DELIMITER, expectedName, expectedCode, "rdacontent"));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"cz"})
  void shouldFetchInstanceFormats(String code) {
    var dataField = new DataFieldImpl("338", ' ', ' ');
    if (isNotEmpty(code)) {
      dataField.addSubfield(new SubfieldImpl('b', code));
    }

    when(instanceReferenceService.getInstanceFormatsByCode("cz"))
      .thenReturn(InstanceFormats.builder()
        .formats(Collections.singletonList(InstanceFormat.builder()
          .name("computer -- other").build())).build());
    when(instanceReferenceService.getInstanceFormatsByCode(null))
      .thenReturn(InstanceFormats.builder()
        .formats(Collections.emptyList()).build());

    var res = mapperHelper.fetchInstanceFormats(dataField);

    assertThat(res).isEqualTo(isEmpty(code) ? null : "computer -- other");
  }

  @Test
  void shouldFetchPublicationRange() {
    var dataField = new DataFieldImpl("362", ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "Began with 1962/64."));
    dataField.addSubfield(new SubfieldImpl('z', "Cf. New serial titles."));

    var res = mapperHelper.fetchPublicationRange(dataField);

    assertThat(res).isEqualTo("Began with 1962/64. Cf. New serial titles");
  }

  @Test
  void shouldFetchResourceTitle() {
    var dataField = new DataFieldImpl("245", ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "Title."));
    dataField.addSubfield(new SubfieldImpl('b', "basic level."));
    dataField.addSubfield(new SubfieldImpl('n', "Part one."));
    dataField.addSubfield(new SubfieldImpl('p', "Student handbook."));

    var res = mapperHelper.fetchResourceTitle(dataField);

    assertThat(res).isEqualTo("Title. Part one. Student handbook. basic level.");
  }

  @ParameterizedTest
  @ValueSource(chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'})
  void shouldFetchIndexTitle(char ind2) {
    var title = "abcdefghijklm.";
    var dataField = new DataFieldImpl("245", ' ', ind2);
    dataField.addSubfield(new SubfieldImpl('a', title));
    dataField.addSubfield(new SubfieldImpl('b', "basic level."));
    dataField.addSubfield(new SubfieldImpl('n', "Part one."));
    dataField.addSubfield(new SubfieldImpl('p', "Student handbook."));
    var expectedTitle = StringUtils.capitalize(title.substring(Character.getNumericValue(ind2)));

    var res = mapperHelper.fetchIndexTitle(dataField);

    assertThat(res).isEqualTo(expectedTitle + " Part one. Student handbook. basic level.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"800", "810", "811", "830"})
  void shouldFetchSeries(String tag) {
    var dataField = new DataFieldImpl(tag, ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "Berenholtz, Jim,"));
    dataField.addSubfield(new SubfieldImpl('d', "1957-"));
    dataField.addSubfield(new SubfieldImpl('t', "Teachings of the feathered serpent ;"));
    dataField.addSubfield(new SubfieldImpl('v', "vbk. 1."));

    var res = mapperHelper.fetchSeries(dataField);

    assertThat(res).isEqualTo("Berenholtz, Jim 1957- Teachings of the feathered serpent  vbk. 1.");
  }

  @Test
  void shouldFetchNotes() {
    var dataField = new DataFieldImpl("520", ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "subfield a."));
    dataField.addSubfield(new SubfieldImpl('b', "subfield b."));

    var res = mapperHelper.fetchNotes(dataField);

    assertThat(res).isEqualTo("subfield a. subfield b");
  }

  @ParameterizedTest
  @CsvSource(value = {"541,1", "542,1", "561,1", "583,1", "590,1", "541,0", "542,0", "561,0", "583,0", "590,0"})
  void shouldAddStaffOnlyPostfixIfIndicator1IsZero(String tag, Character ind1) {
    var dataField = new DataFieldImpl(tag, ind1, ' ');
    dataField.addSubfield(new SubfieldImpl('a', "subfield a."));

    var res = mapperHelper.fetchNotes(dataField);

    assertThat(res).isEqualTo('0' == ind1 ? "subfield a (staff only)" : "subfield a");
  }
}
