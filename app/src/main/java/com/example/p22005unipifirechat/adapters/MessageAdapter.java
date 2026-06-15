package com.example.p22005unipifirechat.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.p22005unipifirechat.interfaces.IMessageActionListener;
import com.example.p22005unipifirechat.modelclasses.Message;
import com.example.p22005unipifirechat.R;
import com.example.p22005unipifirechat.utils.AvatarUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    private Context context;
    private List<Message> mChat;
    private String imageUrl;
    private String otherUsername;


    private IMessageActionListener listener;

    FirebaseUser fuser;

    public MessageAdapter(Context context, List<Message> mChat, String imageUrl, String otherUsername, IMessageActionListener listener){
        this.context = context;
        this.mChat = mChat;
        this.imageUrl = imageUrl;
        this.otherUsername = otherUsername;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_right, parent, false);
            return new ViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_left, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        Message chat = mChat.get(position);
        holder.show_message.setText(chat.messageText);

        // Set username in the bubble
        if (getItemViewType(position) == MSG_TYPE_LEFT) {
            holder.tvMessageUsername.setText(otherUsername);
            if (holder.imgChatAvatar != null) {
                holder.imgChatAvatar.setVisibility(View.VISIBLE);
                AvatarUtils.setAvatar(holder.imgChatAvatar, imageUrl);
            }
        } else {
            // For own messages
            holder.tvMessageUsername.setText(context.getString(R.string.yes).equals("Ναι") ? "Εσείς" : "You");
        }

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (listener != null) {
                    listener.onMessageLongClick(chat);
                }
                return true;
            }
        });
    }


    @Override
    public int getItemCount() {
        return mChat.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView show_message;
        public TextView tvMessageUsername;
        public ImageView imgChatAvatar;

        public ViewHolder(View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.tvMessage);
            tvMessageUsername = itemView.findViewById(R.id.tvMessageUsername);
            imgChatAvatar = itemView.findViewById(R.id.imgChatAvatar);
        }
    }

    @Override
    public int getItemViewType(int position) {
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        if (fuser != null && mChat.get(position).senderId.equals(fuser.getUid())){
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }
}
