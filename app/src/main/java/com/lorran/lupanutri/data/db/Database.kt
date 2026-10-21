package com.lorran.lupanutri.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Insert
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val code: String,
    val json: String,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val isManual: Boolean = false,
    val isFavorite: Boolean = false,
    val lastViewedAt: Long? = null,
    val favoritedAt: Long? = null,
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE code = :code")
    suspend fun get(code: String): ProductEntity?

    @Query("SELECT * FROM products WHERE code = :code")
    fun observe(code: String): Flow<ProductEntity?>

    @Upsert
    suspend fun upsert(entity: ProductEntity)

    @Query("SELECT * FROM products WHERE lastViewedAt IS NOT NULL ORDER BY lastViewedAt DESC LIMIT 300")
    fun observeHistory(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isFavorite = 1 ORDER BY favoritedAt DESC")
    fun observeFavorites(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :q || '%' OR brand LIKE '%' || :q || '%' LIMIT 20")
    suspend fun searchLocal(q: String): List<ProductEntity>

    @Query("UPDATE products SET isFavorite = :fav, favoritedAt = :at WHERE code = :code")
    suspend fun setFavorite(code: String, fav: Boolean, at: Long?)

    @Query("UPDATE products SET lastViewedAt = NULL WHERE code = :code")
    suspend fun removeFromHistory(code: String)

    @Query("UPDATE products SET lastViewedAt = NULL")
    suspend fun clearHistory()

    @Query("DELETE FROM products WHERE code = :code")
    suspend fun delete(code: String)
}

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val code: String?,
    val checked: Boolean = false,
    val createdAt: Long,
)

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY checked ASC, createdAt DESC")
    fun observeAll(): Flow<List<ShoppingItem>>

    @Insert
    suspend fun insert(item: ShoppingItem): Long

    @Query("SELECT COUNT(*) FROM shopping_items WHERE code = :code AND checked = 0")
    suspend fun countOpen(code: String): Int

    @Query("SELECT COUNT(*) FROM shopping_items WHERE code = :code AND checked = 0")
    fun observeOpen(code: String): Flow<Int>

    @Query("UPDATE shopping_items SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM shopping_items WHERE code = :code AND checked = 0")
    suspend fun deleteOpenByCode(code: String)

    @Query("DELETE FROM shopping_items WHERE checked = 1")
    suspend fun clearChecked()
}

@Database(entities = [ProductEntity::class, ShoppingItem::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun products(): ProductDao
    abstract fun shopping(): ShoppingDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shopping_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`text` TEXT NOT NULL, `code` TEXT, " +
                        "`checked` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "lupa.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
