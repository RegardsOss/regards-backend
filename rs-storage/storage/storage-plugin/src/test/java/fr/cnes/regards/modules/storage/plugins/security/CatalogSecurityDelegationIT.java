package fr.cnes.regards.modules.storage.plugins.security;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.transaction.BeforeTransaction;

import com.google.gson.Gson;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.models.domain.Model;
import fr.cnes.regards.modules.search.client.ISearchClient;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.plugin.security.CatalogSecurityDelegation;
import fr.cnes.regards.modules.storage.plugin.security.ISecurityDelegation;
import fr.cnes.regards.modules.storage.plugins.datastorage.local.MockingResourceServiceConfiguration;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@ContextConfiguration(classes = { MockingResourceServiceConfiguration.class, MockedFeignClientConf.class })
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class CatalogSecurityDelegationIT extends AbstractRegardsServiceIT {

    private static final String CATALOG_SECURITY_DELEGATION_LABEL = "CatalogSecurityDelegationIT";

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IPluginService pluginService;

    private PluginConfiguration catalogSecuDelegConf;

    @Autowired
    private ISearchClient searchClient;

    @Autowired
    private IProjectUsersClient projectUsersClient;

    @Autowired
    private IAIPDao aipDao;

    @Autowired
    private Gson gson;

    @Autowired
    private IAuthenticationResolver authenticationResolver;

    @BeforeTransaction
    public void setTenant() {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
    }

    @Before
    public void init() throws ModuleException {
        pluginService.addPluginPackage(ISecurityDelegation.class.getPackage().getName());
        pluginService.addPluginPackage(CatalogSecurityDelegation.class.getPackage().getName());
        PluginMetaData catalogSecuDelegMeta = PluginUtils.createPluginMetaData(CatalogSecurityDelegation.class,
                                                                               CatalogSecurityDelegation.class
                                                                                       .getPackage().getName(),
                                                                               ISecurityDelegation.class.getPackage()
                                                                                       .getName());
        catalogSecuDelegConf = new PluginConfiguration(catalogSecuDelegMeta, CATALOG_SECURITY_DELEGATION_LABEL);
        catalogSecuDelegConf = pluginService.savePluginConfiguration(catalogSecuDelegConf);
    }

    @Test
    public void testHasAccess() throws ModuleException, IOException {
        ISecurityDelegation toTest = pluginService.getPlugin(catalogSecuDelegConf.getId());
        // test while copnsidered admin
        // lets test with an ip id we have right in catalog
        String catalogOk = new UniformResourceName(OAISIdentifier.AIP, EntityType.COLLECTION, DEFAULT_TENANT,
                                                   UUID.randomUUID(), 1).toString();
        Mockito.when(searchClient.getEntity(UniformResourceName.fromString(catalogOk))).thenReturn(
                new ResponseEntity<>(new Resource<>(
                        new Collection(Model.build("name", "desc", EntityType.COLLECTION), DEFAULT_TENANT,
                                       "CatalogOK")), HttpStatus.OK));
        Assert.assertTrue("Catalog should have authorized the access to this aip", toTest.hasAccess(catalogOk));
        //lets test with an unknown ip id in catalog but known into storage
        String catalogUnknown = new UniformResourceName(OAISIdentifier.AIP, EntityType.COLLECTION, DEFAULT_TENANT,
                                                        UUID.randomUUID(), 1).toString();
        AIP aip = getAipFromFile();
        aip.setId(UniformResourceName.fromString(catalogUnknown));
        aip.addEvent(EventType.SUBMISSION.name(), "lets bypass everything");
        aipDao.save(aip);
        Mockito.when(searchClient.getEntity(UniformResourceName.fromString(catalogUnknown)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        Mockito.when(projectUsersClient.isAdmin(authenticationResolver.getUser()))
                .thenReturn(new ResponseEntity<>(Boolean.TRUE, HttpStatus.OK));
        Assert.assertTrue("Plugin should have authorize the access to this aip because we are considered admin",
                          toTest.hasAccess(catalogUnknown));
        //lets test with an unknown ip id in storage and catalog
        String unknown = new UniformResourceName(OAISIdentifier.AIP, EntityType.COLLECTION, DEFAULT_TENANT,
                                                 UUID.randomUUID(), 1).toString();
        Mockito.when(searchClient.getEntity(UniformResourceName.fromString(unknown)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        try {
            toTest.hasAccess(unknown);
            Assert.fail("Plugin should have thrown an EntityNotFoundException");
        } catch (EntityNotFoundException e ) {
            //nothing but it means there was an exception so everything is good!
        }

        // test while not considered admin
        // lets test with an ip id we have right in catalog
        Assert.assertTrue("Catalog should have authorized the access to this aip", toTest.hasAccess(catalogOk));
        // lets test with an ip id we don't have right in catalog
        String catalogNok = new UniformResourceName(OAISIdentifier.AIP, EntityType.COLLECTION, DEFAULT_TENANT,
                                                    UUID.randomUUID(), 1).toString();
        Mockito.when(searchClient.getEntity(UniformResourceName.fromString(catalogNok)))
                .thenReturn(new ResponseEntity<>(HttpStatus.FORBIDDEN));
        Assert.assertFalse("Catalog should not have authorized the access to this aip", toTest.hasAccess(catalogNok));
        //lets test with an unknown ip id in catalog but known into storage
        Mockito.when(projectUsersClient.isAdmin(authenticationResolver.getUser()))
                .thenReturn(new ResponseEntity<>(Boolean.FALSE, HttpStatus.OK));
        Assert.assertFalse("Plugin should not have authorize the access to this aip because we are not considered admin", toTest.hasAccess(catalogUnknown));
        //lets test with an unknown ip id in storage and catalog
        try {
            toTest.hasAccess(unknown);
            Assert.fail("Plugin should have thrown an EntityNotFoundException");
        } catch (EntityNotFoundException e ) {
            //nothing but it means there was an exception so everything is good!
        }
    }

    private AIP getAipFromFile() throws IOException {
        FileReader fr = new FileReader("src/test/resources/aip_sample.json");
        BufferedReader br = new BufferedReader(fr);
        String fileLine = br.readLine();
        AIP aip = gson.fromJson(fileLine, AIP.class);
        br.close();
        fr.close();
        return aip;
    }

}
