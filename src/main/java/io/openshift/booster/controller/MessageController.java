package io.openshift.booster.controller;

import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.openshift.booster.exception.NotFoundException;
import io.openshift.booster.exception.UnprocessableEntityException;
import io.openshift.booster.exception.UnsupportedMediaTypeException;
import io.openshift.booster.model.Message;
import io.openshift.booster.repository.MessageRepository;

@RestController
@RequestMapping(value = "/api/messages")
public class MessageController {

    private final MessageRepository repository;

    public MessageController(MessageRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}")
    public Message get(@PathVariable("id") Integer id) {
        verifyMessageExists(id);

        return repository.findById(id).get();
    }

    @GetMapping
    public List<Message> getAll() {
        Spliterator<Message> messages = repository.findAll()
                .spliterator();

        return StreamSupport
                .stream(messages, false)
                .collect(Collectors.toList());
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public Message post(@RequestBody(required = false) Message message) {
        verifyCorrectPayload(message);

        return repository.save(message);
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{id}")
    public Message put(@PathVariable("id") Integer id, @RequestBody(required = false) Message message) {
        verifyMessageExists(id);
        verifyCorrectPayload(message);

        message.setId(id);
        return repository.save(message);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Integer id) {
        verifyMessageExists(id);

        repository.deleteById(id);
    }

    private void verifyMessageExists(Integer id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException(String.format("Message with id=%d was not found", id));
        }
    }

    private void verifyCorrectPayload(Message message) {
        if (Objects.isNull(message)) {
            throw new UnsupportedMediaTypeException("Message cannot be null");
        }

        if (!Objects.isNull(message.getId())) {
            throw new UnprocessableEntityException("Id field must be generated");
        }
    }

}
