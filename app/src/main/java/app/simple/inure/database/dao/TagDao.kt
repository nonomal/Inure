package app.simple.inure.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.simple.inure.models.Tag

@Dao
interface TagDao {

    /**
     * Get all tags
     */
    @Query("SELECT * FROM tags ORDER BY tag COLLATE nocase")
    suspend fun getTags(): MutableList<Tag>

    /**
     * Get all tags but name only where
     * package is not empty
     */
    @Query("SELECT DISTINCT tag FROM tags ORDER BY tag COLLATE nocase")
    suspend fun getTagsNameOnly(): MutableList<String>

    /**
     * Get tag where [Tag.tag] is equal to [tag]
     */
    @Query("SELECT * FROM tags WHERE tag = :tag")
    fun getTag(tag: String): Tag

    /**
     * Get a set of all package names under Trackers tag
     * where [Tag.tag] is equal to "Trackers"
     */
    @Query("SELECT packages FROM tags WHERE tag = 'Trackers'")
    suspend fun getTrackers(): String

    /**
     * Get all tags where [Tag.packages] contains [packageName]
     */
    @Query("SELECT tag FROM tags WHERE packages LIKE '%' || :packageName || '%'")
    suspend fun getTagsByPackage(packageName: String): MutableList<String>

    /**
     * Delete a [Tag] item
     * from the table
     */
    @Delete
    suspend fun deleteTag(tag: Tag)

    /**
     * Insert a new [Tag] item
     * into the table
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag)

    /**
     * Update a [Tag] item
     * from the table
     */
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTag(tag: Tag)

    /**
     * Delete the entire table
     */
    @Query("DELETE FROM tags")
    suspend fun nukeTable()
}
