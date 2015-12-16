package ru.cfif.cs.android.slideshow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.yandex.disk.client.*;
import com.yandex.disk.client.exceptions.WebdavException;

//import android.app.*;

public class DownloadFilesDialogFragment {

	private static final String TAG = "LoadFileFragment";

	private static final String WORK_FRAGMENT_TAG = "LoadFileFragment.Background";

	private static final String FILE_ITEM = "example.file.items";

	private static final int PROGRESS_DIV = 1024 * 1024;

	private final Context context;
	private Credentials credentials;
	private ArrayList<ListItem> items = new ArrayList<>();
	private ArrayList<File> images = new ArrayList<>();
	private final AtomicInteger downloadedFiles = new AtomicInteger(0);
	private volatile boolean ready;
	private volatile boolean cancelled;
//	private ListItem first;

//	private DownloadFilesRetainedFragment workFragment;

	protected static final String CREDENTIALS = "example.credentials";

	protected ProgressDialog dialog;
	private Thread slideshowThread;

	public static DownloadFilesDialogFragment newInstance(Context context, Credentials credentials,
		ArrayList<ListItem> images,	ListItem first)
	{
		return new DownloadFilesDialogFragment(context, credentials, images, first);
	}

	private DownloadFilesDialogFragment(Context context, Credentials credentials, ArrayList<ListItem> items, ListItem first) {
		int i = items.indexOf(first);
		this.items.add(first);
		System.out.println(items);
		for (int j = 1; j < items.size(); i = (i == items.size() - 1) ? 0 : i + 1, j++)
			this.items.add(items.get(i));
		this.credentials = credentials;
		this.context = context;
		loadFile(context, credentials);
	}

	public void cancelSlideshow() {
		cancelled = true;
		if (slideshowThread.isAlive())
			slideshowThread.interrupt();
	}

	public void slideshow(ListItem first, final Handler handler, final int delay) {
		slideshowThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Message message;
				int i = 0;
				long lastSendTime = 0;
				while (true) {
					long waitingTime = System.currentTimeMillis() - lastSendTime;
					if (waitingTime < delay) {
						try {
							Thread.sleep(delay - waitingTime);
						} catch (InterruptedException e) {
							break;
						}
					}
					while (!ready) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							break;
						}
					}

