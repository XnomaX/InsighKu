package com.example.insightku.core.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.insightku.core.data.model.Category

@Dao
interface CategoryDao {
    @Query("SELECT COUNT(*) FROM categories WHERE isActive = 1")
    suspend fun countActiveCategories(): Int

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE isActive = 1 AND categoryType = :type ORDER BY name ASC")
    fun getCategoriesByType(type: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE isActive = 1 AND categoryType = :type AND isSystemCategory = 0 ORDER BY name ASC")
    fun getUserCategoriesByType(type: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): Category?

    @Query("SELECT * FROM categories WHERE name = :name")
    suspend fun getCategoryByName(name: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategoriesFromRemote(categories: List<Category>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :categoryId AND isSystemCategory = 0")
    suspend fun deleteCategory(categoryId: String)

    @Query("UPDATE categories SET isActive = 0 WHERE id = :categoryId AND isSystemCategory = 0")
    suspend fun deactivateCategory(categoryId: String)

    @Query("DELETE FROM categories WHERE isSystemCategory = 0")
    suspend fun deleteAllUserCategories()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}


