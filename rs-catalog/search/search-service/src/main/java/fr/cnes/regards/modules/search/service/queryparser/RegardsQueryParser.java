/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.queryparser;

import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TooManyListenersException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.QueryParserHelper;
import org.apache.lucene.queryparser.flexible.core.config.QueryConfigHandler;
import org.apache.lucene.queryparser.flexible.standard.CommonQueryParserConfiguration;
import org.apache.lucene.queryparser.flexible.standard.config.FuzzyConfig;
import org.apache.lucene.queryparser.flexible.standard.config.LegacyNumericConfig;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler.ConfigurationKeys;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler.Operator;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.apache.lucene.queryparser.flexible.standard.processors.StandardQueryNodeProcessorPipeline;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;

import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.search.service.queryparser.builder.RegardsQueryTreeBuilder;

/**
 * @author Marc Sordi
 *
 */
public class RegardsQueryParser extends QueryParserHelper implements CommonQueryParserConfiguration {

    public RegardsQueryParser() {
        super(new StandardQueryConfigHandler(), new StandardSyntaxParser(),
              new StandardQueryNodeProcessorPipeline(null), new RegardsQueryTreeBuilder());
        setEnablePositionIncrements(true);
    }

    @Override
    public String toString() {
        return "<RegardsQueryParser config=\"" + this.getQueryConfigHandler() + "\"/>";
    }

    /**
     * Overrides {@link QueryParserHelper#parse(String, String)} so it casts the return object to {@link Query}. For
     * more reference about this method, check {@link QueryParserHelper#parse(String, String)}.
     *
     * @param query
     *            the query string
     * @param defaultField
     *            the default field used by the text parser
     *
     * @return the object built from the query
     *
     * @throws QueryNodeException
     *             if something wrong happens along the three phases
     */
    @Override
    public ICriterion parse(final String query, final String defaultField) throws QueryNodeException {
        return (ICriterion) super.parse(query, defaultField);
    }

    /**
     * Gets implicit operator setting, which will be either {@link Operator#AND} or {@link Operator#OR}.
     */
    public StandardQueryConfigHandler.Operator getDefaultOperator() {
        return getQueryConfigHandler().get(ConfigurationKeys.DEFAULT_OPERATOR);
    }

    /**
     * Sets the boolean operator of the QueryParser. In default mode ( {@link Operator#OR}) terms without any modifiers
     * are considered optional: for example <code>capital of Hungary</code> is equal to
     * <code>capital OR of OR Hungary</code>.<br>
     * In {@link Operator#AND} mode terms are considered to be in conjunction: the above mentioned query is parsed as
     * <code>capital AND of AND Hungary</code>
     */
    public void setDefaultOperator(final StandardQueryConfigHandler.Operator operator) {
        getQueryConfigHandler().set(ConfigurationKeys.DEFAULT_OPERATOR, operator);
    }

    /**
     * Set to <code>true</code> to allow leading wildcard characters.
     * <p>
     * When set, <code>*</code> or <code>?</code> are allowed as the first character of a PrefixQuery and WildcardQuery.
     * Note that this can produce very slow queries on big indexes.
     * <p>
     * Default: false.
     */
    @Override
    public void setLowercaseExpandedTerms(final boolean lowercaseExpandedTerms) {
        getQueryConfigHandler().set(ConfigurationKeys.LOWERCASE_EXPANDED_TERMS, lowercaseExpandedTerms);
    }

    /**
     * @see #setLowercaseExpandedTerms(boolean)
     */
    @Override
    public boolean getLowercaseExpandedTerms() {
        final Boolean lowercaseExpandedTerms = getQueryConfigHandler().get(ConfigurationKeys.LOWERCASE_EXPANDED_TERMS);

        if (lowercaseExpandedTerms == null) {
            return true;

        } else {
            return lowercaseExpandedTerms;
        }
    }

    /**
     * Set to <code>true</code> to allow leading wildcard characters.
     * <p>
     * When set, <code>*</code> or <code>?</code> are allowed as the first character of a PrefixQuery and WildcardQuery.
     * Note that this can produce very slow queries on big indexes.
     * <p>
     * Default: false.
     */
    @Override
    public void setAllowLeadingWildcard(final boolean allowLeadingWildcard) {
        getQueryConfigHandler().set(ConfigurationKeys.ALLOW_LEADING_WILDCARD, allowLeadingWildcard);
    }

    /**
     * Set to <code>true</code> to enable position increments in result query.
     * <p>
     * When set, result phrase and multi-phrase queries will be aware of position increments. Useful when e.g. a
     * StopFilter increases the position increment of the token that follows an omitted token.
     * <p>
     * Default: false.
     */
    @Override
    public void setEnablePositionIncrements(final boolean enabled) {
        getQueryConfigHandler().set(ConfigurationKeys.ENABLE_POSITION_INCREMENTS, enabled);
    }

