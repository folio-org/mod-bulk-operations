package org.folio.bulkops.domain.bean;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.folio.bulkops.domain.dto.IdentifierType;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("user")
public class User extends BulkOperationsEntity {

  @JsonProperty("username")
  private String username;

  @JsonProperty("id")
  private String id;

  @JsonProperty("externalSystemId")
  private String externalSystemId;

  @JsonProperty("barcode")
  private String barcode;

  @JsonProperty("active")
  private Boolean active;

  @JsonProperty("type")
  private String type;

  @JsonProperty("patronGroup")
  private String patronGroup;

  @JsonProperty("departments")
  @Valid
  private Set<UUID> departments = null;

  @JsonProperty("meta")
  private Object meta;

  @JsonProperty("proxyFor")
  @Valid
  private List<String> proxyFor = null;

  @JsonProperty("personal")
  private Personal personal;

  @JsonProperty("enrollmentDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Date enrollmentDate;

  @JsonProperty("expirationDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Date expirationDate;

  @JsonProperty("createdDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Date createdDate;

  @JsonProperty("updatedDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Date updatedDate;

  @JsonProperty("metadata")
  private Metadata metadata;

  @JsonProperty("tags")
  private Tags tags;

  @JsonProperty("customFields")
  @Valid
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
