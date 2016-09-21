/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.hateoas.Identifiable;

/**
 * BorrowedRoleProjectUser is a decorator for {@link IProjectUser} objects.<br>
 * The calls to the {@link IProjectUser} interface are intercepted and decorated. Finally the calls are delegated to the
 * decorated {@link IProjectUser} object.<br>
 * Here only {@link #getRole} is decorated in order to return the borrowedRole instead of the decorated
 * {@link IProjectUser}'s role
 *
 * @author xbrochard
 *
 */
public class BorrowedRoleProjectUser implements Identifiable<Long>, IProjectUser {

    private final ProjectUser decorated_;

    private final Role borrowedRole_;

    public BorrowedRoleProjectUser(ProjectUser pDecorated, Role pBorrowedRole) {
        super();
        decorated_ = pDecorated;
        borrowedRole_ = pBorrowedRole;
    }

    @Override
    public Role getRole() {
        return borrowedRole_;
    }

    // Proxy all other methods to the decorated ProjectUser

    @Override
    public void setRole(Role pRole) {
        decorated_.setRole(pRole);
    }

    @Override
    public LocalDateTime getLastConnection() {
        return decorated_.getLastConnection();
    }

    @Override
    public void setLastConnection(LocalDateTime pLastConnection) {
        decorated_.setLastConnection(pLastConnection);
    }

    @Override
    public LocalDateTime getLastUpdate() {
        return decorated_.getLastUpdate();
    }

    @Override
    public void setLastUpdate(LocalDateTime pLastUpdate) {
        decorated_.setLastUpdate(pLastUpdate);
    }

    @Override
    public Long getId() {
        return decorated_.getId();
    }

    @Override
    public UserStatus getStatus() {
        return decorated_.getStatus();
    }

    @Override
    public void setStatus(UserStatus pStatus) {
        decorated_.setStatus(pStatus);
    }

    @Override
    public List<MetaData> getMetaData() {
        return decorated_.getMetaData();
    }

    @Override
    public void setMetaData(List<MetaData> pMetaData) {
        decorated_.setMetaData(pMetaData);
    }

    @Override
    public ProjectUser accept() {
        return decorated_.accept();
    }

    @Override
    public ProjectUser deny() {
        return decorated_.deny();
    }

    @Override
    public Account getAccount() {
        return decorated_.getAccount();
    }

    @Override
    public void setAccount(Account pAccount) {
        decorated_.setAccount(pAccount);
    }

    @Override
    public List<ResourcesAccess> getPermissions() {
        return decorated_.getPermissions();
    }

    @Override
    public void setPermissions(List<ResourcesAccess> pPermissions) {
        decorated_.setPermissions(pPermissions);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ProjectUser) && (((ProjectUser) o).getId() == getId());
    }
}
