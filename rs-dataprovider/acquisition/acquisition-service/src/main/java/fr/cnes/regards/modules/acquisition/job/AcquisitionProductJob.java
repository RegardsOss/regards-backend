/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.modules.acquisition.job;

import java.util.Set;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.job.ProductJobParameter;

/**
 * @author Christophe Mertz
 *
 */
public class AcquisitionProductJob extends AbstractJob<Product> {

    @Override
    public void run() {

        // enchainer les différentes Step de l'acquisition pour un Produit
        // pour cela il faut la chaine de génération pour accéder aux plugins configurés
        
        Set<JobParameter> chains = getParameters();
        Product product = chains.iterator().next().getValue();
        

    }

    @Override
    public void setParameters(Set<JobParameter> pParameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        if (parameters.isEmpty()) {
            throw new JobParameterMissingException("No parameter provided");
        }
        if (parameters.size() != 1) {
            throw new JobParameterInvalidException("Only one parameter is expected.");
        }
        JobParameter param = parameters.iterator().next();
        if (!(param instanceof ProductJobParameter)) {
            throw new JobParameterInvalidException("Please use ProductJobParameter in place of JobParameter (this "
                    + "class is here to facilitate your life so please use it.");
        }
        super.parameters = parameters;
    }

}
