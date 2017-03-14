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
import org.springframework.test.context.transaction.BeforeTransaction;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.access.dao.project.ILayoutRepository;
import fr.cnes.regards.modules.access.domain.project.Layout;

/**
 *
 * Class LayoutRepositoryTest
 *
 * DAO Test
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AccessDaoTestConfiguration.class })
@MultitenantTransactional
public class LayoutRepositoryTest {

    @Autowired
    private ILayoutRepository repository;

    /**
     * Runtime tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @BeforeTransaction
    public void beforeTransaction() {
        runtimeTenantResolver.forceTenant("test1");
    }

    @Test
    public void saveLayoutTest() {
        // Create a new layout configuration
        final Layout layout = new Layout();
        layout.setApplicationId("TEST");
        layout.setLayout("{}");
        final Layout newLayout = repository.save(layout);
        final Layout layout2 = repository.findOne(newLayout.getId());
        Assert.assertEquals(newLayout.getLayout(), layout2.getLayout());
    }

    @Test
    public void updateLayoutTest() {
        // Create a new layout configuration
        final Layout layout = new Layout();
        layout.setApplicationId("TEST");
        layout.setLayout("{}");
        final Layout newLayout = repository.save(layout);
        newLayout.setLayout("{\"test\":\"test\"}");
        final Layout layout2 = repository.save(newLayout);
        Assert.assertEquals("{\"test\":\"test\"}", layout2.getLayout());
    }

}
