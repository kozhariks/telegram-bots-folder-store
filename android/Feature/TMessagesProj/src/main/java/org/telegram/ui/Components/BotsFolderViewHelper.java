package org.telegram.ui.Components;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;

/**
 * Helpers for List / Store display mode inside a Bots chat folder.
 */
public final class BotsFolderViewHelper {

    public static final String PREFS = "bots_folder_view";
    public static final String KEY_STORE_MODE = "store_mode";

    private BotsFolderViewHelper() {
    }

    public static boolean isStoreMode() {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_STORE_MODE, true);
    }

    public static void setStoreMode(boolean storeMode) {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_STORE_MODE, storeMode).apply();
    }

    public static void toggleStoreMode() {
        setStoreMode(!isStoreMode());
    }

    /**
     * True when the folder is a pure "Bots" type filter (chat types include only bots).
     */
    public static boolean isBotsOnlyFilter(MessagesController.DialogFilter filter) {
        if (filter == null || filter.isDefault()) {
            return false;
        }
        final int chatFlags = filter.flags & MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS;
        return chatFlags == MessagesController.DIALOG_FILTER_FLAG_BOTS;
    }
}
