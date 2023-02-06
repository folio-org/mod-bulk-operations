package org.folio.bulkops.domain.bean;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.validation.Valid;

import org.folio.bulkops.domain.converter.BooleanConverter;
import org.folio.bulkops.domain.converter.CustomFieldsConverter;
import org.folio.bulkops.domain.converter.DateConverter;
import org.folio.bulkops.domain.converter.PatronGroupConverter;
import org.folio.bulkops.domain.converter.ProxyForConverter;
import org.folio.bulkops.domain.converter.StringConverter;
import org.folio.bulkops.domain.converter.TagsConverter;
import org.folio.bulkops.domain.converter.UserDepartmentsConverter;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;
import com.opencsv.bean.CsvRecurse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

@Getter
@Setter
@EqualsAndHashCode
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("user")
public class User extends BulkOperationsEntity {

  @JsonProperty("username")
  @CsvCustomBindByName(column = "User name", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 0, converter = StringConverter.class)
  private String username;

  @JsonProperty("id")
  @CsvCustomBindByName(column = "User id", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 1, converter = StringConverter.class)
  private String id;

  @JsonProperty("externalSystemId")
  @CsvCustomBindByName(column = "External system id", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 2, converter = StringConverter.class)
  private String externalSystemId;

  @JsonProperty("barcode")
  @CsvCustomBindByName(column = "Barcode", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 3, converter = StringConverter.class)
  private String barcode;

  @JsonProperty("active")
  @CsvCustomBindByName(column = "Active", converter = BooleanConverter.class)
  @CsvCustomBindByPosition(position = 4, converter = BooleanConverter.class)
  private Boolean active;

  @JsonProperty("type")
  @CsvCustomBindByName(column = "Type", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 5, converter = StringConverter.class)
  private String type;

  @JsonProperty("patronGroup")
  @CsvCustomBindByName(column = "Patron group", converter = PatronGroupConverter.class)
  @CsvCustomBindByPosition(position = 6, converter = PatronGroupConverter.class)
  private String patronGroup;

  @JsonProperty("departments")
  @Valid
  @CsvCustomBindByName(column = "Departments", converter = UserDepartmentsConverter.class)
  @CsvCustomBindByPosition(position = 7, converter = UserDepartmentsConverter.class)
  private Set<UUID> departments = null;

  @JsonProperty("meta")
  private Object meta;

  @JsonProperty("proxyFor")
  @Valid
  @CsvCustomBindByName(column = "Proxy for", converter = ProxyForConverter.class)
  @CsvCustomBindByPosition(position = 8, converter = ProxyForConverter.class)
  private List<String> proxyFor = null;

  @JsonProperty("personal")
  @CsvRecurse
  private Personal personal;

  @JsonProperty("enrollmentDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @CsvCustomBindByName(column = "Enrollment date", converter = DateConverter.class)
  @CsvCustomBindByPosition(position = 19, converter = DateConverter.class)
  private Date enrollmentDate;

  @JsonProperty("expirationDate")
  @CsvCustomBindByName(column = "Expiration date", converter = DateConverter.class)
  @CsvCustomBindByPosition(position = 20, converter = DateConverter.class)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Date expirationDate;

  @JsonProperty("createdDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @CsvCustomBindByName(column = "Created date", converter = DateConverter.class)
  @CsvCustomBindByPosition(position = 21, converter = DateConverter.class)
  private Date createdDate;

  @JsonProperty("updatedDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @CsvCustomBindByName(column = "Updated date", converter = DateConverter.class)
  @CsvCustomBindByPosition(position = 22, converter = DateConverter.class)
  private Date updatedDate;

  @JsonProperty("metadata")
  private Metadata metadata;

  @JsonProperty("tags")
  @CsvCustomBindByName(column = "Tags", converter = TagsConverter.class)
  @CsvCustomBindByPosition(position = 23, converter = TagsConverter.class)
  private Tags tags;

  @JsonProperty("customFields")
  @Valid
  @CsvCustomBindByName(column = "Custom fields", converter = CustomFieldsConverter.class)
  @CsvCustomBindByPosition(position = 24, converter = CustomFieldsConverter.class)
  private Map<String, Object> customFields = null;

  @Override
  public String getIdentifier(IdentifierType identifierType) {
    switch (identifierType) {
    case BARCODE:
      return barcode;
    case EXTERNAL_SYSTEM_ID:
      return externalSystemId;
    case USER_NAME:
      return username;
    default:
      return id;
    }
  }
}
