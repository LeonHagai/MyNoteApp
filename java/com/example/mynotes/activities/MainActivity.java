package com.example.mynotes.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.mynotes.R;
import com.example.mynotes.entities.Note;

import java.util.ArrayList;
import java.util.List;

import adapters.NotesAdapter;
import database.NotesDatabase;
import listener.NotesListener;

import static com.example.mynotes.activities.CreateNoteActivity.REQ_CODE_SELECT_IMAGE;


public class MainActivity extends AppCompatActivity implements NotesListener {

	public static final int REQUEST_CODE_ADD_NOTE = 1;
	public static final int REQUEST_CODE_UPDATE_NOTE = 2;
	public static final int REQUEST_CODE_SHOW_NOTES = 3;
	public static final int REQUEST_CODE_SELECT_IMAGE = 4;
	public static final int REQUEST_CODE_STORAGE_PERMISSION = 5;

	private RecyclerView notesRecyclerView;
	private List<Note> noteList;
	private NotesAdapter notesAdapter;

	private int noteClickedPosition = -1;

	private AlertDialog dialogAddURL;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ImageView imageAddNoteMain = findViewById(R.id.imageAddNote);
		imageAddNoteMain.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(
						new Intent(getApplicationContext(), CreateNoteActivity.class),
						REQUEST_CODE_ADD_NOTE
				);
			}
		});

		notesRecyclerView = findViewById(R.id.notesRecyclerView);
		notesRecyclerView.setLayoutManager(
				// arranging the elements on the screen
				new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
		);

		noteList = new ArrayList<>();
		notesAdapter = new NotesAdapter(noteList, this);
		notesRecyclerView.setAdapter(notesAdapter);

		//is called from onCreate() to display all notes from the db
		//state is false since it fetches from all db
		getNotes(REQUEST_CODE_SHOW_NOTES, false);

		EditText inputSearch = findViewById(R.id.inputSearch);
		inputSearch.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				notesAdapter.cancelTimer();
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (noteList.size() != 0){
					notesAdapter.searchNotes(s.toString());
				}
			}
		});

		findViewById(R.id.imageAddNote).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(
						new Intent(getApplicationContext(), CreateNoteActivity.class),
						REQUEST_CODE_ADD_NOTE
				);
			}
		});

		findViewById(R.id.imageAddImage).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(ContextCompat.checkSelfPermission(
						getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
				) != PackageManager.PERMISSION_GRANTED){
					ActivityCompat.requestPermissions(
							MainActivity.this,
							new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
							REQUEST_CODE_STORAGE_PERMISSION
					);
				} else {
					selectedImage();
				}
			}
		});

		findViewById(R.id.imageAddWebLink).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showAddURLDialog();
			}
		});

	}
	private void selectedImage(){
		//redircting to select
		Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		if(intent.resolveActivity(getPackageManager()) != null){
			startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
		}
	}

	//quick action image
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0){
			if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
				selectedImage();
			} else {
				Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private String getPathFromUri(Uri contentUri){
		String filePath;
		Cursor cursor=getContentResolver()
				.query(contentUri, null, null, null, null);
		if(cursor == null){
			filePath = contentUri.getPath();
		}else{
			cursor.moveToFirst();
			int index = cursor.getColumnIndex( "_data");
			filePath = cursor.getString(index);
			cursor.close();
		}
		return filePath;
	}

	@Override
	public void onNoteClicked(Note note, int position) {
		noteClickedPosition= position;
		Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
		intent.putExtra("isViewOrUpdate", true);
		intent.putExtra("note", note);
		startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
	}

	//getting the request code as a method parameter
	private void getNotes(final int requestCode, final boolean isNoteDeleted) {
		@SuppressLint("StaticFieldLeak")
		// implements async task since does allow the execution on the main thread
		class	GetNotesTask extends AsyncTask<Void, Void, List<Note>>{

			@Override
			protected List<Note> doInBackground(Void... voids) {
				return NotesDatabase
						.getDatabase(getApplicationContext())
						.noteDao().getAllNotes();
			}

			@Override
			protected void onPostExecute(List<Note> notes) {
				super.onPostExecute(notes);

				if(requestCode == REQUEST_CODE_SHOW_NOTES){
					noteList.addAll(notes);
					notesAdapter.notifyDataSetChanged();
				} else if (requestCode == REQUEST_CODE_ADD_NOTE){
					noteList.add(0, notes.get(0));
					notesAdapter.notifyDataSetChanged();
					notesRecyclerView.smoothScrollToPosition(0);
				} else if (requestCode == REQUEST_CODE_UPDATE_NOTE){
					noteList.remove(noteClickedPosition);
					if(isNoteDeleted){
						notesAdapter.notifyItemRemoved(noteClickedPosition);
					}else{
						noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
						notesAdapter.notifyItemChanged(noteClickedPosition);
					}
				}
			}
		}
		new GetNotesTask().execute();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK){
			//is called from onActivityResult(), check if the request code is for add note
			// and RESULT_OK i.e. ew note is added from CreateNote activity
			// state fasle since note is added not deleted
			getNotes(REQUEST_CODE_ADD_NOTE, false);
		} else if(requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK){
			if(data != null){
				//passing value from CreateNoteActivity, whether the note is deleted or
				// not using intent data with key ''isNoteDeleted
				getNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
			}
		} else if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
			if(data != null){
				Uri selectedImageUri = data.getData();

				if(selectedImageUri != null){
					try{
						String selectedImagePath = getPathFromUri(selectedImageUri);
						Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
						intent.putExtra("isFromQuickActions", true);
						intent.putExtra("quickActionType", "image");
						intent.putExtra("imagePath", selectedImagePath);
						startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);


					}catch (Exception exception){
						Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
					}
				}
			}
		}
	}
	private void showAddURLDialog(){
		if(dialogAddURL == null){
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			View view = LayoutInflater.from(this).inflate(
					R.layout.layout_add_url,
					(ViewGroup) findViewById(R.id.layoutAddUrlContainer)
			);
			builder.setView(view);

			dialogAddURL = builder.create();
			if(dialogAddURL.getWindow() != null){
				dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
			}

			final EditText inputURL = view.findViewById(R.id.inputURL);
			inputURL.requestFocus();

			view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(inputURL.getText().toString().trim().isEmpty()){
						Toast.makeText(MainActivity.this, "Enter URL",
								Toast.LENGTH_SHORT).show();
					}else  if(!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()){
						Toast.makeText(MainActivity.this, "Enter valid URL",
								Toast.LENGTH_SHORT).show();
					}else {
						dialogAddURL.dismiss();
						Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
						intent.putExtra("isFromQuickActions", true);
						intent.putExtra("quickActionType", "URL");
						intent.putExtra("URL", inputURL.getText().toString());
						startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
					}
				}
			});

			view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialogAddURL.dismiss();
				}
			});
		}
		dialogAddURL.show();
	}
}