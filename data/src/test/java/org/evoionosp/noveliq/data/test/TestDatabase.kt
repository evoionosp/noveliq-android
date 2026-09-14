package org.evoionosp.noveliq.data.test

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.evoionosp.noveliq.data.library.local.db.NoveliqDatabase

object TestDatabase {
    fun create(): NoveliqDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room
            .inMemoryDatabaseBuilder(context, NoveliqDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }
}
