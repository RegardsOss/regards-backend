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

    private Role role_;

    public RoleLineageIterator(Role pRole) {
        role_ = pRole;
    }

    @Override
    public boolean hasNext() {
        return role_.getParentRole() != null;
    }

    @Override
    public Role next() {
        role_ = role_.getParentRole();
        return role_;
    }

}
