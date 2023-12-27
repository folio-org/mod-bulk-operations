package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
  @JsonProperty("id")
  @CsvCustomBindByName(column = "Instance UUID", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 0, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String id;

  @JsonProperty("_version")
  private Integer version;

  @JsonProperty("discoverySuppress")
  @CsvCustomBindByName(column = "Suppress from discovery", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 1, converter = BooleanConverter.class)
  @UnifiedTableCell
  private Boolean discoverySuppress;

  @JsonProperty("staffSuppress")
  @CsvCustomBindByName(column = "Staff suppress", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 2, converter = BooleanConverter.class)
  @UnifiedTableCell(visible = false)
  private Boolean staffSuppress;

  @JsonProperty("previouslyHeld")
  @CsvCustomBindByName(column = "Previously held", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 3, converter = BooleanConverter.class)
  @UnifiedTableCell(visible = false)
  private Boolean previouslyHeld;

  @JsonProperty("hrid")
  @CsvCustomBindByName(column = "Instance HRID", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 4, converter = StringConverter.class)
  @UnifiedTableCell
  private String hrid;

  @JsonProperty("source")
  @CsvCustomBindByName(column = "Source", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 5, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String source;

  @JsonProperty("catalogedDate")
  @CsvCustomBindByName(column = "Cataloged date", converter = DateWithoutTimeConverter.class)
  @CsvCustomBindByPosition(position = 6, converter = DateWithoutTimeConverter.class)
  @UnifiedTableCell(dataType = DataType.DATE_TIME, visible = false)
  private Date catalogedDate;

  @JsonProperty("statusId")
  @CsvCustomBindByName(column = "Instance status term", converter = InstanceStatusConverter.class)
  @CsvCustomBindByPosition(position = 7, converter = InstanceStatusConverter.class)
  @UnifiedTableCell
  private String statusId;

  @JsonProperty("modeOfIssuanceId")
  @CsvCustomBindByName(column = "Mode of issuance", converter = ModeOfIssuanceConverter.class)
  @CsvCustomBindByPosition(position = 8, converter = ModeOfIssuanceConverter.class)
  @UnifiedTableCell(visible = false)
  private String modeOfIssuanceId;

  @JsonProperty("administrativeNotes")
  @CsvCustomBindByName(column = "Administrative note", converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 9, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> administrativeNotes;

  @JsonProperty("title")
  @CsvCustomBindByName(column = "Resource title", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 10, converter = StringConverter.class)
  @UnifiedTableCell
  private String title;

  @JsonProperty("indexTitle")
  @CsvCustomBindByName(column = "Index title", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 11, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String indexTitle;

  @JsonProperty("series")
  @CsvCustomBindByName(column = "Series statements", converter = SeriesListConverter.class)
  @CsvCustomBindByPosition(position = 12, converter = SeriesListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<Series> series;

  @JsonProperty("contributors")
  @CsvCustomBindByName(column = "Contributors", converter = ContributorListConverter.class)
  @CsvCustomBindByPosition(position = 13, converter = ContributorListConverter.class)
  @UnifiedTableCell
  private List<ContributorName> contributors;

  @JsonProperty("editions")
  @CsvCustomBindByName(column = "Edition", converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 14, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> editions;

  @JsonProperty("physicalDescriptions")
  @CsvCustomBindByName(column = "Physical description", converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 15, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> physicalDescriptions;

  @JsonProperty("instanceTypeId")
  @CsvCustomBindByName(column = "Resource type", converter = InstanceTypeConverter.class)
  @CsvCustomBindByPosition(position = 16, converter = InstanceTypeConverter.class)
  @UnifiedTableCell
  private String instanceTypeId;

  @JsonProperty("natureOfContentTermIds")
  @CsvCustomBindByName(column = "Nature of content", converter = NatureOfContentTermListConverter.class)
  @CsvCustomBindByPosition(position = 17, converter = NatureOfContentTermListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> natureOfContentTermIds;

  @JsonProperty("instanceFormatIds")
  @CsvCustomBindByName(column = "Formats", converter = InstanceFormatListConverter.class)
  @CsvCustomBindByPosition(position = 18, converter = InstanceFormatListConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> instanceFormatIds;

  @JsonProperty("languages")
  @CsvCustomBindByName(column = "Languages", converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 19, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> languages;

  @JsonProperty("publicationFrequency")
  @Valid
  @CsvCustomBindByName(column = "Publication frequency", converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 20, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> publicationFrequency;

  @JsonProperty("publicationRange")
  @CsvCustomBindByName(column = "Publication range", converter = StringListPipedConverter.class)
  @CsvCustomBindByPosition(position = 21, converter = StringListPipedConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> publicationRange;

  @Override
  public String getIdentifier(IdentifierType identifierType) {
    return switch (identifierType) {
      case HRID -> hrid;
      default -> id;
    };
  }
}
