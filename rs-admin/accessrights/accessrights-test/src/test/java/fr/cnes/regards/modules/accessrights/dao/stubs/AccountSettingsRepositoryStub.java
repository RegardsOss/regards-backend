/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.stubs;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.test.repository.JpaRepositoryStub;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountSettingsRepository;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;

/**
 * Stub {@link AccountSettings} jpa repository for test purposes.
 *
 * @author Xavier-Alexandre Brochard
 */
@Repository
@Profile("test")
@Primary
public class AccountSettingsRepositoryStub extends JpaRepositoryStub<AccountSettings>
        implements IAccountSettingsRepository {

}
