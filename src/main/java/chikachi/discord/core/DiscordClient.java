/*
 * Copyright (C) 2017 Chikachi
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package chikachi.discord.core;

import chikachi.discord.core.config.Configuration;
import chikachi.discord.core.config.minecraft.MinecraftConfig;
import chikachi.discord.core.config.types.MessageConfig;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiscordClient extends ListenerAdapter {
    private static DiscordClient instance;
    private ArrayList<ListenerAdapter> eventListeners = new ArrayList<>();
    private boolean isReady = false;
    private JDA jda;

    private DiscordClient() {
    }

    public static DiscordClient getInstance() {
        if (instance == null) {
            instance = new DiscordClient();
        }

        return instance;
    }

    @Override
    public void onReady(ReadyEvent event) {
        CoreLogger.Log("!!! THIS IS AN ALPHA VERSION !!!", true);
        CoreLogger.Log("!!! YOU HAVE BEEN WARNED !!!");
        CoreLogger.Log("Logged in as " + getSelf().getName());

        this.isReady = true;

        MinecraftConfig minecraftConfig = Configuration.getConfig().minecraft;

        DiscordClient.getInstance().broadcast(
            new Message(minecraftConfig.dimensions.generic.messages.serverStart),
            minecraftConfig.dimensions.generic.relayServerStart.getChannels(
                minecraftConfig.dimensions.generic.discordChannel
            )
        );

        this.isReady = false;
    }

    public void connect() {
        connect(false);
    }

    public void connect(boolean noMessage) {
        if (this.jda != null) {
            if (noMessage) {
                CoreLogger.Log("Is already connected", true);
            }
            return;
        }

        String token = Configuration.getConfig().discord.token;

        if (token == null || token.isEmpty()) {
            if (noMessage) {
                CoreLogger.Log("Missing token", true);
            }
            return;
        }

        try {
            JDABuilder builder = new JDABuilder(AccountType.BOT)
                .setToken(token)
                .setAudioEnabled(false)
                .setBulkDeleteSplittingEnabled(false)
                .addEventListener(this);

            for (ListenerAdapter eventListener : this.eventListeners) {
                builder.addEventListener(eventListener);
            }

            this.jda = builder
                .buildAsync();
        } catch (LoginException e) {
            CoreLogger.Log(
                String.format(
                    "Failed to connect to Discord: %s",
                    e.getMessage()
                ),
                true
            );
        } catch (Exception e) {
            CoreLogger.Log("Failed to connect to Discord", true);
            e.printStackTrace();
        }
    }

    public void addEventListner(ListenerAdapter eventListener) {
        if (eventListener != null) {
            if (this.eventListeners.contains(eventListener)) {
                return;
            }

            this.eventListeners.add(eventListener);

            if (this.jda != null) {
                this.jda.addEventListener(eventListener);
            }
        }
    }

    public boolean isConnected() {
        return this.jda != null && (this.isReady || this.jda.getStatus() == JDA.Status.CONNECTED);
    }

    public void disconnect() {
        disconnect(false);
    }

    public void disconnect(boolean noMessage) {
        if (this.jda == null) {
            if (!noMessage) {
                CoreLogger.Log("Is already disconnected", true);
            }
            return;
        }

        this.jda.shutdown(false);
        if (!noMessage) {
            CoreLogger.Log("Disconnected from Discord", true);
        }
        this.jda = null;
    }

    public SelfUser getSelf() {
        if (this.jda == null) {
            return null;
        }

        return this.jda.getSelfUser();
    }

    void broadcast(MessageConfig message, List<Long> channels) {
        broadcast(new Message(message), channels);
    }

    public void broadcast(Message message, Long... channels) {
        broadcast(message, Arrays.asList(channels));
    }

    public void broadcast(Message message, List<Long> channels) {
        if (channels == null || channels.size() == 0 || this.jda == null || this.jda.getStatus() != JDA.Status.CONNECTED) {
            return;
        }

        for (Long channelId : channels) {
            TextChannel channel = this.jda.getTextChannelById(channelId);
            if (channel != null) {
                if (Configuration.getConfig().discord.channels.channels.containsKey(channelId)) {
                    if (Configuration.getConfig().discord.channels.channels.get(channelId).webhook.trim().length() > 0) {
                        WebhookMessage webhookMessage = message.toWebhook(channel);
                        if (webhookMessage.queue(this.jda, channelId)) {
                            continue;
                        }
                    }
                }

                channel.sendMessage(message.getFormattedTextDiscord(channel)).queue();
            }
        }
    }
}