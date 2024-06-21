package org.folio.bulkops.service;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_CODE;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.Utils.encode;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.ContributorTypesClient;
import org.folio.bulkops.client.InstanceFormatsClient;
import org.folio.bulkops.client.InstanceNoteTypesClient;
import org.folio.bulkops.client.InstanceStatusesClient;
import org.folio.bulkops.client.InstanceTypesClient;
import org.folio.bulkops.client.ModesOfIssuanceClient;
import org.folio.bulkops.client.NatureOfContentTermsClient;
import org.folio.bulkops.domain.bean.InstanceFormats;
import org.folio.bulkops.domain.bean.InstanceTypes;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.domain.dto.ContributorTypeCollection;
import org.folio.bulkops.domain.dto.InstanceNoteType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class InstanceReferenceService {
  private final InstanceStatusesClient instanceStatusesClient;
  private final ModesOfIssuanceClient modesOfIssuanceClient;
  private final InstanceTypesClient instanceTypesClient;
  private final NatureOfContentTermsClient natureOfContentTermsClient;
  private final InstanceFormatsClient instanceFormatsClient;
  private final InstanceNoteTypesClient instanceNoteTypesClient;
  private final ContributorTypesClient contributorTypesClient;

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

  @Cacheable(cacheNames = "instanceFormats")
  public InstanceFormats getInstanceFormatsByCode(String code) {
    return isNull(code) ?
      InstanceFormats.builder().formats(Collections.emptyList()).totalRecords(0).build() :
      instanceFormatsClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(code)), 1);
  }

  @Cacheable(cacheNames = "instanceNoteTypesNames")
  public String getNoteTypeNameById(String id) {
    try {
      return instanceNoteTypesClient.getNoteTypeById(id).getName();
    } catch (Exception e) {
      throw new NotFoundException(format("Note type not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "instanceNoteTypes")
  public String getNoteTypeIdByName(String name) {
    var noteTypes = instanceNoteTypesClient.getNoteTypesByQuery(format(QUERY_PATTERN_NAME, encode(name)), 1);
    if (noteTypes.getInstanceNoteTypes().isEmpty()) {
      throw new NotFoundException(format("Note type not found by name=%s", name));
    }
    return noteTypes.getInstanceNoteTypes().get(0).getId().toString();
  }

  @Cacheable(cacheNames = "allInstanceNoteTypes")
  public List<InstanceNoteType> getAllInstanceNoteTypes() {
    return instanceNoteTypesClient.getInstanceNoteTypes(Integer.MAX_VALUE).getInstanceNoteTypes();
  }

  @Cacheable(cacheNames = "contributorTypesByName")
  public ContributorTypeCollection getContributorTypesByName(String name) {
    return isNull(name) ?
      new ContributorTypeCollection().contributorTypes(Collections.emptyList()).totalRecords(0) :
      contributorTypesClient.getByQuery(String.format(QUERY_PATTERN_NAME, name), 1);
  }

  @Cacheable(cacheNames = "contributorTypesByCode")
  public ContributorTypeCollection getContributorTypesByCode(String code) {
    return isNull(code) ?
      new ContributorTypeCollection().contributorTypes(Collections.emptyList()).totalRecords(0) :
      contributorTypesClient.getByQuery(String.format(QUERY_PATTERN_CODE, code), 1);
  }

  @Cacheable(cacheNames = "instanceTypesByNames")
  public InstanceTypes getInstanceTypesByName(String name) {
    return isNull(name) ?
      InstanceTypes.builder().types(Collections.emptyList()).totalRecords(0).build() :
      instanceTypesClient.getByQuery(String.format(QUERY_PATTERN_NAME, name), 1);
  }

  @Cacheable(cacheNames = "instanceTypesByCodes")
  public InstanceTypes getInstanceTypesByCode(String code) {
    return isNull(code) ?
      InstanceTypes.builder().types(Collections.emptyList()).totalRecords(0).build() :
      instanceTypesClient.getByQuery(String.format(QUERY_PATTERN_CODE, code), 1);
  }
}
