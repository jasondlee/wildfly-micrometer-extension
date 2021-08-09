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

import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "WFLYMMTREXT", length = 4)
public interface MicrometerExtensionLogger extends BasicLogger {
    MicrometerExtensionLogger MICROMETER_LOGGER = Logger.getMessageLogger(MicrometerExtensionLogger.class,
            MicrometerExtensionLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating Micrometer Subsystem")
    void activatingSubsystem();

    @LogMessage(level = INFO) // DEBUG
    @Message(id = 2, value = "Micrometer Subsystem is processing deployment")
    void processingDeployment();

    @LogMessage(level = INFO) // DEBUG
    @Message(id = 3, value = "The deployment does not have Jakarta Contexts and Dependency Injection enabled. Skipping Micrometer integration.")
    void noCdiDeployment();

    @Message(id = 4, value = "Deployment %s requires use of the '%s' capability but it is not currently registered")
    DeploymentUnitProcessingException deploymentRequiresCapability(String deploymentName, String capabilityName);
}
