/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.entities.service.visitor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.entities.domain.attribute.DateAttribute;
import fr.cnes.regards.modules.entities.domain.attribute.builder.AttributeBuilder;
import fr.cnes.regards.modules.entities.plugin.MaxDateAttribute;
import fr.cnes.regards.modules.entities.plugin.MinDateAttribute;
import fr.cnes.regards.modules.entities.service.ServiceConfiguration;
import fr.cnes.regards.modules.entities.urn.OAISIdentifier;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.models.domain.IComputedAttribute;
import fr.cnes.regards.modules.models.domain.Model;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;
import fr.cnes.regards.modules.models.service.IAttributeModelService;
import fr.cnes.regards.modules.models.service.IModelService;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ServiceConfiguration.class })
@MultitenantTransactional
public class DateAttributeTest {

    private static final String dataModelFileName = "dataModelDate.xml";

    private static final String datasetModelFileName = "datasetModelDate.xml";

    private static final String minDateAttName = "minDate";

    private static final String maxDateAttName = "maxDate";

    private static final Logger LOG = LoggerFactory.getLogger(DateAttributeTest.class);

    @Autowired
    private IModelService modelService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private IAttributeModelService attModelService;

    private Model dataModel;

    private MinDateAttribute minDatePlugin;

    private MaxDateAttribute maxDatePlugin;

    private AttributeModel minAttributeToCompute;

    private AttributeModel maxAttributeToCompute;

    @Before
    public void init() throws ModuleException, NoSuchMethodException, SecurityException {
        jwtService.injectMockToken("PROJECT", "ADMIN");
        pluginService.addPluginPackage(IComputedAttribute.class.getPackage().getName());
        pluginService.addPluginPackage(MaxDateAttribute.class.getPackage().getName());
        // create a pluginConfiguration with a label for min
        List<PluginParameter> parametersMin = PluginParametersFactory.build()
                .addParameter("attributeToComputeName", "minDate").getParameters();
        PluginMetaData metadataMin = new PluginMetaData();
        metadataMin.setPluginId("MinDateAttribute");
        metadataMin.setAuthor("toto");
        metadataMin.setDescription("titi");
        metadataMin.setVersion("tutu");
        metadataMin.setInterfaceName(IComputedAttribute.class.getName());
        metadataMin.setPluginClassName(MinDateAttribute.class.getName());
        PluginConfiguration confMin = new PluginConfiguration(metadataMin, "MinDateTestConf");
        confMin.setParameters(parametersMin);
        confMin = pluginService.savePluginConfiguration(confMin);
        // create a pluginConfiguration with a label
        List<PluginParameter> parametersMax = PluginParametersFactory.build()
                .addParameter("attributeToComputeName", "maxDate").getParameters();
        PluginMetaData metadataMax = new PluginMetaData();
        metadataMax.setPluginId("MaxDateAttribute");
        metadataMax.setAuthor("toto");
        metadataMax.setDescription("titi");
        metadataMax.setVersion("tutu");
        metadataMax.setInterfaceName(IComputedAttribute.class.getName());
        metadataMax.setPluginClassName(MaxDateAttribute.class.getName());
        PluginConfiguration confMax = new PluginConfiguration(metadataMax, "MaxDateTestConf");
        confMax.setParameters(parametersMax);
        confMax = pluginService.savePluginConfiguration(confMax);
        // get a model for Dataset
        importModel(datasetModelFileName);
        // get a model for DataObject
        importModel(dataModelFileName);
        dataModel = modelService.getModelByName("dataModel");
        // instanciate the plugin
        minDatePlugin = pluginService.getPlugin(confMin.getId());
        maxDatePlugin = pluginService.getPlugin(confMax.getId());
        minAttributeToCompute = attModelService.findByNameAndFragmentName("minDate", null);
        maxAttributeToCompute = attModelService.findByNameAndFragmentName("maxDate", null);
    }