    /**
     * @see #setEnablePositionIncrements(boolean)
     */
    @Override
    public boolean getEnablePositionIncrements() {
        final Boolean enablePositionsIncrements = getQueryConfigHandler()
                .get(ConfigurationKeys.ENABLE_POSITION_INCREMENTS);

        if (enablePositionsIncrements == null) {
            return false;

        } else {
            return enablePositionsIncrements;
        }

    }

    /**
     * By default, it uses {@link MultiTermQuery#CONSTANT_SCORE_REWRITE} when creating a prefix, wildcard and range
     * queries. This implementation is generally preferable because it a) Runs faster b) Does not have the scarcity of
     * terms unduly influence score c) avoids any {@link TooManyListenersException} exception. However, if your
     * application really needs to use the old-fashioned boolean queries expansion rewriting and the above points are
     * not relevant then use this change the rewrite method.
     */
    @Override
    public void setMultiTermRewriteMethod(final MultiTermQuery.RewriteMethod method) {
        getQueryConfigHandler().set(ConfigurationKeys.MULTI_TERM_REWRITE_METHOD, method);
    }

    /**
     * @see #setMultiTermRewriteMethod(org.apache.lucene.search.MultiTermQuery.RewriteMethod)
     */
    @Override
    public MultiTermQuery.RewriteMethod getMultiTermRewriteMethod() {
        return getQueryConfigHandler().get(ConfigurationKeys.MULTI_TERM_REWRITE_METHOD);
    }

    /**
     * Set the fields a query should be expanded to when the field is <code>null</code>
     *
     * @param fields
     *            the fields used to expand the query
     */
    public void setMultiFields(final CharSequence[] fields) {
        CharSequence[] result = fields;
        if (fields == null) {
            result = new CharSequence[0];
        }

        getQueryConfigHandler().set(ConfigurationKeys.MULTI_FIELDS, result);

    }

    /**
     * Returns the fields used to expand the query when the field for a certain query is <code>null</code>
     *
     * @return the fields used to expand the query
     */
    public CharSequence[] getMultiFields() {
        return getQueryConfigHandler().get(ConfigurationKeys.MULTI_FIELDS);
    }

    /**
     * Set the prefix length for fuzzy queries. Default is 0.
     *
     * @param fuzzyPrefixLength
     *            The fuzzyPrefixLength to set.
     */
    @Override
    public void setFuzzyPrefixLength(final int fuzzyPrefixLength) {
        final QueryConfigHandler config = getQueryConfigHandler();
        FuzzyConfig fuzzyConfig = config.get(ConfigurationKeys.FUZZY_CONFIG);

        if (fuzzyConfig == null) {
            fuzzyConfig = new FuzzyConfig();
            config.set(ConfigurationKeys.FUZZY_CONFIG, fuzzyConfig);
        }

        fuzzyConfig.setPrefixLength(fuzzyPrefixLength);

    }

    /**
     * Sets field configuration for legacy numeric fields
     *
     * @deprecated Index with points instead and use {@link #setPointsConfigMap(Map)}
     */
    @Deprecated
    public void setLegacyNumericConfigMap(final Map<String, LegacyNumericConfig> legacyNumericConfigMap) {
        getQueryConfigHandler().set(ConfigurationKeys.LEGACY_NUMERIC_CONFIG_MAP, legacyNumericConfigMap);
    }

    /**
     * Gets field configuration for legacy numeric fields
     *
     * @deprecated Index with points instead and use {@link #getPointsConfigMap()}
     */
    @Deprecated
    public Map<String, LegacyNumericConfig> getLegacyNumericConfigMap() {
        return getQueryConfigHandler().get(ConfigurationKeys.LEGACY_NUMERIC_CONFIG_MAP);
    }

    public void setPointsConfigMap(final Map<String, PointsConfig> pointsConfigMap) {
        getQueryConfigHandler().set(ConfigurationKeys.POINTS_CONFIG_MAP, pointsConfigMap);
    }

    public Map<String, PointsConfig> getPointsConfigMap() {
        return getQueryConfigHandler().get(ConfigurationKeys.POINTS_CONFIG_MAP);
    }

    /**
     * Set locale used by date range parsing.
     */

    @Override
    public void setLocale(final Locale locale) {
        getQueryConfigHandler().set(ConfigurationKeys.LOCALE, locale);
    }

    /**
     * Returns current locale, allowing access by subclasses.
     */

    @Override
    public Locale getLocale() {
        return getQueryConfigHandler().get(ConfigurationKeys.LOCALE);
    }

    @Override
    public void setTimeZone(final TimeZone timeZone) {
        getQueryConfigHandler().set(ConfigurationKeys.TIMEZONE, timeZone);
    }

    @Override
    public TimeZone getTimeZone() {
        return getQueryConfigHandler().get(ConfigurationKeys.TIMEZONE);
    }

