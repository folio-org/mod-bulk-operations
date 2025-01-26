package org.folio.bulkops.service;

import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@Primary
public class FolioTenantService extends TenantService {

  public FolioTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context, FolioSpringLiquibase folioSpringLiquibase, PrepareSystemUserService prepareSystemUserService) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.prepareSystemUserService = prepareSystemUserService;
  }

  private final PrepareSystemUserService prepareSystemUserService;

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    try {
      prepareSystemUserService.setupSystemUser();
    } catch (Exception exception) {
      log.error(exception.getMessage(), exception);
      throw exception;
    }
  }
}
