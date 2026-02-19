package org.folio.bulkops.configs;

import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.AddressTypeClient;
import org.folio.bulkops.client.BulkEditClient;
import org.folio.bulkops.client.CallNumberTypeClient;
import org.folio.bulkops.client.ClassificationTypesClient;
import org.folio.bulkops.client.ConsortiaClient;
import org.folio.bulkops.client.ConsortiumClient;
import org.folio.bulkops.client.ContributorTypesClient;
import org.folio.bulkops.client.CustomFieldsClient;
import org.folio.bulkops.client.DamagedStatusClient;
import org.folio.bulkops.client.DataImportClient;
import org.folio.bulkops.client.DataImportProfilesClient;
import org.folio.bulkops.client.DataImportUploadClient;
import org.folio.bulkops.client.DepartmentClient;
import org.folio.bulkops.client.ElectronicAccessRelationshipClient;
import org.folio.bulkops.client.EntityTypeClient;
import org.folio.bulkops.client.EurekaUserPermissionsClient;
import org.folio.bulkops.client.GroupClient;
import org.folio.bulkops.client.HoldingsNoteTypeClient;
import org.folio.bulkops.client.HoldingsSourceClient;
import org.folio.bulkops.client.HoldingsStorageClient;
import org.folio.bulkops.client.HoldingsTypeClient;
import org.folio.bulkops.client.IllPolicyClient;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.InstanceFormatsClient;
import org.folio.bulkops.client.InstanceNoteTypesClient;
import org.folio.bulkops.client.InstanceStatusesClient;
import org.folio.bulkops.client.InstanceStorageClient;
import org.folio.bulkops.client.InstanceTypesClient;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.client.ItemNoteTypeClient;
import org.folio.bulkops.client.ItemStorageClient;
import org.folio.bulkops.client.LoanTypeClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.client.MappingRulesClient;
import org.folio.bulkops.client.MaterialTypeClient;
import org.folio.bulkops.client.MetadataProviderClient;
import org.folio.bulkops.client.ModesOfIssuanceClient;
import org.folio.bulkops.client.NatureOfContentTermsClient;
import org.folio.bulkops.client.OkapiClient;
import org.folio.bulkops.client.OkapiUserPermissionsClient;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.client.SearchClient;
import org.folio.bulkops.client.SearchConsortium;
import org.folio.bulkops.client.ServicePointClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.client.StatisticalCodeClient;
import org.folio.bulkops.client.StatisticalCodeTypeClient;
import org.folio.bulkops.client.SubjectSourcesClient;
import org.folio.bulkops.client.SubjectTypesClient;
import org.folio.bulkops.client.UserClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@Log4j2
public class HttpClientConfiguration {

