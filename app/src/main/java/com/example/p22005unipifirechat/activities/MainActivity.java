package com.example.p22005unipifirechat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.p22005unipifirechat.R;
import com.example.p22005unipifirechat.interfaces.IUsersActionListener;
import com.example.p22005unipifirechat.interfaces.ChatListListener;
import com.example.p22005unipifirechat.interfaces.ActionResultListener;
import com.example.p22005unipifirechat.interfaces.UserSearchListener;
import com.example.p22005unipifirechat.adapters.UsersAdapter;
import com.example.p22005unipifirechat.modelclasses.User;
import com.example.p22005unipifirechat.utils.AuthManager;
import com.example.p22005unipifirechat.utils.MainManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;



public class MainActivity extends BaseActivity {
    private RecyclerView recyclerViewChats;
    private FloatingActionButton fabNewChat;
    private TextView tvEmptyState;
    private View btnLogout;
    private UsersAdapter usersAdapter;
    private List<User> userList;
    private AuthManager authManager;
    private MainManager mainManager;
    private String currentUserId;
    private ValueEventListener chatListListener;

    // to save the currently open alertDialog after screen rotation
    private static final String KEY_OPEN_DIALOG = "key_open_dialog";
    private static final String KEY_SELECTED_CHAT_UID = "key_selected_chat_uid";
    private static final String KEY_SELECTED_CHAT_USERNAME = "key_selected_chat_username";
    private static final int DIALOG_NONE = 0;
    private static final int DIALOG_LOGOUT = 1;
    private static final int DIALOG_DELETE_CHAT = 2;
    private static final int DIALOG_NEW_CHAT = 3;
    private int currentlyOpenDialog = DIALOG_NONE;  //set currently open dialog to none by default
    //selected chat information
    private String selectedChatUid;
    private String selectedChatUsername;
    // reference to the currently open dialog
    private AlertDialog activeDialog;

    // sidebar elements
    private DrawerLayout drawerLayout;
    private View sidebarLayout;
    private ImageButton btnSidebarBack;
    private ImageButton btnMenu;
    private TextView btnSidebarProfile;
    private TextView btnSidebarPrivates;
    private TextView btnSidebarFavorites;
    private TextView btnSidebarSettings;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupEdgeToEdge();

        initManagers();
        if (authManager.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }
        currentUserId = authManager.getCurrentUser().getUid();

        initViews();
        setupRecyclerView();
        setupClickListeners();
        observeChats();

