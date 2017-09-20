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
package fr.cnes.regards.framework.modules.plugins.service;

import java.util.List;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * Example plugin for complex parameter types instanciation.
 *
 * @author sbinda
 *
 */
@Plugin(description = "Complex Plugin de test", id = "complexPlugin", version = "0.0.1", author = "REGARDS Dream Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class TestPlugin implements ITestPlugin {

    public final static String POJO_PARAM_NAME = "pojo";

    /**
     * Primitive parameter
     */
    @PluginParameter(name = "stringParam", description = "la description", optional = true)
    private String stringParam;

    /**
     * Object parameter
     */
    @PluginParameter(name = POJO_PARAM_NAME, description = "la description", optional = true)
    private TestPojo pojoParam;

    /**
     * Parameterized object parameter
     */
    @PluginParameter(name = "pojoParams", description = "la description", optional = true)
    private List<TestPojo> pojoParams;

    @Override
    public TestPojo getPojoParam() {
        return this.pojoParam;
    }

}