package com.project.maru.community

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CommunityDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_FILE, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }


    fun insertPost(post: CommunityPost): Long {
        val values = ContentValues()
        values.put("category", post.category)
        values.put("title", post.title)
        values.put("content", post.content)
        values.put("owner_id", post.ownerId)
        values.put("author_name", post.authorName)
        values.put("created_at", post.createdAt)
        val db = writableDatabase
        return db.insert(TABLE_NAME, null, values)
    }


    fun getAllPosts(): List<CommunityPost> {
        val db = readableDatabase
        val cursor = db.rawQuery(SQL_SELECT_ALL, null)
        val list = mutableListOf<CommunityPost>()
        try {
            while (cursor.moveToNext()) {
                list.add(readPost(cursor))
            }
        } finally {
            cursor.close()
        }
        return list
    }

    fun getPostById(id: String): CommunityPost? {
        val db = readableDatabase

        val cursor = db.rawQuery(SQL_SELECT_BY_ID, arrayOf(id))
        try {
            if (cursor.moveToNext()) {
                return readPost(cursor)
            }
            return null
        } finally {
            cursor.close()
        }
    }

    private fun readPost(cursor: Cursor): CommunityPost {
        return CommunityPost(
            id = cursor.getLong(0).toString(),
            category = cursor.getString(1),
            title = cursor.getString(2),
            content = cursor.getString(3),
            ownerId = cursor.getString(4),
            authorName = cursor.getString(5),
            createdAt = cursor.getLong(6)
        )
    }

    companion object {
        const val DB_FILE = "maru_community_local.db"
        const val DB_VERSION = 1
        private const val TABLE_NAME = "community_posts"
        private const val SQL_CREATE_TABLE = """
            CREATE TABLE community_posts (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                owner_id TEXT NOT NULL,
                author_name TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """
        private const val SQL_SELECT_ALL =
            "SELECT _id, category, title, content, owner_id, author_name, created_at " +
            "FROM community_posts ORDER BY created_at DESC, _id DESC"
        private const val SQL_SELECT_BY_ID =
            "SELECT _id, category, title, content, owner_id, author_name, created_at " +
            "FROM community_posts WHERE _id = ?"
    }
}
