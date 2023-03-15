package com.github.j0rdanit0.chatgptdiscordbot.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties( "open-ai" )
public class OpenAiProperties
{
    private final String token = System.getenv( "OPENAI_API_KEY" );
    private String chatModel;
    private Duration timeout;
}
