package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Home-screen style grid of Mini Apps for a Bots folder.
 * Tap → bot chat; long-press → Mini App.
 * Uses the same top/bottom padding as the dialogs list so content scrolls
 * under the top chrome and bottom tab bar (edge-to-edge).
 */
public class BotsFolderStoreView extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

    public interface Callback {
        /** Open Mini App (long-press). */
        void onOpenBot(TLRPC.User bot);

        /** Open bot chat (short tap). */
        void onOpenBotChat(TLRPC.User bot);
    }

    private static final int COLUMNS = 4;

    private final Theme.ResourcesProvider resourcesProvider;
    private final int currentAccount;
    private final Callback callback;

    private final RecyclerListView gridView;
    private final GridAdapter adapter;
    private final TextView emptyView;
    private Runnable onScrollInvalidate;

    public BotsFolderStoreView(Context context, int currentAccount, Theme.ResourcesProvider resourcesProvider, Callback callback) {
        super(context);
        this.currentAccount = currentAccount;
        this.resourcesProvider = resourcesProvider;
        this.callback = callback;

        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(false);
        setFocusable(false);
        // Same as dialogs list: no opaque fill — ContentView clips us under the action bar.
        setBackgroundColor(0);

        emptyView = new TextView(context);
        emptyView.setText("No apps yet\nOpen a Mini App once — it will appear here");
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        emptyView.setLineSpacing(AndroidUtilities.dp(4), 1f);
        addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 24, 0, 24, 0));

        gridView = new RecyclerListView(context, resourcesProvider);
        gridView.setClipToPadding(false);
        gridView.setClipChildren(false);
        gridView.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), 0);
        gridView.setLayoutManager(new GridLayoutManager(context, COLUMNS));
        gridView.setOverScrollMode(OVER_SCROLL_NEVER);
        gridView.setVerticalScrollBarEnabled(false);
        adapter = new GridAdapter();
        gridView.setAdapter(adapter);
        gridView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (onScrollInvalidate != null) {
                    onScrollInvalidate.run();
                }
            }
        });
        gridView.setOnItemClickListener((view, position) -> {
            TLRPC.User user = adapter.getItem(position);
            if (user == null) {
                return;
            }
            // Short tap — open chat (same as a dialog row).
            if (callback != null) {
                callback.onOpenBotChat(user);
            }
        });
        gridView.setOnItemLongClickListener((view, position) -> {
            TLRPC.User user = adapter.getItem(position);
            if (user == null) {
                return false;
            }
            // Long-press — open Mini App.
            if (callback != null) {
                callback.onOpenBot(user);
            } else {
                MessagesController.getInstance(currentAccount).openApp(user, 0);
            }
            return true;
        });
        addView(gridView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        updateColors();
    }

    /**
     * Match dialogs RecyclerListView padding so icons scroll under top/bottom chrome.
     */
    public void syncListPaddings(int topPadding, int bottomPadding) {
        int left = AndroidUtilities.dp(12);
        int right = AndroidUtilities.dp(12);
        if (gridView.getPaddingTop() != topPadding
                || gridView.getPaddingBottom() != bottomPadding
                || gridView.getPaddingLeft() != left
                || gridView.getPaddingRight() != right) {
            gridView.setPadding(left, Math.max(0, topPadding), right, Math.max(0, bottomPadding));
        }
        if (emptyView.getLayoutParams() instanceof LayoutParams) {
            LayoutParams lp = (LayoutParams) emptyView.getLayoutParams();
            if (lp.topMargin != 0) {
                lp.topMargin = 0;
                emptyView.setLayoutParams(lp);
            }
        }
    }

    public void setOnScrollInvalidate(Runnable runnable) {
        onScrollInvalidate = runnable;
    }

    public RecyclerListView getGridView() {
        return gridView;
    }

    /**
     * Same metric DialogsActivity uses for listView → scrollYOffset:
     * how far the first item has scrolled up into the top padding.
     * Returns null if not measurable yet (do not force chrome scrollY).
     */
    public Float computeVisiblePartAfterScroll() {
        if (getVisibility() != View.VISIBLE || gridView.getVisibility() != View.VISIBLE || adapter.getItemCount() == 0) {
            return null;
        }
        RecyclerView.ViewHolder holder = gridView.findViewHolderForAdapterPosition(0);
        if (holder == null || holder.itemView == null) {
            return null;
        }
        return gridView.getPaddingTop() - holder.itemView.getY();
    }

    /**
     * Keep folder chrome (stories / search) at the same scrollY as sibling folders.
     * Same contract as DialogsActivity listView: scrollToPositionWithOffset(0, scrollYOffset).
     */
    public void syncScrollToOffset(int scrollYOffset) {
        if (!(gridView.getLayoutManager() instanceof GridLayoutManager) || adapter.getItemCount() == 0) {
            return;
        }
        ((GridLayoutManager) gridView.getLayoutManager()).scrollToPositionWithOffset(0, scrollYOffset);
    }

    public void updateColors() {
        setBackgroundColor(0);
        gridView.setBackgroundColor(0);
        emptyView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        for (int i = 0; i < gridView.getChildCount(); i++) {
            View child = gridView.getChildAt(i);
            if (child instanceof AppIconCell) {
                ((AppIconCell) child).applyTheme();
            }
        }
        invalidate();
    }

    public void onShown(int scrollYOffset) {
        updateColors();
        adapter.reload();
        emptyView.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        gridView.setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
        // Provisional align; DialogsActivity.syncBotsFolderStorePaddings(..., true) pins to chrome.
        syncScrollToOffset(scrollYOffset);
    }

    /**
     * Re-read unread counts into visible cells (e.g. after returning from a bot chat).
     * Does not rebuild the app list or reset scroll.
     */
    public void refreshUnreadBadges() {
        if (getVisibility() != View.VISIBLE || adapter.getItemCount() == 0) {
            return;
        }
        for (int i = 0; i < gridView.getChildCount(); i++) {
            View child = gridView.getChildAt(i);
            if (!(child instanceof AppIconCell)) {
                continue;
            }
            int position = gridView.getChildAdapterPosition(child);
            TLRPC.User user = adapter.getItem(position);
            if (user != null) {
                ((AppIconCell) child).setUnread(unreadForUser(user.id));
            }
        }
        // Bind any off-screen holders when they scroll back into view.
        adapter.notifyItemRangeChanged(0, adapter.getItemCount(), PAYLOAD_UNREAD);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.dialogsUnreadCounterChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.openedChatChanged);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.dialogsUnreadCounterChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.openedChatChanged);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        // Still refresh when not visible so counts are correct on next show / resume.
        if (id == NotificationCenter.dialogsNeedReload) {
            if (getVisibility() == View.VISIBLE) {
                adapter.reload();
                emptyView.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                gridView.setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
            }
            return;
        }
        if (id == NotificationCenter.updateInterfaces) {
            int mask = args != null && args.length > 0 && args[0] instanceof Integer ? (Integer) args[0] : ~0;
            final int unreadMask = MessagesController.UPDATE_MASK_READ_DIALOG_MESSAGE
                    | MessagesController.UPDATE_MASK_NEW_MESSAGE
                    | MessagesController.UPDATE_MASK_SELECT_DIALOG;
            if ((mask & unreadMask) != 0) {
                refreshUnreadBadges();
            }
            return;
        }
        if (id == NotificationCenter.dialogsUnreadCounterChanged
                || id == NotificationCenter.openedChatChanged) {
            refreshUnreadBadges();
        }
    }

    private static final Object PAYLOAD_UNREAD = new Object();

    private int unreadForUser(long userId) {
        TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogs_dict.get(userId);
        if (dialog == null) {
            return 0;
        }
        int count = dialog.unread_count;
        if (count <= 0 && dialog.unread_mark) {
            return 1;
        }
        return Math.max(0, count);
    }

    private class GridAdapter extends RecyclerListView.SelectionAdapter {
        private final ArrayList<TLRPC.User> apps = new ArrayList<>();

        void reload() {
            apps.clear();
            HashSet<Long> seen = new HashSet<>();
            MessagesController mc = MessagesController.getInstance(currentAccount);

            ArrayList<TLRPC.TL_topPeer> webapps = MediaDataController.getInstance(currentAccount).webapps;
            if (webapps != null) {
                for (int i = 0; i < webapps.size(); i++) {
                    long dialogId = DialogObject.getPeerDialogId(webapps.get(i).peer);
                    TLRPC.User user = mc.getUser(dialogId);
                    if (user == null || !user.bot || seen.contains(user.id)) {
                        continue;
                    }
                    seen.add(user.id);
                    apps.add(user);
                }
            }

            ArrayList<TLRPC.Dialog> dialogs = mc.getDialogs(0);
            if (dialogs != null) {
                for (int i = 0; i < dialogs.size(); i++) {
                    TLRPC.Dialog d = dialogs.get(i);
                    if (d == null || d.id <= 0) {
                        continue;
                    }
                    TLRPC.User user = mc.getUser(d.id);
                    if (user == null || !user.bot || seen.contains(user.id)) {
                        continue;
                    }
                    seen.add(user.id);
                    apps.add(user);
                }
            }

            notifyDataSetChanged();
        }

        TLRPC.User getItem(int position) {
            if (position < 0 || position >= apps.size()) {
                return null;
            }
            return apps.get(position);
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerListView.Holder(new AppIconCell(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TLRPC.User user = apps.get(position);
            ((AppIconCell) holder.itemView).setUser(user, unreadForUser(user.id));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull java.util.List payloads) {
            if (!payloads.isEmpty() && payloads.contains(PAYLOAD_UNREAD)) {
                TLRPC.User user = apps.get(position);
                if (user != null) {
                    ((AppIconCell) holder.itemView).setUnread(unreadForUser(user.id));
                }
                return;
            }
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    private class AppIconCell extends FrameLayout {
        private final BackupImageView imageView;
        private final AvatarDrawable avatarDrawable = new AvatarDrawable();
        private final TextView nameView;
        private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint badgeTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF badgeRect = new RectF();
        private int unreadCount;

        AppIconCell(Context context) {
            super(context);
            setWillNotDraw(false);
            setClipToPadding(false);
            setClipChildren(false);

            imageView = new BackupImageView(context);
            imageView.setRoundRadius(AndroidUtilities.dp(16));
            addView(imageView, LayoutHelper.createFrame(60, 60, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0, 0, 0));

            nameView = new TextView(context);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            nameView.setGravity(Gravity.CENTER_HORIZONTAL);
            nameView.setMaxLines(2);
            nameView.setEllipsize(TextUtils.TruncateAt.END);
            nameView.setLineSpacing(0, 0.95f);
            addView(nameView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 4, 68, 4, 0));

            badgeTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            badgeTextPaint.setTextAlign(Paint.Align.CENTER);
            badgeTextPaint.setTextSize(AndroidUtilities.dp(11));
            applyTheme();
        }

        void applyTheme() {
            nameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            badgePaint.setColor(Theme.getColor(Theme.key_chats_unreadCounter, resourcesProvider));
            badgeTextPaint.setColor(Theme.getColor(Theme.key_chats_unreadCounterText, resourcesProvider));
            invalidate();
        }

        void setUnread(int unread) {
            if (unreadCount == unread) {
                return;
            }
            unreadCount = unread;
            invalidate();
        }

        void setUser(TLRPC.User user, int unread) {
            applyTheme();
            unreadCount = unread;
            avatarDrawable.setInfo(currentAccount, user);
            imageView.setForUserOrChat(user, avatarDrawable);
            String title = UserObject.getUserName(user);
            if (TextUtils.isEmpty(title) && user.username != null) {
                title = user.username;
            }
            nameView.setText(title);
            invalidate();
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (unreadCount <= 0 || imageView.getVisibility() != View.VISIBLE) {
                return;
            }
            String text = unreadCount > 99 ? "99+" : String.valueOf(unreadCount);
            float textW = badgeTextPaint.measureText(text);
            float h = AndroidUtilities.dp(18);
            float w = Math.max(h, textW + AndroidUtilities.dp(10));
            float cx = imageView.getRight() - AndroidUtilities.dp(2);
            float cy = imageView.getTop() + AndroidUtilities.dp(2);
            badgeRect.set(cx - w, cy, cx, cy + h);
            canvas.drawRoundRect(badgeRect, h / 2f, h / 2f, badgePaint);
            float textY = badgeRect.centerY() - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f;
            canvas.drawText(text, badgeRect.centerX(), textY, badgeTextPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(100), MeasureSpec.EXACTLY));
        }
    }
}