        //to show again the active dialog after screen rotation
        if (savedInstanceState != null) {
            currentlyOpenDialog = savedInstanceState.getInt(KEY_OPEN_DIALOG, DIALOG_NONE);
            selectedChatUid = savedInstanceState.getString(KEY_SELECTED_CHAT_UID);
            selectedChatUsername = savedInstanceState.getString(KEY_SELECTED_CHAT_USERNAME);
            restoreDialogState();
        }
    }

    private void setupEdgeToEdge() {
        //screen settings set up for edge to edge
        View toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
                return insets;
            });
        }
        
        View fab = findViewById(R.id.fabNewChat);
        if (fab != null) {
            ViewCompat.setOnApplyWindowInsetsListener(fab, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    float density = getResources().getDisplayMetrics().density;
                    params.bottomMargin = (int) (24 * density) + systemBars.bottom;
                    params.setMarginEnd((int) (24 * density) + systemBars.right);
                    v.setLayoutParams(params);
                }
                return insets;
            });
        }

        //logout button in the sidebar should be placed above the device's navigation bar
        View sidebar = findViewById(R.id.sidebarLayout); // Το βρίσκουμε απευθείας για να μην είναι null!
        if (sidebar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(sidebar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                // Προσθέτουμε δυναμικό padding ΜΟΝΟ στο κάτω μέρος, ίσο με το navigation bar του εκάστοτε κινητού
                v.setPadding(0, 0, 0, systemBars.bottom);
                return insets;
            });
        }
    }

    private void initManagers() {
        authManager = AuthManager.getInstance();
        mainManager = MainManager.getInstance();
    }

    private void initViews() {
        recyclerViewChats = findViewById(R.id.recyclerViewChats);
        fabNewChat = findViewById(R.id.fabNewChat);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnLogout = findViewById(R.id.btnLogout);

        //sidebar elements initialization
        drawerLayout = findViewById(R.id.drawerLayout);
        sidebarLayout = findViewById(R.id.sidebarLayout);
        btnSidebarBack = findViewById(R.id.btnSidebarBack);
        btnMenu = findViewById(R.id.btnMenu);
        btnSidebarProfile = findViewById(R.id.btnSidebarProfile);
        btnSidebarPrivates = findViewById(R.id.btnSidebarPrivates);
        btnSidebarFavorites = findViewById(R.id.btnSidebarFavorites);
        btnSidebarSettings = findViewById(R.id.btnSidebarSettings);

        // set the sidebar's width
        sidebarLayout.post(() -> {
            ViewGroup.LayoutParams params = sidebarLayout.getLayoutParams();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.75);
            sidebarLayout.setLayoutParams(params);
        });
    }

    private void setupRecyclerView() {
        //adapter for the recyclerView
        recyclerViewChats.setHasFixedSize(true);
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        usersAdapter = new UsersAdapter(this, userList, new IUsersActionListener() {
            @Override
            public void onUserClicked(User user) {
                openChatActivity(user.uid, user.username);
            }

            @Override
            public void onDeleteChatClicked(User user) {
                showDeleteChatDialog(user);
            }
        });
        recyclerViewChats.setAdapter(usersAdapter);
    }

    private void setupClickListeners() {
        fabNewChat.setOnClickListener(v -> showNewChatDialog());
        //findViewById(R.id.btnProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        btnLogout.setOnClickListener(v -> {
            //drawerLayout.closeDrawer(GravityCompat.START);
            showLogoutDialog();
        });

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        //sidebar elements click listeners
        btnSidebarBack.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));

        btnSidebarProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        btnSidebarPrivates.setOnClickListener(v -> {
            startActivity(new Intent(this, PrivatesActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        btnSidebarFavorites.setOnClickListener(v -> {
            startActivity(new Intent(this, FavoritesActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        btnSidebarSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_OPEN_DIALOG, currentlyOpenDialog);
        outState.putString(KEY_SELECTED_CHAT_UID, selectedChatUid);
        outState.putString(KEY_SELECTED_CHAT_USERNAME, selectedChatUsername);
    }


    private void restoreDialogState() {
        if (currentlyOpenDialog == DIALOG_LOGOUT) {
            showLogoutDialog();
        } else if (currentlyOpenDialog == DIALOG_DELETE_CHAT) {
            if (selectedChatUid != null && selectedChatUsername != null) {
                User user = new User();
                user.uid = selectedChatUid;
                user.username = selectedChatUsername;
                showDeleteChatDialog(user);
            }
        } else if (currentlyOpenDialog == DIALOG_NEW_CHAT) {
            showNewChatDialog();
        }
    }





    // access all the chats of currentUser via the ChatListListener interface (through MainManager)
    private void observeChats() {
        chatListListener = mainManager.observeChatList(currentUserId, new ChatListListener() {
            @Override
            public void onChatListUpdated(List<User> users) {
                tvEmptyState.setVisibility(View.GONE);
                recyclerViewChats.setVisibility(View.VISIBLE);
                userList.clear(); // clean the list before adding new items
                //add the new items to the list and notify the adapter to show these items
                userList.addAll(users);
                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onEmpty() {
                tvEmptyState.setVisibility(View.VISIBLE);
                recyclerViewChats.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }






    // currentUser logout method
    private void showLogoutDialog() {
        currentlyOpenDialog = DIALOG_LOGOUT;

        activeDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton(R.string.yes, (dialog, which) -> handleLogout())
                .setNegativeButton(R.string.no, null)
                .create();

        //when alertDialog is dismissed, set currentlyOpenDialog to none
        // so that no dialog is open
        activeDialog.setOnDismissListener(dialog -> {
            currentlyOpenDialog = DIALOG_NONE;
            activeDialog = null;
        });

        activeDialog.show();
    }

    private void handleLogout() {
        authManager.signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.revokeAccess().addOnCompleteListener(task -> navigateToLogin());
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }





    private void showDeleteChatDialog(User user) {
        //selected chat information to delete
        //using the selected chat's user UID and his username
        selectedChatUid = user.uid;
        selectedChatUsername = user.username;
        currentlyOpenDialog = DIALOG_DELETE_CHAT;

        activeDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.delete_chat_title)
                .setMessage(getString(R.string.delete_chat_confirmation, user.username))
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    mainManager.deleteChat(currentUserId, user.uid, new ActionResultListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(MainActivity.this, R.string.chat_deleted_successfully, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.no, null)
                .create();

        //when alertDialog is dismissed, set currentlyOpenDialog to none
        // so that no dialog is open
        activeDialog.setOnDismissListener(dialog -> {
            currentlyOpenDialog = DIALOG_NONE;
            selectedChatUid = null;
            selectedChatUsername = null;
            activeDialog = null;
        });

        activeDialog.show();
    }




    // search for a user by username to start a new conversation
    private void showNewChatDialog() {
        currentlyOpenDialog = DIALOG_NEW_CHAT;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.new_chat);
        builder.setMessage(R.string.enter_username_for_new_chat);

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(R.string.chat, (dialog, which) -> {
            String usernameInput = input.getText().toString().trim();
            if (!usernameInput.isEmpty()) {
                //mainManager is used to search for a user by username because it communicates with the Realtime Database
                mainManager.findUserByUsername(usernameInput, new UserSearchListener() {
                    @Override
                    public void onUserFound(String uid, String username) {
                        //if the user is found, open the chat activity using his username and his UID
                        openChatActivity(uid, username);
                    }

                    @Override
                    public void onUserNotFound() {
                        Toast.makeText(MainActivity.this, R.string.user_not_found, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        activeDialog = builder.create();

        activeDialog.setOnDismissListener(dialog -> {
            currentlyOpenDialog = DIALOG_NONE;
            activeDialog = null;
        });

        activeDialog.show();
    }


    private void openChatActivity(String uid, String username) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("other_uid", uid);
        intent.putExtra("other_username", username);
        startActivity(intent);
    }




    @Override
    protected void onDestroy() {
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
        }
        activeDialog = null;

        //release the listener to save resources
        super.onDestroy();
        if (chatListListener != null) {
            FirebaseDatabase.getInstance().getReference("ChatList").child(currentUserId).removeEventListener(chatListListener);
        }
    }
}
