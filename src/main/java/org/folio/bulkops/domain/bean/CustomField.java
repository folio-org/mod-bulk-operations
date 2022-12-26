package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class CustomField {
  @JsonProperty("id")
  private String id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("refId")
  private String refId;

  @JsonProperty("type")
  private CustomFieldTypes type;

  @JsonProperty("entityType")
  private String entityType;

  @JsonProperty("visible")
  private Boolean visible;

  @JsonProperty("required")
  private Boolean required;

  @JsonProperty("isRepeatable")
  private Boolean isRepeatable;

  @JsonProperty("order")
  private Integer order;

  @JsonProperty("helpText")
  private String helpText;

  @JsonProperty("checkboxField")
  private CheckBoxField checkboxField;

  @JsonProperty("selectField")
  private SelectField selectField;

  @JsonProperty("textField")
  private TextField textField;

  @JsonProperty("metadata")
  private Metadata metadata;
}

