package com.github.j0rdanit0.chatgptdiscordbot.event;

import com.github.j0rdanit0.chatgptdiscordbot.service.ConversationService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.StartThreadSpec;
import discord4j.core.spec.ThreadChannelEditSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class MessageEventListener extends EventListener<MessageCreateEvent>
{
    public static final ReactionEmoji SPEECH_BALLOON = ReactionEmoji.unicode( "\uD83D\uDCAC" );

    private final ConversationService conversationService;
    private final TaskExecutor taskExecutor;

    public MessageEventListener( GatewayDiscordClient discordClient, ConversationService conversationService, TaskExecutor taskExecutor )
    {
        super( discordClient );
        this.conversationService = conversationService;
        this.taskExecutor = taskExecutor;
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

        if ( !isBot )
        {
            taskExecutor.execute( () -> {
                boolean mentionsTheBot = event
                  .getMessage()
                  .getUserMentionIds()
                  .contains( event.getClient().getSelfId() );

                MessageChannel messageChannel = event.getMessage().getChannel().block();

                if ( mentionsTheBot && messageChannel instanceof TextChannel textChannel )
                {
                    startConversation( event.getMessage(), textChannel );
                }
                else if ( messageChannel instanceof ThreadChannel threadChannel )
                {
                    continueConversation( event.getMessage(), threadChannel );
                }
            } );
        }

        return result;
    }

    private void startConversation( Message promptMessage, TextChannel textChannel )
    {
        log.info( "Got a new prompt" );
        User prompter = promptMessage.getAuthor().orElseThrow();
        Snowflake userId = prompter.getId();
        String prompt = promptMessage.getContent();
        String botPersonality = String.join( " ", textChannel.getName().split( "-" ) );

        ThreadChannel threadChannel = promptMessage.startThread(
          StartThreadSpec
            .builder()
            .name( "New chat" )
            .reason( prompt )
            .autoArchiveDuration( ThreadChannel.AutoArchiveDuration.DURATION4 )
            .build()
        ).block();

        threadChannel.addMember( prompter ).subscribe();

        doWithTyping( threadChannel, () -> {
            String reply;
            try
            {
                reply = conversationService.startConversation( threadChannel.getId(), userId, prompt, botPersonality );

                String newThreadTitle = conversationService.getNewThreadTitle( threadChannel.getId() );
                threadChannel.edit(
                  ThreadChannelEditSpec
                    .builder()
                    .name( newThreadTitle )
                    .build()
                ).subscribe( null, exception -> log.error( "Unable to get new thread title", exception ) );
            }
            catch ( Exception exception )
            {
                log.error( "Unable to send prompt", exception );
                reply = "Error occurred: " + exception.getMessage();
            }

            threadChannel.createMessage( reply ).subscribe();
        } );
    }

    private void continueConversation( Message promptMessage, ThreadChannel threadChannel )
    {
        log.info( "Continuing an existing conversation" );

        doWithTyping( threadChannel, () -> {
            Snowflake userId = promptMessage.getAuthor().orElseThrow().getId();
            String prompt = promptMessage.getContent();

            String reply;
            try
            {
                reply = conversationService.continueConversation( threadChannel.getId(), userId, prompt );
            }
            catch ( Exception exception )
            {
                log.error( "Unable to continue conversation", exception );
                reply = "Error occurred: " + exception.getMessage();
            }

            threadChannel.createMessage( reply ).subscribe();
        } );
    }

    private void doWithTyping( MessageChannel messageChannel, Runnable runnable )
    {
        Message typingMessage = messageChannel.createMessage( SPEECH_BALLOON.asFormat() ).block();

        try
        {
            runnable.run();
        }
        finally
        {
            typingMessage.delete().subscribe();
        }
    }
}
