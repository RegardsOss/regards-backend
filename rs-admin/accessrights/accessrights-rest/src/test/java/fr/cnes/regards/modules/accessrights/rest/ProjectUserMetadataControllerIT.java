package fr.cnes.regards.modules.accessrights.rest;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IMetaDataRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.QuotaHelperService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class ProjectUserMetadataControllerIT extends AbstractRegardsTransactionalIT {

    private static final String EMAIL = "email@test.com";

    private static final String ERROR_MESSAGE = "Cannot reach model attributes";

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IProjectUserRepository projectUserRepository;

    @Autowired
    private IMetaDataRepository metaDataRepository;

    @MockBean
    private QuotaHelperService quotaHelperService;

    private ProjectUser projectUser;

    @Before
    public void setUp() {
        Role publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString()).orElse(null);
        projectUser = projectUserRepository
                .save(new ProjectUser(EMAIL, publicRole, new ArrayList<>(), new HashSet<>()));
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to update a user's metadata.")
    public void updateUserMetaData() {
        final List<MetaData> newPermissionList = new ArrayList<>();
        newPermissionList.add(metaDataRepository.save(new MetaData()));
        newPermissionList.add(metaDataRepository.save(new MetaData()));

        performDefaultPut(ProjectUserMetadataController.REQUEST_MAPPING_ROOT,
                          newPermissionList,
                          customizer().expectStatusOk(),
                          ERROR_MESSAGE,
                          projectUser.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to retrieve a user's metadata.")
    public void getUserMetaData() {
        performDefaultGet(ProjectUserMetadataController.REQUEST_MAPPING_ROOT,
                          customizer().expectStatusOk(),
                          ERROR_MESSAGE,
                          projectUser.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to delete a user's metadata.")
    public void deleteUserMetaData() {
        performDefaultDelete(ProjectUserMetadataController.REQUEST_MAPPING_ROOT,
                             customizer().expectStatusOk(),
                             ERROR_MESSAGE,
                             projectUser.getId());
    }

}
