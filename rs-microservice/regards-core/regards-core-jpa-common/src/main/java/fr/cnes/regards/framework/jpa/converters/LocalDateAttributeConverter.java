package fr.cnes.regards.framework.jpa.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Date;
import java.time.LocalDate;

/**
 * This {@link AttributeConverter} allows to convert a LocalDate to persist with JPA.
 * 
 * @author Christophe Mertz
 *
 */
@Converter(autoApply = true)
public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, Date> {

    @Override
    public Date convertToDatabaseColumn(LocalDate pLocDate) {
        return pLocDate == null ? null : Date.valueOf(pLocDate);
    }

    @Override
    public LocalDate convertToEntityAttribute(Date pSqlDate) {
        return pSqlDate == null ? null : pSqlDate.toLocalDate();
    }
}