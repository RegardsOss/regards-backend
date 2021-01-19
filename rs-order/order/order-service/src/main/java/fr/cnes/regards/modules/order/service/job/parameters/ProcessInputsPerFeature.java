/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.service.job.parameters;

import fr.cnes.regards.modules.order.domain.OrderDataFile;

import java.util.List;
import java.util.Map;


/**
 * This class is a wrapper around a map, used as a Job Parameter
 * in {@link fr.cnes.regards.modules.order.service.job.parameters.ProcessInputsPerFeatureJobParameter}.
 *
 * The wrapper allows for an easy ser/deser by Gson without having to deal
 * with type adapters for the map generic type parameters.
 *
 * @author Guillaume Andrieu
 */
@lombok.Value
public class ProcessInputsPerFeature {

    Map<ProcessOutputFeatureDesc, List<OrderDataFile>> filesPerFeature;
}
