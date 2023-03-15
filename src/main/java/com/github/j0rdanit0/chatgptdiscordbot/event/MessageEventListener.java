package com.github.j0rdanit0.chatgptdiscordbot.event;

import com.github.j0rdanit0.chatgptdiscordbot.service.ConversationService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class MessageEventListener extends EventListener<MessageCreateEvent>
{
    private final ConversationService conversationService;

    public MessageEventListener( GatewayDiscordClient discordClient, ConversationService conversationService )
    {
        super( discordClient );
        this.conversationService = conversationService;
    }

    @Override
    public Class<MessageCreateEvent> getEventType()
    {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute( MessageCreateEvent event )
    {
        Mono<Void> result = Mono.empty();
        boolean isBot = event
          .getMessage()
          .getAuthor()
          .map( User::isBot )
          .orElse( false );

        boolean mentionsBot = event
          .getMessage()
          .getUserMentionIds()
          .contains( event.getClient().getSelfId() );

        if ( !isBot && mentionsBot )
        {
            log.info( "Got a new prompt" );
            result = event
              .getMessage()
              .getChannel()
              .cast( TextChannel.class )
              .flatMap( channel -> {
                  Snowflake userId = event.getMessage().getAuthor().orElseThrow().getId();
                  String prompt = event.getMessage().getContent();
                  String botPersonality = String.join( " ", channel.getName().split( "-" ) );

                  return conversationService
                    .sendPrompt( channel.getId(), userId, prompt, botPersonality )
                    .map( reply -> MessageCreateSpec
                      .builder()
                      .messageReference( event.getMessage().getId() )
                      .content( reply )
                      .build()
                    )
                    .flatMap( channel::createMessage )
                    .then();
              } );
        }

        return result;
    }
}
