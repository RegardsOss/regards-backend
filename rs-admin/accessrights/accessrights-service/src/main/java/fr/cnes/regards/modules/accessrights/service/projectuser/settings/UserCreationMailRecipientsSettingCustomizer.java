package fr.cnes.regards.modules.accessrights.service.projectuser.settings;

import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserCreationMailRecipientsSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    public UserCreationMailRecipientsSettingCustomizer() {
        super(AccessSettings.USER_CREATION_MAIL_RECIPIENTS,
              "parameter [mail recipient] can be null or must be valid strings");
    }

    @Override
    protected boolean isProperValue(Object value) {
        if (value == null) {
            return true;
        }
        return value instanceof Set && ((Set<?>) value).stream().allMatch(String.class::isInstance);
    }

}
