package com.example.mynotes.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mynotes.R;
import com.example.mynotes.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import database.NotesDatabase;

public class CreateNoteActivity extends AppCompatActivity {
	private EditText inputNoteTitle, inputNoteSubtitle, inputNoteText;
	private TextView textDateTime, textWebURL;
	private View viewSubtitleIndicator;
	private ImageView imageNote;
	private LinearLayout layoutWebURL;

	private String selectedNoteColor;
	private String selectedImagePath;

	public static final int REQ_CODE_STORAGE_PERMISSION = 1;
	public static final int REQ_CODE_SELECT_IMAGE = 2;

	private AlertDialog dialogAddURL;
	private AlertDialog dialogDeleteNote;

	private Note alreadyAvailableNote;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_note);

		inputNoteTitle = findViewById(R.id.inputNoteTitle);
		inputNoteSubtitle = findViewById(R.id.inputNoteSubtitle);
		inputNoteText = findViewById(R.id.inputNoteText);
		textDateTime = findViewById(R.id.textDateTime);
		viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator);
		imageNote = findViewById(R.id.imageNote);
		textWebURL = findViewById(R.id.textWebURL);
		layoutWebURL = findViewById(R.id.layoutWebURL);

		textDateTime.setText(
				new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault())
				.format(new Date())
		);

		ImageView imageBack = findViewById(R.id.imageBack);
		imageBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		ImageView imageSave = findViewById(R.id.imageSave);
		imageSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveNote();
			}
		});

		//default UI color
		selectedNoteColor = "#333333";
		selectedImagePath = "";

		if(getIntent().getBooleanExtra("isViewOrUpdate", false)){
			alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
			setViewOrUpdateNote();
		}

		findViewById(R.id.imageRemoveWebURL).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				imageNote.setImageBitmap(null);
				imageNote.setVisibility(View.GONE);
			}
		});
		findViewById(R.id.imageRemoveImage).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				imageNote.setImageBitmap(null);
				imageNote.setVisibility(View.GONE);
				findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
				selectedImagePath = "";
			}
		});

		if(getIntent().getBooleanExtra("isFromQuickActions", false)){
			String type = getIntent().getStringExtra("quickActionType");
			if(type !=null){
				if(type.equals("image")){
					selectedImagePath = getIntent().getStringExtra("imagePath");
					imageNote.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath));
					imageNote.setVisibility(View.VISIBLE);
					findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
				} else if(type.equals("URL")){
					textWebURL.setText(getIntent().getStringExtra("URL"));
					layoutWebURL.setVisibility(View.VISIBLE);
				}
			}
		}

		initMiscellaneous();
		setSubtitleIndicatorColor();
	}

	private void setViewOrUpdateNote(){
		inputNoteTitle.setText(alreadyAvailableNote.getTitle());
		inputNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
		inputNoteText.setText(alreadyAvailableNote.getNoteText());
		textDateTime.setText(alreadyAvailableNote.getDateTime());

		if (alreadyAvailableNote.getImagePath() != null &&
		!alreadyAvailableNote.getImagePath().trim().isEmpty()){
			imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
			imageNote.setVisibility(View.VISIBLE);
			findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
			selectedImagePath = alreadyAvailableNote.getImagePath();
		}

		if (alreadyAvailableNote.getWebLink() != null &&
		!alreadyAvailableNote.getWebLink().trim().isEmpty()){
			textWebURL.setText(alreadyAvailableNote.getWebLink());
			layoutWebURL.setVisibility(View.VISIBLE);
		}

	}


	private void saveNote(){
		if(inputNoteTitle.getText().toString().trim().isEmpty()){
			Toast.makeText(this, "Note title is required", Toast.LENGTH_SHORT).show();
			return;
		}
		else if(inputNoteSubtitle.getText().toString().trim().isEmpty()
				&& inputNoteText.getText().toString().trim().isEmpty()){
			Toast.makeText(this, "Note cannot be empty", Toast.LENGTH_SHORT).show();
			return;
		}

		final Note note = new Note();
		note.setTitle(inputNoteTitle.getText().toString());
		note.setSubtitle(inputNoteSubtitle.getText().toString());
		note.setNoteText(inputNoteText.getText().toString());
		note.setDateTime(textDateTime.getText().toString());
		note.setColor(selectedNoteColor);
		note.setImagePath(selectedImagePath);

		if(layoutWebURL.getVisibility() == View.VISIBLE){
			note.setWebLink(textWebURL.getText().toString());
		}

		//setting id of new note from an already available note, we have set
		// onConflictStrategy to REPLACE in NoteDao ie if id of new note is already
		// available in the db then it will be replaced with the new note and
		// our note get updated
		// =@Insert(onConflict = OnConflictStrategy.REPLACE)
		// = void insertNote(Note note);
		if(alreadyAvailableNote != null){
			note.setId(alreadyAvailableNote.getId());
		}


		@SuppressLint("StaticFieldLeak")
		class SaveNoteTask extends AsyncTask<Void, Void, Void> {

			@Override
			protected Void doInBackground(Void... voids) {
				NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNotes(note);
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				super.onPostExecute(aVoid);
				Intent intent = new Intent();
				setResult(RESULT_OK, intent);
				finish();
			}
		}
		//NOT THERE, DOES NOT WORK. call onto the function to execute
		new SaveNoteTask().execute();
	}

	private void initMiscellaneous(){
		final LinearLayout layoutMiscellaneous = findViewById(R.id.layoutMiscellinious);
		final BottomSheetBehavior<LinearLayout> bottomSheetBehavior =BottomSheetBehavior.from(layoutMiscellaneous);
		layoutMiscellaneous.findViewById(R.id.textMiscellaneous)
				.setOnClickListener(new View.OnClickListener(){
					@Override
					public void onClick(View v) {
						if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED){
							bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
						} else {
							bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
						}

					}
				});

		final ImageView imageColor1 = layoutMiscellaneous.findViewById(R.id.imageColor1);
		final ImageView imageColor2 = layoutMiscellaneous.findViewById(R.id.imageColor2);
		final ImageView imageColor3 = layoutMiscellaneous.findViewById(R.id.imageColor3);
		final ImageView imageColor4 = layoutMiscellaneous.findViewById(R.id.imageColor4);
		final ImageView imageColor5 = layoutMiscellaneous.findViewById(R.id.imageColor5);

		layoutMiscellaneous.findViewById(R.id.viewColor1).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				selectedNoteColor = "#333333";
				imageColor1.setImageResource(R.drawable.ic_done);
				imageColor2.setImageResource(0);
				imageColor3.setImageResource(0);
				imageColor4.setImageResource(0);
				imageColor5.setImageResource(0);
				setSubtitleIndicatorColor();
			}
		});

		layoutMiscellaneous.findViewById(R.id.viewColor2).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				selectedNoteColor = "#FDBE3B";
				imageColor1.setImageResource(0);
				imageColor2.setImageResource(R.drawable.ic_done);
				imageColor3.setImageResource(0);
				imageColor4.setImageResource(0);
				imageColor5.setImageResource(0);
				setSubtitleIndicatorColor();
			}
		});

		layoutMiscellaneous.findViewById(R.id.viewColor3).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				selectedNoteColor = "#FF4B42";
				imageColor1.setImageResource(0);
				imageColor2.setImageResource(0);
				imageColor3.setImageResource(R.drawable.ic_done);
				imageColor4.setImageResource(0);
				imageColor5.setImageResource(0);
				setSubtitleIndicatorColor();
			}
		});

		layoutMiscellaneous.findViewById(R.id.viewColor4).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				selectedNoteColor = "#3A52FC";
				imageColor1.setImageResource(0);
				imageColor2.setImageResource(0);
				imageColor3.setImageResource(0);
				imageColor4.setImageResource(R.drawable.ic_done);
				imageColor5.setImageResource(0);
				setSubtitleIndicatorColor();
			}
		});

		layoutMiscellaneous.findViewById(R.id.viewColor5).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				selectedNoteColor = "#000000";
				imageColor1.setImageResource(0);
				imageColor2.setImageResource(0);
				imageColor3.setImageResource(0);
				imageColor4.setImageResource(0);
				imageColor5.setImageResource(R.drawable.ic_done);
				setSubtitleIndicatorColor();
			}
		});

		if(alreadyAvailableNote != null && alreadyAvailableNote.getColor() != null
		&& !alreadyAvailableNote.getColor().trim().isEmpty()){
			switch(alreadyAvailableNote.getColor()){
				case "#FDBE3B":
					layoutMiscellaneous.findViewById(R.id.viewColor2).performClick();
					break;
				case "#FF4B42":
	 				layoutMiscellaneous.findViewById(R.id.viewColor3).performClick();
					break;
				case "#3A52FC":
					layoutMiscellaneous.findViewById(R.id.viewColor4).performClick();
					break;
				case "#000000":
					layoutMiscellaneous.findViewById(R.id.viewColor5).performClick();
					break;
			}
		}

		layoutMiscellaneous.findViewById(R.id.layoutAddImage).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
						if(ContextCompat.checkSelfPermission(
								getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
						) != PackageManager.PERMISSION_GRANTED){
							ActivityCompat.requestPermissions(
									CreateNoteActivity.this,
									new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
									REQ_CODE_STORAGE_PERMISSION
							);
						} else {
							selectedImage();
						}
					}
				}
		);

		layoutMiscellaneous.findViewById(R.id.layoutURL).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
				showAddURLDialog();
			}
		});

		//check if alreadyAvailableNote is not null ie user is viewing
		// or updating already added note from the db thus display
		// delete option
		if( alreadyAvailableNote != null){
			layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
			layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
					showDeleteNoteDialog();
				}
			});
		}
	}

	private void showDeleteNoteDialog(){
		if(dialogDeleteNote == null){
			AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
			View view = LayoutInflater.from(this).inflate(
					R.layout.layout_delete_note,
					(ViewGroup) findViewById(R.id.layoutDeleteNoteContainer)
			);
			builder.setView(view);
			dialogDeleteNote = builder.create();
			if(dialogDeleteNote.getWindow() != null) {
				dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
			}
			view.findViewById(R.id.textDeleteNote).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {

					@SuppressLint("StaticFieldLeak")
					class DeleteNoteTask extends AsyncTask<Void, Void, Void> {

						@Override
						protected Void doInBackground(Void... voids) {
							NotesDatabase.getDatabase(getApplicationContext()).noteDao()
									.deleteNotes(alreadyAvailableNote);
							return null;
						}

						@Override
						protected void onPostExecute(Void aVoid) {
							super.onPostExecute(aVoid);
							Intent intent = new Intent();
							intent.putExtra("isNoteDeleted", true);
							setResult(RESULT_OK, intent);
							finish();
						}
					}
					new DeleteNoteTask().execute();
				}
			});

			view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialogDeleteNote.dismiss();
				}
			});

		}

		dialogDeleteNote.show();
	}
	
	private void setSubtitleIndicatorColor(){
		GradientDrawable gradientDrawable = (GradientDrawable) viewSubtitleIndicator.getBackground();
		gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
	}

	private void selectedImage(){
		//redircting to select
		Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		if(intent.resolveActivity(getPackageManager()) != null){
			startActivityForResult(intent, REQ_CODE_SELECT_IMAGE);
		}
	}

