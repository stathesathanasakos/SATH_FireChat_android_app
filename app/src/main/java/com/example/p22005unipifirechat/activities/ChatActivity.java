package com.example.p22005unipifirechat.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.p22005unipifirechat.R;
import com.example.p22005unipifirechat.interfaces.SmartReplyListener;
import com.example.p22005unipifirechat.interfaces.SmartSummaryListener;
import com.example.p22005unipifirechat.interfaces.IMessageActionListener;
import com.example.p22005unipifirechat.interfaces.MessagesListener;
import com.example.p22005unipifirechat.interfaces.MessageActionResultListener;
import com.example.p22005unipifirechat.interfaces.UserImageListener;
import com.example.p22005unipifirechat.adapters.MessageAdapter;
import com.example.p22005unipifirechat.modelclasses.Message;
import com.example.p22005unipifirechat.utils.AiManager;
import com.example.p22005unipifirechat.utils.AuthManager;
import com.example.p22005unipifirechat.utils.ChatManager;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class ChatActivity extends BaseActivity {
    private ImageButton btnBack, btnSend, btnAi, btnCloseSummary;
    private HorizontalScrollView scrollSmartReplies;
    private LinearLayout layoutSmartReplies, layoutSummary;
    private EditText etMessage;
    private TextView tvChatUser, tvSummaryText;
    private RecyclerView recyclerChat;
    private View navigationBarSpacer;
    private MessageAdapter messageAdapter;
    private List<Message> mChat;
    private ChatManager chatManager;
    private AuthManager authManager;
    private AiManager aiManager;
    private SmartReplyListener smartReplyListener;
    private SmartSummaryListener smartSummaryListener;
    private ValueEventListener messagesListener;
    private String otherUserId;
    private String otherUsername;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initManagers();
        initIntentData();
        initViews();
        setupRecyclerView();
        setupClickListeners();
        setupWindowInsets();
        loadChatContent();
    }

    private void initManagers() {
        chatManager = ChatManager.getInstance();
        authManager = AuthManager.getInstance();
        currentUserId = authManager.getCurrentUser().getUid();
        aiManager = AiManager.getInstance();
    }

    private void initIntentData() {
        //Passing these two values from MainActivity via intent.putExtra() to use them here directly
        // avoiding the need to look up the receiver's UID again.
        otherUserId = getIntent().getStringExtra("other_uid");
        otherUsername = getIntent().getStringExtra("other_username");
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnSend = findViewById(R.id.btnSend);
        etMessage = findViewById(R.id.etMessage);
        tvChatUser = findViewById(R.id.tvChatUser);
        recyclerChat = findViewById(R.id.recyclerChat);
        navigationBarSpacer = findViewById(R.id.navigationBarSpacer);
        tvChatUser.setText(otherUsername);
        btnAi = findViewById(R.id.btnAi);
        layoutSummary = findViewById(R.id.layoutSummary);
        btnCloseSummary = findViewById(R.id.btnCloseSummary);
        tvSummaryText = findViewById(R.id.tvSummaryText);
        scrollSmartReplies = findViewById(R.id.scrollSmartReplies);
        layoutSmartReplies = findViewById(R.id.layoutSmartReplies);
    }

    private void setupRecyclerView() {
        recyclerChat.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        //bottom-up scroll
        linearLayoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(linearLayoutManager);
        mChat = new ArrayList<>();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> handleSendMessage());
        btnAi.setOnClickListener(v -> handleSmartReply());
        btnAi.setOnLongClickListener(v -> handleSummary());
        btnCloseSummary.setOnClickListener(v -> layoutSummary.setVisibility(View.GONE));
    }




    private void handleSendMessage() {
        String msg = etMessage.getText().toString().trim();
        // if the message is not empty chatManager will update the database
        if (!TextUtils.isEmpty(msg)) {
            chatManager.sendMessage(currentUserId, otherUserId, msg, null);
            etMessage.setText("");
        } else {
            Toast.makeText(this, R.string.cannot_send_an_empty_message, Toast.LENGTH_SHORT).show();
        }
    }




    private void handleSmartReply() {
        //Ai can't give an output if there are no messages in the conversation
        if (mChat == null || mChat.isEmpty()) {
            Toast.makeText(this, R.string.no_messages_yet, Toast.LENGTH_SHORT).show();
            return;
        }

        //Ai mustn't reply to users own messages so it can work only if the other
        // user sent the most recent message
        Message lastMessage = mChat.get(mChat.size() - 1);
        if (lastMessage.senderId.equals(currentUserId)) {
            Toast.makeText(this, R.string.wait_for_reply, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.ai_thinking, Toast.LENGTH_SHORT).show();

        //Gemini API to generate smart replies and send them via the interface
        AiManager.getInstance().generateSmartReplies(mChat, currentUserId, new SmartReplyListener() {
            @Override
            public void onRepliesGenerated(List<String> suggestions) {
                //run n ui thread to update the UI with the smart replies
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    layoutSmartReplies.removeAllViews(); // remove previous replies

                    for (String reply : suggestions) {
                        // create a chip button for each reply
                        Button chip = new Button(ChatActivity.this);
                        chip.setText(reply);
                        chip.setAllCaps(false); // don't capitalize the text

                        chip.setOnClickListener(v -> {
                            etMessage.setText(reply);
                            scrollSmartReplies.setVisibility(View.GONE);
                        });

                        layoutSmartReplies.addView(chip);
                    }

                    scrollSmartReplies.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    Log.e("GeminiError", "Το πραγματικό σφάλμα είναι: " + errorMessage);

                    Toast.makeText(ChatActivity.this, R.string.smart_reply_error + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }




    private boolean handleSummary() {
        if (mChat == null || mChat.isEmpty()) {
            Toast.makeText(this, R.string.no_messages_yet, Toast.LENGTH_SHORT).show();
            return true;
        }

        // show and prepare the summary layout
        layoutSummary.setVisibility(View.VISIBLE);
        tvSummaryText.setText(R.string.ai_thinking);
        Toast.makeText(this, R.string.ai_thinking, Toast.LENGTH_SHORT).show();

        AiManager.getInstance().generateSummary(mChat, currentUserId, new SmartSummaryListener() {
            @Override
            public void onSummaryGenerated(String summary) {
                //run n ui thread to update the UI with the summary
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    //summary is ready to be displayed
                    tvSummaryText.setText(summary);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    //Log.e("GeminiError", "Real error is: " + errorMessage);

                    Toast.makeText(ChatActivity.this, R.string.summary_error + errorMessage, Toast.LENGTH_SHORT).show();
                    layoutSummary.setVisibility(View.GONE);
                });
            }
        });

        return true;
    }






    private void loadChatContent() {
        // get user image from the database to call the adapter
        chatManager.getUserImage(otherUserId, new UserImageListener() {
            @Override
            public void onImageRetrieved(String imageUrl) {
                setupMessageListener(imageUrl);
            }

            @Override
            public void onError(String error) {
                setupMessageListener("default");
            }
        });
    }

    private void setupMessageListener(String otherUserImageUrl) {
        // initialize the message listener to find new messages in the database
        messagesListener = chatManager.listenForMessages(currentUserId, otherUserId, new MessagesListener() {
            @Override
            public void onMessagesRetrieved(List<Message> messages) {
                //if a new message exists update the recyclerView (UI)
                updateUIWithMessages(messages, otherUserImageUrl);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ChatActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWithMessages(List<Message> messages, String imageUrl) {
        mChat.clear();
        mChat.addAll(messages);

        if (messageAdapter == null) {
            //messageAdapter returns the messages to the recyclerView
            messageAdapter = new MessageAdapter(ChatActivity.this, mChat, imageUrl, otherUsername, new IMessageActionListener() {
                @Override
                public void onMessageLongClick(Message message) {
                    if (message.senderId.equals(currentUserId)) {
                        showDeleteConfirmationDialog(message);
                    } else {
                        Toast.makeText(ChatActivity.this, R.string.not_allowed_to_delete_messages, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            recyclerChat.setAdapter(messageAdapter);
        } else {
            messageAdapter.notifyDataSetChanged();
        }

        scrollToBottom();
    }


    private void scrollToBottom() {
        // smooth scroll to the bottom of the recyclerView using a method import from the adapter
        if (messageAdapter != null && messageAdapter.getItemCount() > 0) {
            //if messages exist scroll to the bottom
            recyclerChat.post(() -> recyclerChat.smoothScrollToPosition(messageAdapter.getItemCount() - 1));
        }
    }


    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootView), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            
            // If the keyboard is shown, we use IME insets, otherwise we use the Navigation bar height
            int bottomInset = ime.bottom > 0 ? ime.bottom : systemBars.bottom;
            
            if (navigationBarSpacer != null) {
                navigationBarSpacer.getLayoutParams().height = bottomInset;
                navigationBarSpacer.requestLayout();            }
            return WindowInsetsCompat.CONSUMED;
        });

        recyclerChat.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                scrollToBottom();
            }
        });
    }





    private void deleteMessage(Message message) {
        if (message.getKey() != null) {
            chatManager.deleteMessage(message.getKey(), new MessageActionResultListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(ChatActivity.this, R.string.message_deleted_successfully, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(ChatActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showDeleteConfirmationDialog(Message message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_message_title)
                .setMessage(R.string.delete_message_confirmation)
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteMessage(message))
                .setNegativeButton(R.string.no, null)
                .show();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            FirebaseDatabase.getInstance().getReference("Chats").child(currentUserId).removeEventListener(messagesListener);
        }

        /*
        if (smartReplyManager != null) {
            smartReplyManager.close();
        }
        */
    }
}
