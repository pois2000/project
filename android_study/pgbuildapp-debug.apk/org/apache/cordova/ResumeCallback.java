package org.apache.cordova;

import java.util.ArrayList;
import java.util.List;
import org.apache.cordova.PluginResult.Status;

public class ResumeCallback extends CallbackContext {
    private final String TAG = "CordovaResumeCallback";
    private PluginManager pluginManager;
    private String serviceName;

    public ResumeCallback(String serviceName, PluginManager pluginManager) {
        super("resumecallback", null);
        this.serviceName = serviceName;
        this.pluginManager = pluginManager;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void sendPluginResult(PluginResult pluginResult) {
        synchronized (this) {
            if (this.finished) {
                LOG.w("CordovaResumeCallback", this.serviceName + " attempted to send a second callback to ResumeCallback\nResult was: " + pluginResult.getMessage());
                return;
            }
            this.finished = true;
        }
        PluginResult eventResult = new PluginResult(Status.OK, event);
        List result = new ArrayList();
        result.add(eventResult);
        result.add(pluginResult);
        ((CoreAndroid) this.pluginManager.getPlugin(CoreAndroid.PLUGIN_NAME)).sendResumeEvent(new PluginResult(Status.OK, result));
    }
}
