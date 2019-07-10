package io.thorntail.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;

import io.thorntail.example.model.Message;

@RunWith(Arquillian.class)
@DefaultDeployment
public class MessageServiceTest {
    @Test
    @RunAsClient
    public void listMessages() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8080")
                .path("/api")
                .path("/messages");

        Response response = target.request(MediaType.APPLICATION_JSON).get();
        assertEquals(200, response.getStatus());
        JsonArray values = Json.parse(response.readEntity(String.class)).asArray();
        assertTrue(values.size() > 0);
    }

    @Test
    @RunAsClient
    public void messageById() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8080")
                .path("/api")
                .path("/messages")
                .path("/1"); // message by ID

        Response response = target.request(MediaType.APPLICATION_JSON).get();
        assertEquals(200, response.getStatus());
        JsonObject value = Json.parse(response.readEntity(String.class)).asObject();
        assertEquals("Ciao", value.get("value").asString());
    }

    @Test
    @RunAsClient
    public void createMessage() {
        createNewMessage("Pineapple");
    }

    private JsonObject createNewMessage(String value) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8080")
                .path("/api")
                .path("/messages");

        Response response = target.request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(
                        new Message(value),
                        MediaType.APPLICATION_JSON)
                );
        assertEquals(201, response.getStatus());
        JsonObject json = Json.parse(response.readEntity(String.class)).asObject();
        assertEquals(value, json.get("value").asString());
        return json;
    }

    @Test
    @RunAsClient
    public void modifyMessage() {
        JsonObject lemon = createNewMessage("Lemon");
        int id = lemon.get("id").asInt();

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8080")
                .path("/api")
                .path("/messages")
                .path(String.valueOf(id));

        Response response = target.request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(
                        new Message("Apricot"),
                        MediaType.APPLICATION_JSON)
                );
        assertEquals(200, response.getStatus());
        JsonObject value = Json.parse(response.readEntity(String.class)).asObject();
        assertEquals("Apricot", value.get("value").asString());
        assertEquals(id, value.get("id").asInt());
    }
}
