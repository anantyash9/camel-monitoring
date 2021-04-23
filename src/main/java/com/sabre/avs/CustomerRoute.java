package com.sabre.avs;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CustomerRoute extends RouteBuilder {

    // we can use spring dependency injection
    @Autowired
    ProcessorBean processorBean;

    @Override
    public void configure() throws Exception {
    	// uncomment for offline testing
//    	from("timer:hello?delay=-1").routeId("subscriber")
    	
    	//rout to customer pubsub subscription
        from("{{google.pubsub.customer.subscription}}").routeId("subscriber")
         // uncomment for offline testing
//        .bean(processorBean,"addBody")
        .bean(processorBean,"addTimestamp")
        .bean(processorBean,"logMetrics")
        .to("log:Throughput Logger?level=INFO&groupInterval=10000&groupDelay=60000&groupActiveOnly=false");
    }

}
