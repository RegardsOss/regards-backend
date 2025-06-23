package fr.cnes.regards.framwork.logger;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Utils class for logger
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public final class LogConstants {

    private static final String SECURITY_MARKER_LABEL = "SECURITY";

    public static final Marker SECURITY_MARKER = MarkerFactory.getMarker(SECURITY_MARKER_LABEL);

    private LogConstants() {
        // Nothing to do
    }
}
