package fr.cnes.regards.modules.accessrights.instance.domain;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import java.util.Collections;
import java.util.List;

public final class AccountSettings {

    private AccountSettings() {
    }

    public static final String VALIDATION = "account_validation_mode";

    public static final ValidationMode DEFAULT_VALIDATION_MODE = ValidationMode.AUTO_ACCEPT;

    public static final DynamicTenantSetting VALIDATION_SETTING = new DynamicTenantSetting(
            VALIDATION,
            "Accept Mode",
            DEFAULT_VALIDATION_MODE.getName()
    );
    public static final List<DynamicTenantSetting> SETTING_LIST = Collections.singletonList(
            VALIDATION_SETTING
    );

    public enum ValidationMode {

        MANUAL("manual"),
        AUTO_ACCEPT("auto-accept");

        private final String name;

        ValidationMode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static ValidationMode fromName(String name) {
            for (ValidationMode validationMode : values()) {
                if (validationMode.getName().equals(name)) {
                    return validationMode;
                }
            }
            return null;
        }

    }

}