    /**
     * Sets the default slop for phrases. If zero, then exact phrase matches are required. Default value is zero.
     */
    @Override
    public void setPhraseSlop(final int defaultPhraseSlop) {
        getQueryConfigHandler().set(ConfigurationKeys.PHRASE_SLOP, defaultPhraseSlop);
    }

    public void setAnalyzer(final Analyzer analyzer) {
        getQueryConfigHandler().set(ConfigurationKeys.ANALYZER, analyzer);
    }

    @Override
    public Analyzer getAnalyzer() {
        return getQueryConfigHandler().get(ConfigurationKeys.ANALYZER);
    }

    /**
     * @see #setAllowLeadingWildcard(boolean)
     */
    @Override
    public boolean getAllowLeadingWildcard() {
        final Boolean allowLeadingWildcard = getQueryConfigHandler().get(ConfigurationKeys.ALLOW_LEADING_WILDCARD);

        if (allowLeadingWildcard == null) {
            return false;

        } else {
            return allowLeadingWildcard;
        }
    }

    /**
     * Get the minimal similarity for fuzzy queries.
     */
    @Override
    public float getFuzzyMinSim() {
        final FuzzyConfig fuzzyConfig = getQueryConfigHandler().get(ConfigurationKeys.FUZZY_CONFIG);

        if (fuzzyConfig == null) {
            return FuzzyQuery.defaultMinSimilarity;
        } else {
            return fuzzyConfig.getMinSimilarity();
        }
    }

    /**
     * Get the prefix length for fuzzy queries.
     *
     * @return Returns the fuzzyPrefixLength.
     */
    @Override
    public int getFuzzyPrefixLength() {
        final FuzzyConfig fuzzyConfig = getQueryConfigHandler().get(ConfigurationKeys.FUZZY_CONFIG);

        if (fuzzyConfig == null) {
            return FuzzyQuery.defaultPrefixLength;
        } else {
            return fuzzyConfig.getPrefixLength();
        }
    }

    /**
     * Gets the default slop for phrases.
     */
    @Override
    public int getPhraseSlop() {
        final Integer phraseSlop = getQueryConfigHandler().get(ConfigurationKeys.PHRASE_SLOP);

        if (phraseSlop == null) {
            return 0;

        } else {
            return phraseSlop;
        }
    }

    /**
     * Set the minimum similarity for fuzzy queries. Default is defined on {@link FuzzyQuery#defaultMinSimilarity}.
     */
    @Override
    public void setFuzzyMinSim(final float fuzzyMinSim) {
        final QueryConfigHandler config = getQueryConfigHandler();
        FuzzyConfig fuzzyConfig = config.get(ConfigurationKeys.FUZZY_CONFIG);

        if (fuzzyConfig == null) {
            fuzzyConfig = new FuzzyConfig();
            config.set(ConfigurationKeys.FUZZY_CONFIG, fuzzyConfig);
        }

        fuzzyConfig.setMinSimilarity(fuzzyMinSim);
    }

    /**
     * Sets the boost used for each field.
     *
     * @param boosts
     *            a collection that maps a field to its boost
     */
    public void setFieldsBoost(final Map<String, Float> boosts) {
        getQueryConfigHandler().set(ConfigurationKeys.FIELD_BOOST_MAP, boosts);
    }

    /**
     * Returns the field to boost map used to set boost for each field.
     *
     * @return the field to boost map
     */
    public Map<String, Float> getFieldsBoost() {
        return getQueryConfigHandler().get(ConfigurationKeys.FIELD_BOOST_MAP);
    }

    /**
     * Sets the default {@link Resolution} used for certain field when no {@link Resolution} is defined for this field.
     *
     * @param dateResolution
     *            the default {@link Resolution}
     */
    @Override
    public void setDateResolution(final DateTools.Resolution dateResolution) {
        getQueryConfigHandler().set(ConfigurationKeys.DATE_RESOLUTION, dateResolution);
    }

    /**
     * Returns the default {@link Resolution} used for certain field when no {@link Resolution} is defined for this
     * field.
     *
     * @return the default {@link Resolution}
     */
    public DateTools.Resolution getDateResolution() {
        return getQueryConfigHandler().get(ConfigurationKeys.DATE_RESOLUTION);
    }

    /**
     * Returns the field to {@link Resolution} map used to normalize each date field.
     *
     * @return the field to {@link Resolution} map
     */
    public Map<CharSequence, DateTools.Resolution> getDateResolutionMap() {
        return getQueryConfigHandler().get(ConfigurationKeys.FIELD_DATE_RESOLUTION_MAP);
    }

    /**
     * Sets the {@link Resolution} used for each field
     *
     * @param dateRes
     *            a collection that maps a field to its {@link Resolution}
     */
    public void setDateResolutionMap(final Map<CharSequence, DateTools.Resolution> dateRes) {
        getQueryConfigHandler().set(ConfigurationKeys.FIELD_DATE_RESOLUTION_MAP, dateRes);
    }

}
