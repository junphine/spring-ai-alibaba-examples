/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.observationhandlerexample.controller;

import com.alibaba.cloud.ai.observationhandlerexample.observationHandler.CustomerObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: XiaoYunTao
 * @Date: 2024/12/31
 */
@RestController
@RequestMapping("/chat")
public class ChatModelController {

	@RequestMapping(value = "test", method = RequestMethod.GET)
	public String add(String message) {
		ObservationRegistry registry = ObservationRegistry.create();
		registry.observationConfig().observationHandler(new CustomerObservationHandler());
		ModelManagementOptions managementOptions = ModelManagementOptions.builder().build();
		OllamaChatModel ollamaChatModel = OllamaChatModel.builder()
			.defaultOptions(OllamaOptions.builder().model("qwen2.5").build())
			.observationRegistry(registry)
			.modelManagementOptions(managementOptions)
			.build();
		return ollamaChatModel.call(message);
	}

}
