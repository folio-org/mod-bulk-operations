package org.folio.bulkops.processor.note;

import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ItemReferenceService;
import org.folio.bulkops.service.NoteTableUpdater;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemNoteProcessorTest {

  @Mock
  private ItemReferenceService itemReferenceService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private CacheManager cacheManager;
  @Mock
  private Cache cache;
  @Mock
  private NoteTableUpdater noteTableUpdater;

  @InjectMocks
  private ItemNoteProcessor itemNoteProcessor;

  @BeforeEach
  void setUp() {
    itemNoteProcessor.cacheManager = cacheManager;
    itemNoteProcessor.noteTableUpdater = noteTableUpdater;
  }

  @Test
  void getNoteTypeNamesTest() {
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put("tenant", List.of("central"));
    var noteType1 = NoteType.builder().id("id1").name("noteType1").build();
    var noteType2 = NoteType.builder().id("id2").name("noteType2").build();

    when(folioExecutionContext.getTenantId()).thenReturn("central");
    when(itemReferenceService.getAllItemNoteTypes("central")).thenReturn(List.of(noteType1));
    when(itemReferenceService.getAllItemNoteTypes("member")).thenReturn(List.of(noteType2));

    when(consortiaService.isCurrentTenantCentralTenant(any())).thenReturn(true);
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(cacheManager.getCache("itemNoteTypes")).thenReturn(cache);

    var bulkOperation = new BulkOperation();
    bulkOperation.setUsedTenants(List.of("central", "member"));
    var notesTypes = itemNoteProcessor.getNoteTypeNames(bulkOperation);
    assertTrue(notesTypes.contains(noteType1.getName()));
    assertTrue(notesTypes.contains(noteType2.getName()));
  }
}
