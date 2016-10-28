package fr.cnes.regards.modules.accessrights.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.Couple;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.UserVisibility;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/**
 * {@link IProjectUserService} implementation
 *
 * @author xbrochar
 */
@Service
public class ProjectUserService implements IProjectUserService {

    /**
     * CRUD repository managing {@link ProjectUser}s. Autowired by Spring.
     */
    private final IProjectUserRepository projectUserRepository;

    /**
     * Service handling CRUD operation on {@link Role}s
     */
    private final IRoleService roleService;

    /**
     * CRUD repository managing {@link Role}s. Autowired by Spring.
     */
    private final IRoleRepository roleRepository;

    /**
     * A filter on meta data to keep visible ones only
     */
    private final Predicate<? super MetaData> visibleMetaDataFilter = m -> !UserVisibility.HIDDEN
            .equals(m.getVisibility());

    /**
     * Creates a new instance of the service with passed services/repos
     *
     * @param pProjectUserRepository
     *            The project user repo
     * @param pRoleService
     *            The role service
     * @param pRoleRepository
     *            The role repo
     */
    public ProjectUserService(final IProjectUserRepository pProjectUserRepository, final IRoleService pRoleService,
            final IRoleRepository pRoleRepository) {
        super();
        projectUserRepository = pProjectUserRepository;
        roleService = pRoleService;
        roleRepository = pRoleRepository;
    }

    @Override
    public List<ProjectUser> retrieveUserList() {
        return projectUserRepository.findByStatus(UserStatus.ACCESS_GRANTED);
    }

    @Override
    public ProjectUser retrieveUser(final Long pUserId) {
        final ProjectUser user = projectUserRepository.findOne(pUserId);
        // Filter out hidden meta data
        try (final Stream<MetaData> stream = user.getMetaData().stream()) {
            stream.filter(visibleMetaDataFilter);
        }
        return user;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.IProjectUserService#retrieveCurrentUser()
     */
    @Override
    public ProjectUser retrieveCurrentUser() {
        final String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return projectUserRepository.findOneByEmail(email);
    }

    @Override
    public void updateUser(final Long pUserId, final ProjectUser pUpdatedProjectUser)
            throws InvalidValueException, EntityNotFoundException {
        if (pUpdatedProjectUser.getId() != pUserId) {
            throw new InvalidValueException("Account id specified differs from updated account id");
        }
        if (!existUser(pUserId)) {
            throw new EntityNotFoundException(pUserId.toString(), ProjectUser.class);
        }
        projectUserRepository.save(pUpdatedProjectUser);
    }

    @Override
    public void removeUser(final Long pUserId) {
        projectUserRepository.delete(pUserId);
    }

    @Override
    public void updateUserAccessRights(final String pLogin, final List<ResourcesAccess> pUpdatedUserAccessRights)
            throws EntityNotFoundException {
        if (!existUser(pLogin)) {
            throw new EntityNotFoundException(pLogin, ProjectUser.class);
        }
        final ProjectUser user = projectUserRepository.findOneByEmail(pLogin);

        try (Stream<ResourcesAccess> previous = user.getPermissions().stream();
                Stream<ResourcesAccess> updated = pUpdatedUserAccessRights.stream()) {
            user.setPermissions(Stream.concat(updated, previous).filter(distinctByKey(r -> r.getId()))
                    .collect(Collectors.toList()));
        }

        projectUserRepository.save(user);
    }

    @Override
    public void removeUserAccessRights(final String pLogin) {
        final ProjectUser user = projectUserRepository.findOneByEmail(pLogin);
        user.setPermissions(new ArrayList<>());
        projectUserRepository.save(user);
    }

    @Override
    public List<MetaData> retrieveUserMetaData(final Long pUserId) {
        final ProjectUser user = retrieveUser(pUserId);
        return user.getMetaData();
    }

    @Override
    public void updateUserMetaData(final Long pUserId, final List<MetaData> pUpdatedUserMetaData) {
        final ProjectUser user = retrieveUser(pUserId);
        user.setMetaData(pUpdatedUserMetaData);
        projectUserRepository.save(user);
    }

    @Override
    public void removeUserMetaData(final Long pUserId) {
        final ProjectUser user = retrieveUser(pUserId);
        user.setMetaData(new ArrayList<>());
        projectUserRepository.save(user);
    }

    @Override
    public Couple<List<ResourcesAccess>, Role> retrieveProjectUserAccessRights(final String pLogin,
            final String pBorrowedRoleName) throws InvalidValueException {
        final ProjectUser projectUser = projectUserRepository.findOneByEmail(pLogin);
        final Role userRole = projectUser.getRole();
        Role returnedRole = userRole;

        if (pBorrowedRoleName != null) {
            final Role borrowedRole = roleRepository.findOneByName(pBorrowedRoleName);
            if (roleService.isHierarchicallyInferior(borrowedRole, returnedRole)) {
                returnedRole = borrowedRole;
            } else {
                throw new InvalidValueException(
                        "Borrowed role must be hierachically inferior to the project user's role");
            }
        }

        return new Couple<>(projectUser.getPermissions(), returnedRole);

    }

    @Override
    public boolean existUser(final String pEmail) {
        final ProjectUser projectUser = projectUserRepository.findOneByEmail(pEmail);
        return projectUser != null;
    }

    @Override
    public boolean existUser(final Long pId) {
        return projectUserRepository.exists(pId);
    }

    /**
     * Filter in order to remove doubles (same as distinct) but with custom key extraction.
     * <p>
     * Warning: Keeps the first seen, so the order does matter!
     *
     * For example use as: persons.stream().filter(distinctByKey(p -> p.getName());
     *
     * Removes doubles based on person.name attribute.
     *
     *
     * @param pKeyExtractor
     *            The
     * @param <T>
     *            The type
     * @return A predicate
     * @see http://stackoverflow.com/questions/23699371/java-8-distinct-by-property
     */
    private static <T> Predicate<T> distinctByKey(final Function<? super T, ?> pKeyExtractor) {
        final Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(pKeyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
