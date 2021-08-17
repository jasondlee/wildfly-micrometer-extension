package org.wildfly.extras.micrometer;

import static org.wildfly.extras.micrometer.MicrometerSubsystemDefinition.MICROMETER_REGISTRY_RUNTIME_CAPABILITY;

import java.util.function.Consumer;

import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class MicrometerRegistriesService implements Service {
    private final Consumer<MicrometerRegistries> registriesConsumer;
    private MicrometerRegistries registries;

    static void install(OperationContext context, boolean securityEnabled) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget()
                .addService(MICROMETER_REGISTRY_RUNTIME_CAPABILITY.getCapabilityServiceName());

        Consumer<MicrometerRegistries> consumer = serviceBuilder.provides(MICROMETER_REGISTRY_RUNTIME_CAPABILITY.getCapabilityServiceName());
        MicrometerRegistriesService service = new MicrometerRegistriesService(consumer);
        serviceBuilder.setInstance(service)
                .install();
    }

    public MicrometerRegistriesService(Consumer<MicrometerRegistries> registriesConsumer) {
        this.registriesConsumer = registriesConsumer;
    }

    @Override
    public void start(StartContext context) throws StartException {
        registries = new MicrometerRegistries();
        registriesConsumer.accept(registries);
    }

    @Override
    public void stop(StopContext context) {
        // Clear registries?
    }
}