  @Bean
  public AddressTypeClient addressTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(AddressTypeClient.class);
  }

  @Bean
  public BulkEditClient bulkEditClient(HttpServiceProxyFactory factory) {
    return factory.createClient(BulkEditClient.class);
  }

  @Bean
  public CallNumberTypeClient callNumberTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CallNumberTypeClient.class);
  }

  @Bean
  public ClassificationTypesClient classificationTypesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ClassificationTypesClient.class);
  }

  @Bean
  public ConsortiaClient consortiaClient(HttpServiceProxyFactory factory) {
    log.info("Creating ConsortiaClient instance for consortiaClient");
    return factory.createClient(ConsortiaClient.class);
  }

  @Bean
  public ConsortiumClient consortiumClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ConsortiumClient.class);
  }

  @Bean
  public ContributorTypesClient contributorTypesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ContributorTypesClient.class);
  }

  @Bean
  public CustomFieldsClient customFieldsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CustomFieldsClient.class);
  }

  @Bean
  public DamagedStatusClient damagedStatusClient(HttpServiceProxyFactory factory) {
    return factory.createClient(DamagedStatusClient.class);
  }

  @Bean
  public DataImportClient dataImportClient(HttpServiceProxyFactory factory) {
    return factory.createClient(DataImportClient.class);
  }

  @Bean
  public DataImportProfilesClient dataImportProfilesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(DataImportProfilesClient.class);
  }

  @Bean
  public DataImportUploadClient dataImportUploadClient(HttpServiceProxyFactory factory) {
    return factory.createClient(DataImportUploadClient.class);
  }

  @Bean
  public DepartmentClient departmentClient(HttpServiceProxyFactory factory) {
    return factory.createClient(DepartmentClient.class);
  }

  @Bean
  public ElectronicAccessRelationshipClient electronicAccessRelationshipClient(
      HttpServiceProxyFactory factory) {
    return factory.createClient(ElectronicAccessRelationshipClient.class);
  }

  @Bean
  public EntityTypeClient entityTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(EntityTypeClient.class);
  }

  @Bean
  public EurekaUserPermissionsClient eurekaUserPermissionsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(EurekaUserPermissionsClient.class);
  }

  @Bean
  public GroupClient groupClient(HttpServiceProxyFactory factory) {
    return factory.createClient(GroupClient.class);
  }

  @Bean
  public HoldingsNoteTypeClient holdingsNoteTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(HoldingsNoteTypeClient.class);
  }

  @Bean
  public HoldingsSourceClient holdingsSourceClient(HttpServiceProxyFactory factory) {
    return factory.createClient(HoldingsSourceClient.class);
  }

  @Bean
  public HoldingsStorageClient holdingsStorageClient(HttpServiceProxyFactory factory) {
    return factory.createClient(HoldingsStorageClient.class);
  }

  @Bean
  public HoldingsTypeClient holdingsTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(HoldingsTypeClient.class);
  }

  @Bean
  public IllPolicyClient illPolicyClient(HttpServiceProxyFactory factory) {
    return factory.createClient(IllPolicyClient.class);
  }

  @Bean
  public InstanceClient instanceClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InstanceClient.class);
  }

  @Bean
  public InstanceFormatsClient instanceFormatsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InstanceFormatsClient.class);
  }

  @Bean
  public InstanceNoteTypesClient instanceNoteTypesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InstanceNoteTypesClient.class);
  }

  @Bean
  public InstanceStatusesClient instanceStatusesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InstanceStatusesClient.class);
  }

  @Bean
  public InstanceStorageClient instanceStorageClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InstanceStorageClient.class);
  }

  @Bean
  public InstanceTypesClient instanceTypesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InstanceTypesClient.class);
  }

  @Bean
  public ItemClient itemClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ItemClient.class);
  }

  @Bean
  public ItemNoteTypeClient itemNoteTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ItemNoteTypeClient.class);
  }

  @Bean
  public ItemStorageClient itemStorageClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ItemStorageClient.class);
  }

  @Bean
  public LoanTypeClient loanTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LoanTypeClient.class);
  }

  @Bean
  public LocationClient locationClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LocationClient.class);
  }

  @Bean
  public MappingRulesClient mappingRulesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(MappingRulesClient.class);
  }

  @Bean
  public MaterialTypeClient materialTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(MaterialTypeClient.class);
  }

  @Bean
  public MetadataProviderClient metadataProviderClient(HttpServiceProxyFactory factory) {
    return factory.createClient(MetadataProviderClient.class);
  }

  @Bean
  public ModesOfIssuanceClient modesOfIssuanceClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ModesOfIssuanceClient.class);
  }

  @Bean
  public NatureOfContentTermsClient natureOfContentTermsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(NatureOfContentTermsClient.class);
  }

  @Bean
  public OkapiClient okapiClient(HttpServiceProxyFactory factory) {
    return factory.createClient(OkapiClient.class);
  }

  @Bean
  public OkapiUserPermissionsClient okapiUserPermissionsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(OkapiUserPermissionsClient.class);
  }

  @Bean
  public QueryClient queryClient(HttpServiceProxyFactory factory) {
    return factory.createClient(QueryClient.class);
  }

  @Bean
  public SearchClient searchClient(HttpServiceProxyFactory factory) {
    return factory.createClient(SearchClient.class);
  }

  @Bean
  public SearchConsortium searchConsortium(HttpServiceProxyFactory factory) {
    return factory.createClient(SearchConsortium.class);
  }

  @Bean
  public ServicePointClient servicePointClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ServicePointClient.class);
  }

  @Bean
  public SrsClient srsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(SrsClient.class);
  }

  @Bean
  public StatisticalCodeClient statisticalCodeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(StatisticalCodeClient.class);
  }

  @Bean
  public StatisticalCodeTypeClient statisticalCodeTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(StatisticalCodeTypeClient.class);
  }

  @Bean
  public SubjectSourcesClient subjectSourcesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(SubjectSourcesClient.class);
  }

  @Bean
  public SubjectTypesClient subjectTypesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(SubjectTypesClient.class);
  }

  @Bean
  public UserClient userClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UserClient.class);
  }
}
