package org.wildfly.test.feature.pack.template.subsystem.sanity;

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
public class TestResource {
    public static String COUNTER_NAME = "test_counter";

    @Inject
    private MeterRegistry meterRegistry;

    @GET
    @Path("/")
    public String getCount() {
        Counter counter = meterRegistry.counter(COUNTER_NAME);
        counter.increment();
        return "Count incremented: " + counter.count();
    }
}
