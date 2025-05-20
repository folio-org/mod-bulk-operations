package org.folio.bulkops.service;

import org.folio.bulkops.BaseTest;
import org.springframework.beans.factory.annotation.Autowired;

public class PublicationServiceTest extends BaseTest {

  @Autowired
  private PublicationService publicationService;

  @Autowired
  private PublicationReferenceService publicationReferenceService;
}
