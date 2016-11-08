/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 *
 * Test class for {@link ProjectUser} DAO module
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AccessRightsDaoTestConfiguration.class })
@DirtiesContext
public class ProjectUserDaoTest {

    /**
     * JPA Repository
     */
    @Autowired
    private IProjectUserRepository repository;

    /**
     * Security service to generate tokens.
     */
    @Autowired
    private JWTService jwtService;

    /**
     * @throws JwtException
     *             if the token is wrong
     */
    @Before
    public void setUp() throws JwtException {
        jwtService.injectToken("test1", "USER");
    }

    /**
     * Check that the system updates automaticly the field lastUpdate before any db persistence.
     */
    @Test
    @Purpose("Check that the system updates automaticly the field lastUpdate before any db persistence.")
    public final void setLastUpdateListener() {
        final ProjectUser user = new ProjectUser("email@test.com", null, new ArrayList<>(), new ArrayList<>());

        // Init with a past date (2 days ago)
        final LocalDateTime initial = LocalDateTime.now().minusDays(2);
        user.setLastUpdate(initial);
        Assert.assertEquals(user.getLastUpdate(), initial);

        // Call tested method
        final ProjectUser saved = repository.save(user);

        // Check the value was updated
        Assert.assertNotEquals(saved.getLastUpdate(), initial);
        Assert.assertTrue(saved.getLastUpdate().isAfter(initial));
    }

}
