package fr.cnes.regards.modules.storage.domain.plugin;

public class PluginConfUpdatable {

    private final boolean updateAllowed;

    private final String updateNotAllowedReason;

    private PluginConfUpdatable(boolean updateAllowed, String updateNotAllowedReason) {
        this.updateAllowed = updateAllowed;
        this.updateNotAllowedReason = updateNotAllowedReason;
    }

    public static PluginConfUpdatable allowUpdate() {
        return new PluginConfUpdatable(true, null);
    }

    public static PluginConfUpdatable preventUpdate(String rejectionCause) {
        return new PluginConfUpdatable(false, rejectionCause);
    }

    public boolean isUpdateAllowed() {
        return updateAllowed;
    }

    public String getUpdateNotAllowedReason() {
        return updateNotAllowedReason;
    }

}
