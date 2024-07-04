package org.folio.bulkops.service;

import static java.lang.Character.getNumericValue;
import static java.lang.Character.isDigit;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static org.folio.bulkops.service.Marc21ReferenceProvider.getSubfieldsByTag;
import static org.folio.bulkops.service.Marc21ReferenceProvider.isStaffOnlyNote;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;
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
  private static final String REGEXP_FOR_TEXT_ENDS_WITH_SINGLE_LETTER_AND_PERIOD = "^(.*?)\\s.[.]$";
  private static final String REGEXP_FOR_TEXT_ENDS_WITH_SINGLE_LETTER_AND_PERIOD_FOLLOWED_BY_COMMA = "^(.*?)\\s.,[.]$";
  private static final String PUNCTUATION_TO_REMOVE = ";:,/+= ";

  private final InstanceReferenceService instanceReferenceService;

  public String resolveModeOfIssuance(Leader leader) {
    return switch (leader.getImplDefined1()[0]) {
      case 'a', 'c', 'd', 'm' -> "single unit";
      case 'b', 's' -> "serial";
      case 'i' -> "integrating resource";
      default -> "unspecified";
    };
  }

  public String fetchLanguages(DataField dataField) {
    return dataField.getSubfields('a').stream()
      .map(Subfield::getData)
      .map(Marc21ReferenceProvider::getLanguageByCode)
      .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
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
      case "100", "700" -> "Personal name";
      case "720" -> isDigit(dataField.getIndicator1()) && 2 == getNumericValue(dataField.getIndicator1()) ?
        "Corporate name" : "Personal name";
      case "110", "710" -> "Corporate name";
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
    return String.join(ARRAY_DELIMITER, type.getName(), type.getCode(), type.getSource());
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

  public String fetchNotes(DataField dataField) {
    return trimPeriod(subfieldsToString(dataField.getSubfields(getSubfieldsByTag(dataField.getTag())))) + (isStaffOnlyNote(dataField) ? SPACE + STAFF_ONLY : EMPTY);
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
