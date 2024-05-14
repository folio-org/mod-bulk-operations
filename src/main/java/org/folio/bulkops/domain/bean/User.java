package org.folio.bulkops.domain.bean;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.dto.DataType.DATE_TIME;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.folio.bulkops.domain.converter.BooleanConverter;
import org.folio.bulkops.domain.converter.CustomFieldsConverter;
import org.folio.bulkops.domain.converter.DateWithTimeConverter;
import org.folio.bulkops.domain.converter.DepartmentsConverter;
import org.folio.bulkops.domain.converter.PatronGroupConverter;
import org.folio.bulkops.domain.converter.ProxyForConverter;
import org.folio.bulkops.domain.converter.StringConverter;
import org.folio.bulkops.domain.converter.TagsConverter;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;
import com.opencsv.bean.CsvRecurse;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;


@Getter
@Setter
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("user")
@EqualsAndHashCode(exclude = {"metadata"})
public class User implements BulkOperationsEntity {

  @JsonProperty("username")
  @CsvCustomBindByName(column = "User name", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 0, converter = StringConverter.class)
  @UnifiedTableCell
  private String username;

  @JsonProperty("id")
  @CsvCustomBindByName(column = "User id", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 1, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String id;

  @JsonProperty("externalSystemId")
  @CsvCustomBindByName(column = "External system id", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 2, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String externalSystemId;

  @JsonProperty("barcode")
  @CsvCustomBindByName(column = "Barcode", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 3, converter = StringConverter.class)
  @UnifiedTableCell
  private String barcode;

  @JsonProperty("active")
  @CsvCustomBindByName(column = "Active", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 4, converter = BooleanConverter.class)
  @UnifiedTableCell
  private Boolean active;

  @JsonProperty("type")
  @CsvCustomBindByName(column = "Type", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 5, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String type;

  @JsonProperty("patronGroup")
  @CsvCustomBindByName(column = "Patron group", converter = PatronGroupConverter.class)
  @CsvCustomBindByPosition(position = 6, converter = PatronGroupConverter.class)
  @UnifiedTableCell
  private String patronGroup;

  @JsonProperty("departments")
  @Valid
  @CsvCustomBindByName(column = "Departments", converter = DepartmentsConverter.class)
  @CsvCustomBindByPosition(position = 7, converter = DepartmentsConverter.class)
  @UnifiedTableCell(visible = false)
  private Set<UUID> departments;

  @JsonProperty("meta")
  private Object meta;

  @JsonProperty("proxyFor")
  @Valid
  @CsvCustomBindByName(column = "Proxy for", converter = ProxyForConverter.class)
  @CsvCustomBindByPosition(position = 8, converter = ProxyForConverter.class)
  @UnifiedTableCell(visible = false)
  private List<String> proxyFor;

  @JsonProperty("personal")
  @CsvRecurse
  private Personal personal;

  @JsonProperty("enrollmentDate")
  @CsvCustomBindByName(column = "Enrollment date", converter = DateWithTimeConverter.class)
  @CsvCustomBindByPosition(position = 20, converter = DateWithTimeConverter.class)
  @UnifiedTableCell(dataType = DATE_TIME, visible = false)
  private Date enrollmentDate;

  @JsonProperty("expirationDate")
  @CsvCustomBindByName(column = "Expiration date", converter = DateWithTimeConverter.class)
  @CsvCustomBindByPosition(position = 21, converter = DateWithTimeConverter.class)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @UnifiedTableCell(dataType = DATE_TIME, visible = false)
  private Date expirationDate;

  @JsonProperty("metadata")
  private Metadata metadata;

  @JsonProperty("tags")
  @CsvCustomBindByName(column = "Tags", converter = TagsConverter.class)
  @CsvCustomBindByPosition(position = 22, converter = TagsConverter.class)
  @UnifiedTableCell(visible = false)
  private Tags tags;

  @JsonProperty("customFields")
  @Valid
  @CsvCustomBindByName(column = "Custom fields", converter = CustomFieldsConverter.class)
  @CsvCustomBindByPosition(position = 23, converter = CustomFieldsConverter.class)
  @UnifiedTableCell(visible = false)
  private Map<String, Object> customFields;

  public void setCustomFields(Map<String, Object> customFields) {
    this.customFields = isNull(customFields) ? Collections.emptyMap() : customFields;
  }

  public void setTags(Tags tags) {
    this.tags = isNull(tags) ? new Tags().withTagList(Collections.emptyList()) : tags;
  }

  @Override
  public String getIdentifier(IdentifierType identifierType) {
    return switch (identifierType) {
      case BARCODE -> barcode;
      case EXTERNAL_SYSTEM_ID -> externalSystemId;
      case USER_NAME -> username;
      default -> id;
    };
  }

  @Override
  public Integer _version() {
    return null;
  }
}
