package com.example.p22005unipifirechat.utils;

import com.example.p22005unipifirechat.BuildConfig;
import com.example.p22005unipifirechat.R;
import com.example.p22005unipifirechat.interfaces.SmartReplyListener;
import com.example.p22005unipifirechat.interfaces.SmartSummaryListener;
import com.example.p22005unipifirechat.modelclasses.Message;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AiManager {
    //Singleton Design Pattern
    private static AiManager instance;
    private final GenerativeModelFutures model;
    //executor is used to run the callback on a separate thread
    private final Executor executor;


    private AiManager() {
        // call gemini-2.5-flash model via the API key from the local.properties file
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", BuildConfig.GEMINI_API_KEY);
        model = GenerativeModelFutures.from(gm);
        executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized AiManager getInstance() {
        if (instance == null) {
            instance = new AiManager();
        }
        return instance;
    }




    //set the prompt to generate smart replies
    public void generateSmartReplies(List<Message> chatHistory, String currentUserId, SmartReplyListener listener) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are the user 'Me' in a chat conversation. Read the following recent messages:\n\n");

        // send the last 5 messages to the AI model
        int start = Math.max(0, chatHistory.size() - 5);
        for (int i = start; i < chatHistory.size(); i++) {
            Message msg = chatHistory.get(i);
            // Identify if the message was sent by the local user or the contact
            String sender = msg.getSenderId().equals(currentUserId) ? "Me" : "Friend";
            prompt.append(sender).append(": ").append(msg.getMessageText()).append("\n");
        }

        prompt.append("\nTask: Suggest 3 natural replies for 'Me' to send next. ")
                .append("Rules:\n")
                .append("1. Language: Use the SAME language as the messages above (e.g., if chat is in Greek, reply in Greek).\n")
                .append("2. Length: Maximum 4-5 words per reply.\n")
                .append("3. Format: Output ONLY the replies, strictly separated by the '|' symbol (e.g., Great!|See you then|Not sure).\n")
                .append("4. No preamble: Do not include any introductory text or explanations.");

        //an asynchronous call to the Generative Model
        Content content = new Content.Builder().addText(prompt.toString()).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        // Future is used to run the callback on a separate thread
        // it works like a contract as we don't know when the callback will be executed
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String aiText = result.getText();
                if (aiText != null && !aiText.isEmpty()) {
                    String[] splitReplies = aiText.split("\\|");
                    List<String> repliesList = new ArrayList<>();
                    for (String reply : splitReplies) {
                        repliesList.add(reply.trim().replaceAll("^\"|\"$", "")); // Καθαρίζουμε τυχόν εισαγωγικά
                    }
                    listener.onRepliesGenerated(repliesList);
                } else {
                    listener.onError("" + R.string.smart_reply_error);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                listener.onError(t.getMessage());
            }
        }, executor);
    }





    //smart summary method
    public void generateSummary(List<Message> chatHistory, String currentUserId, SmartSummaryListener listener) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an objective assistant. Your task is to summarize the following chat conversation:\n\n");

        // use the last 10 messages to write the summary
        int start = Math.max(0, chatHistory.size() - 10);
        for (int i = start; i < chatHistory.size(); i++) {
            Message msg = chatHistory.get(i);
            String sender = msg.getSenderId().equals(currentUserId) ? "Me" : "Friend";
            prompt.append(sender).append(": ").append(msg.getMessageText()).append("\n");
        }

        prompt.append("\nTask: Write a concise summary of the conversation above (2-3 sentences).")
                .append("\nRules:")
                .append("\n1. Language: Write the summary in the SAME language used in the chat (e.g., Greek, English).")
                .append("\n2. Perspective: Use the third-person or passive voice (e.g., 'Users decided...').")
                .append("\n3. Tone: Be objective and clear, focusing on the main topics or decisions made.")
                .append("\n4. Content: Do not include meta-comments like 'This is a summary' or 'In this chat'. Just provide the summary text.");

        Content content = new Content.Builder().addText(prompt.toString()).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String aiText = result.getText();
                if (aiText != null && !aiText.isEmpty()) {
                    listener.onSummaryGenerated(aiText.trim());
                } else {
                    listener.onError("" + R.string.summary_error);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                listener.onError(t.getMessage());
            }
        }, executor);
    }
}
