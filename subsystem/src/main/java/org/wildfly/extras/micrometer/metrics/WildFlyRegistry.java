package org.wildfly.extras.micrometer.metrics;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.wildfly.extras.micrometer.MicrometerExtensionLogger;

public class WildFlyRegistry extends PrometheusMeterRegistry {
    public WildFlyRegistry() {
        super(PrometheusConfig.DEFAULT);
    }

    public Meter addMetric(WildFlyMetric metric, MetricMetadata metadata) {
        switch (metadata.getType()) {
            case GAUGE:
                return addGauge(metric, metadata);
            case COUNTER:
                return addCounter(metric, metadata);
            default:
                throw MicrometerExtensionLogger.MICROMETER_LOGGER.unsupportedMetricType(metadata.getType().name());
        }
    }

    public Meter addCounter(WildFlyMetric metric, MetricMetadata metadata) {

        Meter.Id id = new Meter.Id(metadata.getMetricName(),
                Tags.of(getTags(metadata)),
                // spec violation?
                // https://prometheus.io/docs/practices/naming/
                getBaseUnit(metadata),
                metadata.getDescription(),
                Meter.Type.COUNTER);
        return newFunctionCounter(id, metric, value -> getMetricValue(metric, metadata));
    }

    public Meter addGauge(WildFlyMetric metric, MetricMetadata metadata) {
        return Gauge.builder(metadata.getMetricName(), () -> getMetricValue(metric, metadata))
                .description(metadata.getDescription())
                .tags(getTags(metadata))
                .baseUnit(getBaseUnit(metadata))
                .register(this);
    }

    private double getMetricValue(WildFlyMetric metric, MetricMetadata metadata) {
//        System.out.println("***** Returning value for " + metadata.getMetricName());
        OptionalDouble metricValue = metric.getValue();
        return metricValue.isPresent() ?
                scaleToBaseUnit(metricValue.getAsDouble(), metadata.getMeasurementUnit()) :
                0.0;
    }

    private List<Tag> getTags(MetricMetadata metadata) {
        return Arrays.stream(metadata.getTags())
                .map(t -> Tag.of(t.getKey(), t.getValue()))
                .collect(Collectors.toList());
    }

    private String getBaseUnit(MetricMetadata metadata) {
        String measurementUnit = metadata.getBaseMetricUnit();
        return "none".equalsIgnoreCase(measurementUnit) ? null : measurementUnit.toLowerCase();
    }

    private double scaleToBaseUnit(double value, MeasurementUnit unit) {
        return value * MeasurementUnit.calculateOffset(unit, unit.getBaseUnits());
    }
}
