/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.dao;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.modules.configuration.domain.Layout;

/**
 *
 * Class LayoutRepositoryTest
 *
 * DAO Test
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class LayoutRepositoryTest extends AbstractDaoTransactionalTest {

    @Autowired
    private ILayoutRepository repository;

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
