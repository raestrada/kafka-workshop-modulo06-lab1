package com.kafkaworkshop.modulo3lab1;

import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Modulo3lab1Application implements CommandLineRunner {

  private static Logger LOG = LoggerFactory.getLogger(Modulo3lab1Application.class);

  @Autowired
  private Tasks tasks;

  public static void main(String[] args) {
    LOG.info("Starting Modulo3lab1Application");
    SpringApplication.run(Modulo3lab1Application.class, args);
  }

  @Override
  public void run(String... args) throws InterruptedException {
    LOG.info("EXECUTING : command line runner");

    CompletableFuture<Task> produce = tasks.produce(
      Integer.parseInt(System.getenv("MESSAGE_QTY"))
    );

    CompletableFuture.allOf(produce).join();
  }
}
