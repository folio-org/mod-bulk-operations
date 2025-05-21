package org.folio.bulkops.domain.bean;

import static org.folio.bulkops.util.Constants.MARC;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.converter.*;
import org.folio.bulkops.domain.dto.IdentifierType;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("instance")
@EqualsAndHashCode(exclude = "version")
public class Instance implements BulkOperationsEntity {
  public static final String INSTANCE_UUID = "Instance UUID";
  public static final String INSTANCE_HRID = "Instance HRID";
  public static final String INSTANCE_SOURCE = "Source";
  public static final String INSTANCE_MODE_OF_ISSUANCE = "Mode of issuance";
  public static final String INSTANCE_STATISTICAL_CODES = "Statistical code";
  public static final String INSTANCE_RESOURCE_TITLE = "Resource title";
  public static final String INSTANCE_INDEX_TITLE = "Index title";
  public static final String INSTANCE_SERIES_STATEMENTS = "Series statements";
  public static final String INSTANCE_CONTRIBUTORS = "Contributors";
  public static final String INSTANCE_EDITION = "Edition";
  public static final String INSTANCE_PHYSICAL_DESCRIPTION = "Physical description";
  public static final String INSTANCE_RESOURCE_TYPE = "Resource type";
  public static final String INSTANCE_FORMATS = "Formats";
  public static final String INSTANCE_LANGUAGES = "Languages";
  public static final String INSTANCE_PUBLICATION_FREQUENCY = "Publication frequency";
  public static final String INSTANCE_PUBLICATION_RANGE = "Publication range";
  public static final String INSTANCE_ADMINISTRATIVE_NOTE = "Administrative note";
  public static final String INSTANCE_STAFF_SUPPRESS = "Staff suppress";
  public static final String INSTANCE_SUPPRESS_FROM_DISCOVERY = "Suppress from discovery";
  public static final String INSTANCE_PREVIOUSLY_HELD = "Previously held";
  public static final String INSTANCE_CATALOGED_DATE = "Cataloged date";
  public static final String INSTANCE_STATUS_TERM = "Instance status term";
  public static final String INSTANCE_NATURE_OF_CONTENT = "Nature of content";
  public static final String INSTANCE_NOTES = "Notes";
  public static final String INSTANCE_ELECTRONIC_ACCESS = "Electronic access";
  public static final String INSTANCE_SUBJECT = "Subject";
  public static final String INSTANCE_CLASSIFICATION = "Classification";
  public static final String INSTANCE_PUBLICATION = "Publication";

