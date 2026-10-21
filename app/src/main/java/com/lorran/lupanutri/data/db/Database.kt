package com.lorran.lupanutri.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
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

@Database(entities = [ProductEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun products(): ProductDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "lupa.db").build()
    }
}
