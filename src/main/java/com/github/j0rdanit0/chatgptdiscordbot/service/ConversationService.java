package com.github.j0rdanit0.chatgptdiscordbot.service;

import com.github.j0rdanit0.chatgptdiscordbot.configuration.BotProperties;
import com.github.j0rdanit0.chatgptdiscordbot.configuration.OpenAiProperties;
import com.github.j0rdanit0.chatgptdiscordbot.store.ConversationRepository;
import com.theokanning.openai.completion.chat.*;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService
{
    private final OpenAiProperties openAiProperties;
    private final OpenAiReactiveService openAiReactiveService;
    private final BotProperties botProperties;
    private final GatewayDiscordClient gatewayDiscordClient;
    private final ConversationRepository conversationRepository;

    public Flux<String> sendPrompt( Snowflake channelId, Snowflake userId, String prompt, String botPersonality )
    {
        log.debug( "Sending a prompt in channel %s from user %s: %s".formatted( channelId, userId, prompt ) );
        ChatCompletionRequest request = getChatCompletionRequest( channelId, userId, prompt, botPersonality );

        return openAiReactiveService
          .createChatCompletion( request )
          .map( ChatCompletionResult::getChoices )
          .flatMapMany( Flux::fromIterable )
          .map( ChatCompletionChoice::getMessage )
          .doOnNext( responseMessage -> conversationRepository.appendConversationHistory( channelId, userId, responseMessage ) )
          .map( ChatMessage::getContent );
    }

    private ChatCompletionRequest getChatCompletionRequest( Snowflake channelId, Snowflake userId, String prompt, String botPersonality )
    {
        List<ChatMessage> conversationHistory = conversationRepository.getConversationHistory( channelId, userId );
        if ( conversationHistory.isEmpty() )
        {
            ChatMessage initialMessage = new ChatMessage( ChatMessageRole.SYSTEM.value(), "Your personality is \"%s\". Users interact with you via Discord. Your Discord ID is \"%s\", so they will tag you like this: <@%s>. They might also call you by your bot name, which is \"%s\"".formatted( botPersonality, gatewayDiscordClient.getSelfId().asString(), gatewayDiscordClient.getSelfId().asString(), botProperties.getName() ) );
            conversationRepository.appendConversationHistory( channelId, userId, initialMessage );
        }

        ChatMessage promptMessage = new ChatMessage( ChatMessageRole.USER.value(), prompt );
        conversationHistory = conversationRepository.appendConversationHistory( channelId, userId, promptMessage );

        return ChatCompletionRequest
          .builder()
          .model( openAiProperties.getChatModel() )
          .messages( conversationHistory )
          .build();
    }
}
