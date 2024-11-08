package org.folio.bulkops.domain.bean;

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
import org.folio.bulkops.domain.converter.BooleanConverter;
import org.folio.bulkops.domain.converter.ContributorListConverter;
import org.folio.bulkops.domain.converter.DateWithoutTimeConverter;
import org.folio.bulkops.domain.converter.InstanceFormatListConverter;
import org.folio.bulkops.domain.converter.InstanceNoteListConverter;
import org.folio.bulkops.domain.converter.InstanceStatusConverter;
import org.folio.bulkops.domain.converter.InstanceTypeConverter;
import org.folio.bulkops.domain.converter.ModeOfIssuanceConverter;
import org.folio.bulkops.domain.converter.NatureOfContentTermListConverter;
import org.folio.bulkops.domain.converter.SeriesListConverter;
import org.folio.bulkops.domain.converter.StringConverter;
import org.folio.bulkops.domain.converter.StringListPipedConverter;
import org.folio.bulkops.domain.dto.DataType;
import org.folio.bulkops.domain.dto.IdentifierType;

import java.util.Date;
import java.util.List;

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
  @CsvCustomBindByName(column = INSTANCE_CATALOGED_DATE, converter = DateWithoutTimeConverter.class)
  @CsvCustomBindByPosition(position = 6, converter = DateWithoutTimeConverter.class)
  @UnifiedTableCell(dataType = DataType.DATE_TIME, visible = false)
  private Date catalogedDate;

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

  @JsonProperty("administrativeNotes")
  @CsvCustomBindByName(column = INSTANCE_ADMINISTRATIVE_NOTE, converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 9, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> administrativeNotes;

  @JsonProperty("title")
  @CsvCustomBindByName(column = INSTANCE_RESOURCE_TITLE, converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 10, converter = StringConverter.class)
  @UnifiedTableCell
  private String title;

  @JsonProperty("indexTitle")
  @CsvCustomBindByName(column = INSTANCE_INDEX_TITLE, converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 11, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String indexTitle;

  @JsonProperty("series")
  @CsvCustomBindByName(column = INSTANCE_SERIES_STATEMENTS, converter = SeriesListConverter.class)
  @CsvCustomBindByPosition(position = 12, converter = SeriesListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<Series> series;

  @JsonProperty("contributors")
  @CsvCustomBindByName(column = INSTANCE_CONTRIBUTORS, converter = ContributorListConverter.class)
  @CsvCustomBindByPosition(position = 13, converter = ContributorListConverter.class)
  @UnifiedTableCell
  private List<ContributorName> contributors;

  @JsonProperty("editions")
  @CsvCustomBindByName(column = INSTANCE_EDITION, converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 14, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> editions;

  @JsonProperty("physicalDescriptions")
  @CsvCustomBindByName(column = INSTANCE_PHYSICAL_DESCRIPTION, converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 15, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> physicalDescriptions;

  @JsonProperty("instanceTypeId")
  @CsvCustomBindByName(column = INSTANCE_RESOURCE_TYPE, converter = InstanceTypeConverter.class)
  @CsvCustomBindByPosition(position = 16, converter = InstanceTypeConverter.class)
  @UnifiedTableCell
  private String instanceTypeId;

  @JsonProperty("natureOfContentTermIds")
  @CsvCustomBindByName(column = INSTANCE_NATURE_OF_CONTENT, converter = NatureOfContentTermListConverter.class)
  @CsvCustomBindByPosition(position = 17, converter = NatureOfContentTermListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> natureOfContentTermIds;

  @JsonProperty("instanceFormatIds")
  @CsvCustomBindByName(column = INSTANCE_FORMATS, converter = InstanceFormatListConverter.class)
  @CsvCustomBindByPosition(position = 18, converter = InstanceFormatListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> instanceFormatIds;

  @JsonProperty("languages")
  @CsvCustomBindByName(column = INSTANCE_LANGUAGES, converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 19, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> languages;

  @JsonProperty("publicationFrequency")
  @Valid
  @CsvCustomBindByName(column = INSTANCE_PUBLICATION_FREQUENCY, converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 20, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> publicationFrequency;

  @JsonProperty("publicationRange")
  @CsvCustomBindByName(column = INSTANCE_PUBLICATION_RANGE, converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 21, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> publicationRange;

  @JsonProperty("notes")
  @CsvCustomBindByName(column = "Notes", converter = InstanceNoteListConverter.class)
  @CsvCustomBindByPosition(position = 22, converter = InstanceNoteListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<InstanceNote> instanceNotes;

  @JsonProperty("matchKey")
  private String matchKey;
  @JsonProperty("alternativeTitles")
  private List<AlternativeTitle> alternativeTitles;
  @JsonProperty("identifiers")
  private List<Identifier> identifiers;
  @JsonProperty("subjects")
  private List<Subject> subjects;
  @JsonProperty("classifications")
  private List<Classification> classifications;
  @JsonProperty("publication")
  private List<Publication> publications;
  @JsonProperty("electronicAccess")
  private List<ElectronicAccess> electronicAccesses;
  @JsonProperty("statisticalCodeIds")
  private List<String> statisticalCodeIds;
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
}
