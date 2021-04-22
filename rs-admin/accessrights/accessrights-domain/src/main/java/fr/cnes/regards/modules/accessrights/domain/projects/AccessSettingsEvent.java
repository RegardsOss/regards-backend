package fr.cnes.regards.modules.accessrights.domain.projects;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

import java.util.List;
import java.util.Objects;

@Event(target = Target.ALL)
public class AccessSettingsEvent implements ISubscribable {

    private String mode;

    private String role;

    private List<String> groups;

    public AccessSettingsEvent(String mode, String role, List<String> groups) {
        this.mode = mode;
        this.role = role;
        this.groups = groups;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AccessSettingsEvent that = (AccessSettingsEvent) o;
        return Objects.equals(mode, that.mode) && Objects.equals(role, that.role) && Objects.equals(groups, that.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, role, groups);
    }
}
