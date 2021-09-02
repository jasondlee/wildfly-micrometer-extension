package org.wildfly.extras.micrometer;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import org.wildfly.extras.micrometer.metrics.WildFlyRegistry;

public class MicrometerRegistries {
    //    private final PrometheusMeterRegistry jvmRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
//    private final PrometheusMeterRegistry vendorRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
//    private final PrometheusMeterRegistry applicationRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    private final WildFlyRegistry registry = new WildFlyRegistry();

    public MicrometerRegistries() {
        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
    }

    public WildFlyRegistry getRegistry() {
        return registry;
    }
}
