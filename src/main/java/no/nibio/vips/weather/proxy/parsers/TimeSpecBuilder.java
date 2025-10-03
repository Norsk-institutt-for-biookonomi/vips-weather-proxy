package no.nibio.vips.weather.proxy.parsers;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeSpecBuilder {
    private ZonedDateTime start;
    private ZonedDateTime end;

    public TimeSpecBuilder start(ZonedDateTime start) {
        this.start = start;
        return this;
    }

    public TimeSpecBuilder end(ZonedDateTime end) {
        this.end = end;
        return this;
    }

    public String build() {
        if (start == null || end == null) {
            throw new IllegalStateException("Start and end must be set");
        }
        DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        return fmt.format(start) + "/" + fmt.format(end);
    }

}
