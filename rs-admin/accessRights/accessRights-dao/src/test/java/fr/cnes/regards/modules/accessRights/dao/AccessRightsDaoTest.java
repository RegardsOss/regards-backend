/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.framework.security.utils.jwt.exception.MissingClaimException;
import fr.cnes.regards.modules.accessRights.dao.projects.IProjectUserRepository;

/**
 *
 * Class AccessRightsDaoTest
 *
 * Test class for accessRights DAO module
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
     * @throws MissingClaimException
     * @throws InvalidJwtException
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void test() throws InvalidJwtException, MissingClaimException {

        jwtService.injectToken("test1", "USER");

        projectUserRepo.findAll();

    }

}
