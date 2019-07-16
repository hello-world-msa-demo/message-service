/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openshift.booster;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertFalse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import io.openshift.booster.model.Message;
import io.openshift.booster.repository.MessageRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BoosterApplicationTest {

    private static final String MESSAGES_PATH = "api/messages";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private MessageRepository MessageRepository;

    @Before
    public void beforeTest() {
        MessageRepository.deleteAll();
        RestAssured.baseURI = String.format("http://localhost:%d/" + MESSAGES_PATH, port);
    }

    @Test
    public void testGetAll() {
        Message cherry = MessageRepository.save(new Message(1, "Cherry"));
        Message apple = MessageRepository.save(new Message(2, "Apple"));
        requestSpecification()
                .get()
                .then()
                .statusCode(200)
                .body("id", hasItems(cherry.getId(), apple.getId()))
                .body("value", hasItems(cherry.getValue(), apple.getValue()));
    }

    @Test
    public void testGetEmptyArray() {
        requestSpecification()
                .get()
                .then()
                .statusCode(200)
                .body(is("[]"));
    }

    @Test
    public void testGetOne() {
        Message cherry = MessageRepository.save(new Message(3, "Cherry"));
        requestSpecification()
                .get(String.valueOf(cherry.getId()))
                .then()
                .statusCode(200)
                .body("id", is(cherry.getId()))
                .body("value", is(cherry.getValue()));
    }

    @Test
    public void testGetNotExisting() {
        requestSpecification()
                .get("0")
                .then()
                .statusCode(404);
    }

//    @Test
//    public void testPost() {        
//        requestSpecification()
//                .contentType(ContentType.JSON)
//                .body(Collections.singletonMap("value", "Cherry"))
//                .post()
//                .then()
//                .statusCode(201)
//                .body("id", not(isEmptyString()))
//                .body("value", is("Cherry"));
//    }

    @Test
    public void testPostWithWrongPayload() {
        requestSpecification()
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap("id", 0))
                .when()
                .post()
                .then()
                .statusCode(422);
    }

    @Test
    public void testPostWithNonJsonPayload() {
        requestSpecification()
                .contentType(ContentType.XML)
                .when()
                .post()
                .then()
                .statusCode(415);
    }

    @Test
    public void testPostWithEmptyPayload() {
        requestSpecification()
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(415);
    }

    @Test
    public void testPut() {
        Message cherry = MessageRepository.save(new Message(4, "Cherry"));
        requestSpecification()
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap("value", "Lemon"))
                .when()
                .put(String.valueOf(cherry.getId()))
                .then()
                .statusCode(200)
                .body("id", is(cherry.getId()))
                .body("value", is("Lemon"));

    }

    @Test
    public void testPutNotExisting() {
        requestSpecification()
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap("value", "Lemon"))
                .when()
                .put("/0")
                .then()
                .statusCode(404);
    }

    @Test
    public void testPutWithWrongPayload() {
        Message cherry = MessageRepository.save(new Message(5, "Cherry"));
        requestSpecification()
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap("id", 0))
                .when()
                .put(String.valueOf(cherry.getId()))
                .then()
                .statusCode(422);
    }

    @Test
    public void testPutWithNonJsonPayload() {
        Message cherry = MessageRepository.save(new Message(6, "Cherry"));
        requestSpecification()
                .contentType(ContentType.XML)
                .when()
                .put(String.valueOf(cherry.getId()))
                .then()
                .statusCode(415);
    }

    @Test
    public void testPutWithEmptyPayload() {
        Message cherry = MessageRepository.save(new Message(7, "Cherry"));
        requestSpecification()
                .contentType(ContentType.JSON)
                .when()
                .put(String.valueOf(cherry.getId()))
                .then()
                .statusCode(415);
    }

    @Test
    public void testDelete() {
        Message cherry = MessageRepository.save(new Message(8, "Cherry"));
        requestSpecification()
                .delete(String.valueOf(cherry.getId()))
                .then()
                .statusCode(204);
        assertFalse(MessageRepository.existsById(cherry.getId()));
    }

    @Test
    public void testDeleteNotExisting() {
        requestSpecification()
                .delete("/0")
                .then()
                .statusCode(404);
    }


    private RequestSpecification requestSpecification() {
        return given().baseUri(String.format("http://localhost:%d/%s", port, MESSAGES_PATH));
    }
}
