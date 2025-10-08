package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
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
public class JobExecutionCollection {

  @JsonProperty("jobExecutions")
  private List<JobExecution> jobExecutions = new ArrayList<>();

  @JsonProperty("totalRecords")
  private Integer totalRecords;
}
