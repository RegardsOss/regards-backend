/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Bean contract for module management
 *
 * @author Marc Sordi
 *
 * @param <S> module specifications for {@link ModuleReadinessReport}
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
     * Restart module after crashing. Inconsistent state should be detected and cleaned.
     */
    ModuleRestartReport restart();

    /**
     * Detect if current module is ready to run, i.e. all required configurations are done!<br/>
     * Module specification may be given to fulfill missing configuration.<br/>
     * For instance, most of the time, minimal configuration will require at least one plugin configuration.
     *
     * @return whether or not the module is considered ready
     */
    ModuleReadinessReport<S> isReady();
}
