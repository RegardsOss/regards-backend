#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package ${package}.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;

import ${package}.domain.Greeting;
import ${package}.service.GreetingsService;

/**
 * REST module controller
 *
 * TODO Description
 * @author TODO
 */
@RestController
@RequestMapping("/api")
public class GreetingsController implements IResourceController<Greeting> {

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private GreetingsService myService;

    @RequestMapping(value = "/greeting", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send 'greeting' as response")
    public HttpEntity<Resource<Greeting>> greeting(@RequestParam(value = "name", defaultValue = "World") String pName) {
        Greeting greeting = myService.getGreeting(pName);
        return new ResponseEntity<>(new Resource<Greeting>(greeting), HttpStatus.OK);
    }

    @RequestMapping(value = "/me", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send 'me' as response")
    public HttpEntity<Resource<Greeting>> me(@RequestParam(value = "name", defaultValue = "me") String pName) {
        Greeting greeting = myService.getGreeting(pName);
        return new ResponseEntity<>(new Resource<Greeting>(greeting), HttpStatus.OK);
    }

    @Override
    public Resource<Greeting> toResource(Greeting pElement, Object... pExtras) {
        // TODO add hateoas links
        return resourceService.toResource(pElement);
    }
}
