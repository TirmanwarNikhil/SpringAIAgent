package com.nikhil.SpringAIAgent.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "AI Agent Endpoints", description = "Endpoints for interacting with the PDF RAG context agent")
public class AgentController {

	private final ChatClient chatClient;

	private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

	public AgentController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
		this.chatClient = chatClientBuilder
				.defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build()).build();
	}

	@GetMapping("/api/ask")
	@Operation(summary = "Query the PDF Agent", description = "Pass a query string to fetch semantic context derived directly from the uploaded PDF document.")
	public String askAgent(
			@Parameter(description = "The specific question you want answered from the PDF text data context", example = "What is the return policy?") @RequestParam(value = "question") String question) {

		logger.info("Received question: {}", question);

		String response = this.chatClient.prompt().user(question).call().content();

		logger.info("Generated response: {}", response);
		return response;
	}

}
