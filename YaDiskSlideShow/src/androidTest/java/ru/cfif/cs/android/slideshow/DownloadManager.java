package ru.cfif.cs.android.slideshow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.util.Log;
import com.yandex.disk.client.*;
import com.yandex.disk.client.exceptions.WebdavException;

public class DownloadManager {
	private static final String TAG = "LoadFileFragment";

	private ArrayList<ListItem> imageItemList;
	private ArrayList<File> images;
	private AtomicInteger lastLoaded;
	private volatile int startIndex;
	private volatile boolean stop = false;

	public DownloadManager(ArrayList<ListItem> imageItemList) {
		this.imageItemList = imageItemList;
		this.images = new ArrayList<>(imageItemList.size());
	}

	public void loadFiles(final int startIndex, final Credentials credentials, final Context context) {
		this.startIndex = startIndex;
		new Thread(new Runnable() {
			@Override
			public void run () {
				TransportClient client = null;
				try {
					client = TransportClient.getInstance(context, credentials);
					int curIndex = startIndex;
					for (int i = 0; i < imageItemList.size(); curIndex = getCurIndex(i++)) {
						if (stop)
							break;
						images.add(new File(context.getFilesDir(),
							new File(imageItemList.get(curIndex).getFullPath()).getName()));
						client.downloadFile(imageItemList.get(curIndex).getFullPath(), images.get(curIndex),
							new ProgressListener() {
								@Override
								public void updateProgress(long loaded, long total) {

								}

								@Override
								public boolean hasCancelled() {
									return false;
								}
							});
						downloadComplete(imageItemList.get(i));
					}
				} catch (IOException | WebdavException ex) {
					Log.d(TAG, "loadFile", ex);
//					sendException(ex);
				} finally {
					if (client != null) {
						client.shutdown();
					}
				}
			}
		}).start();
	}

	public File getImageAtAbsoluteIndex(int i) {
		return images.get(getCurIndex(i));
	}

	public File getImageAtRelativeIndex(int i) {
		return images.get(i);
	}

	private int getCurIndex(int i) {
		return (i + startIndex) % imageItemList.size();
	}

	private void downloadComplete(ListItem item) {
		System.out.println("Downloaded file: " + item.getFullPath());
		if (lastLoaded == null) {
			lastLoaded = new AtomicInteger(startIndex);
			return;
		}
		if (lastLoaded.incrementAndGet() > imageItemList.size())
			lastLoaded.set(0);
	}

	public void stop() {
		stop = true;
	}
}
