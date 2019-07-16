package io.openshift.booster.model;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "english_message")
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private Integer id;

    @Column(length = 10, unique = true)
    private String value;

    public Message() {

    }

    public Message(Integer id, String value) {
        this.id = id;
        this.value = value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return id + " " + value;
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj)
            return false;
        if (!(obj instanceof Message))
            return false;
        Message that = (Message) obj;
        if (that.value.equals(this.value) && Objects.equals(that.id, this.id))
            return true;
        else
            return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.value);
    }

}