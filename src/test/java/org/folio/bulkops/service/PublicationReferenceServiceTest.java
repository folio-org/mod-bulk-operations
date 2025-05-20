package org.folio.bulkops.service;

import org.folio.bulkops.client.PublicationClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PublicationReferenceServiceTest {

  @Mock
  private PublicationClient publicationClient;

  @InjectMocks
  private PublicationReferenceService publicationReferenceService;
}