    @Test
    @Ignore
    public void testDate() {
        // create the objects
        DataObject obj1 = new DataObject();
        obj1.setModel(dataModel);
        obj1.setIpId(new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, "tenant", UUID.randomUUID(), 1));
        obj1.setLabel("data for test");
        DataObject obj2 = new DataObject();
        obj2.setModel(dataModel);
        obj2.setIpId(new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, "tenant", UUID.randomUUID(), 1));
        obj2.setLabel("data for test");
        DataObject obj3 = new DataObject();
        obj3.setModel(dataModel);
        obj3.setIpId(new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, "tenant", UUID.randomUUID(), 1));
        obj3.setLabel("data for test");
        DataObject obj4 = new DataObject();
        obj4.setModel(dataModel);
        obj4.setIpId(new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, "tenant", UUID.randomUUID(), 1));
        obj4.setLabel("data for test");
        LocalDateTime expectedMinDate = LocalDateTime.now().minusHours(200);
        LocalDateTime expectedMaxDate = LocalDateTime.now().plusHours(200);
        // add the min date attribute
        obj1.getProperties().add(AttributeBuilder.forType(AttributeType.DATE_ISO8601, minDateAttName,
                                                          LocalDateTime.now().minusHours(20)));
        obj2.getProperties().add(AttributeBuilder.forType(AttributeType.DATE_ISO8601, minDateAttName,
                                                          LocalDateTime.now().minusHours(2)));
        obj3.getProperties().add(AttributeBuilder.forType(AttributeType.DATE_ISO8601, minDateAttName, expectedMinDate));
        // add the max date attribute
        obj1.getProperties().add(AttributeBuilder.forType(AttributeType.DATE_ISO8601, maxDateAttName,
                                                          LocalDateTime.now().plusHours(20)));
        obj2.getProperties().add(AttributeBuilder.forType(AttributeType.DATE_ISO8601, maxDateAttName, expectedMaxDate));
        obj3.getProperties().add(AttributeBuilder.forType(AttributeType.DATE_ISO8601, maxDateAttName,
                                                          LocalDateTime.now().plusHours(2)));
        List<DataObject> firstObjs = Lists.newArrayList(obj1, obj2, obj4);
        List<DataObject> secondObjs = Lists.newArrayList(obj3);
        // minDatePlugin.compute(firstObjs);
        // maxDatePlugin.compute(firstObjs);
        // minDatePlugin.compute(secondObjs);
        // maxDatePlugin.compute(secondObjs);
        LocalDateTime minDate = minDatePlugin.getResult();
        LocalDateTime maxDate = maxDatePlugin.getResult();
        Assert.assertEquals(expectedMinDate, minDate);
        Assert.assertEquals(expectedMaxDate, maxDate);
        AttributeBuilderVisitor visitor = new AttributeBuilderVisitor();
        AbstractAttribute<?> minAttribute = minDatePlugin.accept(visitor);
        AbstractAttribute<?> maxAttribute = maxDatePlugin.accept(visitor);
        Assert.assertTrue(minAttribute instanceof DateAttribute);
        Assert.assertEquals(minAttributeToCompute.getName(), minAttribute.getName());
        Assert.assertEquals(minDate, minAttribute.getValue());
        Assert.assertTrue(maxAttribute instanceof DateAttribute);
        Assert.assertEquals(maxAttributeToCompute.getName(), maxAttribute.getName());
        Assert.assertEquals(maxDate, maxAttribute.getValue());
    }

    /**
     * Import model definition file from resources directory
     *
     * @param pFilename filename
     * @throws ModuleException if error occurs
     */
    private void importModel(String pFilename) throws ModuleException {
        try {
            final InputStream input = Files.newInputStream(Paths.get("src", "test", "resources", pFilename));
            modelService.importModel(input);
        } catch (IOException e) {
            String errorMessage = "Cannot import " + pFilename;
            throw new AssertionError(errorMessage);
        }
    }

}
