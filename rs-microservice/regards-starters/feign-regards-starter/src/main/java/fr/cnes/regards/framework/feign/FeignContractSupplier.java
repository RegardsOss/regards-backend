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
package fr.cnes.regards.framework.feign;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.cloud.openfeign.annotation.PathVariableParameterProcessor;
import org.springframework.cloud.openfeign.annotation.RequestHeaderParameterProcessor;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

import feign.Contract;

/**
 * Supply the custom SpringMvcContract to use with Feign.
 * @author Xavier-Alexandre Brochard
 */
public class FeignContractSupplier implements Supplier<Contract> {

    @Override
    public Contract get() {
        return new SpringMvcContract(getCustomAnnotatedArgumentsProcessors());
    }

    /**
     * Customize the default AnnotatedArgumentsProcessors in order to use
     * our CustomRequestParamParameterProcessor instead of the RequestParamParameterProcessor
     * @return the list of processors
     */
    private List<AnnotatedParameterProcessor> getCustomAnnotatedArgumentsProcessors() {
        List<AnnotatedParameterProcessor> annotatedArgumentResolvers = new ArrayList<>();

        annotatedArgumentResolvers.add(new PathVariableParameterProcessor());
        annotatedArgumentResolvers.add(new CustomRequestParamParameterProcessor());
        annotatedArgumentResolvers.add(new RequestHeaderParameterProcessor());

        return annotatedArgumentResolvers;
    }

}
