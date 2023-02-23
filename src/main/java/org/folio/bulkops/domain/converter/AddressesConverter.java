package org.folio.bulkops.domain.converter;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.Address;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.service.UserReferenceHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
      return Arrays.stream(addresses)
        .filter(StringUtils::isNotEmpty)
        .map(this::getAddressFromString)
        .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  @Override
  protected String convertToWrite(Object value) {
    var addresses = (List<Address>) value;
    if (ObjectUtils.isNotEmpty(addresses)) {
      return addresses.stream()
        .map(this::toCsvString)
        .filter(StringUtils::isNotEmpty)
        .collect(Collectors.joining(ITEM_DELIMITER));
    }
    return EMPTY;
  }

  private Address getAddressFromString(String stringAddress) {
    List<String> fields = SpecialCharacterEscaper.restore(Arrays.asList(stringAddress.split(ARRAY_DELIMITER)));
    return Address.builder()
        .id(fields.get(ADDRESS_ID))
          .countryId(fields.get(ADDRESS_COUNTRY_ID))
            .addressLine1(fields.get(ADDRESS_LINE_1))
              .addressLine2(fields.get(ADDRESS_LINE_2))
                .city(fields.get(ADDRESS_CITY))
                  .region(fields.get(ADDRESS_REGION))
                    .postalCode(fields.get(ADDRESS_POSTAL_CODE))
                      .primaryAddress(Boolean.valueOf(fields.get(ADDRESS_PRIMARY_ADDRESS)))
                        .addressTypeId(UserReferenceHelper.service().getAddressTypeIdByDesc(fields.get(ADDRESS_TYPE)))
                          .build();
  }

  private String toCsvString(Address address) {
    List<String> addressData = new ArrayList<>();
    addressData.add(ofNullable(address.getId()).orElse(EMPTY));
    addressData.add(ofNullable(address.getCountryId()).orElse(EMPTY));
    addressData.add(ofNullable(address.getAddressLine1()).orElse(EMPTY));
    addressData.add(ofNullable(address.getAddressLine2()).orElse(EMPTY));
    addressData.add(ofNullable(address.getCity()).orElse(EMPTY));
    addressData.add(ofNullable(address.getRegion()).orElse(EMPTY));
    addressData.add(ofNullable(address.getPostalCode()).orElse(EMPTY));
    addressData.add(nonNull(address.getPrimaryAddress()) ? address.getPrimaryAddress().toString() : EMPTY);
    addressData.add(UserReferenceHelper.service().getAddressTypeDescById(address.getAddressTypeId()));
    return String.join(ARRAY_DELIMITER, SpecialCharacterEscaper.escape(addressData));
  }
}
