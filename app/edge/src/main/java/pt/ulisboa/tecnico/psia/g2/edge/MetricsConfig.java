package pt.ulisboa.tecnico.psia.g2.edge;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

// Histogram buckets for request latency, so Grafana can show p50/p95/p99.
@Singleton
public class MetricsConfig {

    @Produces
    @Singleton
    public MeterFilter latencyHistograms() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().startsWith("http.server.requests") || id.getName().startsWith("http.client.requests")) {
                    return DistributionStatisticConfig.builder().percentilesHistogram(true).build().merge(config);
                }
                return config;
            }
        };
    }
}
