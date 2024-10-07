package org.folio.bulkops.processor.note;

import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180ParserBuilder;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.NoteTableUpdater;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.service.TenantTableUpdater.TENANT_VALUE_IN_CONSORTIA_FOR_MEMBER;

@Log4j2
public abstract class CsvDownloadPreProcessor {

  private static final int FIRST_LINE = 1;

  protected NoteTableUpdater noteTableUpdater;
  protected CacheManager cacheManager;

  protected FolioExecutionContext folioExecutionContext;
  protected ConsortiaService consortiaService;

  @Autowired
  private void setNoteTableUpdater(NoteTableUpdater noteTableUpdater) {
    this.noteTableUpdater = noteTableUpdater;
  }

  @Autowired
  private void setCacheManager(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @Autowired
  protected void setFolioExecutionContext(FolioExecutionContext folioExecutionContext) {
    this.folioExecutionContext = folioExecutionContext;
  }
  @Autowired
  protected void setConsortiaService(ConsortiaService consortiaService) {
    this.consortiaService = consortiaService;
  }

  public byte[] processCsvContent(byte[] input, BulkOperation bulkOperation) {
    boolean isConsortiaTenant = consortiaService.isCurrentTenantCentralTenant(folioExecutionContext.getTenantId());
    boolean isTypeWithTenant = bulkOperation.getEntityType() == EntityType.ITEM || bulkOperation.getEntityType() == EntityType.HOLDINGS_RECORD;

    List<String> noteTypeNames = getNoteTypeNames(bulkOperation);
    var noteTypeHeaders = noteTypeNames.stream()
      .map(noteTableUpdater::concatNotePostfixIfRequired)
      .toList();

    try (var reader = new CSVReaderBuilder(new InputStreamReader(new ByteArrayInputStream(input)))
      .withCSVParser(new RFC4180ParserBuilder().build()).build();
         var stringWriter = new StringWriter()) {
      String[] line;
      while ((line = reader.readNext()) != null) {
        if (reader.getRecordsRead() == FIRST_LINE) {
          var headers = new ArrayList<>(Arrays.asList(line));
          headers.remove(getNotePosition());
          headers.addAll(getNotePosition(), noteTypeHeaders);
          line = headers.stream()
            .map(this::processSpecialCharacters)
            .toArray(String[]::new);
          processTenantInHeaders(line, isConsortiaTenant, isTypeWithTenant);
        } else {
          line = processTenantInRows(line, isConsortiaTenant, isTypeWithTenant);
          line = processNotesData(line, noteTypeNames, bulkOperation);
        }
        stringWriter.write(String.join(",", line) + "\n");
      }
      stringWriter.flush();
      return stringWriter.toString().getBytes();
    } catch (Exception e) {
      log.error(e.getMessage());
      return new byte[0];
    }
  }

  protected abstract List<String> getNoteTypeNames(BulkOperation bulkOperation);

  protected abstract int getNotePosition();

  private String[] processNotesData(String[] line, List<String> noteTypeNames, BulkOperation bulkOperation) {
    return noteTableUpdater.enrichWithNotesByType(new ArrayList<>(Arrays.asList(line)), getNotePosition(), noteTypeNames,
        bulkOperation.getTenantNotePairs(), bulkOperation.getEntityType() == EntityType.INSTANCE ||
          bulkOperation.getEntityType() == EntityType.INSTANCE_MARC).stream()
      .map(this::processSpecialCharacters)
      .toArray(String[]::new);
  }

  private String[] processTenantInHeaders(String[] line, boolean isConsortiaTenant, boolean isTypeWithTenant) {
    if (isTypeWithTenant) {
      int tenantPosition = line.length - 1;
      if (isConsortiaTenant) {
        line[tenantPosition] = TENANT_VALUE_IN_CONSORTIA_FOR_MEMBER;
      } else {
        line = Arrays.copyOf(line, tenantPosition);
      }
    }
    return line;
  }

  private String[] processTenantInRows(String[] line, boolean isConsortiaTenant, boolean isTypeWithTenant) {
    if (isTypeWithTenant && !isConsortiaTenant) {
      int tenantPosition = line.length - 1;
      line = Arrays.copyOf(line, tenantPosition);
    }
    return line;
  }

  private String processSpecialCharacters(String line) {
    if (isNotEmpty(line)) {
      line = line.contains("\"") ? line.replace("\"", "\"\"") : line;
      return line.contains(",") || line.contains("\n") ? "\"" + line + "\"" : line;
    }
    return EMPTY;
  }
}
