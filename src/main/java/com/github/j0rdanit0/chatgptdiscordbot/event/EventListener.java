package com.github.j0rdanit0.chatgptdiscordbot.event;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public abstract class EventListener<T extends Event>
{
    private final GatewayDiscordClient discordClient;

    @PostConstruct
    public void registerEvent()
    {
        discordClient
          .on( getEventType() )
          .flatMap( this::execute )
          .onErrorContinue( this::handleError )
          .subscribe();
    }

    public abstract Class<T> getEventType();

    public abstract Mono<Void> execute( T event );

    public void handleError( Throwable error, Object object )
    {
        log.error( "Unable to process " + getEventType().getSimpleName(), error );
    }
}
