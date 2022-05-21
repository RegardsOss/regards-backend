package fr.cnes.regards.framework.modules.dump.domain;

import java.util.Objects;

public class DumpParameters {

    private boolean isActiveModule;

    private String cronTrigger;

    private String dumpLocation;

    public boolean isActiveModule() {
        return isActiveModule;
    }

    public DumpParameters setActiveModule(boolean activeModule) {
        isActiveModule = activeModule;
        return this;
    }

    public String getCronTrigger() {
        return cronTrigger;
    }

    public DumpParameters setCronTrigger(String cronTrigger) {
        this.cronTrigger = cronTrigger;
        return this;
    }

    public String getDumpLocation() {
        return dumpLocation;
    }

    public DumpParameters setDumpLocation(String dumpLocation) {
        this.dumpLocation = dumpLocation;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DumpParameters)) {
            return false;
        }
        DumpParameters that = (DumpParameters) o;
        return isActiveModule == that.isActiveModule && Objects.equals(cronTrigger, that.cronTrigger) && Objects.equals(
            dumpLocation,
            that.dumpLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isActiveModule, cronTrigger, dumpLocation);
    }

}
