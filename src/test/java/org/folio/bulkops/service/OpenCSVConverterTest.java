package org.folio.bulkops.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.AddressType;
import org.folio.bulkops.domain.bean.AddressTypeCollection;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.CallNumberType;
import org.folio.bulkops.domain.bean.CallNumberTypeCollection;
import org.folio.bulkops.domain.bean.CirculationNote;
import org.folio.bulkops.domain.bean.CustomField;
import org.folio.bulkops.domain.bean.CustomFieldCollection;
import org.folio.bulkops.domain.bean.CustomFieldTypes;
import org.folio.bulkops.domain.bean.DamagedStatus;
import org.folio.bulkops.domain.bean.DamagedStatusCollection;
import org.folio.bulkops.domain.bean.Department;
import org.folio.bulkops.domain.bean.DepartmentCollection;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.domain.bean.ElectronicAccessRelationship;
import org.folio.bulkops.domain.bean.ElectronicAccessRelationshipCollection;
import org.folio.bulkops.domain.bean.Format;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.HoldingsNoteTypeCollection;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.HoldingsRecordsSourceCollection;
import org.folio.bulkops.domain.bean.HoldingsType;
import org.folio.bulkops.domain.bean.HoldingsTypeCollection;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.InstanceFormat;
import org.folio.bulkops.domain.bean.InstanceFormats;
import org.folio.bulkops.domain.bean.InstanceStatus;
import org.folio.bulkops.domain.bean.InstanceStatuses;
import org.folio.bulkops.domain.bean.InstanceType;
import org.folio.bulkops.domain.bean.InstanceTypes;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.ItemLocationCollection;
import org.folio.bulkops.domain.bean.ItemNote;
import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.domain.bean.LoanTypeCollection;
import org.folio.bulkops.domain.bean.MaterialType;
import org.folio.bulkops.domain.bean.MaterialTypeCollection;
import org.folio.bulkops.domain.bean.ModeOfIssuance;
import org.folio.bulkops.domain.bean.ModesOfIssuance;
import org.folio.bulkops.domain.bean.NatureOfContentTerm;
import org.folio.bulkops.domain.bean.NatureOfContentTerms;
import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.bean.NoteTypeCollection;
import org.folio.bulkops.domain.bean.SelectField;
import org.folio.bulkops.domain.bean.SelectFieldOption;
import org.folio.bulkops.domain.bean.SelectFieldOptions;
import org.folio.bulkops.domain.bean.StatisticalCode;
import org.folio.bulkops.domain.bean.StatisticalCodeCollection;
import org.folio.bulkops.domain.bean.Tags;
import org.folio.bulkops.domain.bean.TextField;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserGroup;
import org.folio.bulkops.domain.bean.UserGroupCollection;
import org.folio.bulkops.domain.converter.CustomMappingStrategy;
import org.folio.bulkops.exception.ConverterException;
import org.folio.bulkops.exception.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.opencsv.ICSVWriter.DEFAULT_SEPARATOR;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_REF_ID;
import static org.folio.bulkops.util.Utils.encode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Log4j2
@ExtendWith(MockitoExtension.class)
class OpenCSVConverterTest extends BaseTest {

