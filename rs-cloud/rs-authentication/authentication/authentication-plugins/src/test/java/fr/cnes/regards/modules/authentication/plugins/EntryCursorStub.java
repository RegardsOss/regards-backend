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
package fr.cnes.regards.modules.authentication.plugins;

import org.apache.directory.api.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.SearchResultDone;

import java.util.Iterator;
import java.util.List;

/**
 * Class EntryCursorStub
 * <p>
 * Stub class to test LDAP authentication plugin
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
public class EntryCursorStub implements EntryCursor {

    /**
     * LDAP parameters
     */
    private List<Entry> entries;

    /**
     * stub counter
     */
    private int count = 0;

    public void setEntries(final List<Entry> pEntries) {
        entries = pEntries;
    }

    @Override
    public Iterator<Entry> iterator() {
        return entries.iterator();
    }

    @Override
    public String toString(final String pTabs) {
        return "";
    }

    @Override
    public void setClosureMonitor(final ClosureMonitor pMonitor) {
    }

    @Override
    public boolean previous() {
        return false;
    }

    @Override
    public boolean next() {
        return count < entries.size();
    }

    @Override
    public boolean last() {
        return false;
    }

    @Override
    public boolean isLast() {
        return false;
    }

    @Override
    public boolean isFirst() {
        return false;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isBeforeFirst() {
        return false;
    }

    @Override
    public boolean isAfterLast() {
        return false;
    }

    @Override
    public Entry get() {
        final Entry result = entries.get(count);
        count++;
        return result;
    }

    @Override
    public boolean first() {
        return false;
    }

    @Override
    public void close(final Exception pReason) {
    }

    @Override
    public void close() {
    }

    @Override
    public void beforeFirst() {
    }

    @Override
    public void before(final Entry pElement) {
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public void afterLast() {
    }

    @Override
    public void after(final Entry pElement) {
    }

    @Override
    public SearchResultDone getSearchResultDone() {
        return null;
    }

    @Override
    public int getMessageId() {
        return 0;
    }
}
