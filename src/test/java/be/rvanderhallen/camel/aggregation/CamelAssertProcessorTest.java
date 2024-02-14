package be.rvanderhallen.camel.aggregation;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class CamelAssertProcessorTest extends CamelTestSupport {

    private static final String CONSUMER_ENDPOINT = "direct:input";
    private static final String RESULT_ENDPOINT = "direct:output";

    @Produce(CONSUMER_ENDPOINT)
    private ProducerTemplate inputTemplate;
    
    @EndpointInject("mock:" + RESULT_ENDPOINT)
    private MockEndpoint resultEndpoint;
    
    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from(CONSUMER_ENDPOINT)
                    .process(exchange -> {
                        assert exchange.getIn().getBody() instanceof Integer : "Invalid message body";
                    })
                    .to(RESULT_ENDPOINT);
            }
        };
    }
    
    @Test
    void shouldAddAttachmentToExchange() throws InterruptedException {
        Exchange exchange = ExchangeBuilder.anExchange(context)
                .withBody("test")
                .build();

        resultEndpoint.expectedMessageCount(0);
        resultEndpoint.expectedMinimumMessageCount(0);

        // works in Camel 3.21.0, throws java.lang.AssertionError in Camel 4.3.0
        inputTemplate.send(exchange);
        
        MockEndpoint.assertIsSatisfied(context);
        Assertions.assertThat(exchange.getAllProperties().get(Exchange.EXCEPTION_CAUGHT)).isInstanceOf(CamelExecutionException.class);
        Assertions.assertThat(((CamelExecutionException)exchange.getAllProperties().get(Exchange.EXCEPTION_CAUGHT)).getCause()).isInstanceOf(AssertionError.class);
        Assertions.assertThat(((CamelExecutionException)exchange.getAllProperties().get(Exchange.EXCEPTION_CAUGHT)).getCause().getMessage()).isEqualTo("Invalid message body");

    }
}
