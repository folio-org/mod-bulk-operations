package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.List;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SubjectSourceCollection {

  @JsonProperty("subjectSources")
  @Valid
  private List<SubjectSource> subjectSources = null;

  @JsonProperty("totalRecords")
  private Integer totalRecords;
}
