package be.rvanderhallen.camel.aggregation;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.Builder;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.camel.util.concurrent.SynchronousExecutorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CamelAggregationStrategyTest extends CamelTestSupport {

    private static final String CONSUMER_ENDPOINT = "direct:input";

    @Produce(CONSUMER_ENDPOINT)
    protected ProducerTemplate inputTemplate;
    
    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from(CONSUMER_ENDPOINT)
                    .multicast((oldExchange, newExchange) -> {
                        if (oldExchange == null) {
                            return newExchange;
                        }

                        if (newExchange.getIn().getBody() instanceof String) {
                            oldExchange.getMessage(AttachmentMessage.class).setBody(newExchange.getIn().getBody());
                        } else {
                            byte[] data = newExchange.getIn().getBody(byte[].class);
                            oldExchange.getMessage(AttachmentMessage.class).addAttachment("attachment", new DataHandler(new ByteArrayDataSource(data, "text/plain")));
                        }

                        return oldExchange;
                    }).stopOnException()
                        .to("direct:getBody", "direct:getAttachment")
                    .end();
                
                from("direct:getBody")
                    .setBody(Builder.constant("body"));
                
                from("direct:getAttachment")
                    .setBody(Builder.constant("attachment".getBytes()));
            }
        };
    }
    
    @Test
    void shouldAddAttachmentToExchange() {
        Exchange exchange = ExchangeBuilder.anExchange(context).build();
        
        inputTemplate.send(exchange);

        // works in Camel 3.14.6, starts failing from Camel 3.16.0 onwards
        Assertions.assertTrue(exchange.getMessage(AttachmentMessage.class).hasAttachments());
    }
}
