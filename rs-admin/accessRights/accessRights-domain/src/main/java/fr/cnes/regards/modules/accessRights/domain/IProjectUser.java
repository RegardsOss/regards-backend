/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain;

import java.time.LocalDateTime;
import java.util.List;

public interface IProjectUser {

    Long getId();

    LocalDateTime getLastConnection();

    void setLastConnection(LocalDateTime pLastConnection);

    LocalDateTime getLastUpdate();

    void setLastUpdate(LocalDateTime pLastUpdate);

    UserStatus getStatus();

    void setStatus(UserStatus pStatus);

    List<MetaData> getMetaData();

    void setMetaData(List<MetaData> pMetaData);

    ProjectUser accept();

    ProjectUser deny();

    Account getAccount();

    void setAccount(Account pAccount);

    Role getRole();

    void setRole(Role pRole);

    List<ResourcesAccess> getPermissions();

    void setPermissions(List<ResourcesAccess> pPermissions);
}
