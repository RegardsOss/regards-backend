/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao.stubs;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.test.repository.JpaRepositoryStub;
import fr.cnes.regards.modules.accessRights.dao.projects.IAccessSettingsRepository;
import fr.cnes.regards.modules.accessRights.domain.projects.AccessSettings;

/**
 * Stub {@link AccessSettings} jpa repository for test purposes.
 *
 * @author CS SI
 */
@Repository
@Profile("test")
@Primary
public class AccessSettingsRepositoryStub extends JpaRepositoryStub<AccessSettings>
        implements IAccessSettingsRepository {

    /**
     * Create a stub repository and populate it
     */
    public AccessSettingsRepositoryStub() {
        final AccessSettings settings = new AccessSettings();
        settings.setId(0L);
        settings.setMode("manual");
        entities.add(settings);
    }

}
