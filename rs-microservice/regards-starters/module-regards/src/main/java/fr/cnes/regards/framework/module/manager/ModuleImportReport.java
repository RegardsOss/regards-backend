package fr.cnes.regards.framework.module.manager;

import java.util.Set;

/**
 * Pojo used to know if module configuration import had errors
 * @author Sylvain VISSIERE-GUERINET
 */
public class ModuleImportReport {

    private ModuleInformation moduleInformation;

    private Set<String> importErrors;

    private boolean onlyErrors;

    protected ModuleImportReport() {
    }

    public ModuleImportReport(ModuleInformation moduleInformation, Set<String> importErrors, boolean onlyErrors) {
        this.moduleInformation = moduleInformation;
        this.importErrors = importErrors;
        this.onlyErrors = onlyErrors;
    }

    public ModuleInformation getModuleInformation() {
        return moduleInformation;
    }

    public void setModuleInformation(ModuleInformation moduleInformation) {
        this.moduleInformation = moduleInformation;
    }

    public Set<String> getImportErrors() {
        return importErrors;
    }

    public void setImportErrors(Set<String> importErrors) {
        this.importErrors = importErrors;
    }

    public boolean isOnlyErrors() {
        return onlyErrors;
    }

    public void setOnlyErrors(boolean onlyErrors) {
        this.onlyErrors = onlyErrors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ModuleImportReport that = (ModuleImportReport) o;

        if (onlyErrors != that.onlyErrors) {
            return false;
        }
        if (moduleInformation != null ?
                !moduleInformation.equals(that.moduleInformation) :
                that.moduleInformation != null) {
            return false;
        }
        return importErrors != null ? importErrors.equals(that.importErrors) : that.importErrors == null;
    }

    @Override
    public int hashCode() {
        int result = moduleInformation != null ? moduleInformation.hashCode() : 0;
        result = 31 * result + (importErrors != null ? importErrors.hashCode() : 0);
        result = 31 * result + (onlyErrors ? 1 : 0);
        return result;
    }
}
