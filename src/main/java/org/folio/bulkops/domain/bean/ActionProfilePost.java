package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ActionProfilePost {

  @JsonProperty("id")
  private String id;

  @JsonProperty("profile")
  private ActionProfile profile;

  @JsonProperty("addedRelations")
  private List<ProfileAssociation> addedRelations;
}
