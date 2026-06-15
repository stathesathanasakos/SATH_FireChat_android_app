# SATH FireChat

**SATH FireChat** is a native Android application built to demonstrate modern mobile development practices. It provides a seamless real-time communication experience, using **Firebase** for backend services and Clean architecture to ensure scalability and maintainability.

---

## Features

- Real-time Messaging: Your messages sync everywhere at once. Send a text on one device and see it appear instantly on other's device
- Authentication: Integrated dual-auth system. Jump right in using your Google account or a simple email and password
- Dynamic Theming: Full support for Dark Mode, Light Mode and System Default synchronization
- Global Localization: Instant runtime language switching between English and Greek without application restart
- Profile/Account Management: Personalize your profile by picking an avatar and manage your data with secure account deletion options
- Smart AI Features: Save time with AI-powered "smart replies" and get quick summaries of your long chats so you’re always up to speed

---

## Architecture Deep Dive

The project follows **Clean Architecture** principles, prioritizing the **Single Responsibility Principle (SOLID)**. The logic is divided into distinct layers to avoid "God Activities" and give high maintainability:

### 1. UI Layer (View)
Activities (e.g., `ChatActivity`, `ProfileActivity`) are responsible only for rendering data and capturing user input. They extend the `BaseActivity` which handles global configuration like locale and theme application.

### 2. Logic Layer (Managers)
Singleton Managers (e.g., `AuthManager`, `ChatManager`, `ThemeManager`) act as the brain of the app. With them I handle business logic, data validation, and state management, communicating results back to the UI via proper **Interfaces**.

### 3. Data Layer (Firebase)
Firebase serves as the single source of truth. The app utilizes:
- **Authentication**: To manage user sessions securely
- **Realtime Database**: For a reactive, NoSQL approach to storing messages and user metadata

---
## Database Architecture

### Architecture
I used a NoSQL solution (Realtime Database). I created three distinct, specialized nodes:
- users: Stores essential metadata keyed strictly under a unique User ID (UID)
- Chats: Holds individual message payloads with millisecond timestamps for instantaneous chronological ordering
- ChatList: It is something like a lightweight index for each user's active conversations

In a standard database (like SQL), if I wanted to show the chat history on the main screen, the app would have to search through thousands of users and messages every single time. As more users join the app, this would make the app slow, uses too much internet data, and can even cause crashes. So, I decided to use the Realtime Database and split it into three independent nodes, with no database relations (flat structure).


### Safety Rules
To keep user data safe without breaking the real-time updates, I added security rules directly to the database:
```json
{
  "rules": {
    "users": {
      ".read": "auth != null",
      "$uid": {
        ".write": "auth != null && auth.uid === $uid"
      }
    },
    "Chats": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "ChatList": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```
That way I ensure that:
- Only authenticated users (auth != null) can read chat message paths or search for other users. Anonymous external API calls are blocked.
- A user can only write or update data inside the users/$uid node if their current authenticated session token perfectly matches that specific node's ID. This prevents unauthorized users from modifying other people's profiles.

---

## Key Code Explanations

### Decoupled Logic Pattern
Instead of calling Firebase directly from the Activity, I use a Manager (logic layer) with asynchronous callbacks. This makes the code unit-testable and the UI highly responsive.

```java
// Example of the clean communication pattern in ChatManager.java
public void sendMessage(String senderId, String receiverId, String messageText, MessageActionResultListener listener) {
    DatabaseReference newMsgRef = mDatabase.child("Chats").push();
    // Payload creation...
    newMsgRef.setValue(hashMap).addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
            updateChatList(senderId, receiverId); // Atomic update logic
            if (listener != null) listener.onSuccess();
        } else {
            if (listener != null) listener.onFailure(task.getException().getMessage());
        }
    });
}
```

### Global Configuration via BaseActivity
To ensure that theme and language changes persist and apply immediately to every screen, I utilize a `BaseActivity` context wrapper.

```java
// BaseActivity to handle the locale and theme for every inheriting screen
@Override
protected void attachBaseContext(Context newBase) {
    languageManager = new LanguageManager(newBase);
    super.attachBaseContext(languageManager.setLocale(newBase));
}
```

---
## Tech Stack

- **Language**: Java
- **Backend**: Firebase (Authentication, Realtime Database)
- **Identity**: Google Sign-In SDK
- **UI Framework**: Material Design 3 (Components), ConstraintLayout, Edge-to-Edge
- **Jetpack Libraries**: Activity, Core
- **Persistence**: SharedPreferences (Theme & Locale preference)

---

## Future Roadmap

### 1. Data Privacy & Realtime Database Vulnerabilities
- **The Problem (Insecure Open Access):** Currently, my database allows ANY logged-in user to read and write messages across the entire application. A malicious user could bypass the app's UI,via its URL, and read private conversations belonging to other people.
- **The Solution (Dynamic Security Rules):** Τhe database rules must be updated to inspect each chat message. The system should only grant read/write access if the logged-in user's `UID` perfectly matches either the `senderId` or the `receiverId` of that specific message.

### 2. Sensitive Data Encryption
- **The Problem (Plaintext Messages):** Right now, all chat texts are stored inside Firebase as plain text. If someone gains unauthorized access to the Firebase Console, they can read every user's private conversations
- **One possible Solution (AES-256 + Android Keystore System):** Maybe I can implement symmetric AES-256 encryption using the "Android Keystore System":
  1. **Hardware-Backed Security:** Instead of hardcoding a static key, the key will be generated dynamically inside the device’s hardware-isolated storage (TEE/StrongBox) using a Password-Based Encryption (PBE) approach. The app will never expose the raw key in the code, will only ask the Keystore to encrypt and decrypt the payloads.
  2. **Application Logic Integration:** I can create a `CryptoUtils.java` helper class. The `ChatManager.java` will encrypt the `messageText` string on the phone right before uploading it to the `Chats` node. Then, the `MessagesAdapter.java` will decrypt the string before rendering it on the chat screen.


---

## Setup & Run

1. **Clone the repository**:
   ```sh
   git clone https://github.com/your-username/FireChat.git
   ```
2. **Firebase Configuration**:
   - Create a project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android App with the package name `com.example.p22005unipifirechat`.
   - Download `google-services.json` and place it in the `app/` directory.
   - Enable **Email/Password** and **Google** sign-in methods in the Auth tab.
   - Create a **Realtime Database** instance.
3. **Build**:
   - Open the project in Android Studio.
   - Sync Gradle files and run the application on an emulator or physical device.

---

## Author
**Efstathios Panagiotis Athanasakos**  
*Final-year BSc Computer Science student at the University of Piraeus*
