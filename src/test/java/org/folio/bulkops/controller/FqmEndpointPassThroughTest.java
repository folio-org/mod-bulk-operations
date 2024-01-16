package org.folio.bulkops.controller;

import com.flextrade.jfixture.JFixture;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.EntityTypesClient;
import org.folio.bulkops.client.FqmQueryClient;
import org.folio.bulkops.service.EntityTypesService;
import org.folio.bulkops.service.FqmQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class FqmEndpointPassThroughTest extends BaseTest {

  @Autowired
  private EntityTypesController entityTypesController;
  @SpyBean
  private EntityTypesService entityTypesService;
  @MockBean
  private EntityTypesClient entityTypesClient;

  @Autowired
  private FqmQueryController queryController;
  @SpyBean
  private FqmQueryService queryService;
  @MockBean
  private FqmQueryClient queryClient;

  @Test
  void testEntityTypesPassThrough() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    // Skip getRequests() in the entity type controller, since it's only there to resolve a name conflict in its
    // two parent interfaces
    verifyPassThrough(EntityTypesController.class, entityTypesController, entityTypesService, entityTypesClient, "getRequest");
  }

  @Test
  void testFqmQueryPassThrough() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    verifyPassThrough(FqmQueryController.class, queryController, queryService, queryClient);
  }

  /**
   * Verify that all methods in controller pass straight through to the service and client.
   * <p>
   * Note: There's a build-in assumption here that the controller, service, and client have the same basic interface; as
   * in, if a method exists in the controller, then both the corresponding service and client should have methods with the
   * same signature.
   *
   * @param controllerClass - The concrete class for the controller; this is needed due to Spring's tendency to wrap classes, making it difficult to see what methods are actually overridden
   * @param controller      - The controller to verify
   * @param service         - The backing service for the controller
   * @param client          - The underlying client that the service uses to retrieve data
   * @param methodsToIgnore - Method names to skip
   */
  private <C, S, R> void verifyPassThrough(Class<C> controllerClass, C controller,
                                           S service,
                                           R client, String... methodsToIgnore) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    var ignoredMethods = Set.of(methodsToIgnore);
    for (var controllerMethod : controllerClass.getMethods()) {
      // Skip any non - overridden inherited methods, along with any explicitly skipped ones
      if (!controllerMethod.getDeclaringClass().equals(controllerClass) || ignoredMethods.contains(controllerMethod.getName())) {
        continue;
      }
      // Given a controller, service, and client method
      var serviceMethod = service.getClass().getMethod(controllerMethod.getName(), controllerMethod.getParameterTypes());
      var clientMethod = client.getClass().getMethod(controllerMethod.getName(), controllerMethod.getParameterTypes());

      // And given a set of random method arguments for the controller method
      JFixture fixture = new JFixture();
      var args = Stream.of(controllerMethod.getParameterTypes()).map(fixture::create).toArray();

      // When we call the controller method with those arguments
      controllerMethod.invoke(controller, args);

      // Then the service and client methods should get called with the same arguments exactly once
      serviceMethod.invoke(verify(service, times(1)), args);
      clientMethod.invoke(verify(client, times(1)), args);
    }
  }
}
