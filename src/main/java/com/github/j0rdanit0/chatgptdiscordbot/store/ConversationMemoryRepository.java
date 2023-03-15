package com.github.j0rdanit0.chatgptdiscordbot.store;

import com.theokanning.openai.completion.chat.ChatMessage;
import discord4j.common.util.Snowflake;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConversationMemoryRepository implements ConversationRepository
{
    private static final Map<String, List<ChatMessage>> CONVERSATION_HISTORY = new ConcurrentHashMap<>();

    @Override
    public List<ChatMessage> getConversationHistory( Snowflake channelId, Snowflake userId )
    {
        String key = getKey( channelId, userId );
        return CONVERSATION_HISTORY.getOrDefault( key, new ArrayList<>(1) );
    }

    @Override
    public List<ChatMessage> appendConversationHistory( Snowflake channelId, Snowflake userId, ChatMessage chatMessage )
    {
        List<ChatMessage> conversation = getConversationHistory( channelId, userId );
        conversation.add( chatMessage );

        String key = getKey( channelId, userId );
        CONVERSATION_HISTORY.put( key, conversation );

        return conversation;
    }

    private String getKey( Snowflake channelId, Snowflake userId )
    {
        return channelId.asString() + userId.asString();
    }
}