  @JsonProperty("id")
  @CsvCustomBindByName(column = INSTANCE_UUID, converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 0, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String id;

  @JsonProperty("_version")
  private Integer version;

  @JsonProperty("discoverySuppress")
  @CsvCustomBindByName(column = INSTANCE_SUPPRESS_FROM_DISCOVERY, converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 1, converter = BooleanConverter.class)
  @UnifiedTableCell
  private Boolean discoverySuppress;

  @JsonProperty("staffSuppress")
  @CsvCustomBindByName(column = INSTANCE_STAFF_SUPPRESS, converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 2, converter = BooleanConverter.class)
  @UnifiedTableCell(visible = false)
  private Boolean staffSuppress;

  @JsonProperty("previouslyHeld")
  @CsvCustomBindByName(column = INSTANCE_PREVIOUSLY_HELD, converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 3, converter = BooleanConverter.class)
  @UnifiedTableCell(visible = false)
  private Boolean previouslyHeld;

  @JsonProperty("hrid")
  @CsvCustomBindByName(column = INSTANCE_HRID, converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 4, converter = StringConverter.class)
  @UnifiedTableCell
  private String hrid;

  @JsonProperty("source")
  @CsvCustomBindByName(column = INSTANCE_SOURCE, converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 5, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String source;

  @JsonProperty("catalogedDate")
  @CsvCustomBindByName(column = INSTANCE_CATALOGED_DATE, converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 6, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String catalogedDate;

  @JsonProperty("statusId")
  @CsvCustomBindByName(column = INSTANCE_STATUS_TERM, converter = InstanceStatusConverter.class)
  @CsvCustomBindByPosition(position = 7, converter = InstanceStatusConverter.class)
  @UnifiedTableCell
  private String statusId;

  @JsonProperty("modeOfIssuanceId")
  @CsvCustomBindByName(column = INSTANCE_MODE_OF_ISSUANCE, converter = ModeOfIssuanceConverter.class)
  @CsvCustomBindByPosition(position = 8, converter = ModeOfIssuanceConverter.class)
  @UnifiedTableCell(visible = false)
  private String modeOfIssuanceId;

  @JsonProperty("statisticalCodeIds")
  @CsvCustomBindByName(column = INSTANCE_STATISTICAL_CODES, converter = InstanceStatisticalCodeListConverter.class)
  @CsvCustomBindByPosition(position = 9, converter = InstanceStatisticalCodeListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> statisticalCodeIds;

  @JsonProperty("administrativeNotes")
  @CsvCustomBindByName(column = INSTANCE_ADMINISTRATIVE_NOTE, converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 10, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> administrativeNotes;

  @JsonProperty("title")
  @CsvCustomBindByName(column = INSTANCE_RESOURCE_TITLE, converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 11, converter = StringConverter.class)
  @UnifiedTableCell
  private String title;

  @JsonProperty("indexTitle")
  @CsvCustomBindByName(column = INSTANCE_INDEX_TITLE, converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 12, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String indexTitle;

  @JsonProperty("series")
  @CsvCustomBindByName(column = INSTANCE_SERIES_STATEMENTS, converter = SeriesListConverter.class)
  @CsvCustomBindByPosition(position = 13, converter = SeriesListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<Series> series;

  @JsonProperty("contributors")
  @CsvCustomBindByName(column = INSTANCE_CONTRIBUTORS, converter = ContributorListConverter.class)
  @CsvCustomBindByPosition(position = 14, converter = ContributorListConverter.class)
  @UnifiedTableCell
  private List<ContributorName> contributors;

  @JsonProperty("editions")
  @CsvCustomBindByName(column = INSTANCE_EDITION, converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 15, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> editions;

  @JsonProperty("physicalDescriptions")
  @CsvCustomBindByName(column = INSTANCE_PHYSICAL_DESCRIPTION, converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 16, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> physicalDescriptions;

  @JsonProperty("instanceTypeId")
  @CsvCustomBindByName(column = INSTANCE_RESOURCE_TYPE, converter = InstanceTypeConverter.class)
  @CsvCustomBindByPosition(position = 17, converter = InstanceTypeConverter.class)
  @UnifiedTableCell
  private String instanceTypeId;

  @JsonProperty("natureOfContentTermIds")
  @CsvCustomBindByName(column = INSTANCE_NATURE_OF_CONTENT, converter = NatureOfContentTermListConverter.class)
  @CsvCustomBindByPosition(position = 18, converter = NatureOfContentTermListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> natureOfContentTermIds;

  @JsonProperty("instanceFormatIds")
  @CsvCustomBindByName(column = INSTANCE_FORMATS, converter = InstanceFormatListConverter.class)
  @CsvCustomBindByPosition(position = 19, converter = InstanceFormatListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> instanceFormatIds;

  @JsonProperty("languages")
  @CsvCustomBindByName(column = INSTANCE_LANGUAGES, converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 20, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> languages;

  @JsonProperty("publicationFrequency")
  @Valid
  @CsvCustomBindByName(column = INSTANCE_PUBLICATION_FREQUENCY, converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 21, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> publicationFrequency;

  @JsonProperty("publicationRange")
  @CsvCustomBindByName(column = INSTANCE_PUBLICATION_RANGE, converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 22, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> publicationRange;

  @JsonProperty("notes")
  @CsvCustomBindByName(column = INSTANCE_NOTES, converter = InstanceNoteListConverter.class)
  @CsvCustomBindByPosition(position = 23, converter = InstanceNoteListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<InstanceNote> instanceNotes;

  @JsonProperty("electronicAccess")
  @Valid
  @CsvCustomBindByName(column = INSTANCE_ELECTRONIC_ACCESS, converter = ElectronicAccessListInstanceConverter.class)
  @CsvCustomBindByPosition(position = 24, converter = ElectronicAccessListInstanceConverter.class)
  @UnifiedTableCell(visible = false)
  private List<ElectronicAccess> electronicAccess = null;

  @JsonProperty("subjects")
  @Valid
  @CsvCustomBindByName(column = INSTANCE_SUBJECT, converter = SubjectListConverter.class)
  @CsvCustomBindByPosition(position = 25, converter = SubjectListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<Subject> subject = null;

  @JsonProperty("classifications")
  @Valid
  @CsvCustomBindByName(column = INSTANCE_CLASSIFICATION, converter = ClassificationListConverter.class)
  @CsvCustomBindByPosition(position = 26, converter = ClassificationListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<Classification> classifications;

  @JsonProperty("publication")
  @Valid
  @CsvCustomBindByName(column = INSTANCE_PUBLICATION, converter = PublicationListConverter.class)
  @CsvCustomBindByPosition(position = 27, converter = PublicationListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<Classification> publication =null;

  @JsonProperty("matchKey")
  private String matchKey;
  @JsonProperty("alternativeTitles")
  private List<AlternativeTitle> alternativeTitles;
  @JsonProperty("identifiers")
  private List<Identifier> identifiers;
//  @JsonProperty("publication")
//  private List<Publication> publications;
  @JsonProperty("sourceRecordFormat")
  private String sourceRecordFormat;
  @JsonProperty("statusUpdatedDate")
  private String statusUpdatedDate;
  @JsonProperty("tags")
  private Tags tags;
  @JsonProperty("precedingTitles")
  private List<PrecedingTitle> precedingTitles;
  @JsonProperty("succeedingTitles")
  private List<SucceedingTitle> succeedingTitles;
  @JsonProperty("ISBN")
  private String isbn;
  @JsonProperty("ISSN")
  private String issn;
  @JsonProperty("parentInstances")
  private List<ParentInstance> parentInstances;
  @JsonProperty("childInstances")
  private List<ChildInstance> childInstances;
  @JsonProperty("dates")
  private Dates dates;
  @JsonProperty("isBoundWith")
  private Boolean isBoundWith = false;
  @JsonProperty("deleted")
  private Boolean deleted = false;



  @Override
  public String getIdentifier(IdentifierType identifierType) {
    return switch (identifierType) {
      case HRID -> hrid;
      case ISBN -> isbn;
      case ISSN -> issn;
      default -> id;
    };
  }

  @Override
  public Integer _version() {
    return version;
  }

  @Override
  public boolean isMarcInstance() {
    return MARC.equals(source);
  }
}
