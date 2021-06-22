package com.kafkaworkshop.modulo3lab1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.text.MessageFormat;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;

@Service
public class Tasks {
    private static final Logger logger = LoggerFactory.getLogger(Tasks.class);

    @Autowired
    private Producer producer;

    @Async
	public CompletableFuture<Task> produce(int quantity) throws InterruptedException {
        Task task = new Task();

		logger.info(MessageFormat.format("PRODUCER {0}: generating {1} messages", task.getId(), quantity));

        Lorem lorem = LoremIpsum.getInstance();
        for(int messageCount = 0; messageCount < quantity; messageCount++){
            String message = MessageFormat.format(
                "{0} {1} from country {2}",
                lorem.getName(),
                lorem.getLastName(),
                lorem.getCountry()
            );

            logger.info(MessageFormat.format(
                "PRODUCER {0}: message {1} with value '{2}'",
                messageCount,
                message)
            );

            producer.sendMessage(message);

            Thread.sleep(1000L);
        }
		
		return CompletableFuture.completedFuture(task);
	}
}
