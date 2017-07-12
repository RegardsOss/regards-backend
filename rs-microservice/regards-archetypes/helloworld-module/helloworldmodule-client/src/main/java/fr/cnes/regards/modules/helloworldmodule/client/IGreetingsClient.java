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
package fr.cnes.regards.modules.helloworldmodule.client;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.helloworldmodule.domain.Greeting;

/**
 *
 * TODO Description
 *
 * @author TODO
 *
 */
@RestClient(name = "MyMicroServiceName") // TODO: change name
@RequestMapping(value = "/api", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IGreetingsClient {

    /**
     * Rest resource /api/greeting/{name} Method GET
     *
     * @param name
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/greeting")
    @ResponseBody
    public HttpEntity<Resource<Greeting>> greeting(String pName);

    /**
     * Rest resource /api/me/{name} Method GET
     *
     * @param name
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/me")
    @ResponseBody
    public HttpEntity<Resource<Greeting>> me(String pName);

}
