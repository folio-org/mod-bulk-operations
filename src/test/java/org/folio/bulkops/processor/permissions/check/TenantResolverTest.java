package org.folio.bulkops.processor.permissions.check;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.folio.bulkops.domain.bean.JobParameterNames.BULK_OPERATION_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.exception.AffiliationException;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ErrorService;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.JobExecution;

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
  @Mock
  private ErrorService errorService;

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

  @ParameterizedTest
  @MethodSource("validTenantScenarios")
  void returnsAffiliatedAndPermittedTenantsWhenValid(EntityType entityType, String identifierType, Set<String> tenantIds, Set<String> expectedTenants) {
    var itemIdentifier = new ItemIdentifier().withItemId("item123");
    var jobExecution = mock(JobExecution.class, RETURNS_DEEP_STUBS);
    var userId = UUID.randomUUID();
    var currentTenant = "tenant1";
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(currentTenant);
    when(jobExecution.getJobParameters().getString(BULK_OPERATION_ID)).thenReturn(UUID.randomUUID().toString());
    when(consortiaService.getAffiliatedTenants(anyString(), anyString())).thenReturn(List.of("tenant1", "tenant2"));
    when(readPermissionsValidator.isBulkEditReadPermissionExists(anyString(), eq(entityType))).thenReturn(true);

    var result = tenantResolver.getAffiliatedPermittedTenantIds(entityType, jobExecution, identifierType, tenantIds, itemIdentifier);

    assertThat(result).isEqualTo(expectedTenants);
  }

  @ParameterizedTest
  @MethodSource("invalidTenantScenarios")
  void savesErrorWhenTenantNotAffiliatedOrPermissionMissing(EntityType entityType, String identifierType, Set<String> tenantIds, String expectedErrorMessage) {
    var itemIdentifier = new ItemIdentifier().withItemId("item123");
    var jobExecution = mock(JobExecution.class, RETURNS_DEEP_STUBS);
    var userId = UUID.randomUUID();
    var currentTenant = "tenant1";
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(currentTenant);
    when(jobExecution.getJobParameters().getString(BULK_OPERATION_ID)).thenReturn(UUID.randomUUID().toString());
    when(consortiaService.getAffiliatedTenants(anyString(), anyString())).thenReturn(List.of("tenant1"));
    when(userClient.getUserById(anyString())).thenReturn(new User().withUsername("testuser"));

    var result = tenantResolver.getAffiliatedPermittedTenantIds(entityType, jobExecution, identifierType, tenantIds, itemIdentifier);

    verify(errorService, times(tenantIds.size())).saveError(any(), eq("item123"), eq(expectedErrorMessage), eq(org.folio.bulkops.domain.dto.ErrorType.ERROR));
    assertThat(result).isEmpty();
  }

  private static Stream<Arguments> validTenantScenarios() {
    return Stream.of(
      Arguments.of(EntityType.ITEM, "ID", Set.of("tenant1", "tenant2"), Set.of("tenant1", "tenant2")),
      Arguments.of(EntityType.HOLDINGS_RECORD, "ID", Set.of("tenant1"), Set.of("tenant1"))
    );
  }

  private static Stream<Arguments> invalidTenantScenarios() {
    return Stream.of(
      Arguments.of(EntityType.ITEM, "ID", Set.of("tenant3"), "User testuser does not have required affiliation to view the item record - id=item123 on the tenant tenant3"),
      Arguments.of(EntityType.HOLDINGS_RECORD, "ID", Set.of("tenant4"), "User testuser does not have required affiliation to view the holdings record - id=item123 on the tenant tenant4")
    );
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
