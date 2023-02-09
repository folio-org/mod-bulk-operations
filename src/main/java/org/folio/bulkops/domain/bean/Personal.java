package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opencsv.bean.AbstractBeanField;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.converter.DateConverter;
import org.folio.bulkops.domain.converter.StringConverter;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.service.UserReferenceService;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.adapters.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.adapters.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.adapters.Constants.ITEM_DELIMITER_PATTERN;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Personal {
  @JsonProperty("lastName")
  @CsvCustomBindByName(column = "Last name", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 9, converter = StringConverter.class)
  private String lastName;

  @JsonProperty("firstName")
  @CsvCustomBindByName(column = "First name", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 10, converter = StringConverter.class)
  private String firstName;

  @JsonProperty("middleName")
  @CsvCustomBindByName(column = "Middle name", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 11, converter = StringConverter.class)
  private String middleName;

  @JsonProperty("preferredFirstName")
  @CsvCustomBindByName(column = "Preferred first name", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 12, converter = StringConverter.class)
  private String preferredFirstName;

  @JsonProperty("email")
  @CsvCustomBindByName(column = "Email", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 13, converter = StringConverter.class)
  private String email;

  @JsonProperty("phone")
  @CsvCustomBindByName(column = "Phone", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 14, converter = StringConverter.class)
  private String phone;

  @JsonProperty("mobilePhone")
  @CsvCustomBindByName(column = "Mobile phone", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 15, converter = StringConverter.class)
  private String mobilePhone;

  @JsonProperty("dateOfBirth")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @CsvCustomBindByName(column = "Date of birth", converter = DateConverter.class)
  @CsvCustomBindByPosition(position = 16, converter = DateConverter.class)
  private Date dateOfBirth;

  @JsonProperty("addresses")
  @Valid
  @CsvCustomBindByName(column = "Addresses", converter = AddressesConverter.class)
  @CsvCustomBindByPosition(position = 17, converter = AddressesConverter.class)
  private List<Address> addresses = null;

  @JsonProperty("preferredContactTypeId")
  @CsvCustomBindByName(column = "Preferred contact type id", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 18, converter = StringConverter.class)
  private String preferredContactTypeId;

  public static class AddressesConverter extends AbstractBeanField<String, List<Address>> {

    private static final int ADDRESS_ID = 0;
    private static final int ADDRESS_COUNTRY_ID = 1;
    private static final int ADDRESS_LINE_1 = 2;
    private static final int ADDRESS_LINE_2 = 3;
    private static final int ADDRESS_CITY = 4;
    private static final int ADDRESS_REGION = 5;
    private static final int ADDRESS_POSTAL_CODE = 6;
    private static final int ADDRESS_PRIMARY_ADDRESS = 7;
    private static final int ADDRESS_TYPE = 8;

    @Override
    protected List<Address> convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
      String[] addresses = value.split(ITEM_DELIMITER_PATTERN);
      if (addresses.length > 0) {
        return Arrays.stream(addresses)
          .parallel()
          .filter(StringUtils::isNotEmpty)
          .map(this::getAddressFromString)
          .collect(Collectors.toList());
      }
      return Collections.emptyList();
    }

    @Override
    protected String convertToWrite(Object value) {
      var addresses = (List<Address>) value;
      if (nonNull(addresses)) {
        return addresses.stream()
          .map(this::addressToString)
          .collect(Collectors.joining(ITEM_DELIMITER));
      }
      return EMPTY;
    }

    private Address getAddressFromString(String stringAddress) {
      Address address = new Address();
      List<String> addressFields = SpecialCharacterEscaper.restore(Arrays.asList(stringAddress.split(ARRAY_DELIMITER)));
      address.setId(addressFields.get(ADDRESS_ID));
      address.setCountryId(addressFields.get(ADDRESS_COUNTRY_ID));
      address.setAddressLine1(addressFields.get(ADDRESS_LINE_1));
      address.setAddressLine2(addressFields.get(ADDRESS_LINE_2));
      address.setCity(addressFields.get(ADDRESS_CITY));
      address.setRegion(addressFields.get(ADDRESS_REGION));
      address.setPostalCode(addressFields.get(ADDRESS_POSTAL_CODE));
      address.setPrimaryAddress(Boolean.valueOf(addressFields.get(ADDRESS_PRIMARY_ADDRESS)));
      address.setAddressTypeId(UserReferenceService.service().getAddressTypeIdByDesc(addressFields.get(ADDRESS_TYPE)));
      return address;
    }

    private String addressToString(Address address) {
      List<String> addressData = new ArrayList<>();
      addressData.add(ofNullable(address.getId()).orElse(EMPTY));
      addressData.add(ofNullable(address.getCountryId()).orElse(EMPTY));
      addressData.add(ofNullable(address.getAddressLine1()).orElse(EMPTY));
      addressData.add(ofNullable(address.getAddressLine2()).orElse(EMPTY));
      addressData.add(ofNullable(address.getCity()).orElse(EMPTY));
      addressData.add(ofNullable(address.getRegion()).orElse(EMPTY));
      addressData.add(ofNullable(address.getPostalCode()).orElse(EMPTY));
      addressData.add(nonNull(address.getPrimaryAddress()) ? address.getPrimaryAddress().toString() : EMPTY);
      addressData.add(UserReferenceService.service().getAddressTypeDescById(address.getAddressTypeId()));
      return String.join(ARRAY_DELIMITER, SpecialCharacterEscaper.escape(addressData));
    }
  }
}

