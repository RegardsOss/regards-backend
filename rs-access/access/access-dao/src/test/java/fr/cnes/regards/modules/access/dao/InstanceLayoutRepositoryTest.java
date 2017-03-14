/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.modules.access.dao.instance.IInstanceLayoutRepository;
import fr.cnes.regards.modules.access.domain.instance.InstanceLayout;

/**
 *
 * Class InstanceLayoutRepositoryTest
 *
 * Test DAO
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AccessDaoTestConfiguration.class })
@InstanceTransactional
public class InstanceLayoutRepositoryTest {

    @Autowired
    private IInstanceLayoutRepository repository;

    @Test
    public void saveLayoutTest() {
        // Create a new layout configuration
        final InstanceLayout layout = new InstanceLayout();
        layout.setApplicationId("TEST");
        layout.setLayout("{}");
        final InstanceLayout newLayout = repository.save(layout);
        final InstanceLayout layout2 = repository.findOne(newLayout.getId());
        Assert.assertEquals(newLayout.getLayout(), layout2.getLayout());
    }

    @Test
    public void updateLayoutTest() {
        // Create a new layout configuration
        final InstanceLayout layout = new InstanceLayout();
        layout.setApplicationId("TEST");
        layout.setLayout("{}");
        final InstanceLayout newLayout = repository.save(layout);
        newLayout.setLayout("{\"test\":\"test\"}");
        final InstanceLayout layout2 = repository.save(newLayout);
        Assert.assertEquals("{\"test\":\"test\"}", layout2.getLayout());
    }

}
