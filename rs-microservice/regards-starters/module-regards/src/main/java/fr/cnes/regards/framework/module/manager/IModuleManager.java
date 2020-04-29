/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.module.manager;

import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Bean contract for module management
 * @param <S> module specifications for {@link ModuleReadinessReport}
 * @author Marc Sordi
 */
public interface IModuleManager<S> {

    /**
     * Module information
     */
    ModuleInformation getModuleInformation();

    /**
     * Check if this configuration is applicable to current manager. If so,
     * {@link #importConfigurationAndLog(ModuleConfiguration)} will be called.
     */
    boolean isApplicable(ModuleConfiguration configuration);

    /**
     * Import configuration
     */
    ModuleImportReport importConfigurationAndLog(ModuleConfiguration configuration);

    /**
     * Export configuration
     */
    ModuleConfiguration exportConfiguration() throws ModuleException;

    /**
     * Restart module after crashing. Inconsistent state should be detected and cleaned.<br/>
     * To implement this method, override it and {@link #isRestartImplemented()}
     */
    default ModuleRestartReport restart() {
        throw new UnsupportedOperationException("Restart feature not implemented");
    }

    /**
     * Flag to indicate if restart feature is implemented for current module
     */
    default boolean isRestartImplemented() {
        return false;
    }

    /**
     * Detect if current module is ready to run, i.e. all required configurations are done!<br/>
     * Module specification may be given to fulfill missing configuration.<br/>
     * For instance, most of the time, minimal configuration will require at least one plugin configuration.<br/>
     * To implement this method, override it and {@link #isReadyImplemented()}
     * @return whether or not the module is considered ready
     */
    default ModuleReadinessReport<S> isReady() {
        throw new UnsupportedOperationException("Ready feature not implemented");
    }

    /**
     * Flag to indicate if ready feature is implemented for current module.
     */
    default boolean isReadyImplemented() {
        return false;
    }

    /**
     * Reset current module configuration
     * @param module
     * @return
     */
    default Set<String> resetConfiguration() {
        return Sets.newHashSet();
    }
}
