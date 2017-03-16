/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.datasources.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.datasources.plugins.interfaces.IDBConnectionPlugin;
import fr.cnes.regards.modules.entities.domain.DataObject;

/**
 * A {@link Plugin} to retrieve the data elements from a SQL Database.</br>
 * This {@link Plugin} used a {@link IDBConnectionPlugin} to define to connection to the Database.
 *
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
public abstract class AbstractDataSourcePlugin extends AbstractDataObjectMapping {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataSourcePlugin.class);

    protected static final String SELECT = "SELECT ";

    protected static final String WHERE = " WHERE ";

    protected static final String COMMA = ", ";

    protected static final String SELECT_COUNT = "SELECT COUNT(*) ";

    protected static final String AS = "as";

    protected static final String LIMIT_CLAUSE = " ORDER BY %s LIMIT %d OFFSET %d";

    public abstract IDBConnectionPlugin getDBConnection() throws SQLException;

    // TODO CMZ à voir si utile, sinon à virer
    public int getRefreshRate() {
        // in seconds, 30 minutes
        return 1800;
    }

    // TODO CMZ à voir si utile, sinon à virer
    public boolean isOutOfDate() {
        boolean outDated = true;

        // TODO compute the out dated value

        if (outDated) {
            this.reset();
        }

        return outDated;
    }

    protected abstract String getFromClause();

    protected String getSelectRequest(Pageable pPageable, LocalDateTime pDate) {
        if (pDate != null) {
            return SELECT + buildColumnClause(columns.toArray(new String[0])) + getFromClause() + WHERE
                    + this.keywordLastModificationDate + buildLimitPart(pPageable);
        } else {
            return SELECT + buildColumnClause(columns.toArray(new String[0])) + getFromClause()
                    + buildLimitPart(pPageable);
        }
    }

    protected String getCountRequest(LocalDateTime pDate) {
        if (pDate != null) {
            return SELECT_COUNT + getFromClause() + WHERE + this.keywordLastModificationDate;

        } else {
            return SELECT_COUNT + getFromClause();
        }

    }

    /**
     * 
     * @param pTenant
     * @param pPageable
     * @return
     */
    public Page<DataObject> findAll(String pTenant, Pageable pPageable) {
        return findAll(pTenant, pPageable, null);
    }

    /**
     * 
     * @param pTenant
     * @param pPageable
     * @param pDate
     * @return
     */
    public Page<DataObject> findAll(String pTenant, Pageable pPageable, LocalDateTime pDate) {

        final String selectRequest = getSelectRequest(pPageable, pDate);
        final String countRequest = getCountRequest(pDate);

        LOG.debug("select request :" + selectRequest);
        LOG.debug("count request :" + countRequest);

        try (Connection conn = getDBConnection().getConnection()) {

            return findAll(pTenant, conn, selectRequest, countRequest, pPageable, pDate);

        } catch (SQLException e) {
            LOG.error("Unable to obtain a database connection.", e);
            return null;
        }
    }

    /**
     * Add to the SQL request the part to fetch only a portion of the results.
     * 
     * @param pPage
     *            the page of the element to fetch
     * @return the SQL request
     */
    protected String buildLimitPart(Pageable pPage) {
        StringBuilder str = new StringBuilder(" ");
        final int offset = pPage.getPageNumber() * pPage.getPageSize();
        final String limit = String.format(LIMIT_CLAUSE, orderByColumn, pPage.getPageSize(), offset);
        str.append(limit);

        return str.toString();
    }
}
