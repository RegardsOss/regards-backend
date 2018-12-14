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

package fr.cnes.regards.framework.utils.plugins.bean;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * The implementation of {@link ISampleBeanService}.
 * @author Christophe Mertz
 */
@Service
@Primary
public class SampleBeanService implements ISampleBeanService {

    /**
     * A string attribute
     */
    private String str;

    public String getId() {
        return str;
    }

    @Override
    public void setId(String pId) {
        str = pId;
    }

}
