package org.apache.cordova.file;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.apache.cordova.file.Filesystem.ReadFileCallback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FileUtils extends CordovaPlugin {
    public static int ABORT_ERR = 3;
    public static int ENCODING_ERR = 5;
    public static int INVALID_MODIFICATION_ERR = 9;
    public static int INVALID_STATE_ERR = 7;
    private static final String LOG_TAG = "FileUtils";
    public static int NOT_FOUND_ERR = 1;
    public static int NOT_READABLE_ERR = 4;
    public static int NO_MODIFICATION_ALLOWED_ERR = 6;
    public static int PATH_EXISTS_ERR = 12;
    public static int QUOTA_EXCEEDED_ERR = 10;
    public static int SECURITY_ERR = 2;
    public static int SYNTAX_ERR = 8;
    public static int TYPE_MISMATCH_ERR = 11;
    public static int UNKNOWN_ERR = 1000;
    private static FileUtils filePlugin;
    private boolean configured = false;
    private ArrayList<Filesystem> filesystems;

    private interface FileOp {
        void run(JSONArray jSONArray) throws Exception;
    }

    public void registerFilesystem(Filesystem fs) {
        if (fs != null && filesystemForName(fs.name) == null) {
            this.filesystems.add(fs);
        }
    }

    private Filesystem filesystemForName(String name) {
        Iterator it = this.filesystems.iterator();
        while (it.hasNext()) {
            Filesystem fs = (Filesystem) it.next();
            if (fs != null && fs.name != null && fs.name.equals(name)) {
                return fs;
            }
        }
        return null;
    }

    protected String[] getExtraFileSystemsPreference(Activity activity) {
        return this.preferences.getString("androidextrafilesystems", "files,files-external,documents,sdcard,cache,cache-external,root").split(",");
    }

    protected void registerExtraFileSystems(String[] filesystems, HashMap<String, String> availableFileSystems) {
        HashSet<String> installedFileSystems = new HashSet();
        for (String fsName : filesystems) {
            if (!installedFileSystems.contains(fsName)) {
                String fsRoot = (String) availableFileSystems.get(fsName);
                if (fsRoot != null) {
                    File newRoot = new File(fsRoot);
                    if (newRoot.mkdirs() || newRoot.isDirectory()) {
                        registerFilesystem(new LocalFilesystem(fsName, this.webView.getContext(), this.webView.getResourceApi(), newRoot));
                        installedFileSystems.add(fsName);
                    } else {
                        Log.d(LOG_TAG, "Unable to create root dir for filesystem \"" + fsName + "\", skipping");
                    }
                } else {
                    Log.d(LOG_TAG, "Unrecognized extra filesystem identifier: " + fsName);
                }
            }
        }
    }

    protected HashMap<String, String> getAvailableFileSystems(Activity activity) {
        Context context = activity.getApplicationContext();
        HashMap<String, String> availableFileSystems = new HashMap();
        availableFileSystems.put("files", context.getFilesDir().getAbsolutePath());
        availableFileSystems.put("documents", new File(context.getFilesDir(), "Documents").getAbsolutePath());
        availableFileSystems.put("cache", context.getCacheDir().getAbsolutePath());
        availableFileSystems.put("root", "/");
        if (Environment.getExternalStorageState().equals("mounted")) {
            try {
                availableFileSystems.put("files-external", context.getExternalFilesDir(null).getAbsolutePath());
                availableFileSystems.put("sdcard", Environment.getExternalStorageDirectory().getAbsolutePath());
                availableFileSystems.put("cache-external", context.getExternalCacheDir().getAbsolutePath());
            } catch (NullPointerException e) {
                Log.d(LOG_TAG, "External storage unavailable, check to see if USB Mass Storage Mode is on");
            }
        }
        return availableFileSystems;
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.filesystems = new ArrayList();
        String persistentRoot = null;
        Activity activity = cordova.getActivity();
        String packageName = activity.getPackageName();
        String location = this.preferences.getString("androidpersistentfilelocation", "internal");
        String tempRoot = activity.getCacheDir().getAbsolutePath();
        if ("internal".equalsIgnoreCase(location)) {
            persistentRoot = activity.getFilesDir().getAbsolutePath() + "/files/";
            this.configured = true;
        } else if ("compatibility".equalsIgnoreCase(location)) {
            if (Environment.getExternalStorageState().equals("mounted")) {
                persistentRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
                tempRoot = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + packageName + "/cache/";
            } else {
                persistentRoot = "/data/data/" + packageName;
            }
            this.configured = true;
        }
        if (this.configured) {
            File tmpRootFile = new File(tempRoot);
            File persistentRootFile = new File(persistentRoot);
            tmpRootFile.mkdirs();
            persistentRootFile.mkdirs();
            registerFilesystem(new LocalFilesystem("temporary", webView.getContext(), webView.getResourceApi(), tmpRootFile));
            registerFilesystem(new LocalFilesystem("persistent", webView.getContext(), webView.getResourceApi(), persistentRootFile));
            registerFilesystem(new ContentFilesystem(webView.getContext(), webView.getResourceApi()));
            registerFilesystem(new AssetFilesystem(webView.getContext().getAssets(), webView.getResourceApi()));
            registerExtraFileSystems(getExtraFileSystemsPreference(activity), getAvailableFileSystems(activity));
            if (filePlugin == null) {
                filePlugin = this;
                return;
            }
            return;
        }
        Log.e(LOG_TAG, "File plugin configuration error: Please set AndroidPersistentFileLocation in config.xml to one of \"internal\" (for new applications) or \"compatibility\" (for compatibility with previous versions)");
        activity.finish();
    }

    public static FileUtils getFilePlugin() {
        return filePlugin;
    }

    private Filesystem filesystemForURL(LocalFilesystemURL localURL) {
        if (localURL == null) {
            return null;
        }
        return filesystemForName(localURL.fsName);
    }

    public Uri remapUri(Uri uri) {
        Uri uri2 = null;
        if (LocalFilesystemURL.FILESYSTEM_PROTOCOL.equals(uri.getScheme())) {
            try {
                LocalFilesystemURL inputURL = LocalFilesystemURL.parse(uri);
                Filesystem fs = filesystemForURL(inputURL);
                if (!(fs == null || fs.filesystemPathForURL(inputURL) == null)) {
                    uri2 = Uri.parse("file://" + fs.filesystemPathForURL(inputURL));
                }
            } catch (IllegalArgumentException e) {
            }
        }
        return uri2;
    }

    public boolean execute(String action, String rawArgs, final CallbackContext callbackContext) {
        if (!this.configured) {
            callbackContext.sendPluginResult(new PluginResult(Status.ERROR, "File plugin is not configured. Please see the README.md file for details on how to update config.xml"));
            return true;
        } else if (action.equals("testSaveLocationExists")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) {
                    callbackContext.sendPluginResult(new PluginResult(Status.OK, DirectoryManager.testSaveLocationExists()));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("getFreeDiskSpace")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) {
                    callbackContext.sendPluginResult(new PluginResult(Status.OK, (float) DirectoryManager.getFreeDiskSpace(false)));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("testFileExists")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws JSONException {
                    callbackContext.sendPluginResult(new PluginResult(Status.OK, DirectoryManager.testFileExists(args.getString(0))));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("testDirectoryExists")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws JSONException {
                    callbackContext.sendPluginResult(new PluginResult(Status.OK, DirectoryManager.testFileExists(args.getString(0))));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("readAsText")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws JSONException, MalformedURLException {
                    String encoding = args.getString(1);
                    int start = args.getInt(2);
                    int end = args.getInt(3);
                    FileUtils.this.readFileAs(args.getString(0), start, end, callbackContext, encoding, 1);
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("readAsDataURL")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws JSONException, MalformedURLException {
                    int start = args.getInt(1);
                    int end = args.getInt(2);
                    FileUtils.this.readFileAs(args.getString(0), start, end, callbackContext, null, -1);
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("readAsArrayBuffer")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws JSONException, MalformedURLException {
                    int start = args.getInt(1);
                    int end = args.getInt(2);
                    FileUtils.this.readFileAs(args.getString(0), start, end, callbackContext, null, 6);
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("readAsBinaryString")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws JSONException, MalformedURLException {
                    int start = args.getInt(1);
                    int end = args.getInt(2);
                    FileUtils.this.readFileAs(args.getString(0), start, end, callbackContext, null, 7);
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("write")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws JSONException, FileNotFoundException, IOException, NoModificationAllowedException {
                    callbackContext.sendPluginResult(new PluginResult(Status.OK, (float) FileUtils.this.write(args.getString(0), args.getString(1), args.getInt(2), Boolean.valueOf(args.getBoolean(3)).booleanValue())));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("truncate")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws JSONException, FileNotFoundException, IOException, NoModificationAllowedException {
                    callbackContext.sendPluginResult(new PluginResult(Status.OK, (float) FileUtils.this.truncateFile(args.getString(0), (long) args.getInt(1))));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("requestAllFileSystems")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws IOException, JSONException {
                    callbackContext.success(FileUtils.this.requestAllFileSystems());
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("requestAllPaths")) {
            this.cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        callbackContext.success(FileUtils.this.requestAllPaths());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            return true;
        } else if (action.equals("requestFileSystem")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws IOException, JSONException {
                    int fstype = args.getInt(0);
                    long size = args.optLong(1);
                    if (size == 0 || size <= DirectoryManager.getFreeDiskSpace(true) * 1024) {
                        callbackContext.success(FileUtils.this.requestFileSystem(fstype));
                        return;
                    }
                    callbackContext.sendPluginResult(new PluginResult(Status.ERROR, FileUtils.QUOTA_EXCEEDED_ERR));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("resolveLocalFileSystemURI")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws IOException, JSONException {
                    callbackContext.success(FileUtils.this.resolveLocalFileSystemURI(args.getString(0)));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("getFileMetadata")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws FileNotFoundException, JSONException, MalformedURLException {
                    callbackContext.success(FileUtils.this.getFileMetadata(args.getString(0)));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("getParent")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws JSONException, IOException {
                    callbackContext.success(FileUtils.this.getParent(args.getString(0)));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("getDirectory")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws FileExistsException, IOException, TypeMismatchException, EncodingException, JSONException {
                    callbackContext.success(FileUtils.this.getFile(args.getString(0), args.getString(1), args.optJSONObject(2), true));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("getFile")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws FileExistsException, IOException, TypeMismatchException, EncodingException, JSONException {
                    callbackContext.success(FileUtils.this.getFile(args.getString(0), args.getString(1), args.optJSONObject(2), false));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("remove")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws JSONException, NoModificationAllowedException, InvalidModificationException, MalformedURLException {
                    if (FileUtils.this.remove(args.getString(0))) {
                        callbackContext.success();
                    } else {
                        callbackContext.error(FileUtils.NO_MODIFICATION_ALLOWED_ERR);
                    }
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("removeRecursively")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws JSONException, FileExistsException, MalformedURLException, NoModificationAllowedException {
                    if (FileUtils.this.removeRecursively(args.getString(0))) {
                        callbackContext.success();
                    } else {
                        callbackContext.error(FileUtils.NO_MODIFICATION_ALLOWED_ERR);
                    }
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("moveTo")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws JSONException, NoModificationAllowedException, IOException, InvalidModificationException, EncodingException, FileExistsException {
                    callbackContext.success(FileUtils.this.transferTo(args.getString(0), args.getString(1), args.getString(2), true));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("copyTo")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws JSONException, NoModificationAllowedException, IOException, InvalidModificationException, EncodingException, FileExistsException {
                    callbackContext.success(FileUtils.this.transferTo(args.getString(0), args.getString(1), args.getString(2), false));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (action.equals("readEntries")) {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws FileNotFoundException, JSONException, MalformedURLException {
                    callbackContext.success(FileUtils.this.readEntries(args.getString(0)));
                }
            }, rawArgs, callbackContext);
            return true;
        } else if (!action.equals("_getLocalFilesystemPath")) {
            return false;
        } else {
            threadhelper(new FileOp() {
                public void run(JSONArray args) throws FileNotFoundException, JSONException, MalformedURLException {
                    callbackContext.success(FileUtils.this.filesystemPathForURL(args.getString(0)));
                }
            }, rawArgs, callbackContext);
            return true;
        }
    }

    public LocalFilesystemURL resolveNativeUri(Uri nativeUri) {
        LocalFilesystemURL localURL = null;
        Iterator it = this.filesystems.iterator();
        while (it.hasNext()) {
            LocalFilesystemURL url = ((Filesystem) it.next()).toLocalUri(nativeUri);
            if (url != null && (localURL == null || url.uri.toString().length() < localURL.toString().length())) {
                localURL = url;
            }
        }
        return localURL;
    }

    public String filesystemPathForURL(String localURLstr) throws MalformedURLException {
        try {
            LocalFilesystemURL inputURL = LocalFilesystemURL.parse(localURLstr);
            Filesystem fs = filesystemForURL(inputURL);
            if (fs != null) {
                return fs.filesystemPathForURL(inputURL);
            }
            throw new MalformedURLException("No installed handlers for this URL");
        } catch (IllegalArgumentException e) {
            throw new MalformedURLException("Unrecognized filesystem URL");
        }
    }

    public LocalFilesystemURL filesystemURLforLocalPath(String localPath) {
        LocalFilesystemURL localURL = null;
        int shortestFullPath = 0;
        Iterator it = this.filesystems.iterator();
        while (it.hasNext()) {
            LocalFilesystemURL url = ((Filesystem) it.next()).URLforFilesystemPath(localPath);
            if (url != null && (localURL == null || url.path.length() < shortestFullPath)) {
                localURL = url;
                shortestFullPath = url.path.length();
            }
        }
        return localURL;
    }

    private void threadhelper(final FileOp f, final String rawArgs, final CallbackContext callbackContext) {
        this.cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    f.run(new JSONArray(rawArgs));
                } catch (Exception e) {
                    if (e instanceof EncodingException) {
                        callbackContext.error(FileUtils.ENCODING_ERR);
                    } else if (e instanceof FileNotFoundException) {
                        callbackContext.error(FileUtils.NOT_FOUND_ERR);
                    } else if (e instanceof FileExistsException) {
                        callbackContext.error(FileUtils.PATH_EXISTS_ERR);
                    } else if (e instanceof NoModificationAllowedException) {
                        callbackContext.error(FileUtils.NO_MODIFICATION_ALLOWED_ERR);
                    } else if (e instanceof InvalidModificationException) {
                        callbackContext.error(FileUtils.INVALID_MODIFICATION_ERR);
                    } else if (e instanceof MalformedURLException) {
                        callbackContext.error(FileUtils.ENCODING_ERR);
                    } else if (e instanceof IOException) {
                        callbackContext.error(FileUtils.INVALID_MODIFICATION_ERR);
                    } else if (e instanceof EncodingException) {
                        callbackContext.error(FileUtils.ENCODING_ERR);
                    } else if (e instanceof TypeMismatchException) {
                        callbackContext.error(FileUtils.TYPE_MISMATCH_ERR);
                    } else if (e instanceof JSONException) {
                        callbackContext.sendPluginResult(new PluginResult(Status.JSON_EXCEPTION));
                    } else {
                        e.printStackTrace();
                        callbackContext.error(FileUtils.UNKNOWN_ERR);
                    }
                }
            }
        });
    }

    private JSONObject resolveLocalFileSystemURI(String uriString) throws IOException, JSONException {
        if (uriString == null) {
            throw new MalformedURLException("Unrecognized filesystem URL");
        }
        Uri uri = Uri.parse(uriString);
        LocalFilesystemURL inputURL = LocalFilesystemURL.parse(uri);
        if (inputURL == null) {
            inputURL = resolveNativeUri(uri);
        }
        try {
            Filesystem fs = filesystemForURL(inputURL);
            if (fs == null) {
                throw new MalformedURLException("No installed handlers for this URL");
            } else if (fs.exists(inputURL)) {
                return fs.getEntryForLocalURL(inputURL);
            } else {
                throw new FileNotFoundException();
            }
        } catch (IllegalArgumentException e) {
            throw new MalformedURLException("Unrecognized filesystem URL");
        }
    }

    private JSONArray readEntries(String baseURLstr) throws FileNotFoundException, JSONException, MalformedURLException {
        try {
            LocalFilesystemURL inputURL = LocalFilesystemURL.parse(baseURLstr);
            Filesystem fs = filesystemForURL(inputURL);
            if (fs != null) {
                return fs.readEntriesAtLocalURL(inputURL);
            }
            throw new MalformedURLException("No installed handlers for this URL");
        } catch (IllegalArgumentException e) {
            throw new MalformedURLException("Unrecognized filesystem URL");
        }
    }

    private JSONObject transferTo(String srcURLstr, String destURLstr, String newName, boolean move) throws JSONException, NoModificationAllowedException, IOException, InvalidModificationException, EncodingException, FileExistsException {
        if (srcURLstr == null || destURLstr == null) {
            throw new FileNotFoundException();
        }
        LocalFilesystemURL srcURL = LocalFilesystemURL.parse(srcURLstr);
        LocalFilesystemURL destURL = LocalFilesystemURL.parse(destURLstr);
        Filesystem srcFs = filesystemForURL(srcURL);
        Filesystem destFs = filesystemForURL(destURL);
        if (newName == null || !newName.contains(":")) {
            return destFs.copyFileToURL(destURL, newName, srcFs, srcURL, move);
        }
        throw new EncodingException("Bad file name");
    }

    private boolean removeRecursively(String baseURLstr) throws FileExistsException, NoModificationAllowedException, MalformedURLException {
        try {
            LocalFilesystemURL inputURL = LocalFilesystemURL.parse(baseURLstr);
            if ("".equals(inputURL.path) || "/".equals(inputURL.path)) {
                throw new NoModificationAllowedException("You can't delete the root directory");
            }
            Filesystem fs = filesystemForURL(inputURL);
            if (fs != null) {
                return fs.recursiveRemoveFileAtLocalURL(inputURL);
            }
            throw new MalformedURLException("No installed handlers for this URL");
        } catch (IllegalArgumentException e) {
            throw new MalformedURLException("Unrecognized filesystem URL");
        }
    }

    private boolean remove(String baseURLstr) throws NoModificationAllowedException, InvalidModificationException, MalformedURLException {
        try {
            LocalFilesystemURL inputURL = LocalFilesystemURL.parse(baseURLstr);
            if ("".equals(inputURL.path) || "/".equals(inputURL.path)) {
                throw new NoModificationAllowedException("You can't delete the root directory");
            }
            Filesystem fs = filesystemForURL(inputURL);
            if (fs != null) {
                return fs.removeFileAtLocalURL(inputURL);
            }
            throw new MalformedURLException("No installed handlers for this URL");
        } catch (IllegalArgumentException e) {
            throw new MalformedURLException("Unrecognized filesystem URL");
        }
    }

    private JSONObject getFile(String baseURLstr, String path, JSONObject options, boolean directory) throws FileExistsException, IOException, TypeMismatchException, EncodingException, JSONException {
        try {
            LocalFilesystemURL inputURL = LocalFilesystemURL.parse(baseURLstr);
            Filesystem fs = filesystemForURL(inputURL);
            if (fs != null) {
                return fs.getFileForLocalURL(inputURL, path, options, directory);
            }
            throw new MalformedURLException("No installed handlers for this URL");
        } catch (IllegalArgumentException e) {
            throw new MalformedURLException("Unrecognized filesystem URL");
        }
    }

    private JSONObject getParent(String baseURLstr) throws JSONException, IOException {
        try {
            LocalFilesystemURL inputURL = LocalFilesystemURL.parse(baseURLstr);
            Filesystem fs = filesystemForURL(inputURL);
            if (fs != null) {
                return fs.getParentForLocalURL(inputURL);
            }
            throw new MalformedURLException("No installed handlers for this URL");
        } catch (IllegalArgumentException e) {
            throw new MalformedURLException("Unrecognized filesystem URL");
        }
    }

    private JSONObject getFileMetadata(String baseURLstr) throws FileNotFoundException, JSONException, MalformedURLException {
        try {
            LocalFilesystemURL inputURL = LocalFilesystemURL.parse(baseURLstr);
            Filesystem fs = filesystemForURL(inputURL);
            if (fs != null) {
                return fs.getFileMetadataForLocalURL(inputURL);
            }
            throw new MalformedURLException("No installed handlers for this URL");
        } catch (IllegalArgumentException e) {
            throw new MalformedURLException("Unrecognized filesystem URL");
        }
    }

    private JSONObject requestFileSystem(int type) throws IOException, JSONException {
        JSONObject fs = new JSONObject();
        Filesystem rootFs = null;
        try {
            rootFs = (Filesystem) this.filesystems.get(type);
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        if (rootFs == null) {
            throw new IOException("No filesystem of type requested");
        }
        fs.put("name", rootFs.name);
        fs.put("root", rootFs.getRootEntry());
        return fs;
    }

    private JSONArray requestAllFileSystems() throws IOException, JSONException {
        JSONArray ret = new JSONArray();
        Iterator it = this.filesystems.iterator();
        while (it.hasNext()) {
            ret.put(((Filesystem) it.next()).getRootEntry());
        }
        return ret;
    }

    private static String toDirUrl(File f) {
        return Uri.fromFile(f).toString() + '/';
    }

    private JSONObject requestAllPaths() throws JSONException {
        Context context = this.cordova.getActivity();
        JSONObject ret = new JSONObject();
        ret.put("applicationDirectory", "file:///android_asset/");
        ret.put("applicationStorageDirectory", toDirUrl(context.getFilesDir().getParentFile()));
        ret.put("dataDirectory", toDirUrl(context.getFilesDir()));
        ret.put("cacheDirectory", toDirUrl(context.getCacheDir()));
        if (Environment.getExternalStorageState().equals("mounted")) {
            try {
                ret.put("externalApplicationStorageDirectory", toDirUrl(context.getExternalFilesDir(null).getParentFile()));
                ret.put("externalDataDirectory", toDirUrl(context.getExternalFilesDir(null)));
                ret.put("externalCacheDirectory", toDirUrl(context.getExternalCacheDir()));
                ret.put("externalRootDirectory", toDirUrl(Environment.getExternalStorageDirectory()));
            } catch (NullPointerException e) {
                Log.d(LOG_TAG, "Unable to access these paths, most liklely due to USB storage");
            }
        }
        return ret;
    }

    public JSONObject getEntryForFile(File file) throws JSONException {
        Iterator it = this.filesystems.iterator();
        while (it.hasNext()) {
            JSONObject entry = ((Filesystem) it.next()).makeEntryForFile(file);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }

    @Deprecated
    public static JSONObject getEntry(File file) throws JSONException {
        if (getFilePlugin() != null) {
            return getFilePlugin().getEntryForFile(file);
        }
        return null;
    }

    public void readFileAs(String srcURLstr, int start, int end, final CallbackContext callbackContext, final String encoding, final int resultType) throws MalformedURLException {
        try {
            LocalFilesystemURL inputURL = LocalFilesystemURL.parse(srcURLstr);
            Filesystem fs = filesystemForURL(inputURL);
            if (fs == null) {
                throw new MalformedURLException("No installed handlers for this URL");
            }
            fs.readFileAtURL(inputURL, (long) start, (long) end, new ReadFileCallback() {
                public void handleData(InputStream inputStream, String contentType) {
                    try {
                        PluginResult result;
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        byte[] buffer = new byte[8192];
                        while (true) {
                            int bytesRead = inputStream.read(buffer, 0, 8192);
                            if (bytesRead <= 0) {
                                break;
                            }
                            os.write(buffer, 0, bytesRead);
                        }
                        switch (resultType) {
                            case 1:
                                result = new PluginResult(Status.OK, os.toString(encoding));
                                break;
                            case 6:
                                result = new PluginResult(Status.OK, os.toByteArray());
                                break;
                            case 7:
                                result = new PluginResult(Status.OK, os.toByteArray(), true);
                                break;
                            default:
                                result = new PluginResult(Status.OK, "data:" + contentType + ";base64," + new String(Base64.encode(os.toByteArray(), 2), "US-ASCII"));
                                break;
                        }
                        callbackContext.sendPluginResult(result);
                    } catch (IOException e) {
                        Log.d(FileUtils.LOG_TAG, e.getLocalizedMessage());
                        callbackContext.sendPluginResult(new PluginResult(Status.IO_EXCEPTION, FileUtils.NOT_READABLE_ERR));
                    }
                }
            });
        } catch (IllegalArgumentException e) {
            throw new MalformedURLException("Unrecognized filesystem URL");
        } catch (FileNotFoundException e2) {
            callbackContext.sendPluginResult(new PluginResult(Status.IO_EXCEPTION, NOT_FOUND_ERR));
        } catch (IOException e3) {
            Log.d(LOG_TAG, e3.getLocalizedMessage());
            callbackContext.sendPluginResult(new PluginResult(Status.IO_EXCEPTION, NOT_READABLE_ERR));
        }
    }

    public long write(String srcURLstr, String data, int offset, boolean isBinary) throws FileNotFoundException, IOException, NoModificationAllowedException {
        try {
            LocalFilesystemURL inputURL = LocalFilesystemURL.parse(srcURLstr);
            Filesystem fs = filesystemForURL(inputURL);
            if (fs == null) {
                throw new MalformedURLException("No installed handlers for this URL");
            }
            long x = fs.writeToFileAtURL(inputURL, data, offset, isBinary);
            Log.d("TEST", srcURLstr + ": " + x);
            return x;
        } catch (IllegalArgumentException e) {
            throw new MalformedURLException("Unrecognized filesystem URL");
        }
    }

    private long truncateFile(String srcURLstr, long size) throws FileNotFoundException, IOException, NoModificationAllowedException {
        try {
            LocalFilesystemURL inputURL = LocalFilesystemURL.parse(srcURLstr);
            Filesystem fs = filesystemForURL(inputURL);
            if (fs != null) {
                return fs.truncateFileAtURL(inputURL, size);
            }
            throw new MalformedURLException("No installed handlers for this URL");
        } catch (IllegalArgumentException e) {
            throw new MalformedURLException("Unrecognized filesystem URL");
        }
    }
}
