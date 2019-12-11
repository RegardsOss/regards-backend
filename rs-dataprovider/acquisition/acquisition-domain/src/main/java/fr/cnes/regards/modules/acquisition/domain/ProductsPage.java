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
package fr.cnes.regards.modules.acquisition.domain;

/**
 * POJO to handle schedule of {@link Product}s by page.
 *
 * @author Sébastien Binda
 *
 */
public class ProductsPage {

    /**
     * Does still remains {@link Product}s to schedule ?
     */
    private boolean next = false;

    /**
     * Number of scheduled {@link Product}s on this iteration.
     */
    private long scheduled = 0L;

    /**
     *
     * @param next Does still remains {@link Product}s to schedule ?
     * @param scheduled Number of scheduled {@link Product}s on this iteration.
     * @return {@link ProductsPage}
     */
    public static ProductsPage build(boolean next, long scheduled) {
        ProductsPage page = new ProductsPage();
        page.next = next;
        page.scheduled = scheduled;
        return page;
    }

    /**
     * Does still remains {@link Product}s to schedule ?
     * @return boolean
     */
    public boolean hasNext() {
        return next;
    }

    /**
     * Number of scheduled {@link Product}s on this iteration.
     * @return long
     */
    public long getScheduled() {
        return scheduled;
    }

}
