package org.folio.bulkops.service;

import static java.util.Objects.nonNull;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalReferenceDataService {

    private final FolioExecutionContext folioExecutionContext;
    private final CacheManager cacheManager;

    @Cacheable(cacheNames = "statisticalCodeId")
    public String getTenantByStatisticalCodeId(String statisticalCodeId) {
        return folioExecutionContext.getTenantId();
    }

    @Cacheable(cacheNames = "illPolicyId")
    public String getTenantByIllPolicyId(String illPolicyId) {
        return folioExecutionContext.getTenantId();
    }

    @Cacheable(cacheNames = "locationId")
    public String getTenantByLocationId(String locationId) {
        return folioExecutionContext.getTenantId();
    }

    @Cacheable(cacheNames = "holdingsSourceId")
    public String getTenantByHoldingsSourceId(String holdingsSourceId) {
        return folioExecutionContext.getTenantId();
    }

    @Cacheable(cacheNames = "holdingsTypeId")
    public String getTenantByHoldingsTypeId(String holdingsTypeId) {
        return folioExecutionContext.getTenantId();
    }

    @Cacheable(cacheNames = "callNumberTypeId")
    public String getTenantByCallNumberTypeId(String callNumberTypeId) {
        return folioExecutionContext.getTenantId();
    }

    @Cacheable(cacheNames = "urlRelationshipId")
    public String getTenantByUrlRelationshipId(String urlRelationshipId) {
        return folioExecutionContext.getTenantId();
    }

    public void enrichWithTenant(Item item, String tenantId) {
        if (nonNull(item.getElectronicAccess())) {
            item.getElectronicAccess().forEach(el -> el.setTenantId(tenantId));
        }
        if (nonNull(item.getNotes())) {
            item.getNotes().forEach(note -> note.setTenantId(tenantId));
        }
        if (nonNull(item.getStatisticalCodes())) {
            item.getStatisticalCodes().forEach(codeId -> Objects.requireNonNull(cacheManager.getCache("statisticalCodeId")).put(codeId, tenantId));
        }
        if (nonNull(item.getItemLevelCallNumberType())) {
            Objects.requireNonNull(cacheManager.getCache("callNumberTypeId")).put(item.getItemLevelCallNumberType(), tenantId);
        }
    }

    public void enrichWithTenant(HoldingsRecord holdingsRecord, String tenantId) {
        if (nonNull(holdingsRecord.getElectronicAccess())) {
            holdingsRecord.getElectronicAccess().forEach(el -> el.setTenantId(tenantId));
        }
        if (nonNull(holdingsRecord.getNotes())) {
            holdingsRecord.getNotes().forEach(note -> note.setTenantId(tenantId));
        }
        if (nonNull(holdingsRecord.getStatisticalCodeIds())) {
            holdingsRecord.getStatisticalCodeIds().forEach(codeId -> Objects.requireNonNull(cacheManager.getCache("statisticalCodeId")).put(codeId, tenantId));
        }
        if (nonNull(holdingsRecord.getIllPolicyId())) {
            Objects.requireNonNull(cacheManager.getCache("illPolicyId")).put(holdingsRecord.getIllPolicyId(), tenantId);
        }
        if (nonNull(holdingsRecord.getEffectiveLocationId())) {
            updateTenantForLocation(holdingsRecord.getEffectiveLocationId(), tenantId);
        }
        if (nonNull(holdingsRecord.getPermanentLocationId())) {
            updateTenantForLocation(holdingsRecord.getPermanentLocationId(), tenantId);
        }
        if (nonNull(holdingsRecord.getSourceId())) {
            Objects.requireNonNull(cacheManager.getCache("holdingsSourceId")).put(holdingsRecord.getSourceId(), tenantId);
        }
        if (nonNull(holdingsRecord.getHoldingsTypeId())) {
            Objects.requireNonNull(cacheManager.getCache("holdingsTypeId")).put(holdingsRecord.getHoldingsTypeId(), tenantId);
        }
        if (nonNull(holdingsRecord.getTemporaryLocationId())) {
            updateTenantForLocation(holdingsRecord.getTemporaryLocationId(), tenantId);
        }
    }

    public void updateTenantForLocation(String id, String tenantId) {
        Objects.requireNonNull(cacheManager.getCache("locationId")).put(id, tenantId);
    }

    public void updateTenantForUrlRelationship(String id, String tenantId) {
        Objects.requireNonNull(cacheManager.getCache("urlRelationshipId")).put(id, tenantId);
    }
}
