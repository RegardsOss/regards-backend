package fr.cnes.regards.framework.jpa.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * This {@link AttributeConverter} allows to convert a {@link OffsetDateTime} to persist with JPA.
 *
 * @author Christophe Mertz
 * @author oroussel
 */
@Converter(autoApply = true)
public class OffsetDateTimeAttributeConverter implements AttributeConverter<OffsetDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(OffsetDateTime offsetDateTime) {
        return (offsetDateTime == null) ?
                null :
                // Take UTC date as local date to have an UTC date into database
                Timestamp.valueOf(offsetDateTime.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
    }

    @Override
    public OffsetDateTime convertToEntityAttribute(Timestamp pSqlTimestamp) {
        OffsetDateTime date;
        date = (pSqlTimestamp == null) ?
                null :
                // Read Timestamp, transform to Local Date as it is an UTC data (which is the case in DB) and transform
                // to OffsetDateTime, keeping it as UTC (ouch !)
                OffsetDateTime.ofInstant(pSqlTimestamp.toLocalDateTime().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
                        .withOffsetSameInstant(ZoneOffset.UTC);
        return date;
    }
}