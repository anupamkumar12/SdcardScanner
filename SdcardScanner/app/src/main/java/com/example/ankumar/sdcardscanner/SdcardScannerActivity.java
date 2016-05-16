package com.example.ankumar.sdcardscanner;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class SdcardScannerActivity extends Activity implements View.OnClickListener{
	private static final String TAG = "SdcardScannerActivity";
	private static final int PERMISSIONS_CODE = 1;
	private static final int MSG_START_SCAN = 2;
	private static final int MSG_SCAN_CANCELLED = 3;
	private static final int MSG_UPDATE_AVERAGE = 4;
	private static final int MSG_UPDATE_LARGEST_FILES_LIST = 5;
	private static final int MSG_UPDATE_EXTENSION_FREQ_LIST = 6;

	private static final int NOTIFICATION_ID = 100;
	NotificationCompat.Builder mBuilder;
	NotificationManager mNotifyMgr;

	private static final String ACTION = "action";
	private static final String FILE_LIST ="file";
	private static final String EXTENSION_LIST ="extension";
	private static final String AVERAGE ="average";

	private Button startScanButton;
	private Button stopScanButton;
	private ProgressBar progressBar;
	private TextView averageFileSizeView;

	long averageFileSize;
	ArrayList<FileInfo> largestFilesList;
	ArrayList<FileTypeFrequency> fileExtensionList;

	private ListView listView1;
	private ListView listView2;
	private ListAdapter listAdapter1;
	private ListAdapter listAdapter2;

	private TextView headerTextView1;
	private TextView headerTextView2;

	private ShareActionProvider mShareActionProvider;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(SdcardScannerService.AVERAGE_FILE_SIZE_RESULT.equals(action)) {
				averageFileSize = intent.getLongExtra(AVERAGE, 0);
				mHandler.sendEmptyMessage(MSG_UPDATE_AVERAGE);
			} else if (SdcardScannerService.LARGEST_FILES_LIST_RESULT.equals(action)) {
				largestFilesList = (ArrayList<FileInfo>) intent.getSerializableExtra(FILE_LIST);
				mHandler.sendEmptyMessage(MSG_UPDATE_LARGEST_FILES_LIST);
			} else if(SdcardScannerService.FILES_EXTENSION_FREQ_LIST_RESULT.equals(action)) {
				fileExtensionList = (ArrayList<FileTypeFrequency>) intent.getSerializableExtra(EXTENSION_LIST);
				mHandler.sendEmptyMessage(MSG_UPDATE_EXTENSION_FREQ_LIST);
			} else {
				return;
			}
			cancelNotification();
			setViewsVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
			startScanButton.setEnabled(true);

			Intent shareIntent = createShareIntent();
			if (shareIntent != null && mShareActionProvider != null) {
				mShareActionProvider.setShareIntent(shareIntent);
			}
		}
	};

	private Handler mHandler = new Handler(Looper.getMainLooper()) {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case MSG_START_SCAN:
					clearAdapters();
					progressBar.setVisibility(View.VISIBLE);
					startScanButton.setEnabled(false);
					setViewsVisibility(View.GONE);
					sendNotification();
					break;

				case MSG_SCAN_CANCELLED:
					progressBar.setVisibility(View.GONE);
					startScanButton.setEnabled(true);
					cancelNotification();
					break;

				case MSG_UPDATE_AVERAGE:
					averageFileSizeView.setText(getResources().getString(R.string.average_file_size) + averageFileSize);
					break;

				case MSG_UPDATE_LARGEST_FILES_LIST:
					listAdapter1.clear();
					listAdapter1.addAll(largestFilesList);
					break;

				case MSG_UPDATE_EXTENSION_FREQ_LIST:
					listAdapter2.clear();
					listAdapter2.addAll(fileExtensionList);
					break;

			}
		}
	};

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(FILE_LIST,largestFilesList);
		outState.putSerializable(EXTENSION_LIST,fileExtensionList);
		outState.putString(AVERAGE, averageFileSizeView.getText().toString());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if(savedInstanceState != null) {
			largestFilesList = (ArrayList<FileInfo>) savedInstanceState.get(FILE_LIST);
			fileExtensionList = (ArrayList<FileTypeFrequency>) savedInstanceState.get(EXTENSION_LIST);

			if(!largestFilesList.isEmpty() && !fileExtensionList.isEmpty()) {
				averageFileSizeView.setText((String) savedInstanceState.get(AVERAGE));

				listAdapter1 = new ListAdapter(this, R.layout.rowlayout, R.id.filelistView);
				listView1.setAdapter(listAdapter1);
				listAdapter1.addAll(largestFilesList);

				listAdapter2 = new ListAdapter(this, R.layout.rowlayout, R.id.extensionsListView);
				listView2.setAdapter(listAdapter2);
				listAdapter2.addAll(fileExtensionList);

				setViewsVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sdcard_scan);

		startScanButton = (Button) findViewById(R.id.start_scan);
		startScanButton.setOnClickListener(this);
		stopScanButton = (Button) findViewById(R.id.stop_scan);
		stopScanButton.setOnClickListener(this);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		progressBar.setIndeterminate(true);
		progressBar.setProgress(ProgressDialog.STYLE_SPINNER);
		averageFileSizeView = (TextView) findViewById(R.id.averageFileSize);
		headerTextView1 = (TextView) findViewById(R.id.header1);
		headerTextView2 = (TextView) findViewById(R.id.header2);

		listView1 = (ListView) findViewById(R.id.filelistView);
		listAdapter1 = new ListAdapter(this, R.layout.rowlayout, R.id.filelistView);

		listView2 = (ListView) findViewById(R.id.extensionsListView);
		listAdapter2 = new ListAdapter(this, R.layout.rowlayout, R.id.extensionsListView);
	}

	@Override
	protected void onStart() {
		super.onStart();

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(SdcardScannerService.AVERAGE_FILE_SIZE_RESULT);
		intentFilter.addAction(SdcardScannerService.LARGEST_FILES_LIST_RESULT);
		intentFilter.addAction(SdcardScannerService.FILES_EXTENSION_FREQ_LIST_RESULT);
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver,intentFilter);
		listView1.setAdapter(listAdapter1);
		listView2.setAdapter(listAdapter2);
	}

	@Override
	protected void onStop() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onBackPressed()
	{
		super.onBackPressed();  // optional depending on your needs
		Intent stopIntent = new Intent(this, SdcardScannerService.class);
		stopIntent.putExtra(ACTION, SdcardScannerService.SCAN_CANCELLED);
		startService(stopIntent);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}


	@Override
	protected void onPause() {
		super.onPause();
		setViewsVisibility(View.GONE);
		progressBar.setVisibility(View.GONE);
		startScanButton.setEnabled(true);
		cancelNotification();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.start_scan:
				mHandler.sendEmptyMessage(MSG_START_SCAN);
				if(!hasPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)) {
					askForPermission();
					return;
				}
				Intent startIntent = new Intent(this, SdcardScannerService.class);
				startIntent.putExtra(ACTION, SdcardScannerService.START_SCAN);
				startService(startIntent);
				break;

			case R.id.stop_scan:
				mHandler.sendEmptyMessage(MSG_SCAN_CANCELLED);
				Intent stopIntent = new Intent(this, SdcardScannerService.class);
				stopIntent.putExtra(ACTION, SdcardScannerService.SCAN_CANCELLED);
				startService(stopIntent);
				break;
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if(requestCode == PERMISSIONS_CODE){
			if(hasPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)){
				Intent intent = new Intent(this, SdcardScannerService.class);
				intent.putExtra(ACTION, SdcardScannerService.START_SCAN);
				startService(intent);
			}
		}

	}

	private void askForPermission() {
		String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
		ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_CODE);
	}

	private boolean hasPermissions(String permission){
		return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
	}

	private void setViewsVisibility(int visibility) {
		listView1.setVisibility(visibility);
		listView2.setVisibility(visibility);
		headerTextView1.setVisibility(visibility);
		headerTextView2.setVisibility(visibility);
		averageFileSizeView.setVisibility(visibility);
	}

	private void clearAdapters() {
		listAdapter1.clear();
		listAdapter2.clear();
	}

	private void sendNotification() {
		Intent resultIntent = new Intent(this, SdcardScannerActivity.class);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(
				this,
				0,
				resultIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder = new NotificationCompat.Builder(this)
						.setContentTitle(getResources().getString(R.string.scan_in_progress))
						.setSmallIcon(R.drawable.search)
						.setContentIntent(resultPendingIntent);
		mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotifyMgr.notify(NOTIFICATION_ID, mBuilder.build());
	}

	private void cancelNotification() {
		mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotifyMgr.cancel(NOTIFICATION_ID);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_main_actions, menu);
		MenuItem item = menu.findItem(R.id.menu_item_share);


		mShareActionProvider = (ShareActionProvider) item.getActionProvider();
		return true;
	}

	private Intent createShareIntent() {
		if(largestFilesList != null && !largestFilesList.isEmpty() &&
				fileExtensionList != null && !fileExtensionList.isEmpty() &&
				averageFileSizeView.getText()!=null) {

			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND_MULTIPLE);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, averageFileSizeView.getText());

			HashMap<String,Long> fileMap = new HashMap<>();
			for(FileInfo file : largestFilesList) {
				fileMap.put(file.getPath(),file.getSize());
			}
			intent.putExtra(FILE_LIST,fileMap);

			HashMap<String,Long> extensionMap = new HashMap<>();
			for(FileTypeFrequency file : fileExtensionList) {
				extensionMap.put(file.fileExtension, file.frequency);
			}
			intent.putExtra(EXTENSION_LIST, extensionMap);
			return intent;
		}
		return null;
	}
}
