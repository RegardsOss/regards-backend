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

package fr.cnes.regards.modules.datasources.utils;

import com.google.common.collect.Maps;
import com.google.gson.stream.JsonReader;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.datasources.domain.AbstractAttributeMapping;
import fr.cnes.regards.modules.datasources.domain.DataSourceModelMapping;
import fr.cnes.regards.modules.datasources.domain.ModelMappingAdapter;
import fr.cnes.regards.modules.datasources.domain.Table;
import fr.cnes.regards.modules.datasources.plugins.exception.DataSourceException;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.entities.domain.attribute.DateAttribute;
import fr.cnes.regards.modules.entities.domain.attribute.StringAttribute;
import fr.cnes.regards.modules.entities.domain.attribute.builder.AttributeBuilder;
import fr.cnes.regards.modules.entities.domain.converter.GeometryAdapter;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.indexer.domain.DataType;
import fr.cnes.regards.modules.models.domain.Model;
import org.elasticsearch.common.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.*;
import java.util.*;

/**
 * This class allows to process a SQL request to a SQL Database.</br>
 * For each data reads in the Database, a {@link DataObject} is created. This {@link DataObject} are compliant with a
 * {@link Model}.</br>
 * Some attributes extracts from the Database are specials. For each one, a {@link DataObject} property is set :
 * <li>the primary key of the data
 * <li>the data file of the data
 * <li>the thumbnail of the data
 * <li>the update date of the data
 *
 * @author Christophe Mertz
 */
