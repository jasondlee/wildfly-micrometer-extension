package org.wildfly.extension.feature.pack.template.example;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * @author <a href="mailto:jasondlee@redhat.com">Jason Lee</a>
 */
@RequestScoped
@Path("/")
public class JaxRsResource {

    @Inject
    private MeterRegistry meterRegistry;

    @GET
    @Path("/")
    public String getGreeting() {
        Counter counter = meterRegistry.counter("fp_demo_counter");
        counter.increment();
        return "Count incremented: " + counter.count();
    }
}
