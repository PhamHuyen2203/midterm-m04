package com.example.dals;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class DatabaseHelper {

    public static final String DATABASE_NAME =
            "MCommerce.sqlite";

    /*
     * Khi thay đổi file MCommerce.sqlite trong assets,
     * tăng phiên bản này lên 2, 3, 4...
     */
    private static final int DATABASE_VERSION = 1;

    private static final int BUFFER_SIZE = 8192;

    private static final String PREFERENCE_NAME =
            "mcommerce_database_preferences";

    private static final String KEY_DATABASE_VERSION =
            "installed_database_version";

    private DatabaseHelper() {
        // Không cho phép khởi tạo đối tượng.
    }

    /**
     * Kiểm tra và cài đặt database từ assets.
     */
    public static synchronized void prepareDatabase(
            Context context
    ) throws IOException {

        Context applicationContext =
                context.getApplicationContext();

        File databaseFile =
                applicationContext.getDatabasePath(
                        DATABASE_NAME
                );

        SharedPreferences preferences =
                applicationContext.getSharedPreferences(
                        PREFERENCE_NAME,
                        Context.MODE_PRIVATE
                );

        int installedVersion =
                preferences.getInt(
                        KEY_DATABASE_VERSION,
                        0
                );

        boolean databaseDoesNotExist =
                !databaseFile.exists();

        boolean databaseIsEmpty =
                databaseFile.exists()
                        && databaseFile.length() == 0;

        boolean databaseNeedsUpgrade =
                installedVersion < DATABASE_VERSION;

        if (databaseDoesNotExist
                || databaseIsEmpty
                || databaseNeedsUpgrade) {

            copyDatabaseFromAssets(
                    applicationContext,
                    databaseFile
            );

            preferences.edit()
                    .putInt(
                            KEY_DATABASE_VERSION,
                            DATABASE_VERSION
                    )
                    .apply();
        }
    }

    /**
     * Sao chép file database từ assets vào bộ nhớ ứng dụng.
     */
    private static void copyDatabaseFromAssets(
            Context context,
            File databaseFile
    ) throws IOException {

        File databaseDirectory =
                databaseFile.getParentFile();

        if (databaseDirectory != null
                && !databaseDirectory.exists()) {

            boolean created =
                    databaseDirectory.mkdirs();

            if (!created
                    && !databaseDirectory.exists()) {

                throw new IOException(
                        "Unable to create database directory."
                );
            }
        }

        File temporaryFile =
                new File(
                        databaseFile.getAbsolutePath()
                                + ".tmp"
                );

        copyAssetToFile(
                context,
                temporaryFile
        );

        deleteFileIfExists(
                new File(
                        databaseFile.getAbsolutePath()
                                + "-wal"
                )
        );

        deleteFileIfExists(
                new File(
                        databaseFile.getAbsolutePath()
                                + "-shm"
                )
        );

        deleteFileIfExists(databaseFile);

        boolean renamed =
                temporaryFile.renameTo(databaseFile);

        if (!renamed) {

            deleteFileIfExists(temporaryFile);

            throw new IOException(
                    "Unable to install database file."
            );
        }
    }

    /**
     * Đọc file trong assets và ghi ra file đích.
     */
    private static void copyAssetToFile(
            Context context,
            File destinationFile
    ) throws IOException {

        try (
                InputStream inputStream =
                        context.getAssets()
                                .open(DATABASE_NAME);

                OutputStream outputStream =
                        new FileOutputStream(
                                destinationFile
                        )
        ) {

            byte[] buffer =
                    new byte[BUFFER_SIZE];

            int numberOfBytesRead;

            while (
                    (numberOfBytesRead =
                            inputStream.read(buffer)) > 0
            ) {

                outputStream.write(
                        buffer,
                        0,
                        numberOfBytesRead
                );
            }

            outputStream.flush();
        }
    }

    /**
     * Xóa file nếu file tồn tại.
     */
    private static void deleteFileIfExists(
            File file
    ) throws IOException {

        if (file.exists() && !file.delete()) {

            throw new IOException(
                    "Unable to delete file: "
                            + file.getAbsolutePath()
            );
        }
    }

    /**
     * Mở database ở chế độ đọc và ghi.
     */
    public static SQLiteDatabase openDatabase(
            Context context
    ) throws IOException {

        prepareDatabase(context);

        File databaseFile =
                context.getApplicationContext()
                        .getDatabasePath(
                                DATABASE_NAME
                        );

        if (!databaseFile.exists()) {

            throw new IOException(
                    "Database file does not exist."
            );
        }

        SQLiteDatabase database =
                SQLiteDatabase.openDatabase(
                        databaseFile.getAbsolutePath(),
                        null,
                        SQLiteDatabase.OPEN_READWRITE
                );

        database.execSQL(
                "PRAGMA foreign_keys = ON"
        );

        return database;
    }

    /**
     * Lấy đường dẫn database runtime.
     */
    public static File getRuntimeDatabaseFile(
            Context context
    ) {

        return context.getApplicationContext()
                .getDatabasePath(
                        DATABASE_NAME
                );
    }
}