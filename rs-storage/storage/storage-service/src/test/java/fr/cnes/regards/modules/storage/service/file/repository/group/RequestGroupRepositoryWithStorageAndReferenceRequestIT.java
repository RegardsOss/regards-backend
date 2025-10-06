/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */

package fr.cnes.regards.modules.storage.service.file.repository.group;

import fr.cnes.regards.modules.storage.dao.IRequestGroupRepository;
import fr.cnes.regards.modules.storage.domain.database.request.RequestGroup;
import org.junit.Before;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Purpose is to test {@link IRequestGroupRepository#findGroupDones}, on groups composed either of
 * reference request or storage request.<br>
 * All the tests annotated with @Test are in {@link AbstractRequestGroupRepositoryIT}.<br/>
 * Setup only group of reference request.<br/>
 * The used finder is {@link IRequestGroupRepository#findGroupDones}, it retrieves
 * terminated group of any type of request. An empty group is considered terminated. A group composed only
 * of terminated request is also considered as terminated.
 *
 * @author Olivier Navarro
 **/
public class RequestGroupRepositoryWithStorageAndReferenceRequestIT extends AbstractRequestGroupRepositoryIT {

    /**
     * create 2 group of reference request: group1, group2.<br/>
     * create 2 group of storage request: group3, group4.<br/>
     */
    @Before
    public void setUpGroups() {
        // group1 group2 are group of reference request
        Stream.of("group1", "group2").map(this::newReferenceGroup).forEach(groupRepository::save);
        // group3 group4 are group of storage request
        Stream.of("group3", "group4").map(this::newStorageGroup).forEach(groupRepository::save);
        assumeThat(groupRepository.findAll()).hasSize(4);
    }

    /**
     * The used finder is {@link IRequestGroupRepository#findGroupDones}, it retrieves
     * terminated group of any type of request.
     */
    public final Set<String> findGroupIds() {
        return groupRepository.findGroupDones(10).stream().map(RequestGroup::getId).collect(Collectors.toSet());
    }

}
