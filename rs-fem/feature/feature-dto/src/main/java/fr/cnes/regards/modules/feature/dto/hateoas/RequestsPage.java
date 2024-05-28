/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.feature.dto.RequestInfo;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Page implementation with {@link RequestInfo} values
 *
 * @param <T> the type of which the page consists.
 * @author Sébastien Binda
 */

public class RequestsPage<T> extends PageImpl<T> {

    private final RequestsInfo info;

    private final Pageable pageable;

    public RequestsPage(List<T> content, RequestsInfo info, Pageable pageable, long total) {
        super(content, pageable, total);
        this.pageable = pageable;
        this.info = info;

    }

    public RequestsPage(List<T> content, RequestsInfo info) {
        super(content);
        this.pageable = null;
        this.info = info;
    }

    public RequestsInfo getInfo() {
        return info;
    }

    @Override
    public Pageable getPageable() {
        return pageable;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return super.equals(obj);
    }
}
