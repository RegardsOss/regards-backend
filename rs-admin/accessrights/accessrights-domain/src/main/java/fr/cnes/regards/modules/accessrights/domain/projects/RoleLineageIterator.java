package fr.cnes.regards.modules.accessrights.domain.projects;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Specific implementation of an {@link Iterator} in order to iterate through the {@link Role#getParentRole()}
 * relation.<br>
 * The {@link #next()} method returns the passed role's parent, then its parent, and so on until a null parent is
 * reached.
 *
 * @author xbrochard
 * @author Sébastien Binda
 *
 */
public class RoleLineageIterator implements Iterator<Role> {

    /**
     * The current role
     */
    private Role role;

    public RoleLineageIterator(final Role pRole) {
        role = pRole;
    }

    @Override
    public boolean hasNext() {
        return role.getParentRole() != null;
    }

    @Override
    public Role next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        } else {
            role = role.getParentRole();
            return role;
        }
    }

}
