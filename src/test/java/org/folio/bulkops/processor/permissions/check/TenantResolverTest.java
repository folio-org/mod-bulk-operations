package org.folio.bulkops.processor.permissions.check;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.exception.AffiliationException;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantResolverTest {

  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private UserClient userClient;
  @Mock
  private PermissionsValidator permissionsValidator;
  @Mock
  private ReadPermissionsValidator readPermissionsValidator;

  @InjectMocks
  private TenantResolver tenantResolver;

  @ParameterizedTest
  @MethodSource("supportedEntityTypesAndIdentifierTypes")
  void shouldNotThrowAffiliationExceptionForSupportedEntityTypesIfTenantIsAffiliated(EntityType entityType, IdentifierType identifierType) {
    var identifier = UUID.randomUUID().toString();
    var userId = UUID.randomUUID();
    var currentTenant = "tenant1";
    var affiliatedTenants = List.of("tenant3", currentTenant);

    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(currentTenant);
    when(consortiaService.getAffiliatedTenants(currentTenant, userId.toString())).thenReturn(affiliatedTenants);
    when(readPermissionsValidator.isBulkEditReadPermissionExists(currentTenant, entityType)).thenReturn(true);

    assertDoesNotThrow(() -> tenantResolver.checkAffiliatedPermittedTenantIds(entityType, identifierType.getValue(),
            Set.of(currentTenant), identifier));
  }

  @ParameterizedTest
  @MethodSource("supportedEntityTypesAndIdentifierTypes")
  void shouldThrowAffiliationExceptionForSupportedEntityTypesIfTenantIsNotAffiliated(EntityType entityType, IdentifierType identifierType) {
    var identifier = UUID.randomUUID().toString();
    var userId = UUID.randomUUID();
    var currentTenant = "tenant1";
    var affiliatedTenants = List.of("tenant3", "tenant2");

    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(currentTenant);
    when(userClient.getUserById(userId.toString())).thenReturn(User.builder().id(userId.toString()).build());
    when(consortiaService.getAffiliatedTenants(currentTenant, userId.toString())).thenReturn(affiliatedTenants);

    assertThrows(AffiliationException.class, () -> tenantResolver.checkAffiliatedPermittedTenantIds(entityType, identifierType.getValue(),
            Set.of(currentTenant), identifier));
  }

  @ParameterizedTest
  @MethodSource("unsupportedEntityTypesAndIdentifierTypes")
  void shouldThrowUnsupportedExceptionIfEntityTypeIsNotSupported(EntityType entityType, IdentifierType identifierType) {
    var identifier = UUID.randomUUID().toString();
    var userId = UUID.randomUUID();
    var currentTenant = "tenant1";
    var affiliatedTenants = List.of("tenant3", currentTenant);

    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(currentTenant);
    when(userClient.getUserById(userId.toString())).thenReturn(User.builder().id(userId.toString()).build());
    when(consortiaService.getAffiliatedTenants(currentTenant, userId.toString())).thenReturn(affiliatedTenants);

    Executable action = () -> tenantResolver.checkAffiliatedPermittedTenantIds(
            entityType,
            identifierType.getValue(),
            Set.of(currentTenant),
            identifier
    );

    assertThrows(UnsupportedOperationException.class, action);
  }

  private static Stream<Arguments> supportedEntityTypesAndIdentifierTypes() {
    return Arrays.stream(EntityType.values())
            .filter(type -> type == EntityType.ITEM || type == EntityType.HOLDINGS_RECORD)
            .flatMap(entityType ->
                    Arrays.stream(IdentifierType.values())
                            .map(identifierType -> Arguments.of(entityType, identifierType))
            );
  }

  private static Stream<Arguments> unsupportedEntityTypesAndIdentifierTypes() {
    return Arrays.stream(EntityType.values())
            .filter(type -> type != EntityType.ITEM && type != EntityType.HOLDINGS_RECORD)
            .flatMap(entityType ->
                    Arrays.stream(IdentifierType.values())
                            .map(identifierType -> Arguments.of(entityType, identifierType))
            );
  }
}
