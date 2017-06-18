package com.adobe.phonegap.contentsync;

import android.app.Activity;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.webkit.CookieManager;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipFile;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Sync extends CordovaPlugin {
    public static final int CONNECTION_ERROR = 2;
    private static final HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };
    public static final int INVALID_URL_ERROR = 1;
    private static final String LOG_TAG = "ContentSync";
    private static final int MAX_BUFFER_SIZE = 16384;
    public static final String PREVIOUS_VERSION = "PREVIOUS_VERSION";
    private static final String PROP_CACHED = "cached";
    private static final String PROP_LOADED = "loaded";
    private static final String PROP_LOCAL_PATH = "localPath";
    private static final String PROP_PROGRESS = "progress";
    private static final String PROP_STATUS = "status";
    private static final String PROP_TOTAL = "total";
    private static final int STATUS_COMPLETE = 3;
    private static final int STATUS_DOWNLOADING = 1;
    private static final int STATUS_EXTRACTING = 2;
    private static final int STATUS_STOPPED = 0;
    private static final String TYPE_LOCAL = "local";
    private static final String TYPE_MERGE = "merge";
    private static final String TYPE_REPLACE = "replace";
    public static final int UNZIP_ERROR = 3;
    private static HashMap<String, ProgressEvent> activeRequests = new HashMap();
    private static final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    }};

    private static class ExposedGZIPInputStream extends GZIPInputStream {
        public ExposedGZIPInputStream(InputStream in) throws IOException {
            super(in);
        }

        public Inflater getInflater() {
            return this.inf;
        }
    }

    private static class ProgressEvent {
        private boolean aborted;
        private long loaded;
        private double percentage;
        private int status = 0;
        private File targetFile;
        private long total;

        public long getLoaded() {
            return this.loaded;
        }

        public void setLoaded(long loaded) {
            this.loaded = loaded;
            updatePercentage();
        }

        public void addLoaded(long add) {
            this.loaded += add;
            updatePercentage();
        }

        public long getTotal() {
            return this.total;
        }

        public void setTotal(long total) {
            this.total = total;
            updatePercentage();
        }

        public int getStatus() {
            return this.status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public boolean isAborted() {
            return this.aborted;
        }

        public void setAborted(boolean aborted) {
            this.aborted = aborted;
        }

        public File getTargetFile() {
            return this.targetFile;
        }

        public void setTargetFile(File targetFile) {
            this.targetFile = targetFile;
        }

        public JSONObject toJSONObject() throws JSONException {
            JSONObject jsonProgress = new JSONObject();
            jsonProgress.put(Sync.PROP_PROGRESS, this.percentage);
            jsonProgress.put(Sync.PROP_STATUS, getStatus());
            jsonProgress.put(Sync.PROP_LOADED, getLoaded());
            jsonProgress.put(Sync.PROP_TOTAL, getTotal());
            return jsonProgress;
        }

        private void updatePercentage() {
            this.percentage = Math.floor(((((double) getLoaded()) / ((double) getTotal())) * 100.0d) / 2.0d);
            if (getStatus() == 2) {
                this.percentage += 50.0d;
            }
        }
    }

    private static abstract class TrackingInputStream extends FilterInputStream {
        public abstract long getTotalRawBytesRead();

        public TrackingInputStream(InputStream in) {
            super(in);
        }
    }

    private static class SimpleTrackingInputStream extends TrackingInputStream {
        private long bytesRead = 0;

        public SimpleTrackingInputStream(InputStream stream) {
            super(stream);
        }

        private int updateBytesRead(int newBytesRead) {
            if (newBytesRead != -1) {
                this.bytesRead += (long) newBytesRead;
            }
            return newBytesRead;
        }

        public int read() throws IOException {
            return updateBytesRead(super.read());
        }

        public int read(byte[] bytes, int offset, int count) throws IOException {
            return updateBytesRead(super.read(bytes, offset, count));
        }

        public long getTotalRawBytesRead() {
            return this.bytesRead;
        }
    }

    private static class TrackingGZIPInputStream extends TrackingInputStream {
        private ExposedGZIPInputStream gzin;

        public TrackingGZIPInputStream(ExposedGZIPInputStream gzin) throws IOException {
            super(gzin);
            this.gzin = gzin;
        }

        public long getTotalRawBytesRead() {
            return this.gzin.getInflater().getBytesRead();
        }
    }

    private boolean download(java.lang.String r33, java.io.File r34, org.json.JSONObject r35, com.adobe.phonegap.contentsync.Sync.ProgressEvent r36, org.apache.cordova.CallbackContext r37, boolean r38) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x003d in list [B:100:0x0283]
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:42)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler$1.run(JadxDecompiler.java:199)
*/
        /*
        r32 = this;
        r28 = "ContentSync";
        r29 = new java.lang.StringBuilder;
        r29.<init>();
        r30 = "download ";
        r29 = r29.append(r30);
        r0 = r29;
        r1 = r33;
        r29 = r0.append(r1);
        r29 = r29.toString();
        android.util.Log.d(r28, r29);
        r28 = android.util.Patterns.WEB_URL;
        r0 = r28;
        r1 = r33;
        r28 = r0.matcher(r1);
        r28 = r28.matches();
        if (r28 != 0) goto L_0x003e;
    L_0x002c:
        r28 = "Invalid URL";
        r29 = 1;
        r0 = r32;
        r1 = r28;
        r2 = r29;
        r3 = r37;
        r0.sendErrorMessage(r1, r2, r3);
        r23 = 0;
    L_0x003d:
        return r23;
    L_0x003e:
        r0 = r32;
        r0 = r0.webView;
        r28 = r0;
        r21 = r28.getResourceApi();
        r28 = android.net.Uri.parse(r33);
        r0 = r21;
        r1 = r28;
        r24 = r0.remapUri(r1);
        r26 = org.apache.cordova.CordovaResourceApi.getUriType(r24);
        r28 = 6;
        r0 = r26;
        r1 = r28;
        if (r0 != r1) goto L_0x007c;
    L_0x0060:
        r27 = 1;
    L_0x0062:
        if (r27 != 0) goto L_0x007f;
    L_0x0064:
        r28 = 5;
        r0 = r26;
        r1 = r28;
        if (r0 == r1) goto L_0x007f;
    L_0x006c:
        r16 = 1;
    L_0x006e:
        monitor-enter(r36);
        r28 = r36.isAborted();
        if (r28 == 0) goto L_0x0082;
    L_0x0075:
        r23 = 0;
        monitor-exit(r36);
        goto L_0x003d;
    L_0x0079:
        r28 = move-exception;
        monitor-exit(r36);
        throw r28;
    L_0x007c:
        r27 = 0;
        goto L_0x0062;
    L_0x007f:
        r16 = 0;
        goto L_0x006e;
    L_0x0082:
        monitor-exit(r36);
        r9 = 0;
        r17 = 0;
        r18 = 0;
        r22 = 0;
        r14 = 0;
        r8 = 0;
        r23 = 1;
        r19 = 0;
        r20 = 0;
        r28 = android.net.Uri.fromFile(r34);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r21;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r25 = r0.remapUri(r1);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r36;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r34;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0.setTargetFile(r1);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = 1;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r36;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0.setStatus(r1);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = "ContentSync";	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = new java.lang.StringBuilder;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29.<init>();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r30 = "Download file: ";	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = r29.append(r30);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r29;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r24;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = r0.append(r1);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = r29.toString();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        android.util.Log.d(r28, r29);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = "ContentSync";	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = new java.lang.StringBuilder;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29.<init>();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r30 = "Target file: ";	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = r29.append(r30);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r29;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r34;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = r0.append(r1);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = r29.toString();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        android.util.Log.d(r28, r29);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = "ContentSync";	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = new java.lang.StringBuilder;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29.<init>();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r30 = "size = ";	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = r29.append(r30);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r30 = r34.length();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = r29.append(r30);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = r29.toString();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        android.util.Log.d(r28, r29);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        if (r16 == 0) goto L_0x015c;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x0104:
        r0 = r21;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r24;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r20 = r0.openForRead(r1);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r20;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r0.length;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = r0;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r30 = -1;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = (r28 > r30 ? 1 : (r28 == r30 ? 0 : -1));	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        if (r28 == 0) goto L_0x0125;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x0118:
        r0 = r20;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r0.length;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = r0;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r36;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0.setTotal(r1);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x0125:
        r15 = new com.adobe.phonegap.contentsync.Sync$SimpleTrackingInputStream;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r20;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r0.inputStream;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = r0;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r15.<init>(r0);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r14 = r15;
    L_0x0133:
        if (r8 != 0) goto L_0x033d;
    L_0x0135:
        monitor-enter(r36);	 Catch:{ all -> 0x02d6 }
        r28 = r36.isAborted();	 Catch:{ all -> 0x02d6 }
        if (r28 == 0) goto L_0x0296;	 Catch:{ all -> 0x02d6 }
    L_0x013c:
        r23 = 0;	 Catch:{ all -> 0x02d6 }
        monitor-exit(r36);	 Catch:{ all -> 0x02d6 }
        monitor-enter(r36);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        monitor-exit(r36);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        safeClose(r14);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        safeClose(r19);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        if (r9 == 0) goto L_0x003d;
    L_0x0149:
        if (r38 == 0) goto L_0x003d;
    L_0x014b:
        if (r27 == 0) goto L_0x003d;
    L_0x014d:
        r13 = r9;
        r13 = (javax.net.ssl.HttpsURLConnection) r13;
        r0 = r17;
        r13.setHostnameVerifier(r0);
        r0 = r18;
        r13.setSSLSocketFactory(r0);
        goto L_0x003d;
    L_0x015c:
        r0 = r21;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r24;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r9 = r0.createHttpConnection(r1);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        if (r27 == 0) goto L_0x017b;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x0166:
        if (r38 == 0) goto L_0x017b;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x0168:
        r0 = r9;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = (javax.net.ssl.HttpsURLConnection) r0;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r13 = r0;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r18 = trustAllHosts(r13);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r17 = r13.getHostnameVerifier();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = DO_NOT_VERIFY;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r13.setHostnameVerifier(r0);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x017b:
        r28 = "GET";	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r9.setRequestMethod(r0);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = r24.toString();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r32;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r11 = r0.getCookies(r1);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        if (r11 == 0) goto L_0x0197;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x0190:
        r28 = "cookie";	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r9.setRequestProperty(r0, r11);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x0197:
        r28 = "Accept-Encoding";	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = "gzip";	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r29;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r9.setRequestProperty(r0, r1);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        if (r35 == 0) goto L_0x01a9;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x01a4:
        r0 = r35;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        addHeadersToRequest(r9, r0);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x01a9:
        r9.connect();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = r9.getResponseCode();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = 304; // 0x130 float:4.26E-43 double:1.5E-321;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r29;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        if (r0 != r1) goto L_0x01fd;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x01b8:
        r8 = 1;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r9.disconnect();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = new java.lang.StringBuilder;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28.<init>();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = "Resource not modified: ";	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = r28.append(r29);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r33;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = r0.append(r1);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = r28.toString();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = 2;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r30 = r9.getResponseCode();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r32;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r2 = r29;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r3 = r37;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r4 = r30;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0.sendErrorMessage(r1, r2, r3, r4);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r23 = 0;
        if (r9 == 0) goto L_0x003d;
    L_0x01ea:
        if (r38 == 0) goto L_0x003d;
    L_0x01ec:
        if (r27 == 0) goto L_0x003d;
    L_0x01ee:
        r13 = r9;
        r13 = (javax.net.ssl.HttpsURLConnection) r13;
        r0 = r17;
        r13.setHostnameVerifier(r0);
        r0 = r18;
        r13.setSSLSocketFactory(r0);
        goto L_0x003d;
    L_0x01fd:
        r28 = r9.getContentEncoding();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        if (r28 == 0) goto L_0x020f;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x0203:
        r28 = r9.getContentEncoding();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = "gzip";	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = r28.equalsIgnoreCase(r29);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        if (r28 == 0) goto L_0x025e;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x020f:
        r10 = r9.getContentLength();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = -1;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        if (r10 == r0) goto L_0x025e;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x0219:
        r0 = (long) r10;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = r0;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r30 = r32.getFreeSpace();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = (r28 > r30 ? 1 : (r28 == r30 ? 0 : -1));	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        if (r28 <= 0) goto L_0x0254;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x0224:
        r8 = 1;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r9.disconnect();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r28 = "Not enough free space to download";	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r29 = 2;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r30 = r9.getResponseCode();	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0 = r32;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r2 = r29;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r3 = r37;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r4 = r30;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0.sendErrorMessage(r1, r2, r3, r4);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r23 = 0;
        if (r9 == 0) goto L_0x003d;
    L_0x0241:
        if (r38 == 0) goto L_0x003d;
    L_0x0243:
        if (r27 == 0) goto L_0x003d;
    L_0x0245:
        r13 = r9;
        r13 = (javax.net.ssl.HttpsURLConnection) r13;
        r0 = r17;
        r13.setHostnameVerifier(r0);
        r0 = r18;
        r13.setSSLSocketFactory(r0);
        goto L_0x003d;
    L_0x0254:
        r0 = (long) r10;
        r28 = r0;
        r0 = r36;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r1 = r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        r0.setTotal(r1);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x025e:
        r14 = getInputStream(r9);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        goto L_0x0133;
    L_0x0264:
        r28 = move-exception;
        monitor-exit(r36);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        throw r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x0267:
        r12 = move-exception;
        r23 = 0;
        r28 = r12.getLocalizedMessage();	 Catch:{ IOException -> 0x0358, all -> 0x02e0 }
        r29 = 2;	 Catch:{ IOException -> 0x0358, all -> 0x02e0 }
        r30 = r9.getResponseCode();	 Catch:{ IOException -> 0x0358, all -> 0x02e0 }
        r0 = r32;	 Catch:{ IOException -> 0x0358, all -> 0x02e0 }
        r1 = r28;	 Catch:{ IOException -> 0x0358, all -> 0x02e0 }
        r2 = r29;	 Catch:{ IOException -> 0x0358, all -> 0x02e0 }
        r3 = r37;	 Catch:{ IOException -> 0x0358, all -> 0x02e0 }
        r4 = r30;	 Catch:{ IOException -> 0x0358, all -> 0x02e0 }
        r0.sendErrorMessage(r1, r2, r3, r4);	 Catch:{ IOException -> 0x0358, all -> 0x02e0 }
    L_0x0281:
        if (r9 == 0) goto L_0x003d;
    L_0x0283:
        if (r38 == 0) goto L_0x003d;
    L_0x0285:
        if (r27 == 0) goto L_0x003d;
    L_0x0287:
        r13 = r9;
        r13 = (javax.net.ssl.HttpsURLConnection) r13;
        r0 = r17;
        r13.setHostnameVerifier(r0);
        r0 = r18;
        r13.setSSLSocketFactory(r0);
        goto L_0x003d;
    L_0x0296:
        monitor-exit(r36);	 Catch:{ all -> 0x02d6 }
        r28 = 16384; // 0x4000 float:2.2959E-41 double:8.0948E-320;
        r0 = r28;	 Catch:{ all -> 0x02d6 }
        r6 = new byte[r0];	 Catch:{ all -> 0x02d6 }
        r7 = 0;	 Catch:{ all -> 0x02d6 }
        r0 = r21;	 Catch:{ all -> 0x02d6 }
        r1 = r25;	 Catch:{ all -> 0x02d6 }
        r19 = r0.openOutputStream(r1);	 Catch:{ all -> 0x02d6 }
    L_0x02a6:
        r7 = r14.read(r6);	 Catch:{ all -> 0x02d6 }
        if (r7 <= 0) goto L_0x0335;	 Catch:{ all -> 0x02d6 }
    L_0x02ac:
        monitor-enter(r36);	 Catch:{ all -> 0x02d6 }
        r28 = r36.isAborted();	 Catch:{ all -> 0x02d6 }
        if (r28 == 0) goto L_0x02f8;	 Catch:{ all -> 0x02d6 }
    L_0x02b3:
        r23 = 0;	 Catch:{ all -> 0x02d6 }
        monitor-exit(r36);	 Catch:{ all -> 0x02d6 }
        monitor-enter(r36);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        monitor-exit(r36);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        safeClose(r14);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        safeClose(r19);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        if (r9 == 0) goto L_0x003d;
    L_0x02c0:
        if (r38 == 0) goto L_0x003d;
    L_0x02c2:
        if (r27 == 0) goto L_0x003d;
    L_0x02c4:
        r13 = r9;
        r13 = (javax.net.ssl.HttpsURLConnection) r13;
        r0 = r17;
        r13.setHostnameVerifier(r0);
        r0 = r18;
        r13.setSSLSocketFactory(r0);
        goto L_0x003d;
    L_0x02d3:
        r28 = move-exception;
        monitor-exit(r36);	 Catch:{ all -> 0x02d6 }
        throw r28;	 Catch:{ all -> 0x02d6 }
    L_0x02d6:
        r28 = move-exception;
        monitor-enter(r36);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        monitor-exit(r36);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        safeClose(r14);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        safeClose(r19);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        throw r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x02e0:
        r28 = move-exception;
        if (r9 == 0) goto L_0x02f4;
    L_0x02e3:
        if (r38 == 0) goto L_0x02f4;
    L_0x02e5:
        if (r27 == 0) goto L_0x02f4;
    L_0x02e7:
        r13 = r9;
        r13 = (javax.net.ssl.HttpsURLConnection) r13;
        r0 = r17;
        r13.setHostnameVerifier(r0);
        r0 = r18;
        r13.setSSLSocketFactory(r0);
    L_0x02f4:
        throw r28;
    L_0x02f5:
        r28 = move-exception;
        monitor-exit(r36);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        throw r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x02f8:
        monitor-exit(r36);	 Catch:{ all -> 0x02d6 }
        r28 = "ContentSync";	 Catch:{ all -> 0x02d6 }
        r29 = new java.lang.StringBuilder;	 Catch:{ all -> 0x02d6 }
        r29.<init>();	 Catch:{ all -> 0x02d6 }
        r30 = "bytes read = ";	 Catch:{ all -> 0x02d6 }
        r29 = r29.append(r30);	 Catch:{ all -> 0x02d6 }
        r0 = r29;	 Catch:{ all -> 0x02d6 }
        r29 = r0.append(r7);	 Catch:{ all -> 0x02d6 }
        r29 = r29.toString();	 Catch:{ all -> 0x02d6 }
        android.util.Log.d(r28, r29);	 Catch:{ all -> 0x02d6 }
        r28 = 0;	 Catch:{ all -> 0x02d6 }
        r0 = r19;	 Catch:{ all -> 0x02d6 }
        r1 = r28;	 Catch:{ all -> 0x02d6 }
        r0.write(r6, r1, r7);	 Catch:{ all -> 0x02d6 }
        r28 = r14.getTotalRawBytesRead();	 Catch:{ all -> 0x02d6 }
        r0 = r36;	 Catch:{ all -> 0x02d6 }
        r1 = r28;	 Catch:{ all -> 0x02d6 }
        r0.setLoaded(r1);	 Catch:{ all -> 0x02d6 }
        r0 = r32;	 Catch:{ all -> 0x02d6 }
        r1 = r37;	 Catch:{ all -> 0x02d6 }
        r2 = r36;	 Catch:{ all -> 0x02d6 }
        r0.updateProgress(r1, r2);	 Catch:{ all -> 0x02d6 }
        goto L_0x02a6;
    L_0x0332:
        r28 = move-exception;
        monitor-exit(r36);	 Catch:{ all -> 0x02d6 }
        throw r28;	 Catch:{ all -> 0x02d6 }
    L_0x0335:
        monitor-enter(r36);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        monitor-exit(r36);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        safeClose(r14);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        safeClose(r19);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x033d:
        if (r9 == 0) goto L_0x003d;
    L_0x033f:
        if (r38 == 0) goto L_0x003d;
    L_0x0341:
        if (r27 == 0) goto L_0x003d;
    L_0x0343:
        r13 = r9;
        r13 = (javax.net.ssl.HttpsURLConnection) r13;
        r0 = r17;
        r13.setHostnameVerifier(r0);
        r0 = r18;
        r13.setSSLSocketFactory(r0);
        goto L_0x003d;
    L_0x0352:
        r28 = move-exception;
        monitor-exit(r36);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        throw r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x0355:
        r28 = move-exception;
        monitor-exit(r36);	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
        throw r28;	 Catch:{ Throwable -> 0x0267, all -> 0x02e0 }
    L_0x0358:
        r28 = move-exception;
        goto L_0x0281;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.adobe.phonegap.contentsync.Sync.download(java.lang.String, java.io.File, org.json.JSONObject, com.adobe.phonegap.contentsync.Sync$ProgressEvent, org.apache.cordova.CallbackContext, boolean):boolean");
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("sync")) {
            sync(args, callbackContext);
            return true;
        } else if (action.equals("download")) {
            final String source = args.getString(0);
            final File target = new File(this.cordova.getActivity().getCacheDir().getAbsolutePath(), source.substring(source.lastIndexOf("/") + 1, source.length()));
            final JSONObject headers = new JSONObject();
            finalContext = callbackContext;
            this.cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    if (Sync.this.download(source, target, headers, Sync.this.createProgressEvent("download"), finalContext, false)) {
                        JSONObject retval = new JSONObject();
                        try {
                            retval.put("archiveURL", target.getAbsolutePath());
                        } catch (JSONException e) {
                        }
                        finalContext.sendPluginResult(new PluginResult(Status.OK, retval));
                    }
                }
            });
            return true;
        } else if (action.equals("unzip")) {
            String tempPath = args.getString(0);
            if (tempPath.startsWith("file://")) {
                tempPath = tempPath.substring(7);
            }
            final File source2 = new File(tempPath);
            final String target2 = args.getString(1);
            finalContext = callbackContext;
            this.cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    Sync.this.unzipSync(source2, target2, Sync.this.createProgressEvent("unzip"), finalContext);
                    finalContext.sendPluginResult(new PluginResult(Status.OK));
                }
            });
            return true;
        } else if (!action.equals("cancel")) {
            return false;
        } else {
            ProgressEvent progress = (ProgressEvent) activeRequests.get(args.getString(0));
            if (progress == null) {
                return false;
            }
            progress.setAborted(true);
            return false;
        }
    }

    private void sendErrorMessage(String message, int type, CallbackContext callbackContext) {
        sendErrorMessage(message, type, callbackContext, -1);
    }

    private void sendErrorMessage(String message, int type, CallbackContext callbackContext, int httpResponseCode) {
        Log.e(LOG_TAG, message);
        JSONObject error = new JSONObject();
        try {
            error.put("type", type);
            error.put("responseCode", httpResponseCode);
        } catch (JSONException e) {
        }
        callbackContext.error(error);
    }

    private long getFreeSpace() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        return ((long) stat.getAvailableBlocks()) * ((long) stat.getBlockSize());
    }

    private ProgressEvent createProgressEvent(String id) {
        ProgressEvent progress = new ProgressEvent();
        synchronized (activeRequests) {
            activeRequests.put(id, progress);
        }
        return progress;
    }

    private void sync(JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject headers;
        boolean copyCordovaAssets;
        final String src = args.getString(0);
        final String id = args.getString(1);
        if (args.optJSONObject(3) != null) {
            headers = args.optJSONObject(3);
        } else {
            headers = new JSONObject();
        }
        final boolean copyRootApp = args.getBoolean(5);
        final boolean trustEveryone = args.getBoolean(7);
        if (copyRootApp) {
            copyCordovaAssets = true;
        } else {
            copyCordovaAssets = args.getBoolean(4);
        }
        final String manifestFile = args.getString(8);
        Log.d(LOG_TAG, "sync called with id = " + id + " and src = " + src + "!");
        final ProgressEvent progress = createProgressEvent(id);
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Sync.this.webView.clearCache(true);
            }
        });
        final JSONArray jSONArray = args;
        final CallbackContext callbackContext2 = callbackContext;
        this.cordova.getThreadPool().execute(new Runnable() {
            /* JADX WARNING: inconsistent code. */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run() {
                synchronized (progress) {
                    if (progress.isAborted()) {
                    }
                }
            }
        });
    }

    private void savePrefs() {
        Editor editor = this.cordova.getActivity().getSharedPreferences(this.cordova.getActivity().getPackageName(), 0).edit();
        editor.putInt(PREVIOUS_VERSION, getCurrentAppVersion());
        editor.commit();
    }

    private boolean hasAppBeenUpdated() {
        Activity activity = this.cordova.getActivity();
        int previousAppVersion = activity.getSharedPreferences(activity.getPackageName(), 0).getInt(PREVIOUS_VERSION, -1);
        int currentAppVersion = getCurrentAppVersion();
        Log.d(LOG_TAG, "current = " + currentAppVersion);
        Log.d(LOG_TAG, "previous = " + previousAppVersion);
        if (currentAppVersion > previousAppVersion) {
            return true;
        }
        return false;
    }

    private int getCurrentAppVersion() {
        Activity activity = this.cordova.getActivity();
        int currentAppVersion = -1;
        try {
            return activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            return currentAppVersion;
        }
    }

    private boolean isZipFile(File targetFile) {
        try {
            ZipFile zip = new ZipFile(targetFile);
            Log.d(LOG_TAG, "seems like a zip file");
            return true;
        } catch (IOException e) {
            Log.d(LOG_TAG, "not a zip file");
            return false;
        }
    }

    private String getOutputDirectory(String id) {
        String outputDirectory = this.cordova.getActivity().getFilesDir().getAbsolutePath();
        outputDirectory = (outputDirectory + (outputDirectory.endsWith(File.separator) ? "" : File.separator)) + id;
        Log.d(LOG_TAG, "output dir = " + outputDirectory);
        return outputDirectory;
    }

    private File createDownloadFileLocation(String id) {
        File file = null;
        try {
            file = File.createTempFile("cdv_" + (id.lastIndexOf("/") > -1 ? id.substring(id.lastIndexOf("/") + 1, id.length()) : id), ".tmp", this.cordova.getActivity().getCacheDir());
        } catch (IOException e1) {
            Log.e(LOG_TAG, e1.getLocalizedMessage(), e1);
        }
        return file;
    }

    private File backupExistingDirectory(String outputDirectory, String type, File dir) {
        File backup = new File(outputDirectory + ".bak");
        if (dir.exists()) {
            if (type.equals(TYPE_MERGE)) {
                try {
                    copyFolder(dir, backup);
                } catch (IOException e) {
                    Log.e(LOG_TAG, e.getLocalizedMessage(), e);
                }
            } else {
                dir.renameTo(backup);
            }
        }
        return backup;
    }

    private void copyRootApp(String outputDirectory, String manifestFile) {
        boolean wwwExists = new File(outputDirectory, "www").exists();
        boolean copied = false;
        if (!(manifestFile == null || "".equals(manifestFile))) {
            Log.d(LOG_TAG, "Manifest copy");
            try {
                copyRootAppByManifest(outputDirectory, manifestFile, wwwExists);
                copied = true;
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getLocalizedMessage(), e);
            }
        }
        if (!copied) {
            Log.d(LOG_TAG, "Long copy");
            try {
                copyAssetFileOrDir(outputDirectory, "www", wwwExists);
            } catch (IOException e2) {
                Log.e(LOG_TAG, e2.getLocalizedMessage(), e2);
            }
        }
    }

    private void copyRootAppByManifest(String outputDirectory, String manifestFile, boolean wwwExists) throws IOException, JSONException {
        File fp = new File(outputDirectory);
        if (!fp.exists()) {
            fp.mkdirs();
        }
        InputStream is = this.cordova.getActivity().getAssets().open("www/" + manifestFile);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        JSONArray files = new JSONObject(new String(buffer, "UTF-8")).getJSONArray("files");
        for (int i = 0; i < files.length(); i++) {
            Log.d(LOG_TAG, "file = " + files.getString(i));
            copyAssetFile(outputDirectory, "www/" + files.getString(i), wwwExists);
        }
    }

    private void copyCordovaAssets(String outputDirectory) {
        try {
            boolean wwwExists = new File(outputDirectory, "www").exists();
            copyAssetFile(outputDirectory, "www/cordova.js", wwwExists);
            copyAssetFile(outputDirectory, "www/cordova_plugins.js", wwwExists);
            copyAssetFileOrDir(outputDirectory, "www/plugins", wwwExists);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to copy asset file", e);
        }
    }

    private void copyAssetFileOrDir(String outputDirectory, String path, boolean wwwExists) throws IOException {
        if (path.contains(".")) {
            try {
                copyAssetFile(outputDirectory, path, wwwExists);
                return;
            } catch (IOException e) {
                copyAssetDir(outputDirectory, path, wwwExists);
                return;
            }
        }
        copyAssetDir(outputDirectory, path, wwwExists);
    }

    private void copyAssetDir(String outputDirectory, String path, boolean wwwExists) throws IOException {
        String[] assets = this.cordova.getActivity().getAssets().list(path);
        if (assets.length != 0) {
            for (String file : assets) {
                copyAssetFileOrDir(outputDirectory, path + File.separator + file, wwwExists);
            }
            return;
        }
        copyAssetFile(outputDirectory, path, wwwExists);
    }

    private static TrackingInputStream getInputStream(URLConnection conn) throws IOException {
        String encoding = conn.getContentEncoding();
        if (encoding == null || !encoding.equalsIgnoreCase("gzip")) {
            return new SimpleTrackingInputStream(conn.getInputStream());
        }
        return new TrackingGZIPInputStream(new ExposedGZIPInputStream(conn.getInputStream()));
    }

    private static void safeClose(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    private String getCookies(String target) {
        boolean gotCookie = false;
        String cookie = null;
        try {
            Method gcmMethod = this.webView.getClass().getMethod("getCookieManager", new Class[0]);
            Class iccmClass = gcmMethod.getReturnType();
            cookie = (String) iccmClass.getMethod("getCookie", new Class[]{String.class}).invoke(iccmClass.cast(gcmMethod.invoke(this.webView, new Object[0])), new Object[]{target});
            gotCookie = true;
        } catch (NoSuchMethodException e) {
        } catch (IllegalAccessException e2) {
        } catch (InvocationTargetException e3) {
        } catch (ClassCastException e4) {
        }
        if (gotCookie) {
            return cookie;
        }
        return CookieManager.getInstance().getCookie(target);
    }

    private static void addHeadersToRequest(URLConnection connection, JSONObject headers) {
        try {
            Iterator<?> iter = headers.keys();
            while (iter.hasNext()) {
                String headerKey = iter.next().toString();
                JSONArray headerValues = headers.optJSONArray(headerKey);
                if (headerValues == null) {
                    headerValues = new JSONArray();
                    headerValues.put(headers.getString(headerKey));
                }
                connection.setRequestProperty(headerKey, headerValues.getString(0));
                for (int i = 1; i < headerValues.length(); i++) {
                    connection.addRequestProperty(headerKey, headerValues.getString(i));
                }
            }
        } catch (JSONException e) {
        }
    }

    private static SSLSocketFactory trustAllHosts(HttpsURLConnection connection) {
        SSLSocketFactory oldFactory = connection.getSSLSocketFactory();
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            connection.setSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return oldFactory;
    }

    private static int readInt(InputStream is) throws IOException {
        return (((is.read() << 8) | is.read()) | (is.read() << 16)) | (is.read() << 24);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean unzipSync(File targetFile, String outputDirectory, ProgressEvent progress, CallbackContext callbackContext) {
        Log.d(LOG_TAG, "unzipSync called");
        Log.d(LOG_TAG, "zip = " + targetFile.getAbsolutePath());
        InputStream inputStream = null;
        ZipFile zip = null;
        boolean anyEntries = false;
        try {
            synchronized (progress) {
                if (progress.isAborted()) {
                }
            }
            zip = r0;
            return false;
            if (r0 != null) {
                try {
                    r0.close();
                } catch (IOException e) {
                }
            }
            zip = r0;
            return false;
            zip = r0;
            return false;
            if (zip == null) {
                return false;
            }
            try {
                zip.close();
                return false;
            } catch (IOException e2) {
                return false;
            }
            if (r0 != null) {
                try {
                    r0.close();
                    zip = r0;
                } catch (IOException e3) {
                    zip = r0;
                }
            } else {
                zip = r0;
            }
            if (anyEntries) {
                return true;
            }
            return false;
            if (r0 != null) {
                try {
                    r0.close();
                } catch (IOException e4) {
                }
            }
            zip = r0;
            return false;
        } catch (Exception e5) {
            Exception e6 = e5;
        }
    }

    private void updateProgress(CallbackContext callbackContext, ProgressEvent progress) {
        try {
            if (progress.getLoaded() != progress.getTotal() || progress.getStatus() == 3) {
                PluginResult pluginResult = new PluginResult(Status.OK, progress.toJSONObject());
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }
        } catch (JSONException e) {
        }
    }

    private Uri getUriForArg(String arg) {
        Uri tmpTarget = Uri.parse(arg);
        CordovaResourceApi resourceApi = this.webView.getResourceApi();
        if (tmpTarget.getScheme() == null) {
            tmpTarget = Uri.fromFile(new File(arg));
        }
        return resourceApi.remapUri(tmpTarget);
    }

    private void copyFolder(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdir();
            }
            for (String file : src.list()) {
                copyFolder(new File(src, file), new File(dest, file));
            }
            return;
        }
        copyFile(new FileInputStream(src), new FileOutputStream(dest));
    }

    private void copyAssetFile(String outputDirectory, String filename, boolean wwwExists) throws IOException {
        String targetFile = filename;
        if (!wwwExists && targetFile.startsWith("www/")) {
            targetFile = targetFile.substring(4, targetFile.length());
        }
        int lastIndex = targetFile.lastIndexOf("/");
        File targetDir;
        if (lastIndex > 0) {
            targetDir = new File(outputDirectory + "/" + targetFile.substring(0, lastIndex));
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
        } else {
            targetDir = new File(outputDirectory);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
        }
        copyFile(this.cordova.getActivity().getAssets().open(filename), new FileOutputStream(new File(outputDirectory, targetFile)));
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        while (true) {
            int length = in.read(buffer);
            if (length > 0) {
                out.write(buffer, 0, length);
            } else {
                in.close();
                out.close();
                return;
            }
        }
    }

    private void removeFolder(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    removeFolder(file);
                }
            }
        }
        directory.delete();
    }
}
