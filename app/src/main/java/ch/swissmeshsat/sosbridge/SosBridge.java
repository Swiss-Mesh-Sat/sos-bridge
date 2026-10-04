package ch.swissmeshsat.sosbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.atak.plugins.impl.PluginContextProvider;
import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.emergency.tool.EmergencyManager;
import com.atakmap.android.emergency.tool.EmergencyType;
import com.atakmap.android.ipc.AtakBroadcast;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;
import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.PaneBuilder;
import gov.tak.api.ui.ToolbarItem;
import gov.tak.api.ui.ToolbarItemAdapter;
import gov.tak.platform.marshal.MarshalManager;

public class SosBridge implements IPlugin {

    private static final String TAG = "SosBridge";

    // Messages sent by SOS Flashlight (our own convention)
    public static final String ACTION_SOS_STARTED = "ch.swissmeshsat.sosbridge.SOS_STARTED";
    public static final String ACTION_SOS_STOPPED = "ch.swissmeshsat.sosbridge.SOS_STOPPED";

    // Minimum delay between two sends (activation or cancellation)
    private static final long MIN_INTERVAL_MS = 2 * 60 * 1000L;

    // Fallback alert type if none is selected in ATAK
    private static final EmergencyType DEFAULT_ALERT_TYPE = EmergencyType.NineOneOne;

    IServiceController serviceController;
    Context pluginContext;
    IHostUIService uiService;
    ToolbarItem toolbarItem;
    Pane templatePane;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean sosActive = false;   // SOS state announced by SOS Flashlight
    private long lastSendTime = 0;       // time of last send, 0 = never
    private boolean syncScheduled = false;

    private final BroadcastReceiver sosReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_SOS_STARTED.equals(action)) {
                sosActive = true;
            } else if (ACTION_SOS_STOPPED.equals(action)) {
                sosActive = false;
            } else {
                return;
            }
            Log.d(TAG, "SOS state received: " + (sosActive ? "ON" : "OFF"));
            synchronize();
        }
    };

    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            syncScheduled = false;
            synchronize();
        }
    };

    public SosBridge(IServiceController serviceController) {
        this.serviceController = serviceController;
        final PluginContextProvider ctxProvider = serviceController
                .getService(PluginContextProvider.class);
        if (ctxProvider != null) {
            pluginContext = ctxProvider.getPluginContext();
            pluginContext.setTheme(R.style.ATAKPluginTheme);
        }

        uiService = serviceController.getService(IHostUIService.class);

        toolbarItem = new ToolbarItem.Builder(
                pluginContext.getString(R.string.app_name),
                MarshalManager.marshal(
                        pluginContext.getResources().getDrawable(R.drawable.ic_launcher),
                        android.graphics.drawable.Drawable.class,
                        gov.tak.api.commons.graphics.Bitmap.class))
                .setListener(new ToolbarItemAdapter() {
                    @Override
                    public void onClick(ToolbarItem item) {
                        showPane();
                    }
                }).setIdentifier(pluginContext.getPackageName())
                .build();
    }

    @Override
    public void onStart() {
        // Listen for messages coming from outside ATAK (SOS Flashlight)
        AtakBroadcast.DocumentedIntentFilter filter =
                new AtakBroadcast.DocumentedIntentFilter(ACTION_SOS_STARTED,
                        "SOS Flashlight started signaling")
                        .addAction(ACTION_SOS_STOPPED,
                                "SOS Flashlight stopped signaling");
        AtakBroadcast.getInstance().registerSystemReceiver(sosReceiver, filter);
        Log.d(TAG, "SOS receiver registered");

        if (uiService == null)
            return;
        uiService.addToolbarItem(toolbarItem);
    }

    @Override
    public void onStop() {
        // Unplug everything that was plugged in onStart()
        AtakBroadcast.getInstance().unregisterSystemReceiver(sosReceiver);
        handler.removeCallbacks(syncRunnable);
        syncScheduled = false;
        Log.d(TAG, "SOS receiver unregistered");

        if (uiService == null)
            return;
        uiService.removeToolbarItem(toolbarItem);
    }

    // Bring the ATAK alert in line with the SOS state, at most one send every MIN_INTERVAL_MS
    private void synchronize() {
        EmergencyManager em = EmergencyManager.getInstance();
        if (em == null) {
            Log.w(TAG, "EmergencyManager not available");
            return;
        }

        // Ask ATAK for the real alert state (it may have been changed by hand)
        boolean alertActive = Boolean.TRUE.equals(em.isEmergencyOn());
        if (sosActive == alertActive) {
            Log.d(TAG, "Already in sync (ATAK alert " + (alertActive ? "ON" : "OFF") + "), nothing to send");
            return;
        }

        long now = SystemClock.elapsedRealtime();
        long wait = (lastSendTime == 0) ? 0 : (lastSendTime + MIN_INTERVAL_MS - now);
        if (wait > 0) {
            if (!syncScheduled) {
                handler.postDelayed(syncRunnable, wait);
                syncScheduled = true;
            }
            Log.d(TAG, "Delay not elapsed, next check in " + (wait / 1000) + " s");
            return;
        }

        // Use the alert type currently selected in ATAK's emergency tool
        EmergencyType type = em.getEmergencyType();
        if (type == null || type == EmergencyType.Cancel) {
            type = DEFAULT_ALERT_TYPE;
        }
        if (sosActive) {
            em.setEmergencyType(type);
            em.initiateRepeat(type, false);
            em.setEmergencyOn(true);
            Log.d(TAG, "Alert SENT (type " + type + ")");
        } else {
            em.cancelRepeat(type, false);
            em.setEmergencyOn(false);
            Log.d(TAG, "Alert CANCELLED (type " + type + ")");
        }
        lastSendTime = now;
    }

    private void showPane() {
        if (templatePane == null) {
            templatePane = new PaneBuilder(PluginLayoutInflater.inflate(pluginContext,
                    R.layout.main_layout, null))
                    .setMetaValue(Pane.RELATIVE_LOCATION, Pane.Location.Default)
                    .setMetaValue(Pane.PREFERRED_WIDTH_RATIO, 0.5D)
                    .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5D)
                    .build();
        }
        if (!uiService.isPaneVisible(templatePane)) {
            uiService.showPane(templatePane, null);
        }
    }
}
