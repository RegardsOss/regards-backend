/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.provider;

import java.util.List;

/**
 * @author svissier
 *
 */
public interface IProjectsProvider {

    public List<String> retrieveProjectList();
}
