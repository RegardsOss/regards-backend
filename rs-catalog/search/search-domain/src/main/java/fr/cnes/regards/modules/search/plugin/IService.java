/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.plugin;

import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 *
 * Plugin applying processus according to its parameters.
 *
 * @author Sylvain Vissiere-Guerinet
 */
@PluginInterface(description = "Plugin applying processus on a query")
public interface IService {

    /**
     * Apply the processus described by this instance of IService
     *
     * @return response HTTP including the content-type and the body wanted
     */
    public ResponseEntity<?> apply();

    /**
     * Can this implementation be used with only one datum's id? Should have a PluginParameter representing this datum
     * @return the answer to this question
     */
    boolean isApplyableOnOneData();

    /**
     * Can this implementation be used with a list of data's id? Should have a PluginParameter corresponding to this
     * list
     * @return the answer to this question
     */
    boolean isApplyableOnManyData();

    /**
     * Can this implementation be used with a query similar to the ones used by searches endpoint? Should have a
     * PluginParameter for the query
     * @return the answer to this question
     */
    boolean isApplyableOnQuery();

}
