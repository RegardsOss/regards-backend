/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.entities.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.Gson;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.modules.crawler.domain.criterion.BooleanMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.datasources.dao.IDataSourceRepository;
import fr.cnes.regards.modules.datasources.domain.DataSource;
import fr.cnes.regards.modules.entities.domain.DataSet;
import fr.cnes.regards.modules.models.dao.IAttributeModelRepository;
import fr.cnes.regards.modules.models.dao.IModelAttributeRepository;
import fr.cnes.regards.modules.models.dao.IModelRepository;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.models.domain.Model;
import fr.cnes.regards.modules.models.domain.ModelAttribute;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.Fragment;
import fr.cnes.regards.modules.models.service.exception.ImportException;
import fr.cnes.regards.modules.models.service.xml.XmlImportHelper;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@TestPropertySource("classpath:application-test.properties")
public class DataSetRepositoryIT extends AbstractDaoTransactionalTest {

    private static final Logger LOG = LoggerFactory.getLogger(DataSetRepositoryIT.class);

    @Autowired
    private IModelRepository modelRepo;

    @Autowired
    private IDataSourceRepository dataSourceRepo;

    @Autowired
    private IDataSetRepository dataSetRepo;

    @Autowired
    private IModelAttributeRepository modelAttRepo;

    @Autowired
    private IAttributeModelRepository attModelRepo;

    private DataSet dataset;

    private Model srcModel;

    private AttributeModel attString;

    private AttributeModel attBoolean;

    private AttributeModel GEO_CRS;

    private AttributeModel GEO_GEOMETRY;

    private AttributeModel Contact_Phone;

    @Autowired
    private Gson gson;

    @Before
    public void init() {
        Model pModel = Model.build("datasetModel", "pDescription", EntityType.DATASET);
        pModel = modelRepo.save(pModel);
        dataset = new DataSet(pModel, "pTenant", "dataset");

        List<Long> confs = new ArrayList<>(2);
        confs.add(1L);
        confs.add(2L);
        dataset.setPluginConfigurationIds(confs);

        setModelInPlace(importModel("sample-model-minimal.xml"));
        dataset.setSubsettingClause(getValidClause());

        DataSource dataSource = new DataSource(srcModel);
        dataSource = dataSourceRepo.save(dataSource);
        dataset.setDataSource(dataSource);

        String stringCLause = gson.toJson(dataset.getSubsettingClause());

        ICriterion criterion = gson.fromJson(stringCLause, ICriterion.class);
        dataset = dataSetRepo.save(dataset);
    }

    @Test
    public void testFindOneWithPluginConfigurations() {
        DataSet result = dataSetRepo.findOneWithPluginConfigurations(dataset.getId());
        Assert.assertTrue(result.getPluginConfigurationIds() != null);
        Assert.assertTrue(result.getPluginConfigurationIds().size() == 2);
        Assert.assertTrue(result.getPluginConfigurationIds().contains(1L));
        Assert.assertTrue(result.getPluginConfigurationIds().contains(2L));
    }

    /**
     * @param pImportModel
     */
    private void setModelInPlace(List<ModelAttribute> pImportModel) {
        srcModel = pImportModel.get(0).getModel();
        srcModel = modelRepo.save(srcModel);
        ModelAttribute attStringModelAtt = pImportModel.stream()
                .filter(ma -> ma.getAttribute().getFragment().getName().equals(Fragment.getDefaultName())
                        && ma.getAttribute().getName().equals("att_string"))
                .findAny().get();
        attString = attStringModelAtt.getAttribute();
        ModelAttribute attBooleanModelAtt = pImportModel.stream()
                .filter(ma -> ma.getAttribute().getFragment().getName().equals(Fragment.getDefaultName())
                        && ma.getAttribute().getName().equals("att_boolean"))
                .findAny().get();
        attBoolean = attBooleanModelAtt.getAttribute();
        ModelAttribute CRS_CRSModelAtt = pImportModel.stream()
                .filter(ma -> ma.getAttribute().getFragment().getName().equals("GEO")
                        && ma.getAttribute().getName().equals("CRS"))
                .findAny().get();
        GEO_CRS = CRS_CRSModelAtt.getAttribute();

        ModelAttribute CRS_GEOMETRYModelAtt = pImportModel.stream()
                .filter(ma -> ma.getAttribute().getFragment().getName().equals("GEO")
                        && ma.getAttribute().getName().equals("GEOMETRY"))
                .findAny().get();
        GEO_GEOMETRY = CRS_GEOMETRYModelAtt.getAttribute();
        ModelAttribute Contact_PhoneModelAtt = pImportModel.stream()
                .filter(ma -> ma.getAttribute().getFragment().getName().equals("Contact")
                        && ma.getAttribute().getName().equals("Phone"))
                .findAny().get();
        Contact_Phone = Contact_PhoneModelAtt.getAttribute();
    }

    /**
     * @return
     */
    private ICriterion getValidClause() {
        // textAtt contains "testContains"
        ICriterion containsCrit = ICriterion.contains("attributes." + attString.getName(), "testContains");
        // textAtt ends with "testEndsWith"
        ICriterion endsWithCrit = ICriterion
                .endsWith("attributes." + GEO_CRS.getFragment().getName() + "." + GEO_CRS.getName(), "testEndsWith");
        // textAtt strictly equals "testEquals"
        ICriterion equalsCrit = ICriterion
                .equals("attributes." + Contact_Phone.getFragment().getName() + "." + Contact_Phone.getName(),
                        "testEquals");

        ICriterion booleanCrit = new BooleanMatchCriterion("attributes." + attBoolean.getName(), true);

        // All theses criterions (AND)
        ICriterion rootCrit = ICriterion.and(containsCrit, endsWithCrit, equalsCrit, booleanCrit);
        return rootCrit;
    }

    private List<ModelAttribute> importModel(String pFilename) {
        InputStream input;
        try {
            input = Files.newInputStream(Paths.get("src", "test", "resources", pFilename));

            return XmlImportHelper.importModel(input);
        } catch (IOException | ImportException e) {
            LOG.debug("import of model failed", e);
            Assert.fail();
        }
        // never reached because test will fail first
        return null;
    }
}
