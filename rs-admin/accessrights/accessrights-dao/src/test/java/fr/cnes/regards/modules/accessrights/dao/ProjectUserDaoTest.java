/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleFactory;

/**
 *
 * Test class for {@link ProjectUser} DAO module
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AccessRightsDaoTestConfiguration.class })
@MultitenantTransactional
public class ProjectUserDaoTest {

    /**
     * JPA Repository
     */
    @Autowired
    private IProjectUserRepository projectUserRepository;

    /**
     * Runtime tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Role projectUserRepository.
     */
    @Autowired
    private IRoleRepository roleRepository;

    @BeforeTransaction
    public void beforeTransaction() {
        runtimeTenantResolver.forceTenant("test1");
    }

    /**
     * Check that the system updates automatically the field lastUpdate before any db persistence.
     */
    @Test
    @Purpose("Check that the system updates automatically the field lastUpdate before any db persistence.")
    public final void setLastUpdateListener() {

        final RoleFactory factory = new RoleFactory();
        final Role role = roleRepository.save(factory.createPublic());
        final ProjectUser user = new ProjectUser("email@test.com", role, new ArrayList<>(), new ArrayList<>());

        // Init with a past date (2 days ago)
        final OffsetDateTime initial = OffsetDateTime.now().minusDays(2);
        user.setLastUpdate(initial);
        Assert.assertEquals(user.getLastUpdate(), initial);

        // Call tested method
        final ProjectUser saved = projectUserRepository.save(user);

        // Check the value was updated
        Assert.assertNotEquals(saved.getLastUpdate(), initial);
        Assert.assertTrue(saved.getLastUpdate().isAfter(initial));
    }

}
