package com.patron.feed_suggestor_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FeedSuggestorApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeedSuggestorApiApplication.class, args);
	}

}
