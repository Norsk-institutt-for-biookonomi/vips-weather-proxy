package no.nibio.vips.weather.proxy.parsers.frost.aggregate;

import lombok.Getter;

@Getter
public final class FrostAggregationRule {
    private final boolean mean;
    private final boolean max;
    private final String meanId;
    private final String maxId;

    public FrostAggregationRule(boolean mean, boolean max, String meanId, String maxId) {
        this.mean = mean;
        this.max = max;
        this.meanId = meanId;
        this.maxId = maxId;
    }

}
