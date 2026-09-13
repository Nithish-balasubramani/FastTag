package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ExecutiveEntity
import com.example.data.model.InventoryTagEntity
import com.example.data.model.ProofDocumentEntity
import com.example.data.model.StockMovementLedgerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExecutiveEntity::class,
        InventoryTagEntity::class,
        ProofDocumentEntity::class,
        StockMovementLedgerEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun executiveDao(): ExecutiveDao
    abstract fun inventoryTagDao(): InventoryTagDao
    abstract fun proofDocumentDao(): ProofDocumentDao
    abstract fun stockMovementLedgerDao(): StockMovementLedgerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fastag_inventory.db"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialExecutives(database.executiveDao())
                    }
                }
            }
        }

        suspend fun populateInitialExecutives(dao: ExecutiveDao) {
            val count = dao.countExecutives()
            if (count == 0) {
                val initialExecs = listOf(
                    ExecutiveEntity(executiveId = "EXEC-01", executiveName = "Kumar S.", region = "North Corridor", active = true),
                    ExecutiveEntity(executiveId = "EXEC-02", executiveName = "Ravi Sharma", region = "South Hub", active = true),
                    ExecutiveEntity(executiveId = "EXEC-03", executiveName = "Suresh Patel", region = "West Tollways", active = true),
                    ExecutiveEntity(executiveId = "EXEC-04", executiveName = "Anil Verma", region = "East Expressway", active = true),
                    ExecutiveEntity(executiveId = "EXEC-05", executiveName = "Pooja Reddy", region = "Metro Ring Road", active = true),
                    ExecutiveEntity(executiveId = "EXEC-06", executiveName = "Deepak Joshi", region = "Port Logistics", active = true),
                    ExecutiveEntity(executiveId = "EXEC-07", executiveName = "Manoj Nair", region = "Central Hub", active = true),
                    ExecutiveEntity(executiveId = "EXEC-08", executiveName = "Kavita Rao", region = "Airport Corridor", active = true),
                    ExecutiveEntity(executiveId = "EXEC-09", executiveName = "Vikram Singh", region = "Highway NH-48", active = true)
                )
                dao.insertAll(initialExecs)
            }
        }
    }
}
