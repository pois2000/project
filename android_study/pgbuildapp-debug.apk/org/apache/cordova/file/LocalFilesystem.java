package org.apache.cordova.file;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Build.VERSION;
import android.os.Environment;
import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.apache.cordova.CordovaResourceApi;
import org.json.JSONException;
import org.json.JSONObject;

public class LocalFilesystem extends Filesystem {
    private final Context context;

    private static void copyResource(org.apache.cordova.CordovaResourceApi.OpenForReadResult r14, java.io.OutputStream r15) throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x003a in list [B:11:0x0037]
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
        r11 = r14.inputStream;	 Catch:{ all -> 0x004f }
        r4 = r11 instanceof java.io.FileInputStream;	 Catch:{ all -> 0x004f }
        if (r4 == 0) goto L_0x003b;	 Catch:{ all -> 0x004f }
    L_0x0006:
        r4 = r15 instanceof java.io.FileOutputStream;	 Catch:{ all -> 0x004f }
        if (r4 == 0) goto L_0x003b;	 Catch:{ all -> 0x004f }
    L_0x000a:
        r4 = r14.inputStream;	 Catch:{ all -> 0x004f }
        r4 = (java.io.FileInputStream) r4;	 Catch:{ all -> 0x004f }
        r3 = r4.getChannel();	 Catch:{ all -> 0x004f }
        r0 = r15;	 Catch:{ all -> 0x004f }
        r0 = (java.io.FileOutputStream) r0;	 Catch:{ all -> 0x004f }
        r4 = r0;	 Catch:{ all -> 0x004f }
        r2 = r4.getChannel();	 Catch:{ all -> 0x004f }
        r12 = 0;	 Catch:{ all -> 0x004f }
        r6 = r14.length;	 Catch:{ all -> 0x004f }
        r4 = r14.assetFd;	 Catch:{ all -> 0x004f }
        if (r4 == 0) goto L_0x0028;	 Catch:{ all -> 0x004f }
    L_0x0022:
        r4 = r14.assetFd;	 Catch:{ all -> 0x004f }
        r12 = r4.getStartOffset();	 Catch:{ all -> 0x004f }
    L_0x0028:
        r3.position(r12);	 Catch:{ all -> 0x004f }
        r4 = 0;	 Catch:{ all -> 0x004f }
        r2.transferFrom(r3, r4, r6);	 Catch:{ all -> 0x004f }
    L_0x0030:
        r4 = r14.inputStream;
        r4.close();
        if (r15 == 0) goto L_0x003a;
    L_0x0037:
        r15.close();
    L_0x003a:
        return;
    L_0x003b:
        r8 = 8192; // 0x2000 float:1.14794E-41 double:4.0474E-320;
        r4 = 8192; // 0x2000 float:1.14794E-41 double:4.0474E-320;
        r9 = new byte[r4];	 Catch:{ all -> 0x004f }
    L_0x0041:
        r4 = 0;	 Catch:{ all -> 0x004f }
        r5 = 8192; // 0x2000 float:1.14794E-41 double:4.0474E-320;	 Catch:{ all -> 0x004f }
        r10 = r11.read(r9, r4, r5);	 Catch:{ all -> 0x004f }
        if (r10 <= 0) goto L_0x0030;	 Catch:{ all -> 0x004f }
    L_0x004a:
        r4 = 0;	 Catch:{ all -> 0x004f }
        r15.write(r9, r4, r10);	 Catch:{ all -> 0x004f }
        goto L_0x0041;
    L_0x004f:
        r4 = move-exception;
        r5 = r14.inputStream;
        r5.close();
        if (r15 == 0) goto L_0x005a;
    L_0x0057:
        r15.close();
    L_0x005a:
        throw r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.apache.cordova.file.LocalFilesystem.copyResource(org.apache.cordova.CordovaResourceApi$OpenForReadResult, java.io.OutputStream):void");
    }

    public LocalFilesystem(String name, Context context, CordovaResourceApi resourceApi, File fsRoot) {
        super(Uri.fromFile(fsRoot).buildUpon().appendEncodedPath("").build(), name, resourceApi);
        this.context = context;
    }

    public String filesystemPathForFullPath(String fullPath) {
        return new File(this.rootUri.getPath(), fullPath).toString();
    }

    public String filesystemPathForURL(LocalFilesystemURL url) {
        return filesystemPathForFullPath(url.path);
    }

    private String fullPathForFilesystemPath(String absolutePath) {
        if (absolutePath == null || !absolutePath.startsWith(this.rootUri.getPath())) {
            return null;
        }
        return absolutePath.substring(this.rootUri.getPath().length() - 1);
    }

    public Uri toNativeUri(LocalFilesystemURL inputURL) {
        return nativeUriForFullPath(inputURL.path);
    }

    public LocalFilesystemURL toLocalUri(Uri inputURL) {
        if (!"file".equals(inputURL.getScheme())) {
            return null;
        }
        File f = new File(inputURL.getPath());
        Uri resolvedUri = Uri.fromFile(f);
        String rootUriNoTrailingSlash = this.rootUri.getEncodedPath();
        rootUriNoTrailingSlash = rootUriNoTrailingSlash.substring(0, rootUriNoTrailingSlash.length() - 1);
        if (!resolvedUri.getEncodedPath().startsWith(rootUriNoTrailingSlash)) {
            return null;
        }
        String subPath = resolvedUri.getEncodedPath().substring(rootUriNoTrailingSlash.length());
        if (!subPath.isEmpty()) {
            subPath = subPath.substring(1);
        }
        Builder b = new Builder().scheme(LocalFilesystemURL.FILESYSTEM_PROTOCOL).authority("localhost").path(this.name);
        if (!subPath.isEmpty()) {
            b.appendEncodedPath(subPath);
        }
        if (f.isDirectory() || inputURL.getPath().endsWith("/")) {
            b.appendEncodedPath("");
        }
        return LocalFilesystemURL.parse(b.build());
    }

    public LocalFilesystemURL URLforFilesystemPath(String path) {
        return localUrlforFullPath(fullPathForFilesystemPath(path));
    }

    public JSONObject getFileForLocalURL(LocalFilesystemURL inputURL, String path, JSONObject options, boolean directory) throws FileExistsException, IOException, TypeMismatchException, EncodingException, JSONException {
        boolean create = false;
        boolean exclusive = false;
        if (options != null) {
            create = options.optBoolean("create");
            if (create) {
                exclusive = options.optBoolean("exclusive");
            }
        }
        if (path.contains(":")) {
            throw new EncodingException("This path has an invalid \":\" in it.");
        }
        LocalFilesystemURL requestedURL;
        if (directory && !path.endsWith("/")) {
            path = path + "/";
        }
        if (path.startsWith("/")) {
            requestedURL = localUrlforFullPath(Filesystem.normalizePath(path));
        } else {
            requestedURL = localUrlforFullPath(Filesystem.normalizePath(inputURL.path + "/" + path));
        }
        File fp = new File(filesystemPathForURL(requestedURL));
        if (create) {
            if (exclusive && fp.exists()) {
                throw new FileExistsException("create/exclusive fails");
            }
            if (directory) {
                fp.mkdir();
            } else {
                fp.createNewFile();
            }
            if (!fp.exists()) {
                throw new FileExistsException("create fails");
            }
        } else if (!fp.exists()) {
            throw new FileNotFoundException("path does not exist");
        } else if (directory) {
            if (fp.isFile()) {
                throw new TypeMismatchException("path doesn't exist or is file");
            }
        } else if (fp.isDirectory()) {
            throw new TypeMismatchException("path doesn't exist or is directory");
        }
        return makeEntryForURL(requestedURL);
    }

    public boolean removeFileAtLocalURL(LocalFilesystemURL inputURL) throws InvalidModificationException {
        File fp = new File(filesystemPathForURL(inputURL));
        if (!fp.isDirectory() || fp.list().length <= 0) {
            return fp.delete();
        }
        throw new InvalidModificationException("You can't delete a directory that is not empty.");
    }

    public boolean exists(LocalFilesystemURL inputURL) {
        return new File(filesystemPathForURL(inputURL)).exists();
    }

    public boolean recursiveRemoveFileAtLocalURL(LocalFilesystemURL inputURL) throws FileExistsException {
        return removeDirRecursively(new File(filesystemPathForURL(inputURL)));
    }

    protected boolean removeDirRecursively(File directory) throws FileExistsException {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                removeDirRecursively(file);
            }
        }
        if (directory.delete()) {
            return true;
        }
        throw new FileExistsException("could not delete: " + directory.getName());
    }

    public LocalFilesystemURL[] listChildren(LocalFilesystemURL inputURL) throws FileNotFoundException {
        File fp = new File(filesystemPathForURL(inputURL));
        if (fp.exists()) {
            File[] files = fp.listFiles();
            if (files == null) {
                return null;
            }
            LocalFilesystemURL[] entries = new LocalFilesystemURL[files.length];
            for (int i = 0; i < files.length; i++) {
                entries[i] = URLforFilesystemPath(files[i].getPath());
            }
            return entries;
        }
        throw new FileNotFoundException();
    }

    public JSONObject getFileMetadataForLocalURL(LocalFilesystemURL inputURL) throws FileNotFoundException {
        File file = new File(filesystemPathForURL(inputURL));
        if (file.exists()) {
            JSONObject metadata = new JSONObject();
            try {
                metadata.put("size", file.isDirectory() ? 0 : file.length());
                metadata.put("type", this.resourceApi.getMimeType(Uri.fromFile(file)));
                metadata.put("name", file.getName());
                metadata.put("fullPath", inputURL.path);
                metadata.put("lastModifiedDate", file.lastModified());
                return metadata;
            } catch (JSONException e) {
                return null;
            }
        }
        throw new FileNotFoundException("File at " + inputURL.uri + " does not exist.");
    }

    private void copyFile(Filesystem srcFs, LocalFilesystemURL srcURL, File destFile, boolean move) throws IOException, InvalidModificationException, NoModificationAllowedException {
        if (move) {
            String realSrcPath = srcFs.filesystemPathForURL(srcURL);
            if (realSrcPath != null && new File(realSrcPath).renameTo(destFile)) {
                return;
            }
        }
        copyResource(this.resourceApi.openForRead(srcFs.toNativeUri(srcURL)), new FileOutputStream(destFile));
        if (move) {
            srcFs.removeFileAtLocalURL(srcURL);
        }
    }

    private void copyDirectory(Filesystem srcFs, LocalFilesystemURL srcURL, File dstDir, boolean move) throws IOException, NoModificationAllowedException, InvalidModificationException, FileExistsException {
        if (move) {
            String realSrcPath = srcFs.filesystemPathForURL(srcURL);
            if (realSrcPath != null) {
                File srcDir = new File(realSrcPath);
                if (dstDir.exists()) {
                    if (dstDir.list().length > 0) {
                        throw new InvalidModificationException("directory is not empty");
                    }
                    dstDir.delete();
                }
                if (srcDir.renameTo(dstDir)) {
                    return;
                }
            }
        }
        if (dstDir.exists()) {
            if (dstDir.list().length > 0) {
                throw new InvalidModificationException("directory is not empty");
            }
        } else if (!dstDir.mkdir()) {
            throw new NoModificationAllowedException("Couldn't create the destination directory");
        }
        for (LocalFilesystemURL childLocalUrl : srcFs.listChildren(srcURL)) {
            File target = new File(dstDir, new File(childLocalUrl.path).getName());
            if (childLocalUrl.isDirectory) {
                copyDirectory(srcFs, childLocalUrl, target, false);
            } else {
                copyFile(srcFs, childLocalUrl, target, false);
            }
        }
        if (move) {
            srcFs.recursiveRemoveFileAtLocalURL(srcURL);
        }
    }

    public JSONObject copyFileToURL(LocalFilesystemURL destURL, String newName, Filesystem srcFs, LocalFilesystemURL srcURL, boolean move) throws IOException, InvalidModificationException, JSONException, NoModificationAllowedException, FileExistsException {
        if (new File(filesystemPathForURL(destURL)).exists()) {
            LocalFilesystemURL destinationURL = makeDestinationURL(newName, srcURL, destURL, srcURL.isDirectory);
            Uri dstNativeUri = toNativeUri(destinationURL);
            Uri srcNativeUri = srcFs.toNativeUri(srcURL);
            if (dstNativeUri.equals(srcNativeUri)) {
                throw new InvalidModificationException("Can't copy onto itself");
            } else if (!move || srcFs.canRemoveFileAtLocalURL(srcURL)) {
                File destFile = new File(dstNativeUri.getPath());
                if (destFile.exists()) {
                    if (!srcURL.isDirectory && destFile.isDirectory()) {
                        throw new InvalidModificationException("Can't copy/move a file to an existing directory");
                    } else if (srcURL.isDirectory && destFile.isFile()) {
                        throw new InvalidModificationException("Can't copy/move a directory to an existing file");
                    }
                }
                if (!srcURL.isDirectory) {
                    copyFile(srcFs, srcURL, destFile, move);
                } else if (dstNativeUri.toString().startsWith(srcNativeUri.toString() + '/')) {
                    throw new InvalidModificationException("Can't copy directory into itself");
                } else {
                    copyDirectory(srcFs, srcURL, destFile, move);
                }
                return makeEntryForURL(destinationURL);
            } else {
                throw new InvalidModificationException("Source URL is read-only (cannot move)");
            }
        }
        throw new FileNotFoundException("The source does not exist");
    }

    public long writeToFileAtURL(LocalFilesystemURL inputURL, String data, int offset, boolean isBinary) throws IOException, NoModificationAllowedException {
        byte[] rawData;
        boolean append = false;
        if (offset > 0) {
            truncateFileAtURL(inputURL, (long) offset);
            append = true;
        }
        if (isBinary) {
            rawData = Base64.decode(data, 0);
        } else {
            rawData = data.getBytes();
        }
        ByteArrayInputStream in = new ByteArrayInputStream(rawData);
        FileOutputStream out;
        try {
            byte[] buff = new byte[rawData.length];
            String absolutePath = filesystemPathForURL(inputURL);
            out = new FileOutputStream(absolutePath, append);
            in.read(buff, 0, buff.length);
            out.write(buff, 0, rawData.length);
            out.flush();
            out.close();
            if (isPublicDirectory(absolutePath)) {
                broadcastNewFile(Uri.fromFile(new File(absolutePath)));
            }
            return (long) rawData.length;
        } catch (NullPointerException e) {
            throw new NoModificationAllowedException(inputURL.toString());
        } catch (Throwable th) {
            out.close();
        }
    }

    private boolean isPublicDirectory(String absolutePath) {
        if (VERSION.SDK_INT >= 21) {
            for (File f : this.context.getExternalMediaDirs()) {
                if (f != null && absolutePath.startsWith(f.getAbsolutePath())) {
                    return true;
                }
            }
        }
        return absolutePath.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    private void broadcastNewFile(Uri nativeUri) {
        this.context.sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", nativeUri));
    }

    public long truncateFileAtURL(LocalFilesystemURL inputURL, long size) throws IOException {
        if (new File(filesystemPathForURL(inputURL)).exists()) {
            RandomAccessFile raf = new RandomAccessFile(filesystemPathForURL(inputURL), "rw");
            try {
                if (raf.length() >= size) {
                    raf.getChannel().truncate(size);
                } else {
                    size = raf.length();
                    raf.close();
                }
                return size;
            } finally {
                raf.close();
            }
        } else {
            throw new FileNotFoundException("File at " + inputURL.uri + " does not exist.");
        }
    }

    public boolean canRemoveFileAtLocalURL(LocalFilesystemURL inputURL) {
        return new File(filesystemPathForURL(inputURL)).exists();
    }
}
