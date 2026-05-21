package com.example.insightku.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.insightku.data.model.Category

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): Category?

    @Query("SELECT * FROM categories WHERE name = :name")
    suspend fun getCategoryByName(name: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    // IGNORE strategy: never restore a category that was deleted locally.
    // Mirrors the same pattern used for transactions (insertTransactionsFromRemote).
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategoriesFromRemote(categories: List<Category>)

    // Keep REPLACE for explicit local inserts (add/update from UI)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: String)

    @Query("UPDATE categories SET isActive = 0 WHERE id = :categoryId")
    suspend fun deactivateCategory(categoryId: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}
