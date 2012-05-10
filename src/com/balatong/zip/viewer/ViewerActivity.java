package com.balatong.zip.viewer;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

import com.balatong.BaseActivity;
import com.balatong.logger.Logger;
import com.balatong.zip.R;
import com.balatong.zip.io.ContentsExtractor;
import com.balatong.zip.io.FileReader;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ViewerActivity extends BaseActivity {

	private static Logger logger = Logger.getLogger(ViewerActivity.class
			.getName());

	private AdView adView;

	private File file;
	private FileReader reader;
	private ContentsAdapter zipContentsAdapter;

	private ProgressBar activityBar;
	private TextView statusBar;
	private ListView directoryContents;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.zip_viewer);

		initAdView();

		activityBar = (ProgressBar) findViewById(R.id.pbar_status_activity);
		statusBar = (TextView) findViewById(R.id.txt_status_message);
		directoryContents = (ListView) findViewById(R.id.list_directory_contents);
		zipContentsAdapter = new ContentsAdapter(this, directoryContents);

		readZipFile(getIntent());

		initializeMenus();
	}

	private void initAdView() {
		AdView adView = (AdView) this.findViewById(R.id.adView);
		adView.loadAd(new AdRequest());
	}

	private void readZipFile(Intent intent) {
		if (intent.getData() == null)
			return;

		file = new File(intent.getData().getPath());
		if (!file.isFile()) {
			logger.debug("Path " + file.getAbsolutePath()
					+ " is not a valid file.");
			statusBar.setText(getString(R.string.err_not_valid_zip_file,
					file.getAbsolutePath()));
			return;
		}
		reader = createReader();
		reader.execute(file);
		toggleMenus(true);
	}

	private void toggleMenus(Boolean enabled) {
		ImageButton menuUnzip = (ImageButton) findViewById(R.id.img_btn_menu_unzip);
		menuUnzip.setEnabled(enabled);
		ImageButton menuCheck = (ImageButton) findViewById(R.id.img_btn_menu_check);
		menuCheck.setEnabled(enabled);
		ImageButton menuAdd = (ImageButton) findViewById(R.id.img_btn_menu_add);
		menuAdd.setEnabled(enabled);
		ImageButton menuDelete = (ImageButton) findViewById(R.id.img_btn_menu_delete);
		menuDelete.setEnabled(enabled);
		ImageButton menuInfo = (ImageButton) findViewById(R.id.img_btn_menu_info);
		menuInfo.setEnabled(enabled);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		switch (requestCode) {
		case 0:
			if (resultCode == RESULT_OK)
				readZipFile(intent);
			break;
		}
	}

	private FileReader createReader() {
		FileReader reader = new FileReader() {
			protected void onPreExecute() {
				super.onPreExecute();
				activityBar.setIndeterminate(false);
				activityBar.setVisibility(ProgressBar.VISIBLE);
			}

			protected void onProgressUpdate(Integer... progress) {
				super.onProgressUpdate(progress);
				statusBar
						.setText(getString(R.string.reading_file, progress[0]));
			}

			protected void onPostExecute(Map<String, Object> result) {
				super.onPostExecute(result);
				activityBar.setVisibility(ProgressBar.INVISIBLE);
				statusBar.setText("");

				zipContentsAdapter.setSource(result);
				directoryContents.setAdapter(zipContentsAdapter);
			}
		};
		return reader;
	}

	private void initializeMenus() {
		ImageButton menuOpen = (ImageButton) findViewById(R.id.img_btn_menu_open);
		menuOpen.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("*/zip");
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				try {
					startActivityForResult(Intent.createChooser(intent, null),
							0);
				} catch (android.content.ActivityNotFoundException ex) {
					Toast.makeText(
							getParent(),
							getResources().getText(
									R.string.info_install_file_manager),
							Toast.LENGTH_SHORT).show();
				}
			}
		});

		ImageButton menuUnzip = (ImageButton) findViewById(R.id.img_btn_menu_unzip);
		menuUnzip.setEnabled(false);
		menuUnzip.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LayoutInflater inflater = getLayoutInflater();
				final View viewExtractPath = inflater.inflate(
						R.layout.extract_path, null);

				AlertDialog.Builder builder = new AlertDialog.Builder(v
						.getContext());
				builder.setTitle(v.getContext().getText(
						R.string.extract_to_directory));
				builder.setView(viewExtractPath);
				builder.setPositiveButton(android.R.string.ok,
						new AlertDialog.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								final String extractPath = ((EditText) viewExtractPath
										.findViewById(R.id.txt_extract_path))
										.getText().toString();
								final Map<String, Object> zipEntries = zipContentsAdapter
										.getCheckedItems();
								ContentsExtractor extractor = new ContentsExtractor() {
									protected void onPreExecute() {
										super.onPreExecute();
										activityBar.setIndeterminate(true);
										activityBar
												.setVisibility(ProgressBar.VISIBLE);
										statusBar
												.setText(getResources()
														.getString(
																R.string.extracting_files));
										ImageButton menuUnzip = (ImageButton) findViewById(R.id.img_btn_menu_open);
										menuUnzip.setEnabled(false);
										toggleMenus(false);
									}

									protected void onPostExecute(Integer result) {
										super.onPostExecute(result);
										activityBar
												.setVisibility(ProgressBar.INVISIBLE);
										statusBar
												.setText(getResources()
														.getString(
																R.string.extracted_num_files,
																result));
										zipContentsAdapter.uncheckItems();
										ImageButton menuUnzip = (ImageButton) findViewById(R.id.img_btn_menu_open);
										menuUnzip.setEnabled(true);
										toggleMenus(true);
									}

									protected void onProgressUpdate(
											String... values) {
										super.onProgressUpdate(values);
										statusBar
												.setText(getResources()
														.getString(
																R.string.extracting_file,
																values[0]));

									}
								};
								extractor.setFile(file);
								extractor.execute(zipEntries, extractPath);
							}
						});
				builder.setNeutralButton(android.R.string.cancel, null);
				builder.create().show();
			}
		});

		ImageButton menuCheck = (ImageButton) findViewById(R.id.img_btn_menu_check);
		menuCheck.setEnabled(false);
		menuCheck.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(v
						.getContext());
				builder.setTitle(v.getContext().getText(
						R.string.warn_not_yet_implemented));
				builder.setMessage(v.getContext().getText(
						R.string.info_available_in_paid_version));
				builder.setPositiveButton(android.R.string.ok, null);
				builder.create().show();
				// reader.checkFile();
			}
		});

		ImageButton menuAdd = (ImageButton) findViewById(R.id.img_btn_menu_add);
		menuAdd.setEnabled(false);
		menuAdd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(v
						.getContext());
				builder.setTitle(v.getContext().getText(
						R.string.warn_not_yet_implemented));
				builder.setMessage(v.getContext().getText(
						R.string.info_available_in_paid_version));
				builder.setPositiveButton(android.R.string.ok, null);
				builder.create().show();
				// reader.addFile();
			}
		});

		ImageButton menuDelete = (ImageButton) findViewById(R.id.img_btn_menu_delete);
		menuDelete.setEnabled(false);
		menuDelete.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(v
						.getContext());
				builder.setTitle(v.getContext().getText(
						R.string.warn_not_yet_implemented));
				builder.setMessage(v.getContext().getText(
						R.string.info_available_in_paid_version));
				builder.setPositiveButton(android.R.string.ok, null);
				builder.create().show();
				// reader.deleteFile();
			}
		});

		ImageButton menuInfo = (ImageButton) findViewById(R.id.img_btn_menu_info);
		menuInfo.setEnabled(false);
		menuInfo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(v
						.getContext());
				builder.setTitle(v.getContext().getText(
						R.string.warn_not_yet_implemented));
				builder.setMessage(v.getContext().getText(
						R.string.info_available_in_paid_version));
				builder.setPositiveButton(android.R.string.ok, null);
				builder.create().show();
				// reader.showFileInfo();
			}
		});
	}

	@Override
	public void onStop() {
		super.onStop();
		if (reader != null)
			reader.closeFile();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (reader != null)
			reader.closeFile();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (adView != null) {
			adView.destroy();
		}
		if (reader != null)
			reader.closeFile();
	}

}