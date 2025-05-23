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

package com.alibaba.cloud.ai.application.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.memory.ChatMemory;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@Service
public class SAAChatService {

	private final ChatClient chatClient;

	private final PromptTemplate deepThinkPromptTemplate;

	public SAAChatService(
			SimpleLoggerAdvisor simpleLoggerAdvisor,
			MessageChatMemoryAdvisor messageChatMemoryAdvisor,
			@Qualifier("dashscopeChatModel") ChatModel chatModel,
			@Qualifier("systemPromptTemplate") PromptTemplate systemPromptTemplate,
			@Qualifier("deepThinkPromptTemplate") PromptTemplate deepThinkPromptTemplate
	) {

		this.chatClient = ChatClient.builder(chatModel)
				.defaultSystem(
					systemPromptTemplate.getTemplate()
				).defaultAdvisors(
						simpleLoggerAdvisor,
						messageChatMemoryAdvisor
				).build();

		this.deepThinkPromptTemplate = deepThinkPromptTemplate;
	}

	public Flux<String> chat(String chatId, String model, String prompt) {

		return chatClient.prompt()
				.options(DashScopeChatOptions.builder()
						.withModel(model)
						.withTemperature(0.8)
						.withResponseFormat(DashScopeResponseFormat.builder()
								.type(DashScopeResponseFormat.Type.TEXT)
								.build()
						).build()
				).user(prompt)
				.advisors(memoryAdvisor -> memoryAdvisor
						.param(ChatMemory.CONVERSATION_ID, chatId)
				).stream()
				.content();
	}

	public Flux<String> deepThinkingChat(String chatId, String model, String prompt) {

		return chatClient.prompt()
				.options(DashScopeChatOptions.builder()
						.withModel(model)
						.withTemperature(0.8)
						.withResponseFormat(DashScopeResponseFormat.builder()
								.type(DashScopeResponseFormat.Type.TEXT)
								.build()
						).build()
				).system(deepThinkPromptTemplate.getTemplate())
				.user(prompt)
				.advisors(memoryAdvisor -> memoryAdvisor
						.param(ChatMemory.CONVERSATION_ID, chatId)
				).stream()
				.content();
	}
}
