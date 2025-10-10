package org.folio.bulkops.domain.converter;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.Department;
import org.folio.bulkops.domain.bean.DepartmentCollection;
import org.junit.jupiter.api.Test;

class DepartmentsConverterTest extends BaseTest {

  @Test
  void convertToObjectTestWhenTwoDepartments() {
    UUID uuid1 = UUID.randomUUID();
    UUID uuid2 = UUID.randomUUID();
    when(departmentClient.getByQuery("name==\"dep1\"")).thenReturn(DepartmentCollection.builder()
        .departments(singletonList(Department.builder().id(uuid1.toString()).build())).build());
    when(departmentClient.getByQuery("name==\"dep2\"")).thenReturn(DepartmentCollection.builder()
        .departments(singletonList(Department.builder().id(uuid2.toString()).build())).build());
    DepartmentsConverter converter = new DepartmentsConverter();
    var actual = converter.convertToObject("dep1;dep2");
    assertThat(actual, containsInAnyOrder(List.of(uuid1, uuid2).toArray()));
  }
}
