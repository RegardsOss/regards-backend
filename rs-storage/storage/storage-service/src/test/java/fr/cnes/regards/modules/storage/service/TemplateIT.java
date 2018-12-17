/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.storage.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IAIPSessionRepository;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.WorkingSubsetWrapper;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.DefaultAllocationStrategyPlugin;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalWorkingSubset;
import fr.cnes.regards.modules.templates.dao.ITemplateRepository;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import fr.cnes.regards.modules.templates.service.TemplateServiceConfiguration;

/**
 * Set of visual tests on the result, allows to be sure that the default template is interpreted with no major issues
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@ContextConfiguration(classes = { TestConfig.class, TemplateIT.Config.class })
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=storage_test", "regards.amqp.enabled=true" },
        locations = { "classpath:storage.properties" })
@RegardsTransactional
@ActiveProfiles({ "testAmqp", "disableStorageTasks", "noschdule" })
@DirtiesContext(hierarchyMode = HierarchyMode.EXHAUSTIVE, classMode = ClassMode.BEFORE_CLASS)
public class TemplateIT extends AbstractRegardsServiceTransactionalIT {

    private static final String DATA_STORAGE_CONF_LABEL = "DataStorage_TemplateIT";

    private static final String ALLO_CONF_LABEL = "Allo_TemplateIT";

    private static final String SESSION = "Session 1";

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateIT.class);

    @Autowired
    private ITemplateService templateService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ITemplateRepository templateRepository;

    @Autowired
    private IAIPDao aipDao;

    @Autowired
    private IAIPSessionRepository aipSessionRepo;

    @Autowired
    private IDataFileDao dataFileDao;

    @Test
    public void testNotSubsetted() throws ModuleException, MalformedURLException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        Map<String, Object> dataMap = new HashMap<>();
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class);
        URL baseStorageLocation = new URL("file", "", Paths.get("target/TemplateIT/Local2").toFile().getAbsolutePath());
        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000000L)
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, baseStorageLocation.toString())
                .addParameter(LocalDataStorage.LOCAL_STORAGE_DELETE_OPTION, false).getParameters();
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, DATA_STORAGE_CONF_LABEL, parameters,
                0);
        dataStorageConf.setIsActive(true);
        PrioritizedDataStorage prioritizedDataStorage = prioritizedDataStorageService.create(dataStorageConf);

        // lets simulate as in the code, so lets create a workingSubsetWrapper full of rejected data files
        WorkingSubsetWrapper<LocalWorkingSubset> workingSubsetWrapper = new WorkingSubsetWrapper<>();

        aipSessionRepo.deleteAll();
        AIPSession aipSession = new AIPSession();
        aipSession.setId(SESSION);
        aipSession.setLastActivationDate(OffsetDateTime.now());
        aipSession = aipSessionRepo.save(aipSession);

        AIP aip = aipDao.save(getAIP(), aipSession);
        Set<StorageDataFile> dataFiles = StorageDataFile.extractDataFiles(aip, aipSession);
        dataFiles = Sets.newHashSet(dataFileDao.save(dataFiles));
        Iterator<StorageDataFile> dataFilesIter = dataFiles.iterator();
        int i = 0;
        while (dataFilesIter.hasNext()) {
            i++;
            workingSubsetWrapper.addRejectedDataFile(dataFilesIter.next(), "Test" + i);
        }
        dataMap.put("dataFilesMap", workingSubsetWrapper.getRejectedDataFiles());
        dataMap.put("dataStorage", pluginService.getPluginConfiguration(prioritizedDataStorage.getId()));
        // lets use the template service to get our message
        SimpleMailMessage email = templateService
                .writeToEmail(TemplateServiceConfiguration.NOT_SUBSETTED_DATA_FILES_CODE, dataMap);
        Assert.assertNotEquals(templateRepository.findByCode(TemplateServiceConfiguration.NOT_SUBSETTED_DATA_FILES_CODE)
                .get().getContent(), email.getText());
        LOGGER.info(email.getText());
    }

    @Test
    public void testNotDispatched() throws ModuleException, MalformedURLException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Map<String, Object> dataMap = new HashMap<>();
        PluginMetaData AlloMeta = PluginUtils.createPluginMetaData(DefaultAllocationStrategyPlugin.class);
        PluginConfiguration alloConf = new PluginConfiguration(AlloMeta, ALLO_CONF_LABEL, new ArrayList<>(), 0);
        alloConf.setIsActive(true);
        alloConf = pluginService.savePluginConfiguration(alloConf);

        aipSessionRepo.deleteAll();
        AIPSession aipSession = new AIPSession();
        aipSession.setId(SESSION);
        aipSession.setLastActivationDate(OffsetDateTime.now());
        aipSession = aipSessionRepo.save(aipSession);

        AIP aip = aipDao.save(getAIP(), aipSession);
        Set<StorageDataFile> dataFiles = StorageDataFile.extractDataFiles(aip, aipSession);
        dataFiles = Sets.newHashSet(dataFileDao.save(dataFiles));
        dataMap.put("dataFiles", dataFiles);
        dataMap.put("allocationStrategy", alloConf);
        // lets use the template service to get our message
        SimpleMailMessage email = templateService
                .writeToEmail(TemplateServiceConfiguration.NOT_DISPATCHED_DATA_FILES_CODE, dataMap);
        Assert.assertNotEquals(templateRepository
                .findByCode(TemplateServiceConfiguration.NOT_DISPATCHED_DATA_FILES_CODE).get().getContent(),
                               email.getText());
        LOGGER.info(email.getText());
    }

    private AIP getAIP() throws MalformedURLException {

        UniformResourceName sipId = new UniformResourceName(OAISIdentifier.SIP, EntityType.DATA, getDefaultTenant(),
                UUID.randomUUID(), 1);
        UniformResourceName aipId = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, getDefaultTenant(),
                sipId.getEntityId(), 1);
        AIPBuilder aipBuilder = new AIPBuilder(aipId, Optional.of(sipId), "providerId", EntityType.DATA, SESSION);

        Path path = Paths.get("src", "test", "resources", "data.txt");
        aipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, path, "MD5",
                                                                "de89a907d33a9716d11765582102b2e0");
        aipBuilder.getContentInformationBuilder().setSyntax("text", "description", MimeType.valueOf("text/plain"));
        aipBuilder.addContentInformation();
        aipBuilder.getPDIBuilder().setAccessRightInformation("public");
        aipBuilder.getPDIBuilder().setFacility("CS");
        aipBuilder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event",
                                                                 OffsetDateTime.now());
        aipBuilder.addTags("tag");

        return aipBuilder.build();
    }

    @Configuration
    static class Config {

        @Bean
        public INotificationClient notificationClient() {
            return Mockito.mock(INotificationClient.class);
        }
    }
}
