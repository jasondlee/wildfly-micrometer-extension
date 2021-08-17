package org.wildfly.extras.micrometer.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extras.micrometer.MicrometerExtensionLogger.MICROMETER_LOGGER;
import static org.wildfly.extras.micrometer.MicrometerSubsystemDefinition.MICROMETER_COLLECTOR;
import static org.wildfly.extras.micrometer.MicrometerSubsystemDefinition.MICROMETER_REGISTRY_RUNTIME_CAPABILITY;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.ServerService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentCompleteServiceProcessor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extras.micrometer.MicrometerCdiExtension;
import org.wildfly.extras.micrometer.MicrometerRegistries;
import org.wildfly.extras.micrometer.metrics.MetricCollector;
import org.wildfly.security.manager.WildFlySecurityManager;

public class MicrometerDeploymentService implements Service {
    private final Resource rootResource;
    private final ManagementResourceRegistration managementResourceRegistration;
    private final PathAddress deploymentAddress;
    private final DeploymentUnit deploymentUnit;
    private final Supplier<MetricCollector> metricCollector;
    private final Supplier<Executor> managementExecutor;
    private final Supplier<MicrometerRegistries> registriesSupplier;
    private final boolean exposeAnySubsystem;
    private final List<String> exposedSubsystems;

    public static void install(ServiceTarget serviceTarget,
                               DeploymentPhaseContext deploymentPhaseContext,
                               Resource rootResource,
                               ManagementResourceRegistration managementResourceRegistration,
                               boolean exposeAnySubsystem,
                               List<String> exposedSubsystems) throws DeploymentUnitProcessingException {
        MICROMETER_LOGGER.processingDeployment();

        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        try {
            final WeldCapability weldCapability = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT)
                    .getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
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

        PathAddress deploymentAddress = createDeploymentAddressPrefix(deploymentUnit);

        ServiceBuilder<?> sb = serviceTarget.addService(deploymentUnit.getServiceName().append("micrometer-metrics"));
        Supplier<MetricCollector> metricCollectorSupplier = sb.requires(MICROMETER_COLLECTOR);
        Supplier<Executor> managementExecutorSupplier = sb.requires(ServerService.EXECUTOR_CAPABILITY.getCapabilityServiceName());
        Supplier<MicrometerRegistries> registriesSupplier = sb.requires(MICROMETER_REGISTRY_RUNTIME_CAPABILITY.getCapabilityServiceName());

        /*
         * The deployment metric service depends on the deployment complete service name to ensure that the metrics from
         * the deployment are collected and registered once the deployment services have all be properly installed.
         */
        sb.requires(DeploymentCompleteServiceProcessor.serviceName(deploymentUnit.getServiceName()));
        sb.setInstance(new MicrometerDeploymentService(rootResource, managementResourceRegistration, deploymentAddress,
                        deploymentUnit, metricCollectorSupplier, managementExecutorSupplier, registriesSupplier,
                        exposeAnySubsystem, exposedSubsystems))
                .install();
    }

    public MicrometerDeploymentService(Resource rootResource,
                                       ManagementResourceRegistration managementResourceRegistration,
                                       PathAddress deploymentAddress,
                                       DeploymentUnit deploymentUnit,
                                       Supplier<MetricCollector> metricCollectorSupplier,
                                       Supplier<Executor> managementExecutorSupplier,
                                       Supplier<MicrometerRegistries> registriesSupplier,
                                       boolean exposeAnySubsystem,
                                       List<String> exposedSubsystems) {
        this.rootResource = rootResource;
        this.managementResourceRegistration = managementResourceRegistration;
        this.deploymentAddress = deploymentAddress;
        this.deploymentUnit = deploymentUnit;
        this.metricCollector = metricCollectorSupplier;
        this.managementExecutor = managementExecutorSupplier;
        this.registriesSupplier = registriesSupplier;
        this.exposeAnySubsystem = exposeAnySubsystem;
        this.exposedSubsystems = exposedSubsystems;
    }

    private static PathAddress createDeploymentAddressPrefix(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            return PathAddress.pathAddress(DEPLOYMENT, deploymentUnit.getAttachment(Attachments.MANAGEMENT_NAME));
        } else {
            return createDeploymentAddressPrefix(deploymentUnit.getParent()).append(SUBDEPLOYMENT, deploymentUnit.getName());
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        setupMicrometerCdiBeans();

    }

    @Override
    public void stop(StopContext context) {

    }

    private void setupMicrometerCdiBeans() {
        final ClassLoader initialCl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        final ModuleClassLoader moduleCL = deploymentUnit.getAttachment(Attachments.MODULE).getClassLoader();

        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(moduleCL);

            // We may run into naming conflicts with this. How to solve?
            MicrometerCdiExtension.registerApplicationRegistry(moduleCL,
                    registriesSupplier.get().getApplicationMetricsRegistry());
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(initialCl);
        }
    }

}
