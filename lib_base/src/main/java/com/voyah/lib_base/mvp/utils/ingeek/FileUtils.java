package com.voyah.lib_base.mvp.utils.ingeek;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.lang.reflect.Method;

/**
 * Created by lxx  on 2019/5/14
 */
public class FileUtils {
    private static final String TAG = "FileUtils";

    /**
     * Get file uri
     *
     * @param context context
     * @param file    file
     * @return Uri
     */
    static Uri getFileUri(Context context, File file) {
        if (context == null) {
            Log.e(TAG, "getFileUri current activity is null.");
            return null;
        }
        if (file == null || !file.exists()) {
            Log.e(TAG, "getFileUri file is null or not exists.");
            return null;
        }
        Uri uri;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            uri = Uri.fromFile(file);
        } else {
            uri = getFileContentUri(context, file);
        }
        if (uri == null) {
            uri = forceGetFileUri(file);
        }
        return uri;
    }


    /**
     * getFileContentUri
     * @param context context
     * @param file file
     * @return Uri
     */
    private static Uri getFileContentUri(Context context, File file) {
        String volumeName = "external";
        String filePath = file.getAbsolutePath();
        String[] projection = new String[]{MediaStore.Files.FileColumns._ID};
        Uri uri = null;

        Cursor cursor = context.getContentResolver().query(MediaStore.Files.getContentUri(volumeName), projection,
                MediaStore.Images.Media.DATA + "=? ", new String[] { filePath }, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
                uri = MediaStore.Files.getContentUri(volumeName, id);
            }
            cursor.close();
        }

        return uri;
    }


    /**
     * forceGetFileUri
     * @param shareFile shareFile
     * @return Uri
     */
    private static Uri forceGetFileUri(File shareFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                @SuppressLint("PrivateApi")
                Method rMethod = StrictMode.class.getDeclaredMethod("disableDeathOnFileUriExposure");
                rMethod.invoke(null);
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }

        return Uri.parse("file://" + shareFile.getAbsolutePath());
    }

}
