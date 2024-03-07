package com.github.j0rdanit0.chatgptdiscordbot.configuration;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenAiConfiguration
{
    @Value( "${open-ai.token}" )
    private String openAiToken;

    @Value( "${open-ai.timeout}" )
    private Duration openAiTimeout;

    @Bean
    public OpenAiService openAiService()
    {
        return new OpenAiService( openAiToken, openAiTimeout );
    }
}
