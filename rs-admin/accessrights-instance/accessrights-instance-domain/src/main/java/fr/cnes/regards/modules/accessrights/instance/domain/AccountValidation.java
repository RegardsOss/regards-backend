package fr.cnes.regards.modules.accessrights.instance.domain;

public class AccountValidation {

    public static final String SETTING = "account_validation_mode";

    public static final Mode DEFAULT_MODE = Mode.AUTO_ACCEPT;

    public enum Mode {

        MANUAL("manual"),
        AUTO_ACCEPT("auto-accept");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Mode fromName(String name) {
            for (Mode mode : values()) {
                if (mode.getName().equals(name)) {
                    return mode;
                }
            }
            return null;
        }

    }

}
