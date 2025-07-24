#!/bin/bash

BASE_DIR="AndroidLLMApp"

# Function to create directory if not exists
create_dir() {
  [ ! -d "$1" ] && mkdir -p "$1" && echo "Created dir: $1"
}

# Function to create file if not exists
create_file() {
  [ ! -f "$1" ] && touch "$1" && echo "Created file: $1"
}

# --- Project structure ---
create_dir "$BASE_DIR/app/src/main/java/com/yourname/androidllmapp/data"
create_dir "$BASE_DIR/app/src/main/java/com/yourname/androidllmapp/ui/screens"
create_dir "$BASE_DIR/app/src/main/java/com/yourname/androidllmapp/ui/components"
create_dir "$BASE_DIR/app/src/main/assets"
create_dir "$BASE_DIR/app/src/main/res/drawable"
create_dir "$BASE_DIR/app/src/main/res/layout"
create_dir "$BASE_DIR/app/src/main/res/values"

# --- Files ---
create_file "$BASE_DIR/app/src/main/java/com/yourname/androidllmapp/data/LLMManager.kt"
create_file "$BASE_DIR/app/src/main/java/com/yourname/androidllmapp/data/ChatRepository.kt"
create_file "$BASE_DIR/app/src/main/java/com/yourname/androidllmapp/ui/ChatViewModel.kt"
create_file "$BASE_DIR/app/src/main/java/com/yourname/androidllmapp/ui/MainActivity.kt"
create_file "$BASE_DIR/app/src/main/java/com/yourname/androidllmapp/ui/screens/ChatScreen.kt"
create_file "$BASE_DIR/app/src/main/java/com/yourname/androidllmapp/ui/screens/SettingsScreen.kt"
create_file "$BASE_DIR/app/src/main/java/com/yourname/androidllmapp/ui/components/MessageBubble.kt"
create_file "$BASE_DIR/app/src/main/java/com/yourname/androidllmapp/ui/components/AutoScrollColumn.kt"

create_file "$BASE_DIR/app/src/main/assets/gemma-3n-E2B-it-int4.task"
create_file "$BASE_DIR/app/src/main/assets/gemma-3n-E4B-it-int4.task"

create_file "$BASE_DIR/app/src/main/res/values/colors.xml"
create_file "$BASE_DIR/app/src/main/res/values/themes.xml"
create_file "$BASE_DIR/app/src/main/AndroidManifest.xml"

create_file "$BASE_DIR/app/build.gradle"
create_file "$BASE_DIR/build.gradle"
create_file "$BASE_DIR/settings.gradle"