public abstract class AbstractDataObjectMapping {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataObjectMapping.class);

    /**
     * The PL/SQL key word AS
     */
    protected static final String AS = "as ";

    private static final String BLANK = " ";

    /**
     * A comma used to build the select clause
     */
    private static final String COMMA = ",";

    /**
     * A default date
     */
    private static final OffsetDateTime INIT_DATE = OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private static final GeometryAdapter<?> GEOMETRY_ADAPTER = new GeometryAdapter<>();

    /**
     * A default value to indicates that the count request should be execute
     */
    private static final int RESET_COUNT = -1;

    /**
     * A pattern used to set a date in the statement
     */
    protected static final String LAST_MODIFICATION_DATE_KEYWORD = "%last_modification_date%";

    /**
     * The {@link List} of columns used by this {@link Plugin} to requests the database. This columns are in the
     * {@link Table}.
     */
    protected List<String> columns;

    /**
     * The column name used in the ORDER BY clause
     */
    protected String orderByColumn = "";

    /**
     * The mapping between the attributes in the {@link Model} and the data source
     */
    protected DataSourceModelMapping dataSourceMapping;

    /**
     * The result of the count request
     */
    private int nbItems = RESET_COUNT;

    /**
     * The attribute name used for the date comparison
     */
    private String lastUpdateAttributeName = "";

    /**
     * For each attributes of the data source, this map contains an optional internal attribute type
     */
    private Map<String, InternalAttributes> mappingInternalAttributes = null;

    /**
     * Get {@link DateAttribute}.
     *
     * @param rs the {@link ResultSet}
     * @param attrName the attribute name
     * àparam attrDSName the column name in the external data source
     * @param colName the column name in the {@link ResultSet}
     * @return a new {@link DateAttribute}
     * @throws SQLException if an error occurs in the {@link ResultSet}
     */
    protected abstract AbstractAttribute<?> buildDateAttribute(ResultSet rs, String attrName, String attrDSName,
            String colName) throws SQLException;

    /**
     * Get a {@link LocalDateTime} value from a {@link ResultSet} for a {@link AbstractAttributeMapping}.
     *
     * @param rs The {@link ResultSet} to read
     * @param colName the column name in the {@link ResultSet}
     * @return the {@link OffsetDateTime}
     * @throws SQLException An error occurred when try to read the {@link ResultSet}
     */
    protected OffsetDateTime buildOffsetDateTime(ResultSet rs, String colName) throws SQLException {
        long n = rs.getTimestamp(colName).getTime();
        Instant instant = Instant.ofEpochMilli(n);
        return OffsetDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }

    /**
     * Returns a page of DataObject from the database defined by the {@link Connection} and corresponding to the SQL. A
     * {@link Date} is apply to filter the {@link DataObject} created or updated after this {@link Date}.
     *
     * @param pTenant the tenant name
     * @param pConn a {@link Connection} to a database
     * @param pSelectRequest the SQL request
     * @param pCountRequest the SQL count request
     * @param pPageable the page information
     * @param pDate a {@link Date} used to apply returns the {@link DataObject} update or create after this date
     * @return a page of {@link DataObject}
     */
    protected Page<DataObject> findAll(String pTenant, Connection pConn, String pSelectRequest, String pCountRequest,
            Pageable pPageable, OffsetDateTime pDate) throws DataSourceException {
        List<DataObject> dataObjects = new ArrayList<>();

        try (Statement statement = pConn.createStatement()) {

            String selectRequest = pSelectRequest;
            String countRequest = pCountRequest;

            if (pDate != null) {
                selectRequest = buildDateStatement(selectRequest, pDate);
                countRequest = buildDateStatement(countRequest, pDate);
            }
            LOG.info("select request : " + selectRequest);
            LOG.info("count request : " + countRequest);

            // Retrieve the model
            Model model = new Model();
            model.setId(dataSourceMapping.getModel());

            // Execute the request to get the elements
            try (ResultSet rs = statement.executeQuery(selectRequest)) {
                while (rs.next()) {
                    dataObjects.add(processResultSet(rs, model));
                }
            }

            countItems(statement, countRequest);

            statement.close();
        } catch (SQLException e) {
            LOG.error("Error while retrieving or counting datasource elements", e);
            throw new DataSourceException("Error while retrieving or counting datasource elements", e);
        }

        return new PageImpl<>(dataObjects, pPageable, nbItems);
    }

    /**
     * Execute a SQL request to count the number of items
     *
     * @param pStatement a {@link Statement} used to execute the SQL request
     * @param pCountRequest the SQL count request
     * @throws SQLException an SQL error occurred
     */
    private void countItems(Statement pStatement, String pCountRequest) throws SQLException {
        if ((pCountRequest != null) && !pCountRequest.isEmpty() && (nbItems == RESET_COUNT)) {
            // Execute the request to count the elements
            try (ResultSet rsCount = pStatement.executeQuery(pCountRequest)) {
                if (rsCount.next()) {
                    nbItems = rsCount.getInt(1);
                }
            }
        }
    }

    /**
     * Build a {@link DataObject} for a {@link ResultSet}.
     *
     * @param resultSet the {@link ResultSet}
     * @return the {@link DataObject} created
     * @throws SQLException An SQL error occurred
     */
    protected DataObject processResultSet(ResultSet resultSet, Model model) throws SQLException {
        final DataObject data = new DataObject();

        final Set<AbstractAttribute<?>> attributes = new HashSet<>();
        final Map<String, List<AbstractAttribute<?>>> spaceNames = Maps.newHashMap();

        /**
         * Loop the attributes in the mapping
         */
        for (AbstractAttributeMapping attrMapping : dataSourceMapping.getAttributesMapping()) {

            AbstractAttribute<?> attr = buildAttribute(resultSet, attrMapping);

            if (attr != null) {

                if (attrMapping.isMappedToStaticProperty()) {
                    // static attribute mapping
                    processStaticAttributes(data, attr, attrMapping);
                } else {
                    // dynamic attribute mapping
                    if (!Strings.isNullOrEmpty(attrMapping.getNameSpace())) {
                        if (!spaceNames.containsKey(attrMapping.getNameSpace())) {
                            // It is a new name space
                            spaceNames.put(attrMapping.getNameSpace(), new ArrayList<>());
                        }
                        // Add the attribute to the namespace
                        spaceNames.get(attrMapping.getNameSpace()).add(attr);
                    } else {
                        attributes.add(attr);
                    }
                }
            }
        }

        /**
         * For each name space, add an ObjectAttribute to the list of attribute
         */
        spaceNames.forEach((pName, pAttrs) -> {
            attributes
                    .add(AttributeBuilder.buildObject(pName, pAttrs.toArray(new AbstractAttribute<?>[pAttrs.size()])));
        });

        data.setProperties(attributes);
        data.setModel(model);

        return data;
    }

    /**
     * Get an attribute define in the mapping in a {@link ResultSet}
     *
     * @param rs the {@link ResultSet}
     * @param attrMapping the {@link AbstractAttributeMapping}
     * @return a new {@link AbstractAttribute}
     * @throws SQLException if an error occurs in the {@link ResultSet}
     */
    private AbstractAttribute<?> buildAttribute(ResultSet rs, AbstractAttributeMapping attrMapping)
            throws SQLException {
        AbstractAttribute<?> attr = null;

        final String colName = extractColumnName(attrMapping.getNameDS(), attrMapping.getName(),
                                                 attrMapping.isPrimaryKey());

        switch (attrMapping.getType()) {
            case STRING:
                attr = AttributeBuilder.buildString(attrMapping.getName(), rs.getString(colName));
                break;
            case LONG:
                attr = AttributeBuilder.buildLong(attrMapping.getName(), rs.getLong(colName));
                break;
            case INTEGER:
                attr = AttributeBuilder.buildInteger(attrMapping.getName(), rs.getInt(colName));
                break;
            case BOOLEAN:
                attr = AttributeBuilder.buildBoolean(attrMapping.getName(), rs.getBoolean(colName));
                break;
            case DOUBLE:
                attr = AttributeBuilder.buildDouble(attrMapping.getName(), rs.getDouble(colName));
                break;
            case DATE_ISO8601:
                attr = buildDateAttribute(rs, attrMapping.getName(), attrMapping.getNameDS(), colName);
                break;
            default:
                break;
        }

        // If value was null => no attribute value
        if (rs.wasNull()) {
            return null;
        }

        if (LOG.isDebugEnabled() && (attr != null)) {
            if ((attrMapping.getName() != null) && attrMapping.getName().equals(attrMapping.getNameDS())) {
                LOG.debug("the value for <" + attrMapping.getName() + "> of type <" + attrMapping.getType() + "> is :"
                        + attr.getValue());

            } else {
                LOG.debug("the value for <" + attrMapping.getName() + "|" + attrMapping.getNameDS() + "> of type <"
                        + attrMapping.getType() + "> is :" + attr.getValue());
            }
        }

        return attr;
    }

    /**
     * Extracts a column name from a PL/SQL expression.</br>
     * The column label can be placed after the word 'AS'.
     * If 'AS' is not present the column name is the internal attribute name. 
     *
     * @param attrDataSourceName The PL/SQL expression to analyze
     * @param attrName The attribute name
     * @return the column label extracted from the PL/SQL
     */
    protected String extractColumnName(String attrDataSourceName, String attrName, boolean isPrimaryKey) {
        String colName = "";

        int pos = attrDataSourceName.toLowerCase().lastIndexOf(AS);

        if (pos > 0) {
            String str = attrDataSourceName.substring(pos + AS.length()).trim();
            if (LOG.isDebugEnabled()) {
                LOG.debug("the extracted column name is : <" + str + ">");
            }
            colName = str;
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("the extracted column name is : <" + attrName + ">");
            }
            if (isPrimaryKey) {
                colName = attrDataSourceName;
            } else {
                colName = attrName;
            }
        }

        return colName;
    }

    /**
     * This class extracts data information from an attribute and sets this informations into the
     * {@link DataObject}.</br>
     * The REGARDS internal attributes's that are analyzed :
     * <li>primary key
     * <li>raw data
     * <li>thumbnail
     * <li>label
     * <li>last update date
     * <li>geometry
     *
     * @param pData the current {@link DataObject} to build
     * @param pAttr the current {@link AbstractAttribute} to analyze
     * @param pAttrMapping the {@link AbstractAttributeMapping} for the current attribute
     */
    private void processStaticAttributes(DataObject pData, AbstractAttribute<?> pAttr,
            AbstractAttributeMapping pAttrMapping) {
        if (pAttrMapping.isPrimaryKey()) {
            String val = pAttr.getValue().toString();
            pData.setSipId(val);
        }
        if (pAttrMapping.isRawData() || pAttrMapping.isThumbnail()) {
            String str = ((StringAttribute) pAttr).getValue();
            try {
                DataType type = pAttrMapping.isRawData() ? DataType.RAWDATA : DataType.THUMBNAIL;
                DataFile dataFile = new DataFile();
                dataFile.setDataType(type);
                dataFile.setFileRef(new URI(str));
                pData.addFile(dataFile);
            } catch (URISyntaxException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        if (pAttrMapping.isLastUpdate()) {
            pData.setLastUpdate((OffsetDateTime) pAttr.getValue());
        }
        if (pAttrMapping.isLabel()) {
            pData.setLabel(((StringAttribute) pAttr).getValue());
        }
        if (pAttrMapping.isGeometry()) {
            String str = ((StringAttribute) pAttr).getValue();
            try {
                pData.setGeometry(GEOMETRY_ADAPTER.read(new JsonReader(new StringReader(str))));
            } catch (IOException ioe) {
                LOG.error("Unable to deserialize geometry : " + str, ioe);
            }
        }
    }

    /**
     * Build the select clause with the {@link List} of columns used for the mapping.
     *
     * @param pColumns the comulns used for the mapping
     * @return a {@link String} withe the columns separated by a comma
     */
    protected String buildColumnClause(String... pColumns) {
        StringBuilder clauseStr = new StringBuilder();
        for (String col : pColumns) {
            clauseStr.append(col + COMMA);
        }
        return clauseStr.substring(0, clauseStr.length() - 1) + BLANK;
    }

    /**
     * Replace the key word '%last_modification_date%' in the request to get the data from a date
     *
     * @param pRequest the SQL request
     * @param pDate the date to used to build the date filter
     * @return the SQL request with a from clause to filter the result since a date
     */
    private String buildDateStatement(String pRequest, OffsetDateTime pDate) {
        // Any attribute is defined in the mapping for compare the date, return
        if (getLastUpdateAttributeName().isEmpty()) {
            return pRequest;
        }

        // if any date is defined, replace the keyword and used the first existing date
        if (pDate == null) {
            return pRequest.replaceAll(LAST_MODIFICATION_DATE_KEYWORD, OffsetDateTimeAdapter.format(INIT_DATE));
        } else {
            return pRequest
                    .replaceAll(LAST_MODIFICATION_DATE_KEYWORD,
                                getLastUpdateAttributeName() + "> '" + OffsetDateTimeAdapter.format(pDate) + "'");
        }
    }

    /**
     * This method reset the number of data element from the database.<br>
     */
    protected void reset() {
        nbItems = RESET_COUNT;
    }

    /**
     * Converts the mapping between the attribute of the data source and the attributes of the model from a JSon
     * representation to a {@link List} of {@link AbstractAttributeMapping}.
     *
     * @param pModelJson the mapping in JSon format
     */
    protected void initDataSourceMapping(String pModelJson) {
        ModelMappingAdapter adapter = new ModelMappingAdapter();
        try {
            dataSourceMapping = adapter.read(new JsonReader(new StringReader(pModelJson)));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        extractColumnsFromMapping();
    }

    protected String getLastUpdateAttributeName() {
        if (!lastUpdateAttributeName.isEmpty()) {
            return lastUpdateAttributeName;
        }

        for (Map.Entry<String, InternalAttributes> entry : getMappingInternalAttributes().entrySet()) {
            if (entry.getValue() == InternalAttributes.LAST_UPDATE) {
                lastUpdateAttributeName = entry.getKey();
                LOG.debug("Attribute for date comparison found: " + entry.getKey());
                break;
            }
        }

        return lastUpdateAttributeName;
    }

    /**
     * This method extracts the {@link List} of columns from the data source mapping.
     */
    private void extractColumnsFromMapping() {
        if (columns == null) {
            columns = new ArrayList<>();
        }

        dataSourceMapping.getAttributesMapping().forEach(d -> {

            if (0 > d.getNameDS().toLowerCase().lastIndexOf(AS) && !d.isPrimaryKey()) {
                columns.add(d.getNameDS() + BLANK + AS + d.getName());
            } else {
                columns.add(d.getNameDS());
            }

            if (d.isPrimaryKey()) {
                orderByColumn = d.getNameDS();
            }
        });
    }

    /**
     * For each attributes of the mapping, determine if it is an internal attributes.</br>
     * This initialization is done, only once, before the read of the data.
     */
    private void initMappingInternalAttributes() {
        mappingInternalAttributes = new HashMap<>();

        for (AbstractAttributeMapping attrMapping : dataSourceMapping.getAttributesMapping()) {
            if (attrMapping.isLabel()) {
                mappingInternalAttributes.put(attrMapping.getNameDS(), InternalAttributes.LABEL);
            } else if (attrMapping.isRawData()) {
                mappingInternalAttributes.put(attrMapping.getNameDS(), InternalAttributes.RAW_DATA);
            } else if (attrMapping.isThumbnail()) {
                mappingInternalAttributes.put(attrMapping.getNameDS(), InternalAttributes.THUMBNAIL);
            } else if (attrMapping.isLastUpdate()) {
                mappingInternalAttributes.put(attrMapping.getNameDS(), InternalAttributes.LAST_UPDATE);
            }
        }
    }

    protected Map<String, InternalAttributes> getMappingInternalAttributes() {
        if (mappingInternalAttributes == null) {
            initMappingInternalAttributes();
        }

        return mappingInternalAttributes;
    }

    private enum InternalAttributes {
        /**
         * Identify attribute for the last update attribute
         */
        LAST_UPDATE,

        /**
         * Identify an attribute for a file as {@link DataType}{@link #RAW_DATA}
         */
        RAW_DATA,

        /**
         * Identify an attribute for a file as {@link DataType}{@link #THUMBNAIL}
         */
        THUMBNAIL,

        /**
         * Identify an attribute for a label attribute
         */
        LABEL
    };

}
