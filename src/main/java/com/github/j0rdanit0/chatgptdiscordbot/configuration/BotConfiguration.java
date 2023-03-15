package com.github.j0rdanit0.chatgptdiscordbot.configuration;

import com.github.j0rdanit0.chatgptdiscordbot.store.ConversationMemoryRepository;
import com.github.j0rdanit0.chatgptdiscordbot.store.ConversationRepository;
import com.theokanning.openai.service.OpenAiService;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BotConfiguration
{
    private final BotProperties botProperties;
    private final OpenAiProperties openAiProperties;

    @Bean
    public GatewayDiscordClient discordClient()
    {
        return DiscordClient
          .create( botProperties.getToken() )
          .gateway()
          .setInitialPresence( shardInfo -> ClientPresence.online( ClientActivity.listening( "@" + botProperties.getName() ) ) )
          .login()
          .block();
    }

    @Bean
    public OpenAiService openAiService()
    {
        return new OpenAiService( openAiProperties.getToken(), openAiProperties.getTimeout() );
    }

    @Bean
    public ConversationRepository conversationRepository()
    {
        return new ConversationMemoryRepository();
    }
}
