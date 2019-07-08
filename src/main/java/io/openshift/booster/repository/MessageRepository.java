package io.openshift.booster.repository;

import org.springframework.data.repository.CrudRepository;

import io.openshift.booster.model.Message;

public interface MessageRepository extends CrudRepository<Message, Integer> {
}
