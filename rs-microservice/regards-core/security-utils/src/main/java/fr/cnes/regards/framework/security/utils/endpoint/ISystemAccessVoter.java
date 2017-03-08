/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.endpoint;

import org.springframework.security.access.AccessDecisionVoter;

/**
 *
 * Class IInstanceAdminAccessVoter
 *
 * Interface to implement specific MetodAuthroization voter for system Roles.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public interface ISystemAccessVoter extends AccessDecisionVoter<Object> {

}
