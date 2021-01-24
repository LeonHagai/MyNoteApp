package com.example.mynotes.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.mynotes.entities.Note;

import java.util.List;

@Dao
public interface NoteDao {

	@Query("SELECT * FROM notes ORDER BY id DESC")
	List<Note> getAllNotes();

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	void insertNotes(Note note);

	@Delete
	void deleteNotes(Note note);

}
