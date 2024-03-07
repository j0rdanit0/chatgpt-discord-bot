package com.github.j0rdanit0.chatgptdiscordbot.service;

import com.github.j0rdanit0.chatgptdiscordbot.repository.ConversationRepository;
import com.theokanning.openai.completion.chat.*;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService
{
    private final OpenAiReactiveService openAiReactiveService;
    private final GatewayDiscordClient gatewayDiscordClient;
    private final ConversationRepository conversationRepository;

    @Value( "${bot.name}" )
    private String botName;

    @Value( "${open-ai.chat-model}" )
    private String openAiChatModel;

    public Flux<String> sendPrompt( Snowflake channelId, Snowflake userId, String prompt, String botPersonality )
    {
        log.debug( "Sending a prompt in channel {} from user {}: {}", channelId, userId, prompt );
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
            ChatMessage initialMessage = new ChatMessage( ChatMessageRole.SYSTEM.value(), "Your personality is \"%s\". I am a Discord bot that acts as a proxy between my users and you. Users interact with me via Discord, I give you their prompts, and I give them your responses. Your Discord ID is \"%s\", so they will tag you like this: <@%s>. They might also call you by your bot name, which is \"%s\". Answer at a high school senior level.".formatted( botPersonality, gatewayDiscordClient.getSelfId().asString(), gatewayDiscordClient.getSelfId().asString(), botName ) );
            conversationRepository.appendConversationHistory( channelId, userId, initialMessage );
        }

        ChatMessage promptMessage = new ChatMessage( ChatMessageRole.USER.value(), prompt );
        conversationHistory = conversationRepository.appendConversationHistory( channelId, userId, promptMessage );

        return ChatCompletionRequest
          .builder()
          .model( openAiChatModel )
          .messages( conversationHistory )
          .build();
    }
}
