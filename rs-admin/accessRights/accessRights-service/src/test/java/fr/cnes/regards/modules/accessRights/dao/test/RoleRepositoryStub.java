package fr.cnes.regards.modules.accessRights.dao.test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.dao.IDaoProjectUser;
import fr.cnes.regards.modules.accessRights.dao.IDaoResourcesAccess;
import fr.cnes.regards.modules.accessRights.dao.IRoleRepository;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;

@Repository
public class RoleRepositoryStub implements IRoleRepository {

    private List<Role> roles_ = new ArrayList<>();

    @Autowired
    private IDaoResourcesAccess daoResourcesAccess_;

    @Autowired
    private IDaoProjectUser daoProjectUser_;

    @PostConstruct
    public void init() {
        List<ResourcesAccess> permissionList_ = daoResourcesAccess_.getAll();
        List<ProjectUser> projectUsers_ = daoProjectUser_.getAll();

        // Init default ro
        Role rolePublic = new Role(0L, "Public", null, permissionList_, projectUsers_.subList(8, 10), true, true);
        Role roleRegisteredUser = new Role(1L, "Registered User", rolePublic, null, projectUsers_.subList(5, 8), false,
                true);
        Role roleAdmin = new Role(2L, "Admin", roleRegisteredUser, permissionList_.subList(0, 2),
                projectUsers_.subList(3, 5), false, true);
        Role roleProjectAdmin = new Role(3L, "Project Admin", roleAdmin, permissionList_.subList(2, 3),
                projectUsers_.subList(1, 3), false, true);
        Role roleInstanceAdmin = new Role(4L, "Instance Admin", roleProjectAdmin, new ArrayList<>(),
                projectUsers_.subList(0, 1), false, true);
        roles_.add(rolePublic);
        roles_.add(roleRegisteredUser);
        roles_.add(roleAdmin);
        roles_.add(roleProjectAdmin);
        roles_.add(roleInstanceAdmin);

        // Init some custom roles
        Role role0 = new Role(5L, "Role 0", rolePublic, permissionList_.subList(1, 2), projectUsers_.subList(0, 1));
        Role role1 = new Role(6L, "Role 1", rolePublic, permissionList_.subList(0, 2), projectUsers_.subList(1, 2));
        Role role2 = new Role(7L, "Role 2", rolePublic, permissionList_.subList(1, 3), projectUsers_.subList(0, 2));
        roles_.add(role0);
        roles_.add(role1);
        roles_.add(role2);
    }

    @Override
    public <S extends Role> S save(S pEntity) {
        // roles_.add(pEntity);
        roles_.removeIf(r -> r.equals(pEntity));
        roles_.add(pEntity);
        return pEntity;
    }

    @Override
    public Role findOne(Long pId) {
        return roles_.stream().filter(r -> r.getId().equals(pId)).findFirst().get();
    }

    @Override
    public boolean exists(Long pId) {
        return roles_.stream().filter(r -> r.getId().equals(pId)).findAny().isPresent();
    }

    @Override
    public long count() {
        return roles_.size();
    }

    @Override
    public void delete(Long pId) {
        roles_.removeIf(r -> r.getId().equals(pId));
    }

    @Override
    public void delete(Role pEntity) {
        roles_.remove(pEntity);
    }

    @Override
    public void delete(Iterable<? extends Role> pEntities) {
        for (Role entity : pEntities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        roles_ = new ArrayList<>();
    }

    @Override
    public <S extends Role> List<S> save(Iterable<S> pEntities) {
        List<S> savedEntities = new ArrayList<>();

        for (S entity : pEntities) {
            savedEntities.add(save(entity));
        }

        return savedEntities;
    }

    @Override
    public List<Role> findAll() {
        return roles_;
    }

    @Override
    public List<Role> findAll(Iterable<Long> pIds) {
        return StreamSupport.stream(pIds.spliterator(), false).map(id -> findOne(id)).collect(Collectors.toList());
    }

    @Override
    public Role findByIsDefault(boolean pIsDefault) {
        return roles_.stream().filter(r -> r.isDefault() == pIsDefault).findFirst().get();
    }

    @Override
    public Role findOneByName(String pBorrowedRoleName) {
        return roles_.stream().filter(r -> r.getName().equals(pBorrowedRoleName)).findFirst().get();
    }

}
