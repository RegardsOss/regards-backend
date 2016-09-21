/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import fr.cnes.regards.microservices.core.dao.hibernate.DataSourceBasedMultiTenantConnectionProviderImpl;
import fr.cnes.regards.microservices.core.dao.pojo.User;
import fr.cnes.regards.microservices.core.dao.repository.UserRepository;
import fr.cnes.regards.microservices.core.dao.util.CurrentTenantIdentifierResolverMock;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { MultiTenancyDaoTestConfiguration.class })
public class MultiTenancyDaoTest {

    static final Logger LOG = LoggerFactory.getLogger(MultiTenancyDaoTest.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrentTenantIdentifierResolverMock tenantResolver;

    @Autowired
    private DataSourceBasedMultiTenantConnectionProviderImpl connectionProvider;

    @Autowired
    private DataSource dataSource2;

    @Test
    public void contextLoads() {
        // Nothing to do. Only tests if the spring context is ok.
    }

    // TODO : Auto create schema for the additional datasource ?
    @Test
    public void multitenancyAccessTest() {

        connectionProvider.addDataSource(dataSource2, "test2", "org.hibernate.dialect.HSQLDialect");
        // connectionProvider.addDataSource("jdbc:postgresql://localhost:5432/test1", "postgres", "postgres", "test2",
        // "org.hibernate.dialect.PostgreSQLDialect");

        tenantResolver.setTenant("test1");

        userRepository.deleteAll();
        User newUser = new User("Jean", "Pont");
        newUser = userRepository.save(newUser);
        LOG.info("id=" + newUser.getId());

        User newUser2 = new User("Alain", "Deloin");
        newUser2 = userRepository.save(newUser2);
        LOG.info("id=" + newUser2.getId());

        Iterable<User> list = userRepository.findAll();

        int cpt = 0;
        for (User user : list) {
            cpt++;
        }

        Assert.isTrue(cpt == 2, "Error, there must be 2 elements in the database associated to the tenant test1");

        tenantResolver.setTenant("test2");
        list = userRepository.findAll();

        cpt = 0;
        for (User user : list) {
            cpt++;
        }
        Assert.isTrue(cpt == 0,
                      "Error, there must be no element in the database associated to the tenant test1 (" + cpt + ")");

        tenantResolver.setTenant("test1");

        list = userRepository.findAll();

        cpt = 0;
        for (User user : list) {
            cpt++;
        }

        Assert.isTrue(cpt == 2, "Error, there must be 2 elements in the database associated to the tenant test1");

    }

}
