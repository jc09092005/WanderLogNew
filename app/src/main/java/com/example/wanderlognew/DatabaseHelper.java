package com.example.wanderlognew;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database constraints
    private static final String DATABASE_NAME = "wanderlog.db";
    private static final int DATABASE_VERSION = 3;

    // Table name for journal entries
    public static final String TABLE_JOURNALS = "journals";

    // Column names for the journals table
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_COUNTRY = "country";
    public static final String COLUMN_DATE_START = "date_start";
    public static final String COLUMN_DATE_END = "date_end";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_IMAGE_PATH = "image_path";
    public static final String COLUMN_USERNAME = "username"; // new

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Called when the database is created for the first time
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String createUsersTable = "CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT," +
                "email TEXT," +
                "password TEXT)";
        db.execSQL(createUsersTable);

        // Create the journal entries table
        String createTable = "CREATE TABLE " + TABLE_JOURNALS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_COUNTRY + " TEXT, " +
                COLUMN_DATE_START + " TEXT, " +
                COLUMN_DATE_END + " TEXT, " +
                COLUMN_DESCRIPTION + " TEXT, " +
                COLUMN_IMAGE_PATH + " TEXT, " +
                COLUMN_USERNAME + " TEXT)"; // linked user
        db.execSQL(createTable);
    }

    // Registers a new user by inserting their info into the users table
    public boolean registerUser(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("email", email);
        values.put("password", password);

        long result = db.insert("users", null, values);
        return result != -1; // returns true if insert succeeded
    }

    // Validates login credentials by checking if a user with matching username and password exists
    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM users WHERE username = ? AND password = ?",
                new String[]{username, password});

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Checks if a username is already taken during signup
    public boolean isUsernameTaken(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username = ?", new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Inserts a new journal entry in the journals table, linked to the user
    public void insertJournalEntry(JournalEntry entry, String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, entry.title);
        values.put(COLUMN_COUNTRY, entry.country);
        values.put(COLUMN_DATE_START, entry.dateStart);
        values.put(COLUMN_DATE_END, entry.dateEnd);
        values.put(COLUMN_DESCRIPTION, entry.description);
        values.put(COLUMN_IMAGE_PATH, entry.imageName);
        values.put(COLUMN_USERNAME, username);
        db.insert(TABLE_JOURNALS, null, values);
        db.close();
    }


    // Retrieves all journal entries for a specific user
    public List<JournalEntry> getEntriesForUser(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }

        List<JournalEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_JOURNALS,
                null,
                "username = ?",
                new String[]{username},
                null, null, null);

        // Convert each row to a JournalEntry object
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
            String country = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COUNTRY));
            String start = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_START));
            String end = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_END));
            String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));
            String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH));

            entries.add(new JournalEntry(id, title, country, start, end, description, imagePath));
        }

        cursor.close();
        return entries;
    }

    // Retrieves a single journal entry by its ID
    public JournalEntry getEntryById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_JOURNALS,
                null,
                COLUMN_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null);

        JournalEntry entry = null;
        if (cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
            String country = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COUNTRY));
            String dateStart = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_START));
            String dateEnd = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_END));
            String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));
            String imageName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH));

            entry = new JournalEntry(id, title, country, dateStart, dateEnd, description, imageName);
        }

        cursor.close();
        return entry;
    }

    // Deletes a journal entry byID, only if it belongs to the given username
    public void deleteEntryById(String username, int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete the entry only if it belongs to the given username.
        db.delete(
                TABLE_JOURNALS,
                COLUMN_ID + " = ? AND " + COLUMN_USERNAME + " = ?",
                new String[]{String.valueOf(id), username}
        );
        db.close();
    }


    // Deletes a user and all their associated journal entries
    public void deleteUser(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("users", "username = ?", new String[]{username});
        db.delete(TABLE_JOURNALS, COLUMN_USERNAME + " = ?", new String[]{username}); // delete user's journals
        db.close();
    }

    // Updates a user'r password
    public boolean updatePasswordForUser(String username, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password", newPassword);

        int rowsAffected = db.update("users", values, "username = ?", new String[]{username});
        db.close();
        return rowsAffected > 0;
    }

    // Called when the datbase version changes, drops old tables and recreates them
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_JOURNALS);
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }
}
