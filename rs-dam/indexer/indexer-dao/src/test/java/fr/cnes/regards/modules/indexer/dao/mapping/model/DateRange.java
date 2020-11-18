package fr.cnes.regards.modules.indexer.dao.mapping.model;

import java.time.Duration;
import java.time.LocalDateTime;

public class DateRange {
    public LocalDateTime lowerBound;
    public LocalDateTime upperBound;
    public DateRange(LocalDateTime lowerBound, LocalDateTime upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public interface From { DateRange to(LocalDateTime to); }
    public static From from(LocalDateTime from) { return (to -> new DateRange(from, to)); }
    public interface Starting { DateRange during(Duration duration); }
    public static Starting starting(LocalDateTime from) { return (d -> new DateRange(from, from.plus(d))); }
}
