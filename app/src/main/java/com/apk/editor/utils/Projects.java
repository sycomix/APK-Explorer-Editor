package com.apk.editor.utils;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.apk.editor.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on March 04, 2021
 */
public class Projects {

    public static List<String> getData(Context context) {
        List<String> mData = new ArrayList<>();
        for (File mFile : Objects.requireNonNull(new File(context.getCacheDir().toString()).listFiles())) {
            if (mFile.exists() && mFile.isDirectory() && new File(mFile, "AndroidManifest.xml").exists()) {
                if (Common.getSearchWord() == null) {
                    mData.add(mFile.getAbsolutePath());
                } else if (Common.isTextMatched(mFile.getName(), Common.getSearchWord())) {
                    mData.add(mFile.getAbsolutePath());
                }
            }
        }
        Collections.sort(mData);
        if (!APKEditorUtils.getBoolean("az_order", true, context)) {
            Collections.reverse(mData);
        }
        return mData;
    }

    public static AsyncTasks exportToStorage(String source, String name, String folder, Context context) {
        return new AsyncTasks() {
            private String mExportPath;
            @Override
            public void onPreExecute() {
            }

            @Override
            public void doInBackground() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    APKEditorUtils.mkdir(getExportPath(context) + "/" + folder);
                    mExportPath = getExportPath(context) + "/" + Common.getAppID();
                } else {
                    mExportPath = getExportPath(context);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                        values.put(MediaStore.MediaColumns.MIME_TYPE, "*/*");
                        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                        Uri uri = context.getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                        OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                        outputStream.write(Objects.requireNonNull(APKEditorUtils.read(source)).getBytes());
                        outputStream.close();
                    } catch (IOException ignored) {
                    }
                } else {
                    APKEditorUtils.copy(source, new File(mExportPath, name).getAbsolutePath());
                }
            }

            @Override
            public void onPostExecute() {
                new MaterialAlertDialogBuilder(context)
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle(R.string.app_name)
                        .setMessage(context.getString(R.string.export_complete_message, mExportPath))
                        .setPositiveButton(context.getString(R.string.cancel), (dialog1, id1) -> {
                        }).show();
            }
        };
    }

    public static String getExportPath(Context context) {
        if (Build.VERSION.SDK_INT < 29 && APKEditorUtils.getString("exportPath", null, context) != null) {
            return APKEditorUtils.getString("exportPath", null, context);
        } else {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        }
    }

    public static void exportProject(File path, String name, Context context) {
        new AsyncTasks() {
            private ProgressDialog mProgressDialog;

            @Override
            public void onPreExecute() {
                mProgressDialog = new ProgressDialog(context);
                mProgressDialog.setMessage(context.getString(R.string.exporting, path.getName()));
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                if (APKEditorUtils.exist(getExportPath(context) + "/" + name)) {
                    APKEditorUtils.delete(getExportPath(context) + "/" + name);
                }
            }

            @Override
            public void doInBackground() {
                APKEditorUtils.copyDir(path, new File(getExportPath(context), name));
            }

            @Override
            public void onPostExecute() {
                try {
                    mProgressDialog.dismiss();
                } catch (IllegalArgumentException ignored) {
                }
            }
        }.execute();
    }

    public static void deleteProject(File path, Context context) {
        new AsyncTasks() {
            private ProgressDialog mProgressDialog;

            @Override
            public void onPreExecute() {
                mProgressDialog = new ProgressDialog(context);
                mProgressDialog.setMessage(context.getString(R.string.deleting, path.getName()));
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
            }

            @Override
            public void doInBackground() {
                APKEditorUtils.delete(path.getAbsolutePath());
            }

            @Override
            public void onPostExecute() {
                try {
                    mProgressDialog.dismiss();
                } catch (IllegalArgumentException ignored) {
                }
                Common.isReloading(true);
            }
        }.execute();
    }

}