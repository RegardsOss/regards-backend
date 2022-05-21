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
package fr.cnes.regards.modules.search.service;

import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The converter retrieves attributes regarding their names. It may be internal, static or dynamic attributes.
 * And then build sort properties according to attribute properties.
 *
 * @author Marc Sordi
 */
@Service
public class PageableConverter implements IPageableConverter {

    /**
     * Service retrieving the up-to-date list of {@link AttributeModel}s. Autowired by Spring.
     */
    private final IAttributeFinder finder;

    public PageableConverter(IAttributeFinder finder) {
        this.finder = finder;
    }

    @Override
    public Pageable convert(Pageable pageable) throws OpenSearchUnknownParameter {
        if (pageable != null && pageable.getSort() != null) {
            // Do conversion
            Iterator<Order> orders = pageable.getSort().iterator();
            List<Order> convertedOrders = new ArrayList<>();
            while (orders.hasNext()) {
                Order order = orders.next();
                AttributeModel attModel = finder.findByName(order.getProperty());
                convertedOrders.add(new Order(order.getDirection(), attModel.getFullJsonPath()));
            }
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(convertedOrders));
        }

        // Nothing to do
        return pageable;
    }
}
