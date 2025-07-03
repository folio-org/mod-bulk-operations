package org.folio.bulkops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.domain.bean.HoldingsNote;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemNote;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class LocalReferenceDataServiceTest {

    private LocalReferenceDataService service;
    private Cache cache;

    @BeforeEach
    void setUp() {
        FolioExecutionContext folioExecutionContext = mock(FolioExecutionContext.class);
        CacheManager cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(folioExecutionContext.getTenantId()).thenReturn("tenant1");
        service = new LocalReferenceDataService(folioExecutionContext, cacheManager);
    }

    @Test
    void getTenantByStatisticalCodeId_returnsTenantId() {
        String result = service.getTenantByStatisticalCodeId("code1");
        assertEquals("tenant1", result);
    }

    @Test
    void getTenantByIllPolicyId_returnsTenantId() {
        String result = service.getTenantByIllPolicyId("ill1");
        assertEquals("tenant1", result);
    }

    @Test
    void getTenantByLocationId_returnsTenantId() {
        String result = service.getTenantByLocationId("loc1");
        assertEquals("tenant1", result);
    }

    @Test
    void getTenantByHoldingsSourceId_returnsTenantId() {
        String result = service.getTenantByHoldingsSourceId("src1");
        assertEquals("tenant1", result);
    }

    @Test
    void getTenantByHoldingsTypeId_returnsTenantId() {
        String result = service.getTenantByHoldingsTypeId("type1");
        assertEquals("tenant1", result);
    }

    @Test
    void getTenantByCallNumberTypeId_returnsTenantId() {
        String result = service.getTenantByCallNumberTypeId("call1");
        assertEquals("tenant1", result);
    }

    @Test
    void getTenantByUrlRelationshipId_returnsTenantId() {
        String result = service.getTenantByUrlRelationshipId("url1");
        assertEquals("tenant1", result);
    }

    @Test
    void enrichWithTenant_Item_setsTenantIdAndCaches() {
        Item item = new Item();
        ElectronicAccess ea = new ElectronicAccess();
        ItemNote note = new ItemNote();
        item.setElectronicAccess(Collections.singletonList(ea));
        item.setNotes(Collections.singletonList(note));
        item.setStatisticalCodes(Arrays.asList("code1", "code2"));
        item.setItemLevelCallNumberType("callType1");

        service.enrichWithTenant(item, "tenant2");

        assertEquals("tenant2", ea.getTenantId());
        assertEquals("tenant2", note.getTenantId());
        verify(cache, times(1)).put("code1", "tenant2");
        verify(cache, times(1)).put("code2", "tenant2");
        verify(cache, times(1)).put("callType1", "tenant2");
    }

    @Test
    void enrichWithTenant_HoldingsRecord_setsTenantIdAndCaches() {
        HoldingsRecord holdingsRecord = new HoldingsRecord();
        ElectronicAccess ea = new ElectronicAccess();
        ea.setRelationshipId("rel1");
        HoldingsNote note = new HoldingsNote();
        holdingsRecord.setElectronicAccess(Collections.singletonList(ea));
        holdingsRecord.setNotes(Collections.singletonList(note));
        holdingsRecord.setStatisticalCodeIds(Arrays.asList("code1", "code2"));
        holdingsRecord.setIllPolicyId("ill1");
        holdingsRecord.setEffectiveLocationId("loc1");
        holdingsRecord.setPermanentLocationId("loc2");
        holdingsRecord.setSourceId("src1");
        holdingsRecord.setHoldingsTypeId("type1");
        holdingsRecord.setTemporaryLocationId("loc3");

        service.enrichWithTenant(holdingsRecord, "tenant3");

        assertEquals("tenant3", ea.getTenantId());
        assertEquals("tenant3", note.getTenantId());
        verify(cache, times(1)).put("code1", "tenant3");
        verify(cache, times(1)).put("code2", "tenant3");
        verify(cache, times(1)).put("ill1", "tenant3");
        verify(cache, times(1)).put("src1", "tenant3");
        verify(cache, times(1)).put("type1", "tenant3");
        verify(cache, times(3)).put(startsWith("loc"), eq("tenant3"));
        verify(cache, times(1)).put("rel1", "tenant3");
    }

    @Test
    void updateTenantForLocation_putsInCache() {
        service.updateTenantForLocation("loc1", "tenant4");
        verify(cache, times(1)).put("loc1", "tenant4");
    }

    @Test
    void updateTenantForUrlRelationship_putsInCache() {
        service.updateTenantForUrlRelationship("url1", "tenant5");
        verify(cache, times(1)).put("url1", "tenant5");
    }
}