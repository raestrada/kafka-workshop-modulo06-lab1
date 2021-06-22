package com.kafkaworkshop.modulo3lab1;

import java.util.UUID;

public class Task {

  private String id;

  public Task() {
    UUID uuid = UUID.randomUUID();
    id = uuid.toString();
  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return "Task [id=" + id + "]";
  }
}
