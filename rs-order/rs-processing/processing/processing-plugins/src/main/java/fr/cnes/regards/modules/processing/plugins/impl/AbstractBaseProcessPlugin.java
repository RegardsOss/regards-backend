package fr.cnes.regards.modules.processing.plugins.impl;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import io.vavr.collection.Seq;
import io.vavr.control.Option;

import java.util.List;

import static io.vavr.collection.List.empty;
import static io.vavr.collection.List.ofAll;

/**
 * The absolute base for plugin implementation, with the basic parameters to deal
 * with rights/quotas, activation, etc.
 */
public abstract class AbstractBaseProcessPlugin implements IProcessDefinition {

    @PluginParameter(
            name = "processName",
            label = "Process name",
            description = "Plugin instance name"
    )
    protected String processName;

    @PluginParameter(
            name = "active",
            label = "Activation flag",
            description = "Allows to deactivate temporarily a process, preventing new executions."
    )
    protected Boolean active;

    @PluginParameter(
            name = "allowedTenants",
            label = "Allowed Tenants",
            description = "List of allowed tenants for which this process can be used; empty or missing means all.",
            optional = true
    )
    protected List<String> allowedTenants;

    @PluginParameter(
            name = "allowedUserRoles",
            label = "Allowed User Roles",
            description = "List of allowed user roles for which this process can be used ; empty or missing means all.",
            optional = true
    )
    protected List<String> allowedUserRoles;

    @PluginParameter(
            name = "allowedDatasets",
            label = "Allowed Data Sets",
            description = "List of allowed order datasets for which this process can be used ; empty or missing means all.",
            optional = true
    )
    protected List<String> allowedDatasets;

    @PluginParameter(
            name = "maxConcurrentExecutions",
            label = "Maximum concurrent executions",
            description = "For a given user, how many concurrent executions of this process are allowed. Zero means no limit.",
            defaultValue = "0",
            optional = true
    )
    protected Integer maxConcurrentExecutions;

    @PluginParameter(
            name = "maxBytesInCache",
            label = "Maximum bytes in cache",
            description = "If the process has no dynamic parameters the result can be put in cache. "
                    + " This parameter tells how much memory in bytes is allowed for results of executions of this process.",
            defaultValue = "0",
            optional = true
    )
    protected Long maxBytesInCache;

    /**
     * Workload engine, defining what kind of mechanism to use to manage workload."
     *
     * Is 'JOB' by default.
     *
     * Can be only 'JOB' for the moment, and is not exposed as a plugin parameter.
     * It has been planned to add a 'SPRING_BATCH' engine to deal with more
     * resource-intensive processes, allowing to distribute computation to other agents.
     */
    protected String engineName;

    @Override public boolean isActive() {
        return false;
    }

    @Override public Seq<String> allowedTenants() {
        return emptyOrAllImmutable(allowedTenants);
    }

    @Override public Seq<String> allowedUserRoles() {
        return emptyOrAllImmutable(allowedUserRoles);
    }

    @Override public Seq<String> allowedDatasets() {
        return emptyOrAllImmutable(allowedDatasets);
    }

    @Override public String processName() {
        return processName;
    }

    @Override public Option<Integer> maxConcurrentExecutions() {
        return Option.of(maxConcurrentExecutions).filter(m -> m > 0);
    }

    @Override public Option<Long> maxBytesInCache() {
        return Option.of(maxBytesInCache).filter(m -> m > 0);
    }

    @Override public String engineName() {
        return engineName;
    }

    protected static io.vavr.collection.List<String> emptyOrAllImmutable(List<String> strings) {
        return strings == null ? empty() : ofAll(strings);
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setAllowedTenants(List<String> allowedTenants) {
        this.allowedTenants = allowedTenants;
    }

    public void setAllowedUserRoles(List<String> allowedUserRoles) {
        this.allowedUserRoles = allowedUserRoles;
    }

    public void setAllowedDatasets(List<String> allowedDatasets) {
        this.allowedDatasets = allowedDatasets;
    }

    public void setMaxConcurrentExecutions(Integer maxConcurrentExecutions) {
        this.maxConcurrentExecutions = maxConcurrentExecutions;
    }

    public void setMaxBytesInCache(Long maxBytesInCache) {
        this.maxBytesInCache = maxBytesInCache;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }
}
