/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extras.micrometer;

import static org.wildfly.extras.micrometer.MicrometerSubsystemDefinition.HTTP_EXTENSIBILITY_CAPABILITY;
import static org.wildfly.extras.micrometer.MicrometerSubsystemDefinition.MICROMETER_HTTP_CONTEXT_CAPABILITY;
import static org.wildfly.extras.micrometer.MicrometerSubsystemDefinition.MICROMETER_HTTP_SECURITY_CAPABILITY;
import static org.wildfly.extras.micrometer.MicrometerSubsystemDefinition.MICROMETER_REGISTRY_RUNTIME_CAPABILITY;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

public class MicrometerContextService implements Service {
    public static final String CONTEXT = "/micrometer";

    private final Consumer<MicrometerContextService> consumer;
    private final Supplier<ExtensibleHttpManagement> extensibleHttpManagement;
    private final Supplier<Boolean> securityEnabledSupplier;
    private final Supplier<MicrometerRegistries> registriesSupplier;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    static MicrometerContextService install(OperationContext context, boolean securityEnabled) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget()
                .addService(MICROMETER_HTTP_CONTEXT_CAPABILITY.getCapabilityServiceName());

        Supplier<ExtensibleHttpManagement> extensibleHttpManagement =
                serviceBuilder.requires(context.getCapabilityServiceName(HTTP_EXTENSIBILITY_CAPABILITY, ExtensibleHttpManagement.class));
        Supplier<MicrometerRegistries> registries =
                serviceBuilder.requires(context.getCapabilityServiceName(MICROMETER_REGISTRY_RUNTIME_CAPABILITY.getName(), MicrometerRegistries.class));
        Consumer<MicrometerContextService> metricsContext =
                serviceBuilder.provides(MICROMETER_HTTP_CONTEXT_CAPABILITY.getCapabilityServiceName());

        final Supplier<Boolean> securityEnabledSupplier;
        if (context.getCapabilityServiceSupport().hasCapability(MICROMETER_HTTP_SECURITY_CAPABILITY)) {
            securityEnabledSupplier = serviceBuilder.requires(ServiceName.parse(MICROMETER_HTTP_SECURITY_CAPABILITY));
        } else {
            securityEnabledSupplier = () -> securityEnabled;
        }

        // ???
        MicrometerContextService service = new MicrometerContextService(metricsContext,
                extensibleHttpManagement,
                securityEnabledSupplier,
                registries);

        serviceBuilder.setInstance(service)
                .install();

        return service;
    }

    private MicrometerContextService(Consumer<MicrometerContextService> consumer,
                                     Supplier<ExtensibleHttpManagement> extensibleHttpManagement,
                                     Supplier<Boolean> securityEnabledSupplier,
                                     Supplier<MicrometerRegistries> registriesSupplier) {
        this.consumer = consumer;
        this.extensibleHttpManagement = extensibleHttpManagement;
        this.securityEnabledSupplier = securityEnabledSupplier;
        this.registriesSupplier = registriesSupplier;
    }

    @Override
    public void start(StartContext context) {
        extensibleHttpManagement.get().addManagementHandler(CONTEXT, securityEnabledSupplier.get(), exchange -> {
            lock.readLock().lock();
            try {
                StringBuilder sb = new StringBuilder();
                registriesSupplier.get().getRegistries().forEach(r -> sb.append(r.scrape()));
                exchange.getResponseSender().send(sb.toString());
            } finally {
                lock.readLock().unlock();
            }
        });
        consumer.accept(this);
    }

    @Override
    public void stop(StopContext context) {
        extensibleHttpManagement.get().removeContext(CONTEXT);
        consumer.accept(null);
    }
}
