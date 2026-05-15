package com.andrew.scoreboardbot;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ThreadSystemMessageCleaner extends ListenerAdapter
{
    @Override
    public void onMessageReceived(MessageReceivedEvent e)
    {
        Message message = e.getMessage();
        if(message.getType() != MessageType.THREAD_CREATED) return;

        if(!message.getAuthor().isBot()) return;

        message.delete().queue();
    }
}
