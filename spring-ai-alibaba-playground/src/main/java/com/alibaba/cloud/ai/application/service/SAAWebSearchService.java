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

import com.alibaba.cloud.ai.application.advisor.ReasoningContentAdvisor;
import com.alibaba.cloud.ai.application.rag.WebSearchRetriever;
import com.alibaba.cloud.ai.application.rag.core.IQSSearchEngine;
import com.alibaba.cloud.ai.application.rag.data.DataClean;
import com.alibaba.cloud.ai.application.rag.join.ConcatenationDocumentJoiner;
import com.alibaba.cloud.ai.application.rag.prompt.CustomContextQueryAugmenter;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@Service
public class SAAWebSearchService {

	private final ChatClient chatClient;

	private final SimpleLoggerAdvisor simpleLoggerAdvisor;

	private final ReasoningContentAdvisor reasoningContentAdvisor;

	private final QueryTransformer queryTransformer;

	private final QueryExpander queryExpander;

	private final PromptTemplate queryArgumentPromptTemplate;

	private final WebSearchRetriever webSearchRetriever;

	// It works better here with DeepSeek-R1
	private static final String DEFAULT_WEB_SEARCH_MODEL = "deepseek-r1";

	//TODO DocumentRanker缺失
	public SAAWebSearchService(
			DataClean dataCleaner,
			QueryExpander queryExpander,
			IQSSearchEngine searchEngine,
//			DocumentRanker documentRanker,
			QueryTransformer queryTransformer,
			SimpleLoggerAdvisor simpleLoggerAdvisor,
			@Qualifier("dashscopeChatModel") ChatModel chatModel,
			@Qualifier("queryArgumentPromptTemplate") PromptTemplate queryArgumentPromptTemplate
	) {

		this.queryTransformer = queryTransformer;
		this.queryExpander = queryExpander;
		this.queryArgumentPromptTemplate = queryArgumentPromptTemplate;

		// reasoning content for DeepSeek-r1 is integrated into the output
		this.reasoningContentAdvisor = new ReasoningContentAdvisor(1);

		// Build chatClient
		this.chatClient = ChatClient.builder(chatModel)
				.defaultOptions(
						DashScopeChatOptions.builder()
								.withModel(DEFAULT_WEB_SEARCH_MODEL)
								// stream 模式下是否开启增量输出
								.withIncrementalOutput(true)
								.build())
				.build();

		// 日志
		this.simpleLoggerAdvisor = simpleLoggerAdvisor;

		this.webSearchRetriever = WebSearchRetriever.builder()
				.searchEngine(searchEngine)
				.dataCleaner(dataCleaner)
				.maxResults(2)
				.enableRanker(true)
//				.documentRanker(documentRanker)
				.build();
	}

	//Handle user input
	public Flux<String> chat(String prompt) {

		return chatClient.prompt()
				.advisors(
					createRetrievalAugmentationAdvisor(),
					reasoningContentAdvisor,
					simpleLoggerAdvisor
				).user(prompt)
				.stream()
				.content();
	}

	private RetrievalAugmentationAdvisor createRetrievalAugmentationAdvisor() {

		return RetrievalAugmentationAdvisor.builder()
				.documentRetriever(webSearchRetriever)
				.queryTransformers(queryTransformer)
				.queryAugmenter(
						new CustomContextQueryAugmenter(
								queryArgumentPromptTemplate,
								null,
								true)
				).queryExpander(queryExpander)
				.documentJoiner(new ConcatenationDocumentJoiner())
				.build();
	}

}
