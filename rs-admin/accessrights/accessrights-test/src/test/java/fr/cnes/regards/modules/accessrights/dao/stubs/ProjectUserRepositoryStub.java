/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.stubs;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.test.repository.JpaRepositoryStub;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

@Repository
@Profile("test")
@Primary
public class ProjectUserRepositoryStub extends JpaRepositoryStub<ProjectUser> implements IProjectUserRepository {

    private final IAccountRepository accountRepository;

    public ProjectUserRepositoryStub(final IAccountRepository pAccountService) throws AlreadyExistingException {
        super();
        accountRepository = pAccountService;

        final Role rolePublic = new Role(0L, "Public", null, null, null, true, true);
        final Role roleRegisteredUser = new Role(1L, "Registered User", rolePublic, null, null, false, true);
        final Role roleAdmin = new Role(2L, "Admin", roleRegisteredUser, null, null, false, true);
        final Role roleProjectAdmin = new Role(3L, "Project Admin", roleAdmin, null, null, false, true);
        final Role roleInstanceAdmin = new Role(4L, "Instance Admin", roleProjectAdmin, null, null, false, true);

        entities.add(buildProjectUser(0L, "instance_admin@cnes.fr", roleInstanceAdmin));
        entities.add(buildProjectUser(1L, "project_admin_0@cnes.fr", roleProjectAdmin));
        entities.add(buildProjectUser(2L, "project_admin_1@cnes.fr", roleProjectAdmin));
        entities.add(buildProjectUser(3L, "admin_0@cnes.fr", roleAdmin));
        entities.add(buildProjectUser(4L, "admin_1@cnes.fr", roleAdmin));
        entities.add(buildProjectUser(5L, "registered_user_0@cnes.fr", roleRegisteredUser));
        entities.add(buildProjectUser(6L, "registered_user_1@cnes.fr", roleRegisteredUser));
        entities.add(buildProjectUser(7L, "registered_user_2@cnes.fr", roleRegisteredUser));
        entities.add(buildProjectUser(8L, "registered_user_3@cnes.fr", roleRegisteredUser));
        entities.add(buildProjectUser(9L, "public_0@cnes.fr", rolePublic));
        entities.add(buildProjectUser(10L, "public_1@cnes.fr", rolePublic));

        // getByEmail("admin_0@cnes.fr").accept();
        // getByEmail("project_admin_0@cnes.fr").accept();
        // getByEmail("registered_user_0@cnes.fr").accept();
    }

    private ProjectUser buildProjectUser(final Long pProjectUserId, final String pMail, final Role pRole)
            throws AlreadyExistingException {

        Account account = new Account(pMail, "Firstname", "Lastname", "password");
        account = accountRepository.save(account);

        ProjectUser projectUser;
        projectUser = new ProjectUser(pProjectUserId, LocalDateTime.now(), LocalDateTime.now(),
                UserStatus.ACCESS_GRANTED, new ArrayList<>(), pRole, new ArrayList<>(), pMail);
        return projectUser;
    }

    @Override
    public ProjectUser findOneByEmail(final String pEmail) {
        return entities.stream().filter(r -> r.getEmail().equals(pEmail)).findFirst().get();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository#findByStatus(fr.cnes.regards.modules.
     * accessrights.domain.UserStatus)
     */
    @Override
    public List<ProjectUser> findByStatus(final UserStatus pStatus) {
        return entities.stream().filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCESS))
                .collect(Collectors.toList());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository#findByEmailIn(java.util.Collection)
     */
    @Override
    public List<ProjectUser> findByEmailIn(final Collection<String> pEmail) {
        try (final Stream<ProjectUser> stream = entities.stream()) {
            return stream.filter(e -> pEmail.contains(e.getEmail())).collect(Collectors.toList());
        }
    }

}
