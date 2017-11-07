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
package fr.cnes.regards.framework.jpa.multitenant.utils;

import java.sql.Connection;

import com.mchange.v2.c3p0.AbstractConnectionTester;

/**
 * FIXME
 * @author Marc Sordi
 *
 */
public class MultitenantConnectionTester extends AbstractConnectionTester {

    /*
     * (non-Javadoc)
     * 
     * @see com.mchange.v2.c3p0.AbstractConnectionTester#activeCheckConnection(java.sql.Connection, java.lang.String,
     * java.lang.Throwable[])
     */
    @Override
    public int activeCheckConnection(Connection c, String preferredTestQuery, Throwable[] rootCauseOutParamHolder) {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mchange.v2.c3p0.AbstractConnectionTester#statusOnException(java.sql.Connection, java.lang.Throwable,
     * java.lang.String, java.lang.Throwable[])
     */
    @Override
    public int statusOnException(Connection c, Throwable t, String preferredTestQuery,
            Throwable[] rootCauseOutParamHolder) {
        // TODO Auto-generated method stub
        return 0;
    }

}
