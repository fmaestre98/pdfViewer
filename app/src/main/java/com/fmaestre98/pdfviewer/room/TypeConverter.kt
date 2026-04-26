package com.fmaestre98.pdfviewer.room

import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.Size
import androidx.room.TypeConverter
import com.google.gson.Gson

object TypeConverter {
    private val gson = Gson()


    @TypeConverter
    fun fromPointF(point: PointF?): String? {
        return if (point == null) null else gson.toJson(point)
    }

    @TypeConverter
    fun toPointF(json: String?): PointF? {
        return if (json.isNullOrEmpty()) null else gson.fromJson(json, PointF::class.java)
    }

    @TypeConverter
    fun fromSize(size: Size?): String? {
        return if (size == null) null else gson.toJson(size)
    }

    @TypeConverter
    fun toSize(json: String?): Size? {
        return if (json.isNullOrEmpty()) null else gson.fromJson(json, Size::class.java)
    }

    @TypeConverter
    fun fromRectF(size: RectF?): String? {
        return if (size == null) null else gson.toJson(size)
    }

    @TypeConverter
    fun toRectF(json: String?): RectF? {
        return if (json.isNullOrEmpty()) null else gson.fromJson(json, RectF::class.java)
    }
}