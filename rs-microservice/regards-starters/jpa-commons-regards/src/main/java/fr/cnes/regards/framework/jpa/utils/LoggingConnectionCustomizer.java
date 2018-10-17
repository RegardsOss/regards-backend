package fr.cnes.regards.framework.jpa.utils;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.AbstractConnectionCustomizer;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class LoggingConnectionCustomizer extends AbstractConnectionCustomizer {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingConnectionCustomizer.class);

    @Override
    public void onAcquire(Connection c, String parentDataSourceIdentityToken) throws Exception {
        super.onAcquire(c, parentDataSourceIdentityToken);
        LOG.info("Database connection to acquired");
    }

    @Override
    public void onDestroy(Connection c, String parentDataSourceIdentityToken) throws Exception {
        super.onDestroy(c, parentDataSourceIdentityToken);
        LOG.info("Database connection released");
    }
}
