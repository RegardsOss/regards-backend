/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.api.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchResultDone;

/**
 *
 * Class EntryCursorStub
 *
 * Stub class to test LDAP authentication plugin
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
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
    public boolean previous() throws LdapException, CursorException, IOException {
        return false;
    }

    @Override
    public boolean next() throws LdapException, CursorException, IOException {
        return count < entries.size();
    }

    @Override
    public boolean last() throws LdapException, CursorException, IOException {
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
    public Entry get() throws CursorException, IOException {
        final Entry result = entries.get(count);
        count++;
        return result;
    }

    @Override
    public boolean first() throws LdapException, CursorException, IOException {
        return false;
    }

    @Override
    public void close(final Exception pReason) {
    }

    @Override
    public void close() {
    }

    @Override
    public void beforeFirst() throws LdapException, CursorException, IOException {
    }

    @Override
    public void before(final Entry pElement) throws LdapException, CursorException, IOException {
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public void afterLast() throws LdapException, CursorException, IOException {
    }

    @Override
    public void after(final Entry pElement) throws LdapException, CursorException, IOException {
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
