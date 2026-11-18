package com.xh.hotme.storage.filetransfer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


// ----------------------- model
public class TaskDBOpenHelper extends SQLiteOpenHelper {
    public final static String DATABASE_NAME = "hotme_v2.db";
    public final static int DATABASE_VERSION = 1;

    public TaskDBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS "
            + TaskDBController.TABLE_NAME
            + String.format(
            "("
                + "%s INTEGER PRIMARY KEY, " // id, download id
                + "%s VARCHAR, " // size
                + "%s INTEGER, " // duration
                + "%s INTEGER, " // type
                + "%s VARCHAR, " // url
                + "%s VARCHAR, " // path
                + "%s VARCHAR, " // videoId
                + "%s VARCHAR, " // videoName
                + "%s VARCHAR, " // thumb url
                + "%s VARCHAR," // competitionId
                + "%s VARCHAR," // competitionName
                + "%s VARCHAR," // domainId
                + "%s VARCHAR," // device_id
                + "%s INTEGER," // width
                + "%s INTEGER," // height
                + "%s VARCHAR, " // key
                + "%s TINYINT(1) DEFAULT 0 " // upload
                + ")"
            , TaskModel.ID
            , TaskModel.SIZE
            , TaskModel.DURATION
            , TaskModel.TYPE
            , TaskModel.URL
            , TaskModel.PATH
            , TaskModel.VIDEO_ID
            , TaskModel.VIDEO_NAME
            , TaskModel.THUMB_URL
            , TaskModel.COMPETITION_ID
            , TaskModel.COMPETITION_NAME
            , TaskModel.DOMAIN_ID
            , TaskModel.DEVICE_ID
            , TaskModel.WIDTH
            , TaskModel.HEIGHT
            , TaskModel.KEY
            , TaskModel.UPLOAD

        ));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.delete(TaskDBController.TABLE_NAME, null, null);
    }
}