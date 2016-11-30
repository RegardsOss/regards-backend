package fr.cnes.regards.framework.jpa.converters;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Christophe Mertz
 *
 */
@Converter(autoApply = true)
public class LocalDateTimeAttributeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(LocalDateTimeAttributeConverter.class);

    @Override
    public Timestamp convertToDatabaseColumn(LocalDateTime pLocDateTime) {
        return (pLocDateTime == null ? null : Timestamp.valueOf(pLocDateTime));
    }

    @Override
    public LocalDateTime convertToEntityAttribute(Timestamp pSqlTimestamp) {
        LocalDateTime date = null;
        date = (pSqlTimestamp == null ? null : pSqlTimestamp.toLocalDateTime());
        return date;
    }
}