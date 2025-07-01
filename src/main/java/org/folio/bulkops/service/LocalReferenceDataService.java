package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalReferenceDataService {

    private final FolioExecutionContext folioExecutionContext;

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
}
