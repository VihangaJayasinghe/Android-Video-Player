package com.example.videosystem

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Video(val id: Int, val name: String, val source: String)

class VideoDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "video_system.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_VIDEOS = "videos"
        const val COLUMN_ID = "video_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_SOURCE = "source"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE " + TABLE_VIDEOS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                 + COLUMN_SOURCE + " TEXT" + ")")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_VIDEOS")
        onCreate(db)
    }

    fun addVideo(name: String, source: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_NAME, name)
        values.put(COLUMN_SOURCE, source)
        val id = db.insert(TABLE_VIDEOS, null, values)
        db.close()
        return id
    }

    fun getAllVideos(): List<Video> {
        val videoList = ArrayList<Video>()
        val selectQuery = "SELECT * FROM $TABLE_VIDEOS"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                val source = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOURCE))
                videoList.add(Video(id, name, source))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return videoList
    }
}