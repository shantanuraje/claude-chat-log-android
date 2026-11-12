package com.claude.historyviewer.data.database

import androidx.room.TypeConverter
import kotlinx.datetime.Instant

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long): Instant = Instant.fromEpochMilliseconds(value)

    @TypeConverter
    fun dateToTimestamp(instant: Instant): Long = instant.toEpochMilliseconds()
}