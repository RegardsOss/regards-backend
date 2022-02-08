/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.dto.hateoas;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Extend the {@link PagedModel} to add a "info" field.
 * @param <T> The type of the resources
 * @author Sébastien Binda
 */
public class RequestsPagedModel<T> extends PagedModel<T> {

    private final RequestsInfo info;

    public RequestsPagedModel(RequestsInfo info, Collection<T> content, PageMetadata metadata, Link... links) {
        this(info, content, metadata, Arrays.asList(links));
    }

    public RequestsPagedModel(RequestsInfo info, Collection<T> content, PageMetadata metadata, Iterable<Link> pLinks) {
        super(content, metadata, pLinks);
        this.info = info;
    }

    public RequestsInfo getInfo() {
        return this.info;
    }

    @SuppressWarnings("unchecked")
    public static <T extends EntityModel<S>, S> RequestsPagedModel<T> wrap(Iterable<S> content, PageMetadata metadata,
            RequestsInfo info) {
        ArrayList<T> resources = new ArrayList<T>();

        if (content != null) {
            for (S element : content) {
                if (element != null) {
                    resources.add((T) EntityModel.of(element));
                }
            }
        }

        return new RequestsPagedModel<T>(info, resources, metadata);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = (prime * result) + (info == null ? 0 : info.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RequestsPagedModel<?> other = (RequestsPagedModel<?>) obj;
        if (info == null) {
            if (other.info != null) {
                return false;
            }
        } else if (!info.equals(other.info)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("RequestsPagedResources { content: %s, metadata: %s, links: %s, info: %s }", getContent(),
                             getMetadata(), getLinks(), getInfo());
    }
}
