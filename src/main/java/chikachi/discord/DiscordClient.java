package chikachi.discord;

import chikachi.discord.config.Configuration;
import chikachi.discord.config.experimental.ExperimentalDiscordListener;
import chikachi.discord.config.listener.DiscordListener;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;

public class DiscordClient {
    private static DiscordClient instance;
    private JDA jda;
    private DiscordListener listener = new DiscordListener();
    private ExperimentalDiscordListener experimentalListener = new ExperimentalDiscordListener();
    private TextChannel channel;
    public List<String> queue = new ArrayList<>();

    public static DiscordClient getInstance() {
        if (instance == null) {
            instance = new DiscordClient();
        }

        return instance;
    }

    private DiscordClient() {

    }

    void connect() {
        if (this.jda != null) {
            ChikachiDiscord.Log("Is already connected", true);
            return;
        }

        String token = Configuration.getToken();

        if (token.isEmpty()) {
            ChikachiDiscord.Log("Missing token", true);
            return;
        }

        try {
            this.jda = new JDABuilder()
                    .setBotToken(token)
                    .addListener(this.listener)
                    .addListener(this.experimentalListener)
                    .buildAsync();
        } catch (LoginException e) {
            ChikachiDiscord.Log("Failed to connect to Discord", true);
            e.printStackTrace();
        }
    }

    void disconnect() {
        if (this.jda == null) {
            ChikachiDiscord.Log("Is already disconnected", true);
            return;
        }

        this.jda.shutdown();
        this.jda = null;
    }

    public TextChannel getChannel() {
        if (this.channel == null) {
            this.channel = this.jda.getTextChannelById(Configuration.getChannel());
            if (this.channel == null) {
                ChikachiDiscord.Log("Failed to find channel", true);
                return null;
            }
        }

        return this.channel;
    }

    public boolean sendMessage(String message) {
        if (this.jda == null) {
            this.queue.add(message);
            return true;
        }

        TextChannel channel = getChannel();
        if (channel == null) {
            this.queue.add(message);
            return false;
        }

        if (message.contains("@")) {
            message = " " + message + " ";

            List<User> users = new ArrayList<>(this.channel.getGuild().getUsers());
            users.sort((o1, o2) -> o2.getUsername().length() - o1.getUsername().length());

            for (User user : users) {
                if (message.toLowerCase().contains("@" + user.getUsername().toLowerCase())) {
                    message = message.replaceAll("(?i)@" + user.getUsername() + "\\W", user.getAsMention());
                }
            }

            message = message.trim();
        }

        this.channel.sendMessage(message);
        return true;
    }
}
