package org.folio.bulkops.domain.converter;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.Address;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.service.UserReferenceHelper;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public  class AddressesConverter extends AbstractBeanField<String, List<Address>> {

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
      try {
        return Arrays.stream(addresses)
          .filter(StringUtils::isNotEmpty)
          .map(this::getAddressFromString)
          .collect(Collectors.toList());
      } catch (Exception e) {
        throw new CsvConstraintViolationException(String.format("Addresses were not found: %s", e.getMessage()));
      }
    }
    return Collections.emptyList();
  }

  @Override
  protected String convertToWrite(Object value) {
    var addresses = (List<Address>) value;
    if (ObjectUtils.isNotEmpty(addresses)) {
      return addresses.stream()
        .filter(Objects::nonNull)
        .map(this::toCsvString)
        .filter(StringUtils::isNotEmpty)
        .collect(Collectors.joining(ITEM_DELIMITER));
    }
    return EMPTY;
  }

  private Address getAddressFromString(String stringAddress) {
    List<String> fields = SpecialCharacterEscaper.restore(Arrays.asList(stringAddress.split(ARRAY_DELIMITER, -1)));
    return Address.builder()
        .id(convertToNullableString(fields.get(ADDRESS_ID)))
          .countryId(convertToNullableString(fields.get(ADDRESS_COUNTRY_ID)))
            .addressLine1(convertToNullableString(fields.get(ADDRESS_LINE_1)))
              .addressLine2(convertToNullableString(fields.get(ADDRESS_LINE_2)))
                .city(convertToNullableString(fields.get(ADDRESS_CITY)))
                  .region(convertToNullableString(fields.get(ADDRESS_REGION)))
                    .postalCode(convertToNullableString(fields.get(ADDRESS_POSTAL_CODE)))
                      .primaryAddress(convertToNullableBoolean(fields.get(ADDRESS_PRIMARY_ADDRESS)))
                        .addressTypeId(convertToNullableString(UserReferenceHelper.service().getAddressTypeIdByDesc(fields.get(ADDRESS_TYPE))))
                          .build();
  }

  private String toCsvString(Address address) {
    List<String> data = new ArrayList<>();
    data.add(isEmpty(address.getId()) ? EMPTY : address.getId());
    data.add(isEmpty(address.getCountryId()) ? EMPTY : address.getCountryId());
    data.add(isEmpty(address.getAddressLine1()) ? EMPTY : address.getAddressLine1());
    data.add(isEmpty(address.getAddressLine2()) ? EMPTY : address.getAddressLine2());
    data.add(isEmpty(address.getCity()) ? EMPTY : address.getCity());
    data.add(isEmpty(address.getRegion()) ? EMPTY : address.getRegion());
    data.add(isEmpty(address.getPostalCode()) ? EMPTY : address.getPostalCode());
    data.add(isNull(address.getPrimaryAddress()) ? EMPTY : address.getPrimaryAddress().toString());
    data.add(UserReferenceHelper.service().getAddressTypeDescById(address.getAddressTypeId()));
    return String.join(ARRAY_DELIMITER, escape(data));
  }

  private String convertToNullableString(String str) {
    if (StringUtils.isEmpty(str)) {
      return null;
    }
    return str;
  }

  private Boolean convertToNullableBoolean(String str) {
    if (StringUtils.isEmpty(str)) {
      return null;
    }
    return Boolean.valueOf(str);
  }
}
