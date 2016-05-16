package com.example.ankumar.sdcardscanner;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SdcardScannerService extends Service {

	private static final String TAG = "SdcardScannerService";
	public static final int START_SCAN = 2;
	public static final int SCAN_COMPLETED = 3;
	public static final int SCAN_CANCELLED = 4;

	private static final int NO_OF_FILE_EXTENSIONS = 5;
	private static final int NO_OF_FILES = 10;

	private static final String ACTION = "action";
	public static final String AVERAGE_FILE_SIZE_RESULT = "com.example.ankumar.sdcardscanner.averageFileSize";
	public static final String LARGEST_FILES_LIST_RESULT = "com.example.ankumar.sdcardscanner.largestFilesList";
	public static final String FILES_EXTENSION_FREQ_LIST_RESULT = "com.example.ankumar.sdcardscanner.extensionFreqList";

	private static final String FILE_LIST ="file";
	private static final String EXTENSION_LIST ="extension";
	private static final String AVERAGE ="average";

	IBinder mBinder;
	boolean mAllowRebind;

	private MyTask sdcardScanTask;

	LocalBroadcastManager broadcaster;

	private ArrayList<FileInfo> list = new ArrayList<>();
	ArrayList<FileInfo> largestFilesList;
	ArrayList<FileTypeFrequency> fileExtensionList;
	long averageFileSize;

	@Override
	public void onCreate() {
		super.onCreate();
		broadcaster = LocalBroadcastManager.getInstance(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int extra = intent.getIntExtra(ACTION,0);
		switch (extra) {
			case START_SCAN:
				sdcardScanTask = new MyTask();
				sdcardScanTask.execute();
				break;

			case SCAN_CANCELLED:
				if(sdcardScanTask != null && sdcardScanTask.getStatus() == AsyncTask.Status.RUNNING) {
					sdcardScanTask.cancel(true);
				}
				stopSelf();
				break;
		}
		return START_NOT_STICKY;
	}

	public SdcardScannerService() {
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return mAllowRebind;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private class MyTask extends AsyncTask {
		@Override
		protected Object doInBackground(Object[] params) {
			startExternalStorageScan();
			return null;
		}

		@Override
		protected void onPostExecute(Object o) {
			super.onPostExecute(o);
			Intent intent = new Intent(AVERAGE_FILE_SIZE_RESULT);
			intent.putExtra(AVERAGE, averageFileSize);
			broadcaster.sendBroadcast(intent);

			intent = new Intent(LARGEST_FILES_LIST_RESULT);
			intent.putExtra(FILE_LIST, largestFilesList);
			broadcaster.sendBroadcast(intent);

			intent = new Intent(FILES_EXTENSION_FREQ_LIST_RESULT);
			intent.putExtra(EXTENSION_LIST, fileExtensionList);
			broadcaster.sendBroadcast(intent);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}
	}


	private void startExternalStorageScan() {
		list.clear();
		scan(Environment.getExternalStorageDirectory());
		populateStats();
	}

	public void scan(File dir) {

		File[] listFile = dir.listFiles();
		boolean directoryRead = dir.canRead();
		if (directoryRead && listFile != null) {
			for (int i = 0; i < listFile.length; i++) {

				if (listFile[i].isDirectory()) {
					scan(listFile[i]);
				} else {
					String extension = null;
					String fileName = listFile[i].getName();
					long size = listFile[i].length();
					int index = fileName.lastIndexOf('.');
					if (index > 0) {
						extension = fileName.substring(index+1);
					}
					if(fileName != null && extension != null) {
						FileInfo file = new FileInfo(fileName, size, extension);
						list.add(file);
						//Log.d(TAG,"fileInfo " + file.getPath() + " size " + file.getSize());
					}
				}
			}
		}
	}

	private long getAverageFileSize() {
		if(!list.isEmpty()) {
			long totalSize = 0;
			for(FileInfo file: list) {
				totalSize = totalSize + file.getSize();
			}
			return totalSize/list.size();
		}
		return 0;
	}

	private ArrayList<FileInfo> getLargestFiles(int noOfFiles) {
		ArrayList<FileInfo> largetstFilesList = new ArrayList<>(noOfFiles);

		Collections.sort(list, new Comparator<FileInfo>() {
			public int compare(FileInfo lhs, FileInfo rhs) {
				return ((Long)(rhs.getSize())).compareTo((Long)(lhs.getSize()));
			}
		});

		for(int i=0 ; i < noOfFiles; i++) {
			largetstFilesList.add(list.get(i));
		}
		return largetstFilesList;
	}

	private ArrayList<FileTypeFrequency> getMostCommonFileExtensions(int numberOfFileTypes) {
		HashMap<String, Long> map = new HashMap<>();

		for(FileInfo file : list){
			long value = map.get(file.getExtension()) == null ? 0 : map.get(file.getExtension());
			map.put(file.getExtension(), value+1);
		}

		List list = new LinkedList(map.entrySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object lhs, Object rhs) {
				return ((Comparable) ((Map.Entry) (rhs)).getValue()).compareTo(((Map.Entry) (lhs)).getValue());
			}
		});

		ArrayList<FileTypeFrequency> fileTypeFrequencyList = new ArrayList<>();
		int i=0;
		for (Iterator it = list.iterator(); it.hasNext() && i < numberOfFileTypes; i++) {
			Map.Entry entry = (Map.Entry) it.next();
			FileTypeFrequency temp = new FileTypeFrequency((String)entry.getKey(), (Long)entry.getValue());
			fileTypeFrequencyList.add(temp);
		}
		return fileTypeFrequencyList;

	}

	private void populateStats() {
		averageFileSize = getAverageFileSize();
		//Log.d(TAG,"averagefileSize " + averageFileSize);

		largestFilesList = getLargestFiles(NO_OF_FILES);
//		for(int k=0;k<largestFilesList.size();k++)
//			Log.d(TAG,"fileName " + largestFilesList.get(k).getPath() + " size " + largestFilesList.get(k).getSize());

		fileExtensionList = getMostCommonFileExtensions(NO_OF_FILE_EXTENSIONS);
//		for(FileTypeFrequency item : fileExtensionList) {
//			Log.e(TAG,"key " + item.getFileExtension() + " value " + item.getFrequency());
//		}
	}
}
