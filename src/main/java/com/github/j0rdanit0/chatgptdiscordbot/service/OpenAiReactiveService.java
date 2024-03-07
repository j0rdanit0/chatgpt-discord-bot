package com.github.j0rdanit0.chatgptdiscordbot.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiReactiveService
{
    private final OpenAiService openAiService;

    private final Scheduler scheduler = Schedulers.boundedElastic();

    public Mono<ChatCompletionResult> createChatCompletion( ChatCompletionRequest request )
    {
        return Mono
          .fromSupplier( () -> openAiService.createChatCompletion( request ) )
          .subscribeOn( scheduler )
          .doOnNext( response -> log.debug( "Received response from OpenAI: {}", response ) );
    }
}