//	handling permission result
	public void onRequestPermissionsResult(int requestCode, @NonNull String [] permissions, @NonNull int[] grantResult){
		super.onRequestPermissionsResult(requestCode, permissions, grantResult);
		if(requestCode == REQ_CODE_STORAGE_PERMISSION && grantResult.length > 0){
			if(grantResult[0] == PackageManager.PERMISSION_GRANTED){
				selectedImage();
			} else {
				Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
			}
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data){
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQ_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
			if(data != null){
				Uri selectedImageUri = data.getData();
				if(selectedImageUri != null){
					try{
						InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
						Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
						imageNote.setImageBitmap(bitmap);
						imageNote.setVisibility(View.VISIBLE);
						findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);

						selectedImagePath = getPathFromUri(selectedImageUri);
					}catch(Exception exception){
						Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
					}
				}
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

	private void showAddURLDialog(){
		if(dialogAddURL == null){
			AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
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
						Toast.makeText(CreateNoteActivity.this, "Enter URL",
								Toast.LENGTH_SHORT).show();
					}else  if(!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()){
						Toast.makeText(CreateNoteActivity.this, "Enter valid URL",
								Toast.LENGTH_SHORT).show();
					}else {
						textWebURL.setText(inputURL.getText().toString());
						layoutWebURL.setVisibility(View.VISIBLE);
						dialogAddURL.dismiss();
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