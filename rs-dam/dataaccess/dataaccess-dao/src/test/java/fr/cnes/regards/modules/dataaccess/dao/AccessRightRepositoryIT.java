/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.dataaccess.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;
import fr.cnes.regards.modules.dataaccess.domain.accessright.AccessLevel;
import fr.cnes.regards.modules.dataaccess.domain.accessright.AccessRight;
import fr.cnes.regards.modules.dataaccess.domain.accessright.QualityFilter;
import fr.cnes.regards.modules.dataaccess.domain.accessright.QualityLevel;
import fr.cnes.regards.modules.entities.dao.IDatasetRepository;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.models.dao.IModelRepository;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.models.domain.Model;

/**
 *
 * @author Sylvain Vissiere-Guerinet
 */
@TestPropertySource("classpath:test.properties")
public class AccessRightRepositoryIT extends AbstractDaoTransactionalTest {

    @Autowired
    private IAccessRightRepository repo;

    @Autowired
    private IModelRepository modelRepo;

    @Autowired
    private IDatasetRepository dsRepo;

    @Autowired
    private IAccessGroupRepository agRepo;

    private AccessGroup user1;

    private final String user1Email = "user1@user1.user1";

    private AccessGroup ag1;

    private AccessGroup ag2;

    private final String ag1Name = "AG1";

    private final String ag2Name = "AG2";

    private QualityFilter qf;

    private final AccessLevel al = AccessLevel.FULL_ACCESS;

    private Dataset ds1;

    private Dataset ds2;

    private AccessRight ar1;

    private AccessRight ar2;

    @Before
    public void init() {
        qf = new QualityFilter(10, 0, QualityLevel.ACCEPTED);
        Model model = Model.build("model1", "desc", EntityType.DATASET);
        model = modelRepo.save(model);
        ds1 = new Dataset(model, "PROJECT", "ds1");
        ds1.setLabel("label");
        ds1.setLicence("licence");
        ds1 = dsRepo.save(ds1);
        ds2 = new Dataset(model, "PROJECT", "ds2");
        ds2.setLabel("label");
        ds2.setLicence("licence");
        ds2 = dsRepo.save(ds2);

        ag1 = new AccessGroup(ag1Name);
        ag1 = agRepo.save(ag1);
        ar1 = new AccessRight(qf, al, ds1, ag1);
        ar1 = repo.save(ar1);
        ag2 = new AccessGroup(ag2Name);
        ag2 = agRepo.save(ag2);
        ar2 = new AccessRight(qf, al, ds2, ag2);
        ar2 = repo.save(ar2);
    }

    @Test
    public void testfindAllByAccessGroupName() {
        Page<AccessRight> response = repo.findAllByAccessGroup(ag1, new PageRequest(0, 10));
        Assert.assertTrue(response.getContent().contains(ar1));
        Assert.assertFalse(response.getContent().contains(ar2));
    }

    @Test
    public void testfindAllByDataset() {
        Page<AccessRight> response = repo.findAllByDataset(ds1, new PageRequest(0, 10));
        Assert.assertTrue(response.getContent().contains(ar1));
        Assert.assertFalse(response.getContent().contains(ar2));
    }

    @Test
    public void testfindAllByAccessGroupNameByDataset() {
        Page<AccessRight> response = repo.findAllByAccessGroupAndDataset(ag1, ds1, new PageRequest(0, 10));
        Assert.assertTrue(response.getContent().contains(ar1));
        Assert.assertFalse(response.getContent().contains(ar2));

        response = repo.findAllByAccessGroupAndDataset(ag1, ds2, new PageRequest(0, 10));
        Assert.assertFalse(response.hasContent());
    }

}
