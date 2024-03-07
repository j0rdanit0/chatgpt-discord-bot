package com.github.j0rdanit0.chatgptdiscordbot.configuration;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BotConfiguration
{
    @Value( "${bot.token}" )
    private String botToken;

    @Value( "${bot.name}" )
    private String botName;

    @Bean
    public GatewayDiscordClient discordClient()
    {
        return DiscordClient
          .create( botToken )
          .gateway()
          .setInitialPresence( shardInfo -> ClientPresence.online( ClientActivity.listening( "@" + botName ) ) )
          .login()
          .block();
    }
}
