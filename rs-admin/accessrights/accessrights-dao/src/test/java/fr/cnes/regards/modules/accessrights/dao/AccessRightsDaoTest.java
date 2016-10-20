/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;

/**
 *
 * Class AccessRightsDaoTest
 *
 * Test class for accessrights DAO module
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AccessRightsDaoTestConfiguration.class })
@DirtiesContext
public class AccessRightsDaoTest {

    /**
     * JPA Repository
     */
    @Autowired
    private IProjectUserRepository projectUserRepo;

    /**
     * Security service to generate tokens.
     */
    @Autowired
    private JWTService jwtService;

    /**
     *
     * Test method
     * 
     * @throws JwtException
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void test() throws JwtException {

        jwtService.injectToken("test1", "USER");

        projectUserRepo.findAll();

    }

}
