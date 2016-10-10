package fr.cnes.regards.modules.accessRights.service;

import java.util.Iterator;

import fr.cnes.regards.modules.accessRights.domain.projects.Role;

/**
 * Specific implementation of an {@link Iterator} in order to iterate through the {@link Role#getParentRole()}
 * relation.<br>
 * The {@link #next()} method returns the passed role's parent, then its parent, and so on until a null parent is
 * reached.
 *
 * @author xbrochard
 *
 */
public class RoleLineageIterator implements Iterator<Role> {

    private Role role;

    public RoleLineageIterator(Role pRole) {
        role = pRole;
    }

    @Override
    public boolean hasNext() {
        return role.getParentRole() != null;
    }

    @Override
    public Role next() {
        role = role.getParentRole();
        return role;
    }

}
