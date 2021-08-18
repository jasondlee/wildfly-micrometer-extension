/*
 * Copyright 2021 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.feature.pack.template.subsystem.sanity;

import java.io.IOException;
import java.net.URL;

import javax.inject.Inject;

import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jasondlee@redhat.com">Jason Lee</a>
 */
@RunWith(Arquillian.class)
public class SubsystemSanityTestCase {
    @ArquillianResource
    private ManagementClient managementClient;

    @Inject
    private MeterRegistry meterRegistry;

    @Inject
    private TestResource testResource;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "sanity-test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(SubsystemSanityTestCase.class.getPackage());
        return webArchive;
    }

    @Test
    public void testInjection() {
        Assert.assertNotNull(meterRegistry);
    }

    @Test
    public void testApplicationMetrics() {
        testResource.getCount();

        double counter = meterRegistry.counter(TestResource.COUNTER_NAME).count();
        Assert.assertEquals("Counter should not be zero.", 1.0, counter, 0.0);
    }

    @Test
    @RunAsClient
    public void testMetricsEndpoint() {
        try {
            URL address = new URL("http",
                    managementClient.getMgmtAddress(),
                    managementClient.getMgmtPort(),
                   "/micrometer");

            Response response = new OkHttpClient()
                    .newCall(new Request.Builder()
                            .url(address)
                            .build()
                    ).execute();

            Assert.assertEquals(200, response.code());
            Assert.assertTrue(response.body().string().contains(TestResource.COUNTER_NAME));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

