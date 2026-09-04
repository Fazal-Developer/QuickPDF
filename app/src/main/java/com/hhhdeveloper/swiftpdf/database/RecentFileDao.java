package com.hhhdeveloper.swiftpdf.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.hhhdeveloper.swiftpdf.models.RecentFile;

import java.util.List;

@Dao
public interface RecentFileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RecentFile recentFile);

    @Delete
    void delete(RecentFile recentFile);

    @Query("DELETE FROM recent_files")
    void deleteAll();

    @Query("DELETE FROM recent_files WHERE id = :id")
    void deleteById(int id);

    @Update
    void update(RecentFile recentFile);

    @Query("SELECT * FROM recent_files ORDER BY dateCreated DESC")
    LiveData<List<RecentFile>> getAllFiles();

    @Query("SELECT * FROM recent_files ORDER BY dateCreated DESC LIMIT :limit")
    LiveData<List<RecentFile>> getRecentFiles(int limit);

    @Query("SELECT COUNT(*) FROM recent_files")
    int getCount();

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM recent_files")
    long getTotalSizeBytes();
}
