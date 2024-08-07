package fr.cnes.regards.framework.modules.plugins.domain.event;

import java.util.Set;

/**
 * @author oroussel
 */
public abstract class AbstractPluginConfEvent {

    /**
     * The plugin configuration id
     */
    private Long pluginConfId;

    private final String pluginBusinnessId;

    private final String label;

    /**
     * The action this event reflects
     */
    private PluginServiceAction action;

    /**
     * The plugin types the plugin configuration is configuring
     */
    private Set<String> pluginTypes;

    protected AbstractPluginConfEvent(Long pPluginConfId,
                                      String pluginBusinnessId,
                                      String label,
                                      PluginServiceAction pAction,
                                      Set<String> pPluginTypes) {
        super();
        this.pluginConfId = pPluginConfId;
        this.pluginBusinnessId = pluginBusinnessId;
        this.label = label;
        this.action = pAction;
        this.pluginTypes = pPluginTypes;
    }

    protected AbstractPluginConfEvent() {
        this(null, null, null, null, null);
    }

    /**
     * @return the plugin configuration id
     */
    public Long getPluginConfId() {
        return pluginConfId;
    }

    /**
     * Set the plugin configuration id
     */
    public void setPluginConfId(Long pPluginConfId) {
        pluginConfId = pPluginConfId;
    }

    /**
     * @return the action reflected by this event
     */
    public PluginServiceAction getAction() {
        return action;
    }

    /**
     * Set the action this event is reflecting
     */
    public void setAction(PluginServiceAction pAction) {
        action = pAction;
    }

    /**
     * @return the plugin types
     */
    public Set<String> getPluginTypes() {
        return pluginTypes;
    }

    /**
     * Set the plugin types
     */
    public void setPluginTypes(Set<String> pPluginTypes) {
        pluginTypes = pPluginTypes;
    }

    /**
     * @return the pluginBusinnessId
     */
    public String getPluginBusinnessId() {
        return pluginBusinnessId;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((action == null) ? 0 : action.hashCode());
        result = (prime * result) + ((pluginConfId == null) ? 0 : pluginConfId.hashCode());
        result = (prime * result) + ((pluginTypes == null) ? 0 : pluginTypes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractPluginConfEvent other = (AbstractPluginConfEvent) obj;
        if (action != other.action) {
            return false;
        }
        if (pluginConfId == null) {
            if (other.pluginConfId != null) {
                return false;
            }
        } else if (!pluginConfId.equals(other.pluginConfId)) {
            return false;
        }
        if (pluginTypes == null) {
            return other.pluginTypes == null;
        } else {
            return pluginTypes.equals(other.pluginTypes);
        }
    }

}
