package com.github.j0rdanit0.chatgptdiscordbot.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties( "bot" )
public class BotProperties
{
    private final String token = System.getenv( "CHATGPT_BOT_TOKEN" );
    private String name;
}
