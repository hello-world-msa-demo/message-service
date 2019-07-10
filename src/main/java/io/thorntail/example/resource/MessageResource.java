package io.thorntail.example.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.thorntail.example.model.Message;

@Path("/messages")
@ApplicationScoped
public class MessageResource {
    @PersistenceContext(unitName = "MessagePU")
    private EntityManager em;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Message[] get() {
        return em.createNamedQuery("Messages.findAll", Message.class)
                .getResultList()
                .toArray(new Message[0]);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Message getSingle(@PathParam("id") Integer id) {
        return em.find(Message.class, id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response create(Message message) {
        if (message == null) {
            return error(415, "Invalid payload!");
        }

        if (message.getValue() == null || message.getValue().trim().length() == 0) {
            return error(422, "The value is required!");
        }

        if (message.getId() != null) {
            return error(422, "Id was invalidly set on request.");
        }

        try {
            em.persist(message);
        } catch (Exception e) {
            return error(500, e.getMessage());
        }
        return Response.ok(message).status(201).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response update(@PathParam("id") Integer id, Message message) {
        if (message == null) {
            return error(415, "Invalid payload!");
        }

        if (message.getValue() == null || message.getValue().trim().length() == 0) {
            return error(422, "The value is required!");
        }

        try {
            Message entity = em.find(Message.class, id);

            if (entity == null) {
                return error(404, "Message with id of " + id + " does not exist.");
            }

            entity.setValue(message.getValue());
            em.merge(entity);

            return Response.ok(entity).status(200).build();
        } catch (Exception e) {
            return error(500, e.getMessage());
        }
    }


    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional
    public Response delete(@PathParam("id") Integer id) {
        try {
            Message entity = em.find(Message.class, id);
            em.remove(entity);
        } catch (Exception e) {
            return error(500, e.getMessage());
        }
        return Response.status(204).build();
    }

    private Response error(int code, String message) {
        return Response
                .status(code)
                .entity(Json.createObjectBuilder()
                            .add("error", message)
                            .add("code", code)
                            .build()
                )
                .build();
    }
}