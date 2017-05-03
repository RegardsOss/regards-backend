package fr.cnes.regards.framework.gson.adapters;

import javax.swing.text.DateFormatter;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

import org.junit.Test;

/**
 * Created by oroussel on 03/05/17.
 */
public class DateTest {

    @Test
    public void test() {
        //DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
//        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.S][X]");
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().parseCaseInsensitive().append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .optionalStart().appendOffset("+HH:MM", "Z").toFormatter();

        TemporalAccessor temporalAccessor = formatter.parse("2017-05-03T11:35:14Z");
        System.out.println(temporalAccessor);
        LocalDateTime date = LocalDateTime.from(temporalAccessor);
        System.out.println("local : " + formatter.format(date));
        OffsetDateTime zonedDate = OffsetDateTime.from(temporalAccessor);
        System.out.println("utc ? : " + formatter.format(zonedDate));

        temporalAccessor = formatter.parse("2017-05-03T11:35:14");
        System.out.println(temporalAccessor);
        date = LocalDateTime.from(temporalAccessor);
        System.out.println("local : " + formatter.format(date));
        try {
            if (temporalAccessor.isSupported(ChronoField.OFFSET_SECONDS)) {
                zonedDate = OffsetDateTime.from(temporalAccessor);
            } else {
                zonedDate = OffsetDateTime.of(date, ZoneOffset.UTC);
            }
        } catch (DateTimeException dte) {

        }
        System.out.println("utc ? : " + formatter.format(zonedDate));

        temporalAccessor = formatter.parse("2017-05-03T11:35:14.385");
        System.out.println(temporalAccessor);
        date = LocalDateTime.from(temporalAccessor);
        System.out.println("local : " + formatter.format(date));
        try {
            zonedDate = OffsetDateTime.from(temporalAccessor);
        } catch (DateTimeException dte) {
            zonedDate = OffsetDateTime.of(date, ZoneOffset.UTC);
        }
        System.out.println("utc ? : " + formatter.format(zonedDate));

        temporalAccessor = formatter.parse("2017-05-03T11:35:14.385Z");
        System.out.println(temporalAccessor);
        date = LocalDateTime.from(temporalAccessor);
        System.out.println("local : " + formatter.format(date));
        try {
            zonedDate = OffsetDateTime.from(temporalAccessor);
        } catch (DateTimeException dte) {
            zonedDate = OffsetDateTime.of(date, ZoneOffset.UTC);
        }
        System.out.println("utc ? : " + formatter.format(zonedDate));

        temporalAccessor = formatter.parse("2017-05-03T11:35:14.385+03:00");
        System.out.println(temporalAccessor);
        date = LocalDateTime.from(temporalAccessor);
        System.out.println("local : " + formatter.format(date));
        try {
            zonedDate = OffsetDateTime.from(temporalAccessor);
        } catch (DateTimeException dte) {
            zonedDate = OffsetDateTime.of(date, ZoneOffset.UTC);
        }
        System.out.println("utc ? : " + formatter.format(zonedDate));



        System.out.println("\nNOW : " + formatter.format(OffsetDateTime.now()));
        System.out.println("\nNOW : " + formatter.format(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)));

//        Instant instant = Instant.from(temporalAccessor);
//        System.out.println(instant);
//        System.out.println(formatter.format(instant));

//        instant = Instant.from(formatter.parse("2017-05-03T11:35:00.385"));
//        System.out.println(instant);
//        System.out.println(formatter.format(instant));
//
//        instant = Instant.from(formatter.parse("2017-05-03T11:35:00Z"));
//        System.out.println(instant);
//        System.out.println(formatter.format(instant));
//
//        instant = Instant.from(formatter.parse("2017-05-03T11:35:00.378Z"));
//        System.out.println(instant);
//        System.out.println(formatter.format(instant));
//
//        instant = Instant.from(formatter.parse("2017-05-03T11:35:00+01:00"));
//        System.out.println(instant);
//        System.out.println(formatter.format(instant));
    }
}
