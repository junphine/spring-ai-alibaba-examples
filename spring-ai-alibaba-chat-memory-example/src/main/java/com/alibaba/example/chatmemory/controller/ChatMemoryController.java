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

import com.alibaba.cloud.ai.memory.redis.RedisChatMemory;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@RestController
@RequestMapping("/chat-memory")
public class ChatMemoryController {

	private final ChatClient chatClient;

	private final MessageChatMemoryAdvisor jdbcChatMemory;

	private final MessageChatMemoryAdvisor redisChatMemory;

	//RedisMemory的另一种写法
	private final RedisChatMemory redisMemory;

	public ChatMemoryController(
			ChatModel chatModel,
			@Qualifier("jdbcMessageChatMemoryAdvisor") MessageChatMemoryAdvisor jdbcChatMemory,
			@Qualifier("redisMessageChatMemoryAdvisor") MessageChatMemoryAdvisor redisChatMemory,
			RedisChatMemory redisMemory
	) {

		this.jdbcChatMemory = jdbcChatMemory;
		this.redisChatMemory = redisChatMemory;
		this.chatClient = ChatClient.builder(chatModel).build();
		this.redisMemory = redisMemory;
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
				new MessageChatMemoryAdvisor(
						new InMemoryChatMemory())
		).advisors(
				a -> a
						.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
						.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)
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
								.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
								.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)
				).stream().content();
	}

	/**
	 * Redis Chat Memory 实现
	 */
	@GetMapping("/redis")
	public Flux<String> redis(
			@RequestParam("prompt") String prompt,
			@RequestParam("chatId") String chatId,
			HttpServletResponse response
	) {

		response.setCharacterEncoding("UTF-8");

		return chatClient.prompt(prompt).advisors(
				redisChatMemory
		).advisors(
				a -> a
						.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
						.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)
		).stream().content();
	}

	//RedisMemory的另一种写法
	/**
	 * chat对话
	 */
	@GetMapping("/chat")
	public Flux<String> redisChat(
			@RequestParam("prompt") String prompt,
			@RequestParam("chatId") String chatId,
			HttpServletResponse response
	) {

		response.setCharacterEncoding("UTF-8");

		return chatClient.prompt(prompt)
				.advisors(new MessageChatMemoryAdvisor(redisMemory))
				.advisors(
				a -> a
						.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
						.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)
				)
				.stream()
				.content();
	}

	/**
	 * 获取当前对话历史
	 */
	@GetMapping("/getRedisMemory")
	public List<Message> getRedisMemory(String chatId) {
		List<Message> chatRecord =  redisMemory.get(chatId);
		return chatRecord;
	}

	//删除历史对话
	@DeleteMapping("/deleteRedisMemory")
	public void deleteHistory(String chatId) {
		redisMemory.clear(String.valueOf(chatId));
	}
}
