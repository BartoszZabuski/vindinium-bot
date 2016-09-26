package dev.challenge.vindinium.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import dev.challenge.vindinium.GamesManagerActor;

@Configuration
@EnableAutoConfiguration
public class RootApplicationConfig {

    @Bean
    public ActorSystem actorSystem() throws Exception {
        Config sysConfig = ConfigFactory.load("application.akka.conf");
        ActorSystem system = ActorSystem.create("vindiniumBotSystem", sysConfig);

        ActorRef gameManagerActor = system.actorOf(Props.create(GamesManagerActor.class), "gamesManagerActor");


        return system;
    }

}
