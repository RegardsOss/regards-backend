package fr.cnes.regards.framework.jpa.converters;

import java.sql.Date;
import java.time.LocalDate;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * 
 * @author Christophe Mertz
 *
 */
@Converter(autoApply = true)
public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, Date> {

    @Override
    public Date convertToDatabaseColumn(LocalDate pLocDate) {
        return (pLocDate == null ? null : Date.valueOf(pLocDate));
    }

    @Override
    public LocalDate convertToEntityAttribute(Date pSqlDate) {
        return (pSqlDate == null ? null : pSqlDate.toLocalDate());
    }
}