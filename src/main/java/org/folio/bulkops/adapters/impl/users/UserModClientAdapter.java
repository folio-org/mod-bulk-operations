package org.folio.bulkops.adapters.impl.users;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.adapters.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.adapters.Constants.DATE_TIME_PATTERN;
import static org.folio.bulkops.adapters.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.adapters.Constants.KEY_VALUE_DELIMITER;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.folio.bulkops.adapters.ModClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.Address;
import org.folio.bulkops.domain.bean.CustomField;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class UserModClientAdapter implements ModClient<User> {

  private final UserReferenceResolver userReferenceResolver;
  private final UserClient userClient;

  @Override
  public UnifiedTable convertEntityToUnifiedTable(User user, UUID bulkOperationId, IdentifierType identifierType) {
    var identifier = fetchIdentifier(user, identifierType);
    return new UnifiedTable().header(UserHeaderBuilder.getHeaders())
      .addRowsItem(convertToUnifiedTableRow(user, bulkOperationId, identifier));
  }

  @Override
  public UnifiedTable getUnifiedRepresentationByQuery(String query, long offset, long limit) {
    var users = userClient.getUserByQuery(query, offset, limit)
      .getUsers();
    return new UnifiedTable().header(UserHeaderBuilder.getHeaders())
      .rows(users.isEmpty() ? Collections.emptyList()
          : users.stream()
            .map(u -> convertToUnifiedTableRow(u, null, null))
            .collect(Collectors.toList()));
  }

  @Override
  public Class<User> getProcessedType() {
    return User.class;
  }

  private Row convertToUnifiedTableRow(User user, UUID bulkOperationId, String identifier) {
    return new Row().addRowItem(user.getUsername())
      .addRowItem(user.getId())
      .addRowItem(user.getExternalSystemId())
      .addRowItem(user.getBarcode())
      .addRowItem(isNull(user.getActive()) ? EMPTY
          : user.getActive()
            .toString())
      .addRowItem(user.getType())
      .addRowItem(userReferenceResolver.getPatronGroupNameById(user.getPatronGroup(), bulkOperationId, identifier))
      .addRowItem(fetchDepartments(user, bulkOperationId, identifier))
      .addRowItem(nonNull(user.getProxyFor()) ? String.join(ARRAY_DELIMITER, user.getProxyFor()) : EMPTY)
      .addRowItem(user.getPersonal()
        .getLastName())
      .addRowItem(user.getPersonal()
        .getFirstName())
      .addRowItem(user.getPersonal()
        .getMiddleName())
      .addRowItem(user.getPersonal()
        .getPreferredFirstName())
      .addRowItem(user.getPersonal()
        .getEmail())
      .addRowItem(user.getPersonal()
        .getPhone())
      .addRowItem(user.getPersonal()
        .getMobilePhone())
      .addRowItem(dateToString(user.getPersonal()
        .getDateOfBirth()))
      .addRowItem(addressesToString(user.getPersonal()
        .getAddresses(), bulkOperationId, identifier))
      .addRowItem(isNull(user.getPersonal()
        .getPreferredContactTypeId()) ? EMPTY
            : user.getPersonal()
              .getPreferredContactTypeId())
      .addRowItem(dateToString(user.getEnrollmentDate()))
      .addRowItem(dateToString(user.getExpirationDate()))
      .addRowItem(dateToString(user.getCreatedDate()))
      .addRowItem(dateToString(user.getUpdatedDate()))
      .addRowItem(nonNull(user.getTags()) ? String.join(ARRAY_DELIMITER, user.getTags()
        .getTagList()) : EMPTY)
      .addRowItem(nonNull(user.getCustomFields()) ? customFieldsToString(user.getCustomFields()) : EMPTY);
  }

  private String fetchDepartments(User user, UUID bulkOperationId, String identifier) {
    if (nonNull(user.getDepartments())) {
      return user.getDepartments()
        .stream()
        .map(id -> userReferenceResolver.getDepartmentNameById(id.toString(), bulkOperationId, identifier))
        .collect(Collectors.joining(ARRAY_DELIMITER));
    }
    return EMPTY;
  }

  private String addressesToString(List<Address> addresses, UUID bulkOperationId, String identifier) {
    if (nonNull(addresses)) {
      return addresses.stream()
        .map(address -> addressToString(address, bulkOperationId, identifier))
        .collect(Collectors.joining(ITEM_DELIMITER));
    }
    return EMPTY;
  }

  private String addressToString(Address address, UUID bulkOperationId, String identifier) {
    List<String> addressData = new ArrayList<>();
    addressData.add(ofNullable(address.getId()).orElse(EMPTY));
    addressData.add(ofNullable(address.getCountryId()).orElse(EMPTY));
    addressData.add(ofNullable(address.getAddressLine1()).orElse(EMPTY));
    addressData.add(ofNullable(address.getAddressLine2()).orElse(EMPTY));
    addressData.add(ofNullable(address.getCity()).orElse(EMPTY));
    addressData.add(ofNullable(address.getRegion()).orElse(EMPTY));
    addressData.add(ofNullable(address.getPostalCode()).orElse(EMPTY));
    addressData.add(nonNull(address.getPrimaryAddress()) ? address.getPrimaryAddress()
      .toString() : EMPTY);
    addressData.add(userReferenceResolver.getAddressTypeDescById(address.getAddressTypeId(), bulkOperationId, identifier));
    return String.join(ARRAY_DELIMITER, addressData);
  }

  private String customFieldsToString(Map<String, Object> map) {
    return map.entrySet()
      .stream()
      .map(this::customFieldToString)
      .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String customFieldToString(Map.Entry<String, Object> entry) {
    var customField = userReferenceResolver.getCustomFieldByRefId(entry.getKey());
    switch (customField.getType()) {
    case SINGLE_SELECT_DROPDOWN:
    case RADIO_BUTTON:
      return customField.getName() + KEY_VALUE_DELIMITER + extractValueById(customField, entry.getValue()
        .toString());
    case MULTI_SELECT_DROPDOWN:
      var values = (ArrayList) entry.getValue();
      return customField.getName() + KEY_VALUE_DELIMITER + values.stream()
        .map(v -> extractValueById(customField, v.toString()))
        .collect(Collectors.joining(ARRAY_DELIMITER));
    default:
      return customField.getName() + KEY_VALUE_DELIMITER + entry.getValue();
    }
  }

  private String extractValueById(CustomField customField, String id) {
    var optionalValue = customField.getSelectField()
      .getOptions()
      .getValues()
      .stream()
      .filter(selectFieldOption -> Objects.equals(id, selectFieldOption.getId()))
      .findFirst();
    return optionalValue.isPresent() ? optionalValue.get()
      .getValue() : EMPTY;
  }

  public String dateToString(Date date) {
    var dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
    return nonNull(date) ? dateFormat.format(date) : EMPTY;
  }

  private String fetchIdentifier(User user, IdentifierType identifierType) {
    switch (identifierType) {
    case BARCODE:
      return user.getBarcode();
    case EXTERNAL_SYSTEM_ID:
      return user.getExternalSystemId();
    case USER_NAME:
      return user.getUsername();
    default:
      return user.getId();
    }
  }
}
