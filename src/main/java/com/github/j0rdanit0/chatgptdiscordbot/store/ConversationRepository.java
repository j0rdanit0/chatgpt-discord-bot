package com.github.j0rdanit0.chatgptdiscordbot.store;

import com.theokanning.openai.completion.chat.ChatMessage;
import discord4j.common.util.Snowflake;

import java.util.List;

public interface ConversationRepository
{
    List<ChatMessage> getConversationHistory( Snowflake channelId, Snowflake userId );

    List<ChatMessage> appendConversationHistory( Snowflake channelId, Snowflake userId, ChatMessage chatMessage );
}
