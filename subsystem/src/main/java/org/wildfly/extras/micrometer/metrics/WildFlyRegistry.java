package org.wildfly.extras.micrometer.metrics;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class WildFlyRegistry extends PrometheusMeterRegistry {
    public WildFlyRegistry() {
        super(PrometheusConfig.DEFAULT);
    }

    public Meter addCounter(WildFlyMetric metric, WildFlyMetricMetadata metadata) {
        String measurementUnit = metadata.getMeasurementUnit().getBaseUnits().getName();
        return newFunctionCounter(new Meter.Id(metadata.getMetricName(),
                        Tags.of(getTags(metadata)),
                        // spec violation?
                        // https://prometheus.io/docs/practices/naming/
                        "NONE".equals(measurementUnit) ? null : measurementUnit,
                        metadata.getDescription(),
                        Meter.Type.COUNTER),
                metric,
                value -> value.getValue().getAsDouble());
    }

    public Meter addGauge(WildFlyMetric metric, WildFlyMetricMetadata metadata) {
        return Gauge.builder(metadata.getMetricName(), () -> metric.getValue().getAsDouble())
                .description(metadata.getDescription())
                .tags(getTags(metadata))
                .baseUnit(metadata.getMeasurementUnit().getName())
                .register(this);
    }

    private List<Tag> getTags(WildFlyMetricMetadata metadata) {
        return Arrays.stream(metadata.getTags())
                .map(t -> Tag.of(t.getKey(), t.getValue()))
                .collect(Collectors.toList());
    }
}
