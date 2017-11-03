package fr.cnes.regards.modules.order.domain;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import java.net.URI;
import java.net.URISyntaxException;

import org.hibernate.annotations.Parameter;
import org.springframework.util.MimeType;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converter.MimeTypeConverter;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.oais.urn.converters.UrnConverter;
import fr.cnes.regards.modules.indexer.domain.DataFile;

/**
 * Inherits from DataFile to add nearline state and IP_ID
 * @author oroussel
 */
@Entity
@Table(name = "t_data_file",
        indexes = @Index(name = "data_file_idx", columnList = "checksum, order_id, state, data_objects_ip_id"))
@NamedNativeQueries({
        @NamedNativeQuery(query = "SELECT o.*, sum(df.size) as size FROM {h-schema}t_data_file df, t_order o WHERE "
                + "o.id IN (SELECT id FROM {h-schema}t_order WHERE ?1 <= expiration_date AND percent_complete != 100) "
                + "GROUP BY o.id ORDER BY o.id",
                resultSetMapping = "sumMapping",
                name = "selectSumSizesByOrderId"),
        @NamedNativeQuery(
                query = "SELECT o.*, sum(df.size) as size FROM {h-schema}t_data_file df, t_order o WHERE "
                + "o.id IN (SELECT id FROM {h-schema}t_order WHERE ?1 <= expiration_date AND percent_complete != 100) "
                + "AND df.state IN (?2) GROUP BY o.id ORDER BY o.id",
                resultSetMapping = "sumMapping", name = "selectSumSizesByOrderIdAndStates"),
        @NamedNativeQuery(
                query = "SELECT o.*, count(df.*) as count FROM {h-schema}t_data_file df, t_order o WHERE "
                        + "o.id IN (SELECT id FROM {h-schema}t_order WHERE ?1 <= expiration_date AND percent_complete != 100) "
                        + "AND df.state IN (?2) GROUP BY o.id ORDER BY o.id",
                resultSetMapping = "countMapping", name = "selectCountFilesByOrderIdAndStates"),
        @NamedNativeQuery(
                query = "SELECT o.*, count(df.*) as count FROM {h-schema}t_data_file df, t_order o WHERE "
                        + "o.id IN (SELECT id FROM {h-schema}t_order WHERE ?1 <= expiration_date) "
                        + "AND df.state IN (?2) GROUP BY o.id ORDER BY o.id",
                resultSetMapping = "countMapping", name = "selectCountFilesByOrderIdAndStates4AllOrders")
        })
@SqlResultSetMappings({
        @SqlResultSetMapping(name = "sumMapping", columns = @ColumnResult(name = "size", type = Long.class),
                             entities = @EntityResult(entityClass = Order.class)),
        @SqlResultSetMapping(name = "countMapping", columns = @ColumnResult(name = "count", type = Long.class),
                entities = @EntityResult(entityClass = Order.class))
})
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

    public void setState(FileState state) {
        this.state = state;
    }

    public OrderDataFile() {
    }

    public OrderDataFile(DataFile dataFile, UniformResourceName ipId, Long orderId) {
        setName(dataFile.getName());
        setSize(dataFile.getSize());
        setUri(dataFile.getUri());
        setChecksum(dataFile.getChecksum());
        setDigestAlgorithm(dataFile.getDigestAlgorithm());
        setMimeType(dataFile.getMimeType());
        setState(FileState.PENDING);
        setOnline(dataFile.getOnline());
        setIpId(ipId);
        setOrderId(orderId);
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
        return (super.getUri() != null) ? super.getUri().toString() : null;
    }

    public void setUrl(String url) throws URISyntaxException {
        super.setUri(new URI(url));
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
    public Long getSize() {
        return super.getSize();
    }

    @Override
    @Column(name = "mime_type", length = 64) // See RFC 6838
    @Convert(converter = MimeTypeConverter.class)
    public MimeType getMimeType() {
        return super.getMimeType();
    }

    @Override
    @Column(name = "name", length = 255)
    public String getName() {
        return super.getName();
    }

    @Override
    @Column(name = "online")
    public Boolean getOnline() {
        return super.getOnline();
    }

    @Column(name = "order_id") // No foreign key
    public Long getOrderId() {
        return orderId;
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

    @Override
    public void setOnline(Boolean online) {
        super.setOnline(online);
        if ((online != null) && online) {
            this.state = FileState.ONLINE;
        }
    }
}
