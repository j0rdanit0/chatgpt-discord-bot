package com.github.j0rdanit0.chatgptdiscordbot.service;

import com.github.j0rdanit0.chatgptdiscordbot.repository.ConversationRepository;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService
{
    private final OpenAiService openAiService;
    private final GatewayDiscordClient gatewayDiscordClient;
    private final ConversationRepository conversationRepository;

    @Value( "${bot.name}" )
    private String botName;

    @Value( "${open-ai.chat-model}" )
    private String openAiChatModel;

    public String startConversation( Snowflake threadId, Snowflake userId, String prompt, String botPersonality )
    {
        log.debug( "Sending a prompt in thread {} from user {}: {}", threadId, userId, prompt );
        ChatCompletionRequest request = getFirstChatCompletionRequest( threadId, prompt, botPersonality );

        return requestChatCompletion( request, threadId );
    }

    public String continueConversation( Snowflake threadId, Snowflake userId, String prompt )
    {
        log.debug( "Continuing conversation in thread {} from user {}: {}", threadId, userId, prompt );
        ChatCompletionRequest request = getSubsequentChatCompletionRequest( threadId, prompt );

        return requestChatCompletion( request, threadId );
    }

    private String requestChatCompletion( ChatCompletionRequest request, Snowflake threadId )
    {
        ChatMessage responseMessage = openAiService.createChatCompletion( request ).getChoices().getFirst().getMessage();
        log.debug( "Got response from Open AI:\n{}", responseMessage.getContent() );
        conversationRepository.appendConversationHistory( threadId, responseMessage );
        return responseMessage.getContent();
    }

    public String getNewThreadTitle( Snowflake threadId )
    {
        log.debug( "Getting new thread title" );
        ChatCompletionRequest request = getTitleChatCompletionRequest( threadId );

        return openAiService.createChatCompletion( request ).getChoices().getFirst().getMessage().getContent();
    }

    private ChatCompletionRequest getFirstChatCompletionRequest( Snowflake threadId, String prompt, String botPersonality )
    {
        List<ChatMessage> conversationHistory = conversationRepository.getConversationHistory( threadId );
        if ( conversationHistory.isEmpty() )
        {
            ChatMessage initialMessage = new ChatMessage(
              ChatMessageRole.SYSTEM.value(),
              ( "You are a Discord bot whose personality is \"%s\". " +
                "Users may refer to you like this: <@%s> or this: \"%s\". " +
                "Use Discord's emojis or markdown syntax to style your answers. " +
                "Use no more than 2000 characters. " +
                "Answer at a high school level unless requested otherwise. " +
                "Use brief answers unless requested otherwise. "
              ).formatted( botPersonality, gatewayDiscordClient.getSelfId().asString(), botName ) );
            conversationRepository.appendConversationHistory( threadId, initialMessage );
        }

        return getSubsequentChatCompletionRequest( threadId, prompt );
    }

    private ChatCompletionRequest getSubsequentChatCompletionRequest( Snowflake threadId, String prompt )
    {
        ChatMessage promptMessage = new ChatMessage( ChatMessageRole.USER.value(), prompt );
        List<ChatMessage> conversationHistory = conversationRepository.appendConversationHistory( threadId, promptMessage );

        return ChatCompletionRequest
          .builder()
          .model( openAiChatModel )
          .messages( conversationHistory )
          .build();
    }

    private ChatCompletionRequest getTitleChatCompletionRequest( Snowflake threadId )
    {
        ChatMessage promptMessage = new ChatMessage(
          ChatMessageRole.USER.value(),
          "Give a summary of our conversation so far in 6 words or fewer. " +
            "Optionally, you may include a Discord-supported emoji unicode at the beginning if one exists that represents the key point of the conversation. " );
        List<ChatMessage> conversationHistory = new ArrayList<>( conversationRepository.getConversationHistory( threadId ) );
        conversationHistory.add( promptMessage );

        return ChatCompletionRequest
          .builder()
          .model( openAiChatModel )
          .messages( conversationHistory )
          .build();
    }
}
