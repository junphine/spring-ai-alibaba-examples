/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.toolcall.controller;

import com.alibaba.cloud.ai.toolcalling.githubtoolkit.CreatePullRequestService;
import com.alibaba.cloud.ai.toolcalling.githubtoolkit.GetIssueService;
import com.alibaba.cloud.ai.toolcalling.githubtoolkit.SearchRepositoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/githubtoolkit")
public class GithubtoolkitController {

    private final ChatClient dashScopeChatClient;
    private final SearchRepositoryService searchRepositoryService;
    public final GetIssueService getIssueService;
    public final CreatePullRequestService createPullRequestService;

    public GithubtoolkitController(ChatClient chatClient,
                                   SearchRepositoryService searchRepositoryService,
                                   GetIssueService getIssueService,
                                   CreatePullRequestService createPullRequestService) {
        this.dashScopeChatClient = chatClient;
        this.searchRepositoryService = searchRepositoryService;
        this.getIssueService = getIssueService;
        this.createPullRequestService = createPullRequestService;
    }

    /**
     * No Tool
     */
    @GetMapping("/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = "请帮我搜索仓库spring-ai-alibaba") String query) {
        return dashScopeChatClient.prompt(query).call().content();
    }

    /**
     * search for repositories function
     */
    @GetMapping("/chat-tool-function-search-repository")
    public String chatWithSearchRepositoryFunction(@RequestParam(value = "query", defaultValue = "请帮我搜索仓库spring-ai-alibaba") String query) {
        return dashScopeChatClient.prompt(query).toolCallbacks(
                FunctionToolCallback.builder("searchRepositoryService", searchRepositoryService)
                        .description("search for repositories")
                        .inputType(SearchRepositoryService.Request.class)
                        .build()
        ).call().content();
    }

    /**
     * get issue function
     */
    @GetMapping("/chat-tool-function-get-issue")
    public String chatWithGetIssueFunction(@RequestParam(value = "query", defaultValue = "帮我获取Issue 1") String query) {
        return dashScopeChatClient.prompt(query).toolCallbacks(
                FunctionToolCallback.builder("getIssueService", getIssueService)
                        .description("get Issue")
                        .inputType(GetIssueService.Request.class)
                        .build()
        ).call().content();
    }

    /**
     * create pull request function
     */
    @PostMapping("/chat-tool-function-pull-request")
    public String chatWithCreatePullRequestFunction(@RequestParam(value = "query",
            defaultValue = "帮我创建一个pr：标题为测试，内容是测试使用的pr，我实施更改的分支是test，希望合入的分支是main") String query) {
        return dashScopeChatClient.prompt(query).toolCallbacks(
                FunctionToolCallback.builder("createPullRequestService", createPullRequestService)
                        .description("create Pull Request")
                        .inputType(CreatePullRequestService.Request.class)
                        .build()
        ).call().content();
    }


}
