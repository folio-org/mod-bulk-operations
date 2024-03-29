package org.folio.bulkops.service;

import static java.lang.String.format;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.Utils.encode;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.InstanceFormatsClient;
import org.folio.bulkops.client.InstanceStatusesClient;
import org.folio.bulkops.client.InstanceTypesClient;
import org.folio.bulkops.client.ModesOfIssuanceClient;
import org.folio.bulkops.client.NatureOfContentTermsClient;
import org.folio.bulkops.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class InstanceReferenceService {
  private final InstanceStatusesClient instanceStatusesClient;
  private final ModesOfIssuanceClient modesOfIssuanceClient;
  private final InstanceTypesClient instanceTypesClient;
  private final NatureOfContentTermsClient natureOfContentTermsClient;
  private final InstanceFormatsClient instanceFormatsClient;

  @Cacheable(cacheNames = "instanceStatusNames")
  public String getInstanceStatusNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY : instanceStatusesClient.getById(id).getName();
    } catch (Exception e) {
      throw new NotFoundException(format("Instance status was not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "instanceStatusIds")
  public String getInstanceStatusIdByName(String name) {
    var response = instanceStatusesClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1);
    if (response.getStatuses().isEmpty()) {
      throw new NotFoundException(format("Instance status was not found by name=%s", name));
    }
    return response.getStatuses().get(0).getId();
  }

  @Cacheable(cacheNames = "modesOfIssuanceNames")
  public String getModeOfIssuanceNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY : modesOfIssuanceClient.getById(id).getName();
    } catch (Exception e) {
      throw new NotFoundException(format("Mode of issuance was not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "modesOfIssuanceIds")
  public String getModeOfIssuanceIdByName(String name) {
    var response = modesOfIssuanceClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1);
    if (response.getModes().isEmpty()) {
      throw new NotFoundException(format("Mode of issuance was not found by name=%s", name));
    }
    return response.getModes().get(0).getId();
  }

  @Cacheable(cacheNames = "instanceTypeNames")
  public String getInstanceTypeNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY : instanceTypesClient.getById(id).getName();
    } catch (Exception e) {
      throw new NotFoundException(format("Instance type was not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "instanceTypeIds")
  public String getInstanceTypeIdByName(String name) {
    var response = instanceTypesClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1);
    if (response.getTypes().isEmpty()) {
      throw new NotFoundException(format("Instance type was not found by name=%s", name));
    }
    return response.getTypes().get(0).getId();
  }

  @Cacheable(cacheNames = "natureOfContentTermNames")
  public String getNatureOfContentTermNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY : natureOfContentTermsClient.getById(id).getName();
    } catch (Exception e) {
      throw new NotFoundException(format("Nature of content term was not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "natureOfContentTermIds")
  public String getNatureOfContentTermIdByName(String name) {
    var response = natureOfContentTermsClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1);
    if (response.getTerms().isEmpty()) {
      throw new NotFoundException(format("Nature of content term was not found by name=%s", name));
    }
    return response.getTerms().get(0).getId();
  }

  @Cacheable(cacheNames = "instanceFormatNames")
  public String getInstanceFormatNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY : instanceFormatsClient.getById(id).getName();
    } catch (Exception e) {
      throw new NotFoundException(format("Instance format was not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "instanceFormatIds")
  public String getInstanceFormatIdByName(String name) {
    var response = instanceFormatsClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1);
    if (response.getFormats().isEmpty()) {
      throw new NotFoundException(format("Instance format was not found by name=%s", name));
    }
    return response.getFormats().get(0).getId();
  }
}
