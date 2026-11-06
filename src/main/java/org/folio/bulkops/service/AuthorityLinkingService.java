package org.folio.bulkops.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.EntitiesLinksClient;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
@Log4j2
@RequiredArgsConstructor
public class AuthorityLinkingService {
  private static final String AUTHORITY_SEARCH_PARAMETER = "NATURAL_ID";
  private static final String LINK_STATUS_NEW = "NEW";
  // POC: 240 has several linking rules, data import cannot resolve the rule from the tag alone
  private static final Set<String> SKIPPED_TAGS = Set.of("240");

  private final EntitiesLinksClient entitiesLinksClient;
  private final ObjectMapper objectMapper;

  public Set<String> getAutoLinkingTags() {
    var tags = new HashSet<String>();
    entitiesLinksClient
        .getLinkingRules()
        .forEach(
            rule -> {
              if (rule.path("autoLinkingEnabled").asBoolean(false)) {
                tags.add(rule.path("bibField").asString());
              }
            });
    return tags;
  }

  /** Applies links suggested by mod-entities-links to the records in place. */
  public void linkRecords(List<Record> records) {
    if (records.isEmpty()) {
      return;
    }
    var request = objectMapper.createObjectNode();
    var requestRecords = request.putArray("records");
    records.forEach(marcRecord -> requestRecords.add(toParsedContent(marcRecord)));

    var suggestedRecords =
        entitiesLinksClient
            .suggestLinks(request, AUTHORITY_SEARCH_PARAMETER, false)
            .path("records");

    for (int i = 0; i < records.size(); i++) {
      applySuggestions(records.get(i), suggestedRecords.path(i));
    }
  }

  private ObjectNode toParsedContent(Record marcRecord) {
    var content = objectMapper.createObjectNode();
    content.put("leader", marcRecord.getLeader().marshal());
    var fields = content.putArray("fields");
    marcRecord
        .getDataFields()
        .forEach(
            dataField -> {
              var fieldContent = objectMapper.createObjectNode();
              fieldContent.put("ind1", String.valueOf(dataField.getIndicator1()));
              fieldContent.put("ind2", String.valueOf(dataField.getIndicator2()));
              var subfields = fieldContent.putArray("subfields");
              dataField
                  .getSubfields()
                  .forEach(
                      subfield ->
                          subfields
                              .addObject()
                              .put(String.valueOf(subfield.getCode()), subfield.getData()));
              fields.addObject().set(dataField.getTag(), fieldContent);
            });
    return content;
  }

  // A field returned as NEW is taken exactly as returned; it is newly linked only if its $9 differs
  // from the matched record, which commit finds by comparing matched and proposed records
  private void applySuggestions(Record marcRecord, JsonNode suggestedRecord) {
    var dataFields = marcRecord.getDataFields();
    var suggestedFields = suggestedRecord.path("fields");
    if (suggestedFields.size() != dataFields.size()) {
      log.warn(
          "Suggested fields do not match record fields, record {} is left unchanged",
          marcRecord.getControlNumber());
      return;
    }
    for (int i = 0; i < dataFields.size(); i++) {
      var dataField = dataFields.get(i);
      var suggestedField = suggestedFields.path(i).path(dataField.getTag());
      if (!SKIPPED_TAGS.contains(dataField.getTag())
          && LINK_STATUS_NEW.equals(suggestedField.path("linkDetails").path("status").asString())) {
        replaceSubfields(dataField, suggestedField.path("subfields"));
      }
    }
  }

  private void replaceSubfields(DataField dataField, JsonNode suggestedSubfields) {
    var factory = MarcFactory.newInstance();
    List.copyOf(dataField.getSubfields()).forEach(dataField::removeSubfield);
    suggestedSubfields.forEach(
        subfield ->
            subfield
                .properties()
                .forEach(
                    entry ->
                        dataField.addSubfield(
                            factory.newSubfield(
                                entry.getKey().charAt(0), entry.getValue().asString()))));
  }
}
