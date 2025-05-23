/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.example.chatmemory.controller;

import com.alibaba.cloud.ai.memory.jdbc.MysqlChatMemoryRepository;
import com.alibaba.cloud.ai.memory.redis.RedisChatMemoryRepository;
import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor.TOP_K;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;


/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@RestController
@RequestMapping("/chat-memory")
public class ChatMemoryController {

	private final ChatClient chatClient;

	private final MessageChatMemoryAdvisor jdbcChatMemory;

	private final MysqlChatMemoryRepository mysqlChatMemoryRepository;

	private final RedisChatMemoryRepository redisChatMemoryRepository;

	public ChatMemoryController(
			ChatModel chatModel,
			@Qualifier("jdbcMessageChatMemoryAdvisor") MessageChatMemoryAdvisor jdbcChatMemory,
			MysqlChatMemoryRepository mysqlChatMemoryRepository,
			RedisChatMemoryRepository redisChatMemoryRepository
	) {

		this.jdbcChatMemory = jdbcChatMemory;
		this.mysqlChatMemoryRepository = mysqlChatMemoryRepository;
		this.redisChatMemoryRepository = redisChatMemoryRepository;
		this.chatClient = ChatClient.builder(chatModel).build();
	}

	/**
	 * Spring AI 提供的基于内存的 Chat Memory 实现
	 */
	@GetMapping("/in-memory")
	public Flux<String> memory(
			@RequestParam("prompt") String prompt,
			@RequestParam("chatId") String chatId,
			HttpServletResponse response
	) {

		response.setCharacterEncoding("UTF-8");

		return chatClient.prompt(prompt).advisors(
				MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build()
		).advisors(
				a -> a
						.param(CONVERSATION_ID, chatId)
						.param(TOP_K, 100)
		).stream().content();
	}

	/**
	 * SQLite Chat Memory 实现
	 */
	@GetMapping("/sqlite")
	public Flux<String> sqlite(
			@RequestParam("prompt") String prompt,
			@RequestParam("chatId") String chatId,
			HttpServletResponse response
	) {

		response.setCharacterEncoding("UTF-8");

		return chatClient.prompt(prompt)
				.advisors(jdbcChatMemory)
				.advisors(
						a -> a
								.param(CONVERSATION_ID, chatId)
								.param(TOP_K, 100)
				).stream().content();
	}

	/**
	 * 流式聊天接口（基于 MySQL Chat Memory）
	 */
	@GetMapping("/mysql")
	public Flux<String> mysql(
			@RequestParam("prompt") String prompt,
			@RequestParam("chatId") String chatId,
			HttpServletResponse response) {

		response.setCharacterEncoding("UTF-8");

		ChatMemory chatMemory = MessageWindowChatMemory.builder()
				.chatMemoryRepository(mysqlChatMemoryRepository)
				.maxMessages(10)
				.build();

		return chatClient.prompt(prompt)
				.advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
				.advisors(a -> a
						.param(CONVERSATION_ID, chatId)
						.param(TOP_K, 100)
				)
				.stream()
				.content();
	}
	/**
	 * 流式聊天接口（基于 Redis Chat Memory）
	 */
	@GetMapping("/redis")
	public Flux<String> redis(
			@RequestParam("prompt") String prompt,
			@RequestParam("chatId") String chatId,
			HttpServletResponse response) {

		response.setCharacterEncoding("UTF-8");

		ChatMemory chatMemory = MessageWindowChatMemory.builder()
				.chatMemoryRepository(redisChatMemoryRepository)
				.maxMessages(10)
				.build();

		return chatClient.prompt(prompt)
				.advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
				.advisors(a -> a
						.param(CONVERSATION_ID, chatId)
						.param(TOP_K, 100)
				)
				.stream()
				.content();
	}

}
