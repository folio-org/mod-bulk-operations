package org.folio.bulkops.service;

import static java.lang.Character.getNumericValue;
import static java.lang.Character.isDigit;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.STAFF_ONLY;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.InstanceType;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Leader;
import org.marc4j.marc.Subfield;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MarcToUnifiedTableRowMapperHelper {
  private static final String PERIOD = ".";
  private static final String COMMA = ",";
  private static final String HYPHEN = "-";
  private static final String WHITE_SPACE = " ";
  private static final String REGEXP_FOR_TEXT_ENDS_WITH_SINGLE_LETTER_AND_PERIOD = "^(.*?)\\s.[.]$";
  private static final String REGEXP_FOR_TEXT_ENDS_WITH_SINGLE_LETTER_AND_PERIOD_FOLLOWED_BY_COMMA = "^(.*?)\\s.,[.]$";
  private static final String PUNCTUATION_TO_REMOVE = ";:,/+= ";
  private static final String PERSONAL_NAME = "Personal name";
  private static final String CORPORATE_NAME = "Corporate name";

  private final InstanceReferenceService instanceReferenceService;
  private final Marc21ReferenceProvider referenceProvider;

  public String resolveModeOfIssuance(Leader leader) {
    return switch (leader.getImplDefined1()[0]) {
      case 'a', 'c', 'd', 'm' -> "single unit";
      case 'b', 's' -> "serial";
      case 'i' -> "integrating resource";
      default -> "unspecified";
    };
  }

  public List<String> fetchLanguageCodes(DataField dataField) {
    return dataField.getSubfields('a').stream()
      .map(Subfield::getData)
      .toList();
  }

  public List<String> fetchElectronicAccessCodes(DataField dataField) {
    List<String> allCodes = new ArrayList<>();
    allCodes.add(fetchRelationshipName(dataField));
    allCodes.addAll(fetchElectronicAccessCode(dataField, 'u'));
    allCodes.addAll(fetchElectronicAccessCode(dataField, 'y'));
    allCodes.addAll(fetchElectronicAccessCode(dataField, '3'));
    allCodes.addAll(fetchElectronicAccessCode(dataField, 'z'));
    return allCodes;
  }

  public List<String> fetchSubjectCodes(DataField dataField) {
    List<String> allCodes = new ArrayList<>();
    allCodes.addAll(fetchAllSubjectCodes(dataField));
    allCodes.add(fetchSubjectSourceName(dataField));
    allCodes.add(fetchSubjectTypeName(dataField));
    return allCodes;
  }

  private List<String> fetchElectronicAccessCode(DataField dataField, char code) {
    var subfields = dataField.getSubfields(code);
    if (subfields.isEmpty()) {
      return List.of(HYPHEN);
    }
    return List.of(subfields.stream()
      .map(subfield -> subfield.getData().isBlank() ? HYPHEN : subfield.getData())
      .collect(Collectors.joining(WHITE_SPACE)));
  }

  private List<String> fetchAllSubjectCodes(DataField dataField) {
    var subfields = dataField.getSubfields();
    if (subfields.isEmpty()) {
      return List.of(HYPHEN);
    }
    return List.of(subfields.stream()
      .map(Subfield::getData)
      .collect(Collectors.joining(WHITE_SPACE)).trim());
  }

  private String fetchRelationshipName(DataField dataField) {
    var name = "No information provided";
    var ind2 = dataField.getIndicator2();
    if (ind2 == '2') {
      name = "Related resource";
    } else if (ind2 == '0') {
      name = "Resource";
    } else if (ind2 == '1') {
      name = "Version of resource";
    }
    return name;
  }

  private String fetchSubjectSourceName(DataField dataField) {
    char ind2 = dataField.getIndicator2();
    return switch (ind2) {
      case '0' -> "Library of Congress Subject Headings";
      case '1' -> "Library of Congress Children’s and Young Adults' Subject Headings";
      case '2' -> "Medical Subject Headings";
      case '3' -> "National Agriculture Library subject authority file";
      case '4' -> "Source not specified";
      case '5' -> "Canadian Subject Headings";
      case '6' -> "Répertoire de vedettes-matière";
      case '7' -> ofNullable(dataField.getSubfield('2')).map(Subfield::getData).orElse(HYPHEN);
      default -> HYPHEN;
    };
  }

  private String fetchSubjectTypeName(DataField dataField) {
    var field = dataField.getTag();
    return switch (field) {
      case "600" -> PERSONAL_NAME;
      case "610" -> CORPORATE_NAME;
      case "611" -> "Meeting name";
      case "630" -> "Uniform title";
      case "647" -> "Named event";
      case "648" -> "Chronological term";
      case "650" -> "Topical term";
      case "651" -> "Geographic name";
      case "655" -> "Genre/Form";
      default -> HYPHEN;
    };
  }

  public String fetchContributorName(DataField dataField) {
    var subfields = switch (dataField.getTag()) {
      case "100" -> "abcdfgjklnpqtu";
      case "110", "111", "711" -> "abcdfgklnptu";
      case "700" -> "abcdfgjklnopqtu";
      case "710" -> "abcdfgklnoptu";
      default -> "a";
    };
    return dataField.getSubfields(subfields).stream()
      .map(Subfield::getData)
      .filter(Objects::nonNull)
      .map(this::trimPunctuation)
      .collect(Collectors.joining(SPACE));
  }

  public String fetchNameType(DataField dataField) {
    return switch (dataField.getTag()) {
      case "100", "700" -> PERSONAL_NAME;
      case "720" -> isDigit(dataField.getIndicator1()) && 2 == getNumericValue(dataField.getIndicator1()) ?
        CORPORATE_NAME : PERSONAL_NAME;
      case "110", "710" -> CORPORATE_NAME;
      case "111", "711" -> "Meeting name";
      default -> EMPTY;
    };
  }

  public String fetchContributorType(DataField dataField) {
    var code = nonNull(dataField.getSubfield('4')) ? dataField.getSubfield('4').getData() : null;
    var types = instanceReferenceService.getContributorTypesByCode(code).getContributorTypes();
    if (!types.isEmpty()) {
      return types.get(0).getName();
    }
    var subfield = Set.of("111", "711").contains(dataField.getTag()) ? 'j' : 'e';
    var name = nonNull(dataField.getSubfield(subfield)) ? dataField.getSubfield(subfield).getData() : null;
    types = instanceReferenceService.getContributorTypesByName(name).getContributorTypes();
    if (!types.isEmpty()) {
      return types.get(0).getName();
    }
    return isNull(name) ? EMPTY : name;
  }

  public String fetchEdition(DataField dataField) {
    return subfieldsToStringRemoveEndingPunctuation(dataField.getSubfields("ab"));
  }

  public String fetchPhysicalDescription(DataField dataField) {
    return subfieldsToString(dataField.getSubfields("abcefg3"));
  }

  public String fetchPublicationFrequency(DataField dataField) {
    return trimPeriod(subfieldsToString(dataField.getSubfields("ab")));
  }

  public String fetchResourceType(DataField dataField) {
    var name = nonNull(dataField.getSubfield('a')) ? dataField.getSubfield('a').getData() : null;
    var code = nonNull(dataField.getSubfield('b')) ? dataField.getSubfield('b').getData() : null;
    var type = InstanceType.builder().code("zzz").name("unspecified").source("rdacontent").build();
    if (nonNull(name)) {
      var types = instanceReferenceService.getInstanceTypesByName(name).getTypes();
      type = types.isEmpty() ? type : types.get(0);
    } else if (nonNull(code)) {
      var types = instanceReferenceService.getInstanceTypesByCode(code).getTypes();
      type = types.isEmpty() ? type : types.get(0);
    }
    return type.getName();
  }

  public String fetchInstanceFormats(DataField dataField) {
    var code = nonNull(dataField.getSubfield('b')) ? dataField.getSubfield('b').getData() : null;
    var formats = instanceReferenceService.getInstanceFormatsByCode(code).getFormats();
    return formats.isEmpty() ? null : formats.get(0).getName();
  }

  public String fetchPublicationRange(DataField dataField) {
    return trimPeriod(subfieldsToString(dataField.getSubfields("az")));
  }

  public String fetchResourceTitle(DataField dataField) {
    return subfieldsToString(getSubfieldsByOrderedCodes(dataField, "anpbcfghks"));
  }

  public String fetchIndexTitle(DataField dataField) {
    return getSubfieldsByOrderedCodes(dataField,"anpb").stream()
      .map(subfield -> trimAndCapitalizeIfRequired(subfield, dataField.getIndicator2()))
      .collect(Collectors.joining(SPACE));
  }

  public String fetchSeries(DataField dataField) {
    var subfields = switch (dataField.getTag()) {
      case "800" -> "abcdefghjklmnopqrstuvwx35";
      case "810" -> "abcdefghklmnoprstuvwx35";
      case "811" -> "acdefghjklnpqstuvwx35";
      case "830" -> "adfghklmnoprstvwx35";
      default -> throw new IllegalArgumentException("Series statement cannot be processed for field: " + dataField.getTag());
    };
    return subfieldsToStringRemoveEndingPunctuation(dataField.getSubfields(subfields));
  }

  public String fetchNotes(DataField dataField, boolean forCsv) {
    var staffOnlyTruePostfix = forCsv ? ARRAY_DELIMITER + Boolean.TRUE : SPACE + STAFF_ONLY;
    var staffOnlyFalsePostfix = forCsv ? ARRAY_DELIMITER + Boolean.FALSE : EMPTY;
    return subfieldsToString(dataField.getSubfields(referenceProvider.getSubfieldsByTag(dataField.getTag()))) +
      (referenceProvider.isStaffOnlyNote(dataField) ? staffOnlyTruePostfix : staffOnlyFalsePostfix);
  }

  public List<Subfield> getSubfieldsByOrderedCodes(DataField dataField, String subfields) {
    var list = new ArrayList<Subfield>();
    for (char c : subfields.toCharArray()) {
      var subfield = dataField.getSubfield(c);
      if (subfield != null) {
        list.add(subfield);
      }
    }
    return list;
  }

  private String subfieldsToString(List<Subfield> subfields) {
    return ObjectUtils.isEmpty(subfields) ? EMPTY :
      subfields.stream()
        .map(Subfield::getData)
        .collect(Collectors.joining(SPACE));
  }

  private String subfieldsToStringRemoveEndingPunctuation(List<Subfield> subfields) {
    return ObjectUtils.isEmpty(subfields) ? EMPTY :
      subfields.stream()
        .map(Subfield::getData)
        .map(this::removeEndingPunctuation)
        .collect(Collectors.joining(SPACE));
  }

  private String trimPunctuation(String input) {
    if (input.matches(REGEXP_FOR_TEXT_ENDS_WITH_SINGLE_LETTER_AND_PERIOD) || input.endsWith(HYPHEN)) {
      return input;
    } else if (input.matches(REGEXP_FOR_TEXT_ENDS_WITH_SINGLE_LETTER_AND_PERIOD_FOLLOWED_BY_COMMA)) {
      return input.substring(INTEGER_ZERO, input.length() - 2).concat(PERIOD);
    } else if (input.endsWith(PERIOD) || input.endsWith(COMMA)) {
      return input.substring(INTEGER_ZERO, input.length() - 1);
    }
    return input;
  }

  private String removeEndingPunctuation(String input) {
    if (isNotEmpty(input)) {
      input = input.trim();
      int lastPosition = input.length() - 1;
      if (PUNCTUATION_TO_REMOVE.contains(String.valueOf(input.charAt(lastPosition)))) {
        return input.substring(INTEGER_ZERO, lastPosition);
      }
    }
    return input;
  }

  private String trimPeriod(String input) {
    if (isNotEmpty(input) && input.endsWith(PERIOD)) {
      input = input.substring(0, input.length() - 1);
    }
    return input;
  }

  private String trimAndCapitalizeIfRequired(Subfield subfield, char indicator) {
    if ('a' == subfield.getCode() && isDigit(indicator)) {
      return subfield.getData().length() > getNumericValue(indicator) ?
        StringUtils.capitalize(subfield.getData().substring(getNumericValue(indicator))) :
        EMPTY;
    }
    return subfield.getData();
  }

}
