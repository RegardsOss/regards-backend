/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DataSourceBasedMultiTenantConnectionProviderImpl connectionProvider;

    @Value("${test.datasource.url}")
    private String additionalDataSourceUrl;

    @Value("${test.datasource.username}")
    private String additionalDataSourceUserName;

    @Value("${test.datasource.password}")
    private String additionalDataSourcePassword;

    @Before
    public void initDatasources() {
        // Dynamically add a new datasource with a given associated tenant name
        connectionProvider.addDataSource(additionalDataSourceUrl, additionalDataSourceUserName,
                                         additionalDataSourcePassword, "test1");
    }

    @Test
    public void contextLoads() {
        // Nothing to do
    }

    // TODO : Auto create schema for the additional datasource ?
    // @Test
    public void multitenancyAccessTest() {

        tenantResolver.setTenant("postgres");

        userRepository.deleteAll();
        User newUser = new User("Jean", "Pont");
        newUser = userRepository.save(newUser);
        LOG.info("id=" + newUser.getId());

        Iterable<User> list = userRepository.findAll();

        int cpt = 0;
        for (User user : list) {
            cpt++;
        }

        Assert.isTrue(cpt == 1, "Error, there must be 1 element in the database associated to the tenant test1");

        tenantResolver.setTenant("test1");
        list = userRepository.findAll();

        cpt = 0;
        for (User user : list) {
            cpt++;
        }
        Assert.isTrue(cpt == 0,
                      "Error, there must be no element in the database associated to the tenant test1 (" + cpt + ")");

    }

}
