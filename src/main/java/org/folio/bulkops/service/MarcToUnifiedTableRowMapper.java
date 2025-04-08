package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_CONTRIBUTORS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_EDITION;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_ELECTRONIC_ACCESS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_FORMATS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_HRID;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_INDEX_TITLE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_LANGUAGES;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_MODE_OF_ISSUANCE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_PHYSICAL_DESCRIPTION;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_PUBLICATION_FREQUENCY;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_PUBLICATION_RANGE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_RESOURCE_TITLE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_RESOURCE_TYPE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_SERIES_STATEMENTS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_SOURCE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_SUBJECT;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_UUID;
import static org.folio.bulkops.service.Marc21ReferenceProvider.GENERAL_NOTE;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER_SPACED;
import static org.folio.bulkops.util.Constants.ELECTRONIC_ACCESS_HEADINGS;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;
import static org.folio.bulkops.util.Constants.MARC;
import static org.folio.bulkops.util.Constants.SPECIAL_ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.SPECIAL_ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.SUBJECT_HEADINGS;

import lombok.RequiredArgsConstructor;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Leader;
import org.marc4j.marc.Record;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MarcToUnifiedTableRowMapper {
  private final MarcToUnifiedTableRowMapperHelper helper;
  private final Marc21ReferenceProvider referenceProvider;

  public List<String> processRecord(Record rec, List<String> headers, boolean forCsv) {
    var rowData = new ArrayList<>(Arrays.asList(new String[headers.size()]));
    setSourceMarc(rowData, headers);
    processLeader(rowData, rec.getLeader(), headers);
    processControlFields(rowData, rec.getControlFields(), headers);
    processDataFields(rowData, rec.getDataFields(), headers, forCsv);
    return rowData;
  }

  private void setSourceMarc(List<String> rowData, List<String> headers) {
    var index = headers.indexOf(INSTANCE_SOURCE);
    if (index != -1) {
      rowData.set(index, MARC);
    }
  }

  private void processLeader(List<String> rowData, Leader leader, List<String> headers) {
    var modeOfIssuanceIndex = headers.indexOf(INSTANCE_MODE_OF_ISSUANCE);
    if (modeOfIssuanceIndex != -1) {
      rowData.set(modeOfIssuanceIndex, helper.resolveModeOfIssuance(leader));
    }
  }

  private void processControlFields(List<String> rowData, List<ControlField> controlFields, List<String> headers) {
    controlFields.forEach(controlField -> {
      if ("001".equals(controlField.getTag())) {
        var hridIndex = headers.indexOf(INSTANCE_HRID);
        if (hridIndex != -1) {
          rowData.set(hridIndex, controlField.getData());
        }
      } else if ("008".equals(controlField.getTag())) {
        var index = headers.indexOf(INSTANCE_LANGUAGES);
        if (index != -1) {
          rowData.set(index, controlField.getData().substring(35, 38));
        }
      }
    });
  }

  private void processDataFields(List<String> rowData, List<DataField> dataFields, List<String> headers, boolean forCsv) {
    dataFields.forEach(dataField -> {
      var tag = dataField.getTag();
      switch (tag) {
        case "041" -> processLanguages(rowData, dataField, headers);
        case "100", "110", "111", "700", "710", "711", "720" -> processContributors(rowData, dataField, headers);
        case "245" -> processTitles(rowData, dataField, headers);
        case "250" -> processEdition(rowData, dataField, headers);
        case "300" -> processPhysicalDescription(rowData, dataField, headers);
        case "310", "321" -> processPublicationFrequency(rowData, dataField, headers);
        case "336" -> processResourceType(rowData, dataField, headers);
        case "338" -> processInstanceFormats(rowData, dataField, headers);
        case "362" -> processPublicationRange(rowData, dataField, headers);
        case "600", "610", "611", "630", "647", "648", "650", "651", "655" -> processSubject(rowData, dataField, headers, forCsv);
        case "800", "810", "811", "830" -> processSeries(rowData, dataField, headers);
        case "856" -> processElectronicAccess(rowData, dataField, headers, forCsv);
        case "999" -> processInstanceId(rowData, dataField, headers);
        default -> {
          if (referenceProvider.isMappedNoteTag(tag)) {
            processInstanceNotes(rowData, dataField, headers, forCsv);
          }
        }
      }
    });
  }

  private void processElectronicAccess(List<String> rowData, DataField dataField, List<String> headers, boolean forCsv) {
    var index = headers.indexOf(INSTANCE_ELECTRONIC_ACCESS);
    if (index != -1) {
      var newElAccCodes = new ArrayList<>(helper.fetchElectronicAccessCodes(dataField));
      if (!newElAccCodes.isEmpty()) {
        var existingElAcc = rowData.get(index);
        List<String> existingElAccCodes = isNull(existingElAcc) ?
          new ArrayList<>() :
          new ArrayList<>(Arrays.asList(existingElAcc.split(forCsv ? ARRAY_DELIMITER : SPECIAL_ARRAY_DELIMITER)));
        var existingElAccStr = EMPTY;
        if (!existingElAccCodes.isEmpty()) {
          existingElAccStr = String.join(forCsv ? ARRAY_DELIMITER : SPECIAL_ARRAY_DELIMITER, existingElAccCodes) + (forCsv ? ITEM_DELIMITER_SPACED : SPECIAL_ITEM_DELIMITER);
        } else if (forCsv) {
          existingElAccStr += ELECTRONIC_ACCESS_HEADINGS;
        }
        existingElAccStr += String.join(forCsv ? ARRAY_DELIMITER : SPECIAL_ARRAY_DELIMITER, newElAccCodes);
        rowData.set(index, existingElAccStr);
      }
    }
  }

  private void processSubject(List<String> rowData, DataField dataField, List<String> headers, boolean forCsv) {
    var index = headers.indexOf(INSTANCE_SUBJECT);
    if (index != -1) {
      var newSubjCodes = new ArrayList<>(helper.fetchSubjectCodes(dataField));
      if (!newSubjCodes.isEmpty()) {
        var existingSubj = rowData.get(index);
        List<String> existingSubjCodes = isNull(existingSubj) ?
          new ArrayList<>() :
          new ArrayList<>(Arrays.asList(existingSubj.split(forCsv ? ARRAY_DELIMITER : SPECIAL_ARRAY_DELIMITER)));
        var existingSubjStr = EMPTY;
        if (!existingSubjCodes.isEmpty()) {
          existingSubjStr = String.join(forCsv ? ARRAY_DELIMITER : SPECIAL_ARRAY_DELIMITER, existingSubjCodes) + (forCsv ? ITEM_DELIMITER_SPACED : SPECIAL_ITEM_DELIMITER);
        } else if (forCsv) {
          existingSubjStr += SUBJECT_HEADINGS;
        }
        existingSubjStr += String.join(forCsv ? ARRAY_DELIMITER : SPECIAL_ARRAY_DELIMITER, newSubjCodes);
        rowData.set(index, existingSubjStr);
      }
    }
  }

  private void processLanguages(List<String> rowData, DataField dataField, List<String> headers) {
    var index = headers.indexOf(INSTANCE_LANGUAGES);
    if (index != -1) {
      var newLanguages = new ArrayList<>(helper.fetchLanguageCodes(dataField));
      if (!newLanguages.isEmpty()) {
      List<String> languages = isNull(rowData.get(index)) ?
        new ArrayList<>() :
        new ArrayList<>(Arrays.asList(rowData.get(index).split(ITEM_DELIMITER_SPACED)));
      newLanguages.removeAll(languages);
      languages.addAll(newLanguages);
      rowData.set(index, String.join(ITEM_DELIMITER_SPACED, languages));
      }
    }
  }

  private void processContributors(List<String> rowData, DataField dataField, List<String> headers) {
    var index = headers.indexOf(INSTANCE_CONTRIBUTORS);
    if (index != -1) {
      var contributor = helper.fetchContributorName(dataField);
      rowData.set(index, isNotEmpty(rowData.get(index)) && isNotEmpty(contributor) ?
        String.join(ARRAY_DELIMITER_SPACED, rowData.get(index), contributor) :
        contributor);
    }
  }

  private void processInstanceId(List<String> rowData, DataField dataField, List<String> headers) {
    var index = headers.indexOf(INSTANCE_UUID);
    if (index != -1 && 'f' == dataField.getIndicator1() && 'f' == dataField.getIndicator2() && nonNull(dataField.getSubfield('i'))) {
      rowData.set(index, dataField.getSubfield('i').getData());
    }
  }

  private void processEdition(List<String> rowData, DataField dataField, List<String> headers) {
    var index = headers.indexOf(INSTANCE_EDITION);
    if (index != -1) {
      var edition = helper.fetchEdition(dataField);
      rowData.set(index, isNotEmpty(rowData.get(index)) && isNotEmpty(edition) ?
        String.join(ITEM_DELIMITER_SPACED, rowData.get(index), edition) :
        edition);
    }
  }

  private void processPhysicalDescription(List<String> rowData, DataField dataField, List<String> headers) {
    var index = headers.indexOf(INSTANCE_PHYSICAL_DESCRIPTION);
    if (index != -1) {
      var description = helper.fetchPhysicalDescription(dataField);
      rowData.set(index, isNotEmpty(rowData.get(index)) && isNotEmpty(description) ?
        String.join(ITEM_DELIMITER_SPACED, rowData.get(index), description) :
        description);
    }
  }

  private void processPublicationFrequency(List<String> rowData, DataField dataField, List<String> headers) {
    var index = headers.indexOf(INSTANCE_PUBLICATION_FREQUENCY);
    if (index != -1) {
      var publicationFrequency = helper.fetchPublicationFrequency(dataField);
      rowData.set(index, isNotEmpty(rowData.get(index)) && isNotEmpty(publicationFrequency) ?
        String.join(ITEM_DELIMITER_SPACED, rowData.get(index), publicationFrequency) :
        publicationFrequency);
    }
  }

  private void processResourceType(List<String> rowData, DataField dataField, List<String> headers) {
    var index = headers.indexOf(INSTANCE_RESOURCE_TYPE);
    if (index != -1) {
      var resourceType = helper.fetchResourceType(dataField);
      rowData.set(index, isNotEmpty(rowData.get(index)) && isNotEmpty(resourceType) ?
        String.join(ITEM_DELIMITER_SPACED, rowData.get(index), resourceType) :
        resourceType);
    }
  }

  private void processInstanceFormats(List<String> rowData, DataField dataField, List<String> headers) {
    var index = headers.indexOf(INSTANCE_FORMATS);
    if (index != -1) {
      var format = helper.fetchInstanceFormats(dataField);
      rowData.set(index, isNotEmpty(rowData.get(index)) && isNotEmpty(format) ?
        String.join(ITEM_DELIMITER_SPACED, rowData.get(index), format) :
        format);
    }
  }

  private void processPublicationRange(List<String> rowData, DataField dataField, List<String> headers) {
    var index = headers.indexOf(INSTANCE_PUBLICATION_RANGE);
    if (index != -1) {
      var publicationRange = helper.fetchPublicationRange(dataField);
      rowData.set(index, isNotEmpty(rowData.get(index)) && isNotEmpty(publicationRange) ?
        String.join(ITEM_DELIMITER_SPACED, rowData.get(index), publicationRange) :
        publicationRange);
    }
  }

  private void processTitles(List<String> rowData, DataField dataField, List<String> headers) {
    processResourceTitle(rowData, dataField, headers);
    processIndexTitle(rowData, dataField, headers);
  }

  private void processResourceTitle(List<String> rowData, DataField dataField, List<String> headers) {
    var index = headers.indexOf(INSTANCE_RESOURCE_TITLE);
    if (index != -1) {
      rowData.set(index, helper.fetchResourceTitle(dataField));
    }
  }

  private void processIndexTitle(List<String> rowData, DataField dataField, List<String> headers) {
    var index = headers.indexOf(INSTANCE_INDEX_TITLE);
    if (index != -1) {
      rowData.set(index, helper.fetchIndexTitle(dataField));
    }
  }

  private void processSeries(List<String> rowData, DataField dataField, List<String> headers) {
    var index = headers.indexOf(INSTANCE_SERIES_STATEMENTS);
    if (index != -1) {
      var seriesStatement = helper.fetchSeries(dataField);
      rowData.set(index, isNotEmpty(rowData.get(index)) && isNotEmpty(seriesStatement) ?
        String.join(ITEM_DELIMITER_SPACED, rowData.get(index), seriesStatement) :
        seriesStatement);
    }
  }

  private void processInstanceNotes(List<String> rowData, DataField dataField, List<String> headers, boolean forCsv) {
    var tag = dataField.getTag();
    var index = !headers.contains(referenceProvider.getNoteTypeByTag(tag)) ? headers.indexOf(GENERAL_NOTE) : headers.indexOf(referenceProvider.getNoteTypeByTag(tag));
    if (index != -1) {
      var prefix = forCsv ? headers.get(index) + ARRAY_DELIMITER : EMPTY;
      var notes = prefix + helper.fetchNotes(dataField, forCsv);
      rowData.set(index, isNotEmpty(rowData.get(index)) ?
        String.join(ITEM_DELIMITER_SPACED, rowData.get(index), notes) :
        notes);
    }
  }
}
