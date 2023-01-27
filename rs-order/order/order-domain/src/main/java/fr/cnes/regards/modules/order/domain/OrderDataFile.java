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
package fr.cnes.regards.modules.order.domain;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converter.MimeTypeConverter;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.urn.converters.UrnConverter;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import org.hibernate.annotations.Type;
import org.springframework.util.MimeType;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.Objects;

/**
 * Inherits from DataFile to add nearline state and IP_ID
 *
 * @author oroussel
 */
@Entity
@Table(name = "t_data_file",
    indexes = { @Index(name = "data_file_idx", columnList = "checksum, order_id, state, data_objects_ip_id"),
        @Index(name = "idx_data_file_order_id", columnList = "order_id") })
@NamedNativeQuery(query = """
    SELECT o.*, sum(df.size) as size
    FROM {h-schema}t_data_file df, {h-schema}t_order o
    WHERE df.order_id = o.id
    AND df.size is not NULL
    AND o.id IN (
          SELECT id
          FROM {h-schema}t_order
          WHERE ?1 <= expiration_date
          AND status in ('RUNNING', 'PAUSED')
          )
    GROUP BY o.id
    ORDER BY o.id
    """, resultSetMapping = "sumMapping", name = "selectSumSizesByOrderId")
@NamedNativeQuery(query = """
    SELECT o.*, sum(df.size) as size
    FROM {h-schema}t_data_file df, {h-schema}t_order o
    WHERE df.order_id = o.id
    AND df.size is not NULL
    AND o.id IN (
            SELECT id
            FROM {h-schema}t_order
            WHERE ?1 <= expiration_date
            AND status in ('RUNNING', 'PAUSED')
            )
    AND df.state IN (?2)
    GROUP BY o.id
    ORDER BY o.id
    """, resultSetMapping = "sumMapping", name = "selectSumSizesByOrderIdAndStates")
@NamedNativeQuery( // WARNING : this request is used to count files in error (except DOWNLOAD_ERROR) so
    // only internal files are concerned => df.size must not be null
    query = """
        SELECT o.*, count(df.*) as count
        FROM {h-schema}t_data_file df, {h-schema}t_order o
        WHERE df.order_id = o.id
        AND df.size is not NULL
        AND o.id IN (
                SELECT id
                FROM {h-schema}t_order
                WHERE ?1 <= expiration_date
                AND status in ('RUNNING', 'PAUSED')
                )
        AND df.state IN (?2)
        GROUP BY o.id
        ORDER BY o.id
        """, resultSetMapping = "countMapping", name = "selectCountFilesByOrderIdAndStates")
@NamedNativeQuery( // WARNING : this request permits to count all available files EVEN  external files which
    // haven't a size (but are tagged as reference)
    query = """
        SELECT o.*, count(df.*) as count
        FROM {h-schema}t_data_file df, {h-schema}t_order o
        WHERE df.order_id = o.id
        AND (df.size is not NULL OR (df.size is NULL AND df.reference is TRUE))
        AND o.id IN (
                SELECT id
                FROM {h-schema}t_order
                WHERE ?1 <= expiration_date
                )
        AND df.state IN (?2)
        GROUP BY o.id
        ORDER BY o.id
        """, resultSetMapping = "countMapping", name = "selectCountFilesByOrderIdAndStates4AllOrders")
@SqlResultSetMapping(name = "sumMapping", columns = @ColumnResult(name = "size", type = Long.class),
    entities = @EntityResult(entityClass = Order.class))
@SqlResultSetMapping(name = "countMapping", columns = @ColumnResult(name = "count", type = Long.class),
    entities = @EntityResult(entityClass = Order.class))
public class OrderDataFile extends DataFile implements IIdentifiable<Long> {

    private Long id;

    private FileState state;

    /**
     * Mandatory orderId to know whose data file belongs to BUT without managing a ManyToOne relation (which will be a
     * mess you don't even want to understand, believe me)
     */
    private Long orderId;

