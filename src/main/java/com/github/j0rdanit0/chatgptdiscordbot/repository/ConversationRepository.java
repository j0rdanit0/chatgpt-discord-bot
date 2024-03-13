package com.github.j0rdanit0.chatgptdiscordbot.repository;

import com.theokanning.openai.completion.chat.ChatMessage;
import discord4j.common.util.Snowflake;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ConversationRepository
{
    private static final Map<Snowflake, List<ChatMessage>> CONVERSATION_HISTORY = new ConcurrentHashMap<>();

    public List<ChatMessage> getConversationHistory( Snowflake threadId )
    {
        return CONVERSATION_HISTORY.getOrDefault( threadId, new ArrayList<>(1) );
    }

    public List<ChatMessage> appendConversationHistory( Snowflake threadId, ChatMessage chatMessage )
    {
        List<ChatMessage> conversation = getConversationHistory( threadId );
        conversation.add( chatMessage );

        CONVERSATION_HISTORY.put( threadId, conversation );

        return conversation;
    }
}
