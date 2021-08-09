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

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extras.micrometer.MicrometerExtensionLogger.MICROMETER_LOGGER;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.wildfly.security.manager.WildFlySecurityManager;

public class MicrometerSubsystemDeploymentProcessor implements DeploymentUnitProcessor {
    public static final int PRIORITY = 0x4000;
    private final MicrometerContextService service;

    public MicrometerSubsystemDeploymentProcessor(MicrometerContextService service) {
        this.service = service;
    }

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        MICROMETER_LOGGER.processingDeployment();
        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        try {
            final WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
            if (!weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                // Jakarta RESTful Web Services require Jakarta Contexts and Dependency Injection. Without Jakarta
                // Contexts and Dependency Injection, there's no integration needed
                MICROMETER_LOGGER.noCdiDeployment();
                return;
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            //We should not be here since the subsystem depends on weld capability. Just in case ...
            throw MICROMETER_LOGGER.deploymentRequiresCapability(deploymentPhaseContext.getDeploymentUnit().getName(),
                    WELD_CAPABILITY_NAME);
        }
        setupMicrometerCdiBeans(deploymentPhaseContext, support);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
//        MeterRegistry registry = context.getAttachment(ATTACHMENT_KEY);
//        registry.close();
    }

    private void setupMicrometerCdiBeans(DeploymentPhaseContext deploymentPhaseContext,
                                         CapabilityServiceSupport support) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        final ClassLoader initialCl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final ModuleClassLoader moduleCL = module.getClassLoader();

        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(moduleCL);
//            final MeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

            // We may run into naming conflicts with this. How to solve?
            MicrometerCdiExtension.registerApplicationRegistry(moduleCL, service.getApplicationMetricsRegistry());
        } catch (SecurityException | IllegalArgumentException ex) {
//            MICROMETER_LOGGER.errorResolvingTracer(ex);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(initialCl);
        }
    }
}
