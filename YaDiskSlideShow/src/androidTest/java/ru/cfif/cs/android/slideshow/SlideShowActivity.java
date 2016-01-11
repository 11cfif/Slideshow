package ru.cfif.cs.android.slideshow;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;

public class SlideShowActivity extends Activity {

	public static final int SLIDESHOW_DELAY = 5 * 1000;
	DownloadManager downloadManager;

	private SlideshowThread slideshowThread;
	private volatile boolean stop = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_slide_show);
		ImageFragment fragment = new ImageFragment();
		getFragmentManager()
			.beginTransaction()
			.add(R.id.fragment, fragment)
			.commit();
		Intent intent = getIntent();
		downloadManager = new DownloadManager(intent.<ListItem>getParcelableArrayListExtra(SimpleList.LIST_KEY));
		startSlideshow(intent.getIntExtra(SimpleList.START_ITEM_KEY, 0),
			intent.<Credentials>getParcelableExtra(SimpleList.CREDENTIALS_KEY));
	}

	@Override
	public void onBackPressed() {
		downloadManager.stop();
		stop = true;
		super.onBackPressed();
	}

	public void startSlideshow(int startIndex, Credentials credentials) {
		downloadManager.loadFiles(startIndex, credentials, this);
		Handler handler = new SlideHandler();
		slideshowThread = new SlideshowThread(handler);
		slideshowThread.start();
	}

	class SlideHandler extends Handler {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 1:
				File file = (File)msg.obj;
				file.setReadable(true, false);
				if (stop)
					return;
				getFragmentManager()
					.beginTransaction()
					.replace(R.id.fragment, ImageFragment.newInstance(Uri.fromFile(file)))
					.commit();
				break;
			default:
				throw new AssertionError();
			}
		}
	}

	class SlideshowThread extends Thread {

		private final Handler handler;
		private volatile boolean cancelled = false;

		public SlideshowThread(Handler handler) {
			this.handler = handler;
		}

		public void run() {
			Message message;
			int i = 0;
			long lastSendTime = 0;
			while (!stop) {
				long waitingTime = System.currentTimeMillis() - lastSendTime;
				if (waitingTime < SLIDESHOW_DELAY) {
					try {
						Thread.sleep(SLIDESHOW_DELAY - waitingTime);
					} catch (InterruptedException e) {
						break;
					}
				}
				while (downloadManager.getImageAtAbsoluteIndex(i) == null) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						break;
					}
				}

				if (cancelled)
					break;
				File image = downloadManager.getImageAtAbsoluteIndex(i++);
				message = handler.obtainMessage(1, image);
				handler.sendMessage(message);
				lastSendTime = System.currentTimeMillis();
			}
		}
	}
}