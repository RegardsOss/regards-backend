/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.microservices.core.dao.MultiTenancyDaoTest;
import fr.cnes.regards.microservices.core.dao.pojo.projects.User;
import fr.cnes.regards.microservices.core.dao.repository.projects.UserRepository;
import fr.cnes.regards.microservices.core.dao.util.CurrentTenantIdentifierResolverMock;

/**
 *
 * Test service for transactionnal DAO actions
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Service
public class TransactionServiceTest {

    static final Logger LOG = LoggerFactory.getLogger(MultiTenancyDaoTest.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrentTenantIdentifierResolverMock tenantResolver;

    @Transactional(transactionManager = "projectsJpaTransactionManager", rollbackFor = Exception.class)
    public void addWithError(String pTenant) throws Exception {

        tenantResolver.setTenant(pTenant);

        User plop = userRepository.save(new User("doNotSave", "thisUser"));
        LOG.info("new user created id=" + plop.getId());
        plop = userRepository.save(new User("doNotSave", "thisUser"));
        LOG.info("new user created id=" + plop.getId());
        plop = userRepository.save(new User("doNotSave", "thisUser"));
        LOG.info("new user created id=" + plop.getId());

        throw new Exception("plop");

    }

    public void addWithoutError(String pTenant) {

        tenantResolver.setTenant(pTenant);

        User plop = userRepository.save(new User("doNotSave", "thisUser"));

        LOG.info("new user created id=" + plop.getId());

    }

    public List<User> getUsers(String pTenant) {
        Iterable<User> list = userRepository.findAll();
        List<User> results = new ArrayList<>();
        list.forEach(user -> results.add(user));
        return results;
    }

    public void deleteAll(String pTenant) {
        tenantResolver.setTenant(pTenant);
        userRepository.deleteAll();
    }

}
