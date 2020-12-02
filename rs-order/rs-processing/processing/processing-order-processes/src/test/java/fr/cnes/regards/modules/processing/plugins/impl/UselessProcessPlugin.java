/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.plugins.impl;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.processing.domain.forecast.IRunningDurationForecast;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Try;
import reactor.core.publisher.Mono;

@Plugin(id = "UselessProcessPlugin",
        version = "1.0.0-SNAPSHOT",
        description = "UselessProcessPlugin description",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class UselessProcessPlugin implements IProcessDefinition {

    @Override public Map<String, String> processInfo() {
        return HashMap.empty();
    }

    @Override public Try<IResultSizeForecast> sizeForecast() {
        return Try.success(IResultSizeForecast.zeroSize());
    }

    @Override public Try<IRunningDurationForecast> durationForecast() {
        return Try.success(IRunningDurationForecast.defaultDuration());
    }

    @Override public IExecutable executable() {
        return IExecutable.wrap(Mono::just);
    }

    @Override public Seq<ExecutionParameterDescriptor> parameters() {
        return List.empty();
    }
}
