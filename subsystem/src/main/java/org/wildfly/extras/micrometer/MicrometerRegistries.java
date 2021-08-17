package org.wildfly.extras.micrometer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.wildfly.extras.micrometer.metrics.WildFlyRegistry;

public class MicrometerRegistries {
//    private final PrometheusMeterRegistry jvmRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
//    private final PrometheusMeterRegistry vendorRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
//    private final PrometheusMeterRegistry applicationRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    private final WildFlyRegistry jvmRegistry = new WildFlyRegistry();
    private final WildFlyRegistry vendorRegistry = new WildFlyRegistry();
    private final WildFlyRegistry applicationRegistry = new WildFlyRegistry();

    public MicrometerRegistries() {
        new ClassLoaderMetrics().bindTo(jvmRegistry);
        new JvmMemoryMetrics().bindTo(jvmRegistry);
        new JvmGcMetrics().bindTo(jvmRegistry);
        new ProcessorMetrics().bindTo(jvmRegistry);
        new JvmThreadMetrics().bindTo(jvmRegistry);
    }

    public List<PrometheusMeterRegistry> getRegistries() {
        return Collections.unmodifiableList(Arrays.asList(applicationRegistry, jvmRegistry, vendorRegistry));
    }

    public WildFlyRegistry getApplicationMetricsRegistry() {
        return applicationRegistry;
    }

    public WildFlyRegistry getVendorRegistry() {
        return vendorRegistry;
    }
}
