package websocket;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WebSocketPocTest {

    private static final String THREAD_MSG = "Running [%s] in thread [%s]";
    private static final URI SOCKET_URL = URI.create("wss://echo.websocket.org");
    private WebSocket socket;
    private final WebSocketEventsRegistry webSocketEventsRegistry = new WebSocketEventsRegistry();

    @BeforeClass
    public void setUp() throws IOException, WebSocketException
    {
        System.out.println(String.format(THREAD_MSG, "BeforeClass", Thread.currentThread().getId()));
        socket = new WebSocketFactory()
                .createSocket(SOCKET_URL)
                .addListener(new EventsRegisteringWebSocketListener(webSocketEventsRegistry))
                .connect();
    }

    @BeforeMethod
    public void beforeMethod()
    {
        webSocketEventsRegistry.clearRegisteredTextMessages();
    }

    @Test
    public void basicWebSocketPocTest() throws InterruptedException
    {
        System.out.println(String.format(THREAD_MSG, "BasicTest", Thread.currentThread().getId()));
        socket.sendText("Foo");
        WebSocketEventsRegistryPoller.pollForCondition(webSocketEventsRegistry, registry -> registry.getRegisteredTextMessages().size() == 1, 1000, 250);
        Assert.assertEquals(webSocketEventsRegistry.getRegisteredTextMessages().get(0), "Foo");

    }

    @Test
    public void multicallWebSocketPocTest() throws InterruptedException {
        System.out.println(String.format(THREAD_MSG, "MultiCallTest", Thread.currentThread().getId()));
        socket.sendText("Foq").sendText("Feq").sendText("Fuq");
        WebSocketEventsRegistryPoller.pollForCondition(webSocketEventsRegistry, registry -> registry.getRegisteredTextMessages().size() == 3, 1000, 250);
        Assert.assertEquals(String.join(" ", webSocketEventsRegistry.getRegisteredTextMessages()), "Foq Feq Fuq");
    }

    @AfterClass
    public void tearDown()
    {
        socket.disconnect();
    }


    private static class WebSocketEventsRegistry
    {
        private List<String> registeredTextMessages = new ArrayList<>();

        public void registerTextMessage(String message)
        {
            registeredTextMessages.add(message);
        }

        public List<String> getRegisteredTextMessages()
        {
            return registeredTextMessages;
        }

        public void clearRegisteredTextMessages()
        {
            registeredTextMessages.clear();
        }
    }

    private static class EventsRegisteringWebSocketListener extends WebSocketAdapter
    {

        private static final String THREAD_MSG = "Registering [%s] in thread [%s]";
        private final WebSocketEventsRegistry eventsRegistry;

        public EventsRegisteringWebSocketListener(WebSocketEventsRegistry eventsRegistry)
        {
            this.eventsRegistry = eventsRegistry;
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            System.out.println(String.format(THREAD_MSG, "TextMessage", Thread.currentThread().getId()));
            eventsRegistry.registerTextMessage(text);
        }
    }

    private static class WebSocketEventsRegistryPoller
    {
        static void pollForCondition (WebSocketEventsRegistry eventsRegistry, Predicate<WebSocketEventsRegistry> condition, int timeoutInMillis, int pollRate) throws InterruptedException {

            LocalDateTime to = LocalDateTime.now().plus(timeoutInMillis, ChronoUnit.MILLIS);

            while (LocalDateTime.now().isBefore(to))
            {
                if (condition.test(eventsRegistry)) return;
                else Thread.sleep(pollRate);
            }

            throw new RuntimeException(String.format("Expected condition was not achieved in timeout of [%s] millis", timeoutInMillis));
        }
    }

}
