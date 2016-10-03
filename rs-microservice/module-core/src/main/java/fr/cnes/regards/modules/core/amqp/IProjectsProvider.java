/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import java.util.List;

/**
 * @author svissier
 *
 */
public interface IProjectsProvider {

    public List<String> retrieveProjectList();
}
