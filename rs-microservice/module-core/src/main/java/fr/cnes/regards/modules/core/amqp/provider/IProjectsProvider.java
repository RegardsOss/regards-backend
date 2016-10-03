/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp.provider;

import java.util.List;

/**
 * @author svissier
 *
 */
public interface IProjectsProvider {

    public List<String> retrieveProjectList();
}