  private static class BulkOperationEntityClassProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
        Arguments.of(User.class),
        Arguments.of(Item.class),
        Arguments.of(HoldingsRecord.class),
        Arguments.of(Instance.class)
      );
    }
  }

  @ParameterizedTest
  @ArgumentsSource(BulkOperationEntityClassProvider.class)
  void shouldConvertEntity(Class<BulkOperationsEntity> clazz) throws IOException {
    initMocks();

    /* JSON -> Object */
    var bean = objectMapper.readValue(new FileInputStream(getPathToSample(clazz)), clazz);
    if (bean instanceof Item item) {
      mockItemData();
      bean = buildItem(item);
    }

    /* Object -> CSV */
    var strategy = new CustomMappingStrategy<BulkOperationsEntity>();
    String csv = null;
    strategy.setType(clazz);
    try (Writer writer = new StringWriter()) {
      StatefulBeanToCsv<BulkOperationsEntity> sbc = new StatefulBeanToCsvBuilder<BulkOperationsEntity>(writer)
        .withSeparator(DEFAULT_SEPARATOR)
        .withApplyQuotesToAll(false)
        .withMappingStrategy(strategy)
        .build();
      sbc.write(bean);
      csv = writer.toString();
    } catch (Exception e) {
      Assertions.fail("Error parsing bean to CSV", e);
    }

    /* CSV -> Object */
    List<BulkOperationsEntity> list = new ArrayList<>();
    try (Reader reader = new StringReader(csv)) {
      CsvToBean<BulkOperationsEntity> cb = new CsvToBeanBuilder<BulkOperationsEntity>(reader)
        .withType(clazz)
        .withSkipLines(1)
        .build();
      list = cb.parse().stream().toList();
    } catch (IOException e) {
      Assertions.fail("Error parsing CSV to bean", e);
    }

    assertThat(list).hasSize(1);
    /* compare original and restored beans */
    var result = list.get(0);

    if (result instanceof Item item) {
      assertThat(item)
        .usingRecursiveComparison()
        .ignoringFields("circulationNotes", "effectiveCallNumberComponents", "metadata", "status.date")
        .isEqualTo(bean);
    } else if (result instanceof Instance instance) {
      assertThat(instance)
        .usingRecursiveComparison()
        .ignoringFields("version", "contributors.contributorTypeId", "contributors.contributorTypeText", "contributors.contributorNameTypeId", "contributors.authorityId", "contributors.primary", "matchKey", "alternativeTitles", "identifiers", "subjects", "classifications", "publications", "publicationPeriod", "electronicAccesses", "instanceNotes", "statisticalCodeIds", "sourceRecordFormat", "statusUpdatedDate", "tags", "precedingTitles", "succeedingTitles")
        .isEqualTo(bean);
    } else if (result instanceof HoldingsRecord holdingsRecord) {
      assertThat(holdingsRecord)
        .usingRecursiveComparison()
        .ignoringFields("discoverySuppress", "notes", "version", "instanceId", "effectiveLocationId", "receivingHistory", "metadata")
        .isEqualTo(bean);
    }

  }

  private void mockItemData() {
    when(relationshipClient.getById("f5d0068e-6272-458e-8a81-b85e7b9a14aa"))
      .thenReturn(new ElectronicAccessRelationship().withId("f5d0068e-6272-458e-8a81-b85e7b9a14aa").withName("EAR"));

    when(itemNoteTypeClient.getNoteTypeById("Item@Note@NameX"))
      .thenReturn(NoteType.builder().id("Item@Note@NameX").name("note").build());

    when(itemNoteTypeClient.getNoteTypeById("Item@Note@Name_2"))
      .thenReturn(NoteType.builder().id("Item@Note@Name_2").name("provenance").build());

    when(itemNoteTypeClient.getNoteTypesByQuery(String.format(QUERY_PATTERN_NAME, encode("note")), 1))
      .thenReturn(NoteTypeCollection.builder().itemNoteTypes(List.of(NoteType.builder().id("Item@Note@NameX")
        .name("note").build())).build());

    when(itemNoteTypeClient.getNoteTypesByQuery(String.format(QUERY_PATTERN_NAME, encode("provenance")), 1))
      .thenReturn(NoteTypeCollection.builder().itemNoteTypes(List.of(NoteType.builder().id("Item@Note@Name_2")
        .name("provenance").build())).build());

    when(statisticalCodeClient.getById(anyString())).thenReturn(new StatisticalCode()
      .withId("1c622d0f-2e91-4c30-ba43-2750f9735f51")
      .withCode("St@tistical-Code#1"));
  }

  private static Item buildItem(Item item) {
    return item
      .withAdministrativeNotes(Collections.singletonList("Administrative note"))
      .withElectronicAccess(Collections.singletonList(ElectronicAccess.builder()
        .relationshipId("f5d0068e-6272-458e-8a81-b85e7b9a14aa")
        .uri("http://example2.org")
        .linkText("Link text")
        .materialsSpecification("Materials")
        .publicNote("note").build()))
      .withFormerIds(Collections.singletonList("Former identifier"))
      .withNotes(List.of(ItemNote.builder().itemNoteTypeId("Item@Note@NameX").note("note").staffOnly(true).tenantId("tenant").build(),
        ItemNote.builder().itemNoteTypeId("Item@Note@Name_2").note("provenance").staffOnly(false).tenantId("tenant").build()))
      .withStatisticalCodes(List.of("1c622d0f-2e91-4c30-ba43-2750f9735f51"))
      .withTags(Tags.builder().tagList(Collections.emptyList()).build())
      .withYearCaption(List.of("2001/11/12"))
      .withCheckInNotes(Collections.singletonList(CirculationNote.builder().note("Check in note").staffOnly(true).build()))
      .withCheckOutNotes(Collections.singletonList(CirculationNote.builder().note("Check out note").staffOnly(false).build()));
  }

  @ParameterizedTest
  @ArgumentsSource(BulkOperationEntityClassProvider.class)
  void shouldConvertBadDataEntity(Class<BulkOperationsEntity> clazz) throws IOException {

    initMocks();

    /* JSON -> Bean */
    var bean = objectMapper.readValue(new FileInputStream(getPathToBadData(clazz)), clazz);

    /* Bean -> CSV */
    var strategy = new CustomMappingStrategy<BulkOperationsEntity>();
    String csv = null;
    strategy.setType(clazz);
    try (Writer writer = new StringWriter()) {
      StatefulBeanToCsv<BulkOperationsEntity> sbc = new StatefulBeanToCsvBuilder<BulkOperationsEntity>(writer)
        .withSeparator(DEFAULT_SEPARATOR)
        .withApplyQuotesToAll(false)
        .withThrowExceptions(true)
        .withMappingStrategy(strategy)
        .build();

      csv = process(sbc, writer, bean);
    } catch (Exception e) {
      Assertions.fail("Error parsing bean to CSV", e);
    }

    /* CSV -> Bean */
    List<BulkOperationsEntity> list = new ArrayList<>();
    try (Reader reader = new StringReader(csv)) {
      CsvToBean<BulkOperationsEntity> cb = new CsvToBeanBuilder<BulkOperationsEntity>(reader)
        .withType(clazz)
        .withThrowExceptions(false)
        .withSkipLines(1)
        .build();
      list = cb.parse().stream().toList();
      assertThat(cb.getCapturedExceptions()).hasSize(1);
    } catch (IOException e) {
      Assertions.fail("Error parsing CSV to bean", e);
    }

    /* no NPE expected */
    assertThat(list).isEmpty();
  }

  @SneakyThrows
  public String process(StatefulBeanToCsv<BulkOperationsEntity> sbc, Writer writer, BulkOperationsEntity bean ) {
    try {
      sbc.write(bean);
      return writer.toString();
    } catch (ConverterException e) {
      return process(sbc, writer, bean);
    }
  }


  @ParameterizedTest
  @ArgumentsSource(BulkOperationEntityClassProvider.class)
  void shouldConvertEmptyEntity(Class<BulkOperationsEntity> clazz) {

    BulkOperationsEntity bean = getEmptyBean(clazz);

    var strategy = new CustomMappingStrategy<BulkOperationsEntity>();
    String csv = null;

    strategy.setType(clazz);

    try (Writer writer = new StringWriter()) {

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

    assertThat(list).hasSize(1);
  }

  private static BulkOperationsEntity getEmptyBean(Class<BulkOperationsEntity> clazz) {
    BulkOperationsEntity bean;
    if (clazz.equals(User.class)) {
      bean = new User();
    } else if (clazz.equals(Item.class)) {
      bean = new Item().withVersion(1);
    } else if (clazz.equals(HoldingsRecord.class)) {
      bean = new HoldingsRecord().withVersion(2);
    } else {
      bean = new Instance();
    }
    return bean;
  }

  private static String getPathToSample(Class<BulkOperationsEntity> clazz) {
    if (clazz.equals(User.class)) {
      return "src/test/resources/files/complete_user.json";
    } else if (clazz.equals(Item.class)) {
      return "src/test/resources/files/complete_item.json";
    } else if (clazz.equals(HoldingsRecord.class)) {
      return "src/test/resources/files/complete_holding_record.json";
    } else {
      return "src/test/resources/files/instance.json";
    }
  }

  private static String getPathToBadData(Class<BulkOperationsEntity> clazz) {
    if (clazz.equals(User.class)) {
      return "src/test/resources/files/bad_data_user.json";
    } else if (clazz.equals(Item.class)) {
      return "src/test/resources/files/bad_data_item.json";
    } else if (clazz.equals(HoldingsRecord.class)) {
      return "src/test/resources/files/bad_data_holding_record.json";
    } else {
      return "src/test/resources/files/bad_data_instance.json";
    }
  }

  private void initMocks() {
    // User
    Mockito.reset(groupClient);
    when(departmentClient.getDepartmentById("fae76e1a-3cf4-4640-8731-0d583ddaf571")).thenThrow(new NotFoundException("Not found"));
    when(groupClient.getGroupById("503a81cd-6c26-400f-b620-14c08943697c")).thenReturn(
      new UserGroup().withId("503a81cd-6c26-400f-b620-14c08943697c").withGroup("staff"));
    when(departmentClient.getDepartmentById("4ac6b846-e184-4cdd-8101-68f4af97d103")).thenReturn(new Department()
      .withId("4ac6b846-e184-4cdd-8101-68f4af97d103")
      .withName("Department"));
    when(addressTypeClient.getAddressTypeById(any())).thenReturn(new AddressType().withId("93d3d88d-499b-45d0-9bc7-ac73c3a19880").withDesc("desc").withAddressType("work"));
    when(groupClient.getByQuery("group==\"staff\"")).thenReturn(new UserGroupCollection().withUsergroups(List.of(new UserGroup().withId("503a81cd-6c26-400f-b620-14c08943697c").withGroup("staff"))));
    when(departmentClient.getByQuery("name==\"Department\"")).thenReturn(new DepartmentCollection()
      .withDepartments(List.of(new Department()
        .withId("4ac6b846-e184-4cdd-8101-68f4af97d103")
        .withName("Department"))));
    when(addressTypeClient.getByQuery("desc==\"desc\"")).thenReturn(new AddressTypeCollection().withAddressTypes(List.of(new AddressType().withId("93d3d88d-499b-45d0-9bc7-ac73c3a19880").withDesc("desc").withAddressType("work"))));
    when(addressTypeClient.getByQuery("addressType==\"work\"")).thenReturn(new AddressTypeCollection().withAddressTypes(List.of(new AddressType().withId("93d3d88d-499b-45d0-9bc7-ac73c3a19880").withDesc("desc").withAddressType("work"))));
    when(okapiClient.getModuleIds(any(), any(), any())).thenReturn(JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put("id", "USERS")));
    when(customFieldsClient.getByQuery(any(), eq(format(QUERY_PATTERN_NAME, encode("sierraCheckoutInformation"))))).thenReturn(new CustomFieldCollection().withCustomFields(List.of(new CustomField()
      .withName("sierraCheckoutInformation")
      .withType(CustomFieldTypes.TEXTBOX_LONG)
      .withRefId("sierraCheckoutInformation")
      .withSelectField(new SelectField().withOptions(new SelectFieldOptions().withValues(List.of(new SelectFieldOption().withValue("10")))))
      .withTextField(new TextField().withFieldFormat(Format.TEXT)))));
    when(customFieldsClient.getByQuery(any(), eq(format(QUERY_PATTERN_REF_ID, encode("sierraCheckoutInformation"))))).thenReturn(new CustomFieldCollection().withCustomFields(List.of(new CustomField()
      .withName("sierraCheckoutInformation")
      .withType(CustomFieldTypes.TEXTBOX_LONG)
      .withRefId("sierraCheckoutInformation")
      .withSelectField(new SelectField().withOptions(new SelectFieldOptions().withValues(List.of(new SelectFieldOption().withValue("10")))))
      .withTextField(new TextField().withFieldFormat(Format.TEXT)))));

    // Item
    when(relationshipClient.getById("f5d0068e-6272-458e-8a81-b85e7b9a14aa")).thenReturn(
      new ElectronicAccessRelationship()
        .withId("f5d0068e-6272-458e-8a81-b85e7b9a14aa")
        .withName("EAR"));
    when(relationshipClient.getByQuery("name==EAR")).thenReturn(
      new ElectronicAccessRelationshipCollection().withElectronicAccessRelationships(Collections.singletonList(new ElectronicAccessRelationship()
        .withId("f5d0068e-6272-458e-8a81-b85e7b9a14aa")
        .withName("EAR"))));
    when(callNumberTypeClient.getById("5ba6b62e-6858-490a-8102-5b1369873835"))
      .thenReturn(new CallNumberType()
        .withId("5ba6b62e-6858-490a-8102-5b1369873835")
        .withName("Call-number@type")
        .withSource("s@urce"));
    when(damagedStatusClient.getById("54d1dd76-ea33-4bcb-955b-6b29df4f7930")).thenReturn(new DamagedStatus()
      .withId("54d1dd76-ea33-4bcb-955b-6b29df4f7930")
      .withName("@damaged/status")
      .withSource("-source-"));
    when(itemNoteTypeClient.getNoteTypeById("1dde7141-ec8a-4dae-9825-49ce14c728e7")).thenReturn(new NoteType().withId("1dde7141-ec8a-4dae-9825-49ce14c728e7").withName("Item@Note@NameX"));
    when(itemNoteTypeClient.getNoteTypeById("c3a539b9-9576-4e3a-b6de-d910200b2919")).thenReturn(new NoteType().withId("c3a539b9-9576-4e3a-b6de-d910200b2919").withName("Item@Note@Name_2"));
    when(statisticalCodeClient.getById("1c622d0f-2e91-4c30-ba43-2750f9735f51")).thenReturn(new StatisticalCode()
      .withId("1c622d0f-2e91-4c30-ba43-2750f9735f51")
      .withCode("St@tistical-Code#1"));
    when(statisticalCodeClient.getById("c7a32c50-ea7c-43b7-87ab-d134c8371330")).thenReturn(new StatisticalCode()
      .withId("c7a32c50-ea7c-43b7-87ab-d134c8371330")
      .withCode("St@tistical-Code-2"));
    when(callNumberTypeClient.getByQuery("name==\"Call-number@type\"")).thenReturn(new CallNumberTypeCollection()
      .withCallNumberTypes(List.of((new CallNumberType()
        .withId("5ba6b62e-6858-490a-8102-5b1369873835")
        .withName("Call-number@type")
        .withSource("s@urce")))));
    when(damagedStatusClient.getByQuery("name==\"@damaged/status\"")).thenReturn(new DamagedStatusCollection()
      .withItemDamageStatuses(List.of(new DamagedStatus()
        .withId("54d1dd76-ea33-4bcb-955b-6b29df4f7930")
        .withName("@damaged/status")
        .withSource("-source-"))));
    when(itemNoteTypeClient.getNoteTypesByQuery("name==\"Item@Note@NameX\"", 1)).thenReturn(new NoteTypeCollection().withItemNoteTypes(List.of(new NoteType().withId("1dde7141-ec8a-4dae-9825-49ce14c728e7").withName("Item@Note@NameX"))));
    when(itemNoteTypeClient.getNoteTypesByQuery("name==\"Item@Note@Name_2\"", 1)).thenReturn(new NoteTypeCollection().withItemNoteTypes(List.of(new NoteType().withId("c3a539b9-9576-4e3a-b6de-d910200b2919").withName("Item@Note@Name_2"))));
    when(materialTypeClient.getByQuery("name==\"microform\"")).thenReturn(new MaterialTypeCollection().withMtypes(List.of(new MaterialType().withId("fd6c6515-d470-4561-9c32-3e3290d4ca98").withName("microform"))));
    when(loanTypeClient.getByQuery("name==\"Can circulate\"")).thenReturn(new LoanTypeCollection().withLoantypes(List.of(new LoanType().withId("2b94c631-fca9-4892-a730-03ee529ffe27").withName("Can circulate"))));
    when(loanTypeClient.getByQuery("name==\"Reading room\"")).thenReturn(new LoanTypeCollection().withLoantypes(List.of(new LoanType().withId("2e48e713-17f3-4c13-a9f8-23845bb210a4").withName("Reading room"))));
    when(statisticalCodeClient.getByQuery("code==\"St@tistical-Code#1\"")).thenReturn(new StatisticalCodeCollection()
      .withStatisticalCodes(List.of(new StatisticalCode()
        .withId("1c622d0f-2e91-4c30-ba43-2750f9735f51")
        .withCode("St@tistical-Code#1"))));
    when(statisticalCodeClient.getByQuery("code==\"St@tistical-Code-2\"")).thenReturn(new StatisticalCodeCollection()
      .withStatisticalCodes(List.of(new StatisticalCode()
        .withId("c7a32c50-ea7c-43b7-87ab-d134c8371330")
        .withCode("St@tistical-Code-2"))));
    when(locationClient.getByQuery("name==\"Main Library\"")).thenReturn(new ItemLocationCollection().withLocations(List.of(new ItemLocation().withId("fcd64ce1-6995-48f0-840e-89ffa2288371").withName("Main Library"))));
    when(locationClient.getByQuery("name==\"Popular Reading Collection\"")).thenReturn(new ItemLocationCollection().withLocations(List.of(new ItemLocation().withId("b241764c-1466-4e1d-a028-1a3684a5da87").withName("Popular Reading Collection"))));
    when(damagedStatusClient.getById("bd563d7a-42be-4b2d-bf77-97896e0c1dde")).thenThrow(new NotFoundException("No found"));

    // Holdings record
    when(holdingsTypeClient.getById("0c422f92-0f4d-4d32-8cbe-390ebc33a3e5")).thenReturn(new HoldingsType().withId("0c422f92-0f4d-4d32-8cbe-390ebc33a3e5").withName("Holdings type").withSource("Holdings source"));
    when(locationClient.getLocationById("fcd64ce1-6995-48f0-840e-89ffa2288371")).thenReturn(new ItemLocation().withId("fcd64ce1-6995-48f0-840e-89ffa2288371").withName("Main Library"));
    when(locationClient.getLocationById("b241764c-1466-4e1d-a028-1a3684a5da87")).thenReturn(new ItemLocation().withId("b241764c-1466-4e1d-a028-1a3684a5da87").withName("Popular Reading Collection"));
    when(callNumberTypeClient.getById("cd70562c-dd0b-42f6-aa80-ce803d24d4a1"))
      .thenReturn(new CallNumberType()
        .withId("cd70562c-dd0b-42f6-aa80-ce803d24d4a1")
        .withName("Call-number@type_holding")
        .withSource("s@urce-holding"));
    when(holdingsNoteTypeClient.getNoteTypeById("88914775-f677-4759-b57b-1a33b90b24e0")).thenReturn(new HoldingsNoteType().withId("88914775-f677-4759-b57b-1a33b90b24e0").withName("Holding#Type#Name"));
    when(statisticalCodeClient.getById("264c4f94-1538-43a3-8b40-bed68384b31b")).thenReturn(new StatisticalCode()
      .withId("264c4f94-1538-43a3-8b40-bed68384b31b")
      .withCode("ST1")
      .withName("St@tistical-Code-holding_1"));
    when(statisticalCodeClient.getById("0868921a-4407-47c9-9b3e-db94644dbae7")).thenReturn(new StatisticalCode()
      .withId("0868921a-4407-47c9-9b3e-db94644dbae7")
      .withCode("ST2")
      .withName("St@tistical-Code-holding_2"));
    when(holdingsSourceClient.getById("f32d531e-df79-46b3-8932-cdd35f7a2264")).thenReturn(new HoldingsRecordsSource().withId("f32d531e-df79-46b3-8932-cdd35f7a2264").withName("Holdings@record@source"));
    when(holdingsTypeClient.getByQuery("name==\"Holdings type\"")).thenReturn(new HoldingsTypeCollection().withHoldingsTypes(List.of(new HoldingsType().withId("0c422f92-0f4d-4d32-8cbe-390ebc33a3e5").withName("Holdings type").withSource("Holdings source"))));
    when(locationClient.getByQuery("name==\"Location#1\"")).thenReturn(new ItemLocationCollection().withLocations(List.of(new ItemLocation().withId("fcd64ce1-6995-48f0-840e-89ffa2288371").withName("Location#1"))));
    when(locationClient.getByQuery("name==\"Location#2\"")).thenReturn(new ItemLocationCollection().withLocations(List.of(new ItemLocation().withId("b241764c-1466-4e1d-a028-1a3684a5da87").withName("Popular Reading Collection"))));
    when(callNumberTypeClient.getByQuery("name==\"Call-number@type_holding\"")).thenReturn(new CallNumberTypeCollection().withCallNumberTypes(List.of(new CallNumberType()
      .withId("cd70562c-dd0b-42f6-aa80-ce803d24d4a1")
      .withName("Call-number@type_holding")
      .withSource("s@urce-holding"))));
    when(holdingsNoteTypeClient.getNoteTypesByQuery("name==\"Holding#Type#Name\"", 1)).thenReturn(new HoldingsNoteTypeCollection().withHoldingsNoteTypes(List.of(new HoldingsNoteType().withId("88914775-f677-4759-b57b-1a33b90b24e0").withName("Holding#Type#Name"))));
    when(statisticalCodeClient.getByQuery("name==\"St@tistical-Code-holding_1\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(List.of(new StatisticalCode()
      .withId("264c4f94-1538-43a3-8b40-bed68384b31b")
      .withCode("ST1")
      .withName("St@tistical-Code-holding_1"))));
    when(statisticalCodeClient.getByQuery("name==\"St@tistical-Code-holding_2\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(List.of(new StatisticalCode()
      .withId("0868921a-4407-47c9-9b3e-db94644dbae7")
      .withCode("ST2")
      .withName("St@tistical-Code-holding_2"))));
    when(holdingsSourceClient.getByQuery("name==\"Holdings@record@source\"")).thenReturn(new HoldingsRecordsSourceCollection().withHoldingsRecordsSources(List.of(new HoldingsRecordsSource().withId("f32d531e-df79-46b3-8932-cdd35f7a2264").withName("Holdings@record@source"))));
    when(holdingsSourceClient.getById("a06889ff-d178-4dc8-815a-06f7f97bf975")).thenThrow(new NotFoundException("Not found"));

    // Instance
    var instanceStatus = InstanceStatus.builder().id("2a340d34-6b70-443a-bb1b-1b8d1c65d862").name("Other").build();
    when(instanceStatusesClient.getById("2a340d34-6b70-443a-bb1b-1b8d1c65d862")).thenReturn(instanceStatus);
    when(instanceStatusesClient.getByQuery("name==\"Other\"", 1)).thenReturn(InstanceStatuses.builder()
      .statuses(Collections.singletonList(instanceStatus))
      .totalRecords(1)
      .build());

    var modeOfIssuance = ModeOfIssuance.builder().id("068b5344-e2a6-40df-9186-1829e13cd344").name("serial").build();
    when(modesOfIssuanceClient.getById("068b5344-e2a6-40df-9186-1829e13cd344")).thenReturn(modeOfIssuance);
    when(modesOfIssuanceClient.getByQuery("name==\"serial\"", 1)).thenReturn(ModesOfIssuance.builder()
      .modes(Collections.singletonList(modeOfIssuance))
      .totalRecords(1)
      .build());

    var instanceType = InstanceType.builder().id("6312d172-f0cf-40f6-b27d-9fa8feaf332f").name("text").build();
    when(instanceTypesClient.getById("6312d172-f0cf-40f6-b27d-9fa8feaf332f")).thenReturn(instanceType);
    when(instanceTypesClient.getByQuery("name==\"text\"", 1)).thenReturn(InstanceTypes.builder()
      .types(Collections.singletonList(instanceType))
      .totalRecords(1)
      .build());

    var natureOfContentTerm = NatureOfContentTerm.builder().id("44cd89f3-2e76-469f-a955-cc57cb9e0395").name("textbook").build();
    when(natureOfContentTermsClient.getById("44cd89f3-2e76-469f-a955-cc57cb9e0395")).thenReturn(natureOfContentTerm);
    when(natureOfContentTermsClient.getByQuery("name==\"textbook\"", 1)).thenReturn(NatureOfContentTerms.builder()
      .terms(Collections.singletonList(natureOfContentTerm))
      .totalRecords(1)
      .build());

    var instanceFormat = InstanceFormat.builder().id("fe1b9adb-e0cf-4e05-905f-ce9986279404").name("computer -- other").build();
    when(instanceFormatsClient.getById("fe1b9adb-e0cf-4e05-905f-ce9986279404")).thenReturn(instanceFormat);
    when(instanceFormatsClient.getByQuery("name==\"computer -- other\"", 1)).thenReturn(InstanceFormats.builder()
      .formats(Collections.singletonList(instanceFormat))
      .totalRecords(1)
      .build());
  }
}
