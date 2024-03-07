package com.github.j0rdanit0.chatgptdiscordbot.event;

import com.github.j0rdanit0.chatgptdiscordbot.service.ConversationService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
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
                    .onErrorResume( error -> {
                        log.error( "Unable to send prompt", error );
                        return Mono.just( "Error occurred: " + error.getMessage() );
                    } )
                    .map( reply -> buildReplyMessage( event.getMessage().getId(), reply ) )
                    .flatMap( channel::createMessage )
                    .then();
              } );
        }

        return result;
    }

    private MessageCreateSpec buildReplyMessage( Snowflake messageId, String reply )
    {
        return MessageCreateSpec
          .builder()
          .messageReference( messageId )
          .addEmbed( EmbedCreateSpec
            .builder()
            .color( Color.of( 125, 108, 178 ) )
            .description( reply )
            .build()
          )
          .build();
    }
}