					ready = false;
					if (images.size() == i)
						i = 0;
					if (cancelled)
						break;
					System.out.println("Send");
					message = handler.obtainMessage(1, images.get(i++));
					handler.sendMessage(message);
					lastSendTime = System.currentTimeMillis();
				}
			}
		});
		slideshowThread.start();
	}

	private void loadFile(final Context context, final Credentials credentials) {
		for (ListItem item : items)
			images.add(new File(context.getFilesDir(), new File(item.getFullPath()).getName()));

		for (ListItem item : items) {
			System.out.println("Item = " + item);
		}
		new Thread(new Runnable() {
			@Override
			public void run () {
				TransportClient client = null;
				try {
					client = TransportClient.getInstance(context, credentials);
					for (int i = 0; i < items.size(); i++) {
						client.downloadFile(items.get(i).getFullPath(), images.get(i), new ProgressListener() {
							@Override
							public void updateProgress(long loaded, long total) {

							}

							@Override
							public boolean hasCancelled() {
								return false;
							}
						});
						downloadComplete(items.get(i));
						ready = true;
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

	private void downloadComplete(ListItem item) {
		System.out.println("Downloaded file: " + item.getFullPath());
		downloadedFiles.incrementAndGet();
	}

//
//	@Override
//	public void onActivityCreated (Bundle savedInstanceState) {
//		super.onActivityCreated(savedInstanceState);
//
//		FragmentManager fragmentManager = getFragmentManager();
//		workFragment = (DownloadFilesRetainedFragment) fragmentManager.findFragmentByTag(WORK_FRAGMENT_TAG);
//		if (workFragment == null || workFragment.getTargetFragment() == null) {
//			workFragment = new DownloadFilesRetainedFragment();
//			fragmentManager.beginTransaction().add(workFragment, WORK_FRAGMENT_TAG).commit();
//			workFragment.loadFile(getActivity(), credentials, items);
//		}
//		workFragment.setTargetFragment(this, 0);
//	}
//
//	@Override
//	public void onDetach() {
//		super.onDetach();
//
//		if (workFragment != null) {
//			workFragment.setTargetFragment(null, 0);
//		}
//	}
//
//	@Override
//	public Dialog onCreateDialog(Bundle savedInstanceState) {
//		dialog = new ProgressDialog(getActivity());
//		dialog.setTitle(R.string.loading_file_title);
//		dialog.setMessage(getString(R.string.loading_file_message));
//		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//		dialog.setIndeterminate(true);
//		dialog.setButton(ProgressDialog.BUTTON_NEUTRAL, getString(R.string.loading_file_cancel_button), new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick (DialogInterface dialog, int which) {
//				dialog.dismiss();
//				onCancel();
//			}
//		});
//		dialog.setOnCancelListener(this);
//		dialog.show();
//		return dialog;
//	}
//
//	@Override
//	public void onCancel(DialogInterface dialog) {
//		super.onCancel(dialog);
//		onCancel();
//	}
//
//	public void sendException(final Exception ex) {
//		dialog.dismiss();
//		Toast.makeText(getActivity(), ex.getMessage(), Toast.LENGTH_LONG).show();
//	}
//
//	private void onCancel() {
//		workFragment.cancelDownload();
//	}
//
//	public void onDownloadComplete(List<File> files) {
//		dialog.dismiss();
//		if (files.size() == 1)
//		makeWorldReadableAndOpenFile(files.get(0));
//	}
//
//	private void makeWorldReadableAndOpenFile(File file) {
//		file.setReadable(true, false);
//		Intent intent = new Intent();
//		intent.setAction(Intent.ACTION_VIEW);
//		intent.setDataAndType(Uri.fromFile(file), items.get(0).getContentType());
//		startActivity(Intent.createChooser(intent, getText(R.string.loading_file_chooser_title)));
//	}
//
//	public void setDownloadProgress(long loaded, long total) {
//		if (dialog != null) {
//			if (dialog.isIndeterminate()) {
//				dialog.setIndeterminate(false);
//			}
//			if (total > Integer.MAX_VALUE) {
//				dialog.setProgress((int)(loaded / PROGRESS_DIV));
//				dialog.setMax((int)(total / PROGRESS_DIV));
//			} else {
//				dialog.setProgress((int)loaded);
//				dialog.setMax((int)total);
//			}
//		}
//	}
//
//	public static class DownloadFilesRetainedFragment extends Fragment implements ProgressListener {
//
//		private boolean cancelled;
//		private List<File> results = new ArrayList<>();
//		protected Handler handler;
//
//		@Override
//		public void onCreate(Bundle savedInstanceState) {
//			super.onCreate(savedInstanceState);
//
//			setRetainInstance(true);
//			handler = new Handler();
//		}
//
//		protected void sendException(final Exception ex) {
//			handler.post(new Runnable() {
//				@Override
//				public void run () {
//					DownloadFilesDialogFragment targetFragment = (DownloadFilesDialogFragment) getTargetFragment();
//					if (targetFragment != null) {
//						targetFragment.sendException(ex);
//					}
//				}
//			});
//		}
//
//		public void loadFile(final Context context, final Credentials credentials, final ArrayList<ListItem> items) {
//			for (ListItem item : items) {
//				results.add(new File(context.getFilesDir(), new File(item.getFullPath()).getName()));
//			}
//
//			new Thread(new Runnable() {
//				@Override
//				public void run () {
//					TransportClient client = null;
//					try {
//						client = TransportClient.getInstance(context, credentials);
//						for (int i = 0, resultsSize = results.size(); i < resultsSize; i++)
//							client.downloadFile(items.get(i).getFullPath(), results.get(i), DownloadFilesRetainedFragment.this);
//						downloadComplete();
//					} catch (IOException | WebdavException ex) {
//						Log.d(TAG, "loadFile", ex);
//						sendException(ex);
//					} finally {
//						if (client != null) {
//							client.shutdown();
//						}
//					}
//				}
//			}).start();
//		}
//
//		@Override
//		public void updateProgress (final long loaded, final long total) {
//			handler.post(new Runnable() {
//				@Override
//				public void run () {
//					DownloadFilesDialogFragment targetFragment = (DownloadFilesDialogFragment) getTargetFragment();
//					if (targetFragment != null) {
//						targetFragment.setDownloadProgress(loaded, total);
//					}
//				}
//			});
//		}
//
//		@Override
//		public boolean hasCancelled () {
//			return cancelled;
//		}
//
//		public void downloadComplete() {
//			handler.post(new Runnable() {
//				@Override
//				public void run () {
//					DownloadFilesDialogFragment targetFragment = (DownloadFilesDialogFragment) getTargetFragment();
//					if (targetFragment != null) {
//						targetFragment.onDownloadComplete(results);
//					}
//				}
//			});
//		}
//
//		public void cancelDownload() {
//			cancelled = true;
//		}
//	}
}
