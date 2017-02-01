package fr.cnes.regards.framework.jpa.converters;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * This {@link AttributeConverter} allows to convert a {@link LocalDateTime} to persist with JPA.
 * 
 * @author Christophe Mertz
 *
 */
@Converter(autoApply = true)
public class LocalDateTimeAttributeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(LocalDateTime pLocDateTime) {
        return pLocDateTime == null ? null : Timestamp.valueOf(pLocDateTime);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(Timestamp pSqlTimestamp) {
        LocalDateTime date;
        date = pSqlTimestamp == null ? null : pSqlTimestamp.toLocalDateTime();
        return date;
    }
}