    /**
     * DataObject IP_ID
     */
    private UniformResourceName ipId;

    /**
     * Download error reason
     */
    private String downloadError;

    private String productId;

    public OrderDataFile() {
        super();
    }

    public OrderDataFile(DataFile dataFile, UniformResourceName ipId, Long orderId) {
        this(dataFile, ipId, orderId, null);
    }

    public OrderDataFile(DataFile dataFile, UniformResourceName ipId, Long orderId, String productId) {
        super.setFilename(dataFile.getFilename());
        super.setFilesize(dataFile.getFilesize());
        super.setUri(dataFile.getUri());
        super.setChecksum(dataFile.getChecksum());
        super.setDigestAlgorithm(dataFile.getDigestAlgorithm());
        super.setMimeType(dataFile.getMimeType());
        super.setReference(dataFile.isReference());
        super.setDataType(dataFile.getDataType());
        state = FileState.PENDING;
        super.setOnline(dataFile.isOnline());
        this.ipId = ipId;
        this.orderId = orderId;
        this.productId = productId;
    }

    public void setState(FileState state) {
        this.state = state;
    }

    @Override
    @Id
    @SequenceGenerator(name = "DataFileSequence", initialValue = 1, sequenceName = "seq_data_file")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DataFileSequence")
    public Long getId() {
        return id;
    }

    @Column(name = "data_objects_ip_id", length = UniformResourceName.MAX_SIZE)
    @Convert(converter = UrnConverter.class)
    public UniformResourceName getIpId() {
        return ipId;
    }

    @Column(name = "state", length = 16)
    @Enumerated(EnumType.STRING)
    public FileState getState() {
        return state;
    }

    @Column(name = "url", columnDefinition = "text")
    public String getUrl() {
        return super.getUri();
    }

    public void setUrl(String url) {
        super.setUri(url);
    }

    @Override
    @Column(name = "checksum_algo", length = 10)
    public String getDigestAlgorithm() {
        return super.getDigestAlgorithm();
    }

    @Override
    @Column(name = "checksum", length = 128)
    public String getChecksum() {
        return super.getChecksum();
    }

    @Override
    @Column(name = "size")
    public Long getFilesize() {
        return super.getFilesize();
    }

    @Override
    @Column(name = "mime_type", length = 64) // See RFC 6838
    @Convert(converter = MimeTypeConverter.class)
    public MimeType getMimeType() {
        return super.getMimeType();
    }

    @Override
    @Column(name = "name", length = 255)
    public String getFilename() {
        return super.getFilename();
    }

    @Override
    @Column(name = "online")
    public Boolean isOnline() {
        return super.isOnline();
    }

    @Column(name = "order_id") // No foreign key
    public Long getOrderId() {
        return orderId;
    }

    @Column(name = "download_error_reason")
    @Type(type = "text")
    public String getDownloadError() {
        return downloadError;
    }

    @Override
    @Column(name = "data_type")
    @Enumerated(EnumType.STRING)
    public DataType getDataType() {
        return super.getDataType();
    }

    @Override
    @Column(name = "reference")
    public Boolean isReference() {
        return super.isReference();
    }

    @Size(max = 255, message = "id length is limited to 255 characters.")
    @Column(name = "product_id")
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIpId(UniformResourceName ipId) {
        this.ipId = ipId;
    }

    public void setDownloadError(String downloadError) {
        this.downloadError = downloadError;
    }

    /**
     * Files should be ordered only once: so we only base equals on checksum and digestAlgorithm
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        OrderDataFile dataFile = (OrderDataFile) o;
        return Objects.equals(getChecksum(), dataFile.getChecksum()) && Objects.equals(getDigestAlgorithm(),
                                                                                       dataFile.getDigestAlgorithm());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getChecksum(), getDigestAlgorithm());
    }
}
