package fr.cnes.regards.modules.search.service.queryparser;

import org.apache.lucene.queryparser.flexible.messages.NLS;

/**
 * Flexible Query Parser message bundle class. The messages can be found in
 * <a>resources/RegardsQueryParserMessages.properties</a>
 *
 *
 *
 * @author Xavier-Alexandre Brochard
 */
public class RegardsQueryParserMessages extends NLS {

    private static final String BUNDLE_NAME = RegardsQueryParserMessages.class.getName();

    private RegardsQueryParserMessages() {
        // Do not instantiate
    }

    static {
        // register all string ids with NLS class and initialize static string
        // values
        NLS.initializeMessages(BUNDLE_NAME, RegardsQueryParserMessages.class);
    }

    // static string must match the strings in the property files.
    public static String FIELD_TYPE_UNDETERMINATED;

    public static String UNSUPPORTED_ATTRIBUTE_TYPE;

    public static String MIDDLE_WILDCARD_NOT_ALLOWED;;
}
