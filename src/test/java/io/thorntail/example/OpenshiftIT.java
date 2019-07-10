/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.thorntail.example;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.StringReader;
import java.io.StringWriter;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;
import io.thorntail.example.model.Message;

@RunWith(Arquillian.class)
public class OpenshiftIT {
    @RouteURL(value = "${app.name}", path = "/api/messages")
    @AwaitRoute(path = "/")
    private String url;

    @Before
    public void setup() {
        String jsonData =
                given()
                        .baseUri(url)
                .when()
                        .get()
                .then()
                        .extract().asString();

        JsonArray array = Json.createReader(new StringReader(jsonData)).readArray();
        array.forEach(val -> {
            given()
                    .baseUri(url)
            .when()
                    .delete("/" + ((JsonObject) val).getInt("id"))
            .then()
                    .statusCode(204);
        });
    }

    @Test
    public void retrieveNoMessage() {
        given()
                .baseUri(url)
        .when()
                .get()
        .then()
                .statusCode(200)
                .body(is("[]"));
    }

    @Test
    public void oneMessage() throws Exception {
        createMessage("Peach");

        String payload =
                given()
                        .baseUri(url)
                .when()
                        .get()
                .then()
                        .statusCode(200)
                        .extract().asString();

        JsonArray array = Json.createReader(new StringReader(payload)).readArray();

        assertThat(array).hasSize(1);
        assertThat(array.get(0).getValueType()).isEqualTo(JsonValue.ValueType.OBJECT);

        JsonObject obj = (JsonObject) array.get(0);
        assertThat(obj.getInt("id")).isNotNull().isGreaterThan(0);

        given()
                .baseUri(url)
        .when()
                .pathParam("messageId", obj.getInt("id"))
                .get("/{messageId}")
        .then()
                .statusCode(200)
                .body(containsString("Peach"));
    }

    @Test
    public void createMessage() {
        String payload =
                given()
                        .baseUri(url)
                .when()
                        .contentType(ContentType.JSON)
                        .body(convert(Json.createObjectBuilder().add("name", "Raspberry").build()))
                        .post()
                .then()
                        .statusCode(201)
                        .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getInt("id")).isNotNull().isGreaterThan(0);
        assertThat(obj.getString("name")).isNotNull().isEqualTo("Raspberry");
    }

    @Test
    public void createInvalidPayload() {
        given()
                .baseUri(url)
        .when()
                .contentType(ContentType.TEXT)
                .body("")
                .post()
        .then()
                .statusCode(415);
    }

    @Test
    public void createIllegalPayload() {
        Message badMessage = new Message("Carrot");
        badMessage.setId(2);

        String payload =
                given()
                        .baseUri(url)
                .when()
                        .contentType(ContentType.JSON)
                        .body(badMessage)
                        .post()
                .then()
                        .statusCode(422)
                        .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getString("error")).isNotNull();
        assertThat(obj.getInt("code")).isNotNull().isEqualTo(422);
    }

    @Test
    public void update() throws Exception {
        Message pear = createMessage("Pear");

        String response =
                given()
                        .baseUri(url)
                .when()
                        .pathParam("messageId", pear.getId())
                        .get("/{messageId}")
                .then()
                        .statusCode(200)
                        .extract().asString();

        pear = new ObjectMapper().readValue(response, Message.class);

        pear.setValue("Not Pear");

        response =
                given()
                        .baseUri(url)
                .when()
                        .pathParam("messageId", pear.getId())
                        .contentType(ContentType.JSON)
                        .body(new ObjectMapper().writeValueAsString(pear))
                        .put("/{messageId}")
                .then()
                        .statusCode(200)
                        .extract().asString();

        Message updatedPear = new ObjectMapper().readValue(response, Message.class);

        assertThat(pear.getId()).isEqualTo(updatedPear.getId());
        assertThat(updatedPear.getValue()).isEqualTo("Not Pear");
    }

    @Test
    public void updateWithUnknownId() throws Exception {
        Message bad = new Message("bad");
        bad.setId(12345678);

        given()
                .baseUri(url)
        .when()
                .pathParam("messageId", bad.getId())
                .contentType(ContentType.JSON)
                .body(new ObjectMapper().writeValueAsString(bad))
                .put("/{messageId}")
        .then()
                .statusCode(404)
                .extract().asString();
    }

    @Test
    public void updateInvalidPayload() {
        given()
                .baseUri(url)
        .when()
                .contentType(ContentType.TEXT)
                .body("")
                .post()
        .then()
                .statusCode(415);
    }

    @Test
    public void updateIllegalPayload() throws Exception {
        Message carrot = createMessage("Carrot");
        carrot.setValue(null);

        String payload =
                given()
                        .baseUri(url)
                .when()
                        .pathParam("messageId", carrot.getId())
                        .contentType(ContentType.JSON)
                        .body(new ObjectMapper().writeValueAsString(carrot))
                        .put("/{messageId}")
                .then()
                        .statusCode(422)
                        .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getString("error")).isNotNull();
        assertThat(obj.getInt("code")).isNotNull().isEqualTo(422);
    }

    @Test
    public void testDelete() throws Exception {
        Message orange = createMessage("Orange");

        given()
                .baseUri(url)
        .when()
                .delete("/" + orange.getId())
        .then()
                .statusCode(204);

        given()
                .baseUri(url)
        .when()
                .get()
        .then()
                .statusCode(200)
                .body(is("[]"));
    }

    @Test
    public void deleteWithUnknownId() {
        given()
                .baseUri(url)
        .when()
                .delete("/unknown")
        .then()
                .statusCode(404);

        given()
                .baseUri(url)
        .when()
                .get()
        .then()
                .statusCode(200)
                .body(is("[]"));
    }

    private Message createMessage(String name) throws Exception {
        String payload =
                given()
                        .baseUri(url)
                .when()
                        .contentType(ContentType.JSON)
                        .body(convert(Json.createObjectBuilder().add("name", name).build()))
                        .post()
                .then()
                        .statusCode(201)
                        .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getInt("id")).isNotNull().isGreaterThan(0);

        return new ObjectMapper().readValue(payload, Message.class);
    }

    private String convert(JsonObject object) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(stringWriter);
        jsonWriter.writeObject(object);
        jsonWriter.close();
        return stringWriter.toString();
    }
}
