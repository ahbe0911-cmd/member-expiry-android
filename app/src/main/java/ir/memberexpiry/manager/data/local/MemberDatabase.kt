package ir.memberexpiry.manager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MemberEntity::class, ActivityLogEntity::class], version = 1, exportSchema = true)
abstract class MemberDatabase : RoomDatabase() {
    abstract fun members(): MemberDao
    abstract fun activities(): ActivityLogDao

    companion object {
        @Volatile private var instance: MemberDatabase? = null
        fun getInstance(context: Context): MemberDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, MemberDatabase::class.java, "members.db")
                .build().also { instance = it }
        }
    }
}
