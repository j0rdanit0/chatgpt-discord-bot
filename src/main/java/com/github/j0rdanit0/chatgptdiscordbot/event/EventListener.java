package com.github.j0rdanit0.chatgptdiscordbot.event;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class EventListener<T extends Event>
{
    private final GatewayDiscordClient discordClient;
    private final Class<T> eventType;

    @SafeVarargs
    protected EventListener( GatewayDiscordClient discordClient, T... reified )
    {
        if ( reified.length > 0 )
        {
            throw new IllegalArgumentException( "Please don't pass any values to classTypeArray. It's a trick to detect the class automagically." );
        }

        this.discordClient = discordClient;
        this.eventType = (Class<T>) reified.getClass().getComponentType();
    }

    @PostConstruct
    public void registerEvent()
    {
        discordClient
          .on( eventType )
          .flatMap( event -> execute( event )
            .onErrorResume( error -> {
                handleError( error );
                return Mono.empty();
            } )
          )
          .subscribe();
    }

    public abstract Mono<Void> execute( T event );

    public void handleError( Throwable error )
    {
        log.error( "Unable to process " + eventType.getSimpleName(), error );
    }
}
