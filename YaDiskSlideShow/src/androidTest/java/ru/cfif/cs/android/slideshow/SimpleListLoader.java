package ru.cfif.cs.android.slideshow;

import java.io.IOException;
import java.text.Collator;
import java.util.*;

import android.content.Context;
import android.os.Handler;
import android.support.v4.content.AsyncTaskLoader;
import com.yandex.disk.client.*;
import com.yandex.disk.client.exceptions.CancelledPropfindException;
import com.yandex.disk.client.exceptions.WebdavException;

public class SimpleListLoader extends AsyncTaskLoader<List<ListItem>> {

	private Credentials credentials;
	private String dir;
	private Handler handler;

	private List<ListItem> fileItemList;
	private Exception exception;
	private boolean hasCancelled;

	private static final int ITEMS_PER_REQUEST = 20;

	private  final List<ListItem> images = new ArrayList<>();

	private static Collator collator = Collator.getInstance();
	static {
		collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
	}
	private final Comparator<ListItem> FILE_ITEM_COMPARATOR = new Comparator<ListItem>() {
		@Override
		public int compare(ListItem f1, ListItem f2) {
			if (f1.isCollection() && !f2.isCollection()) {
				return -1;
			} else if (f2.isCollection() && !f1.isCollection()) {
				return 1;
			} else if (f2.isCollection() && f1.isCollection()) {
				return collator.compare(f1.getDisplayName(), f2.getDisplayName());
			} else if (f1.getMediaType().equals(SimpleList.IMAGE_MEDIA_TYPE) && !f2.getMediaType().equals(SimpleList.IMAGE_MEDIA_TYPE)) {
				return -1;
			} else if (f2.getMediaType().equals(SimpleList.IMAGE_MEDIA_TYPE) && !f1.getMediaType().equals(SimpleList.IMAGE_MEDIA_TYPE)) {
				return 1;
			} else {
				return collator.compare(f1.getDisplayName(), f2.getDisplayName());
			}
		}
	};

	public SimpleListLoader(Context context, Credentials credentials, String dir) {
		super(context);
		handler = new Handler();
		this.credentials = credentials;
		this.dir = dir;
	}

	@Override
	protected void onStartLoading() {
		forceLoad();
	}

	@Override
	protected void onReset() {
		super.onReset();
		hasCancelled = true;
		images.clear();
	}

	@Override
	public List<ListItem> loadInBackground() {
		fileItemList = new ArrayList<>();
		hasCancelled = false;
		TransportClient client = null;
		try {
			client = TransportClient.getInstance(getContext(), credentials);
			client.getList(dir, ITEMS_PER_REQUEST, new ListParsingHandler() {

				// First item in PROPFIND is the current collection name
				boolean ignoreFirstItem = true;

				@Override
				public boolean hasCancelled() {
					return hasCancelled;
				}

				@Override
				public void onPageFinished(int itemsOnPage) {
					ignoreFirstItem = true;
					handler.post(new Runnable() {
						@Override
						public void run () {
							Collections.sort(fileItemList, FILE_ITEM_COMPARATOR);
							deliverResult(new ArrayList<>(fileItemList));
						}
					});
				}

				@Override
				public boolean handleItem(ListItem item) {
					System.out.println(" ---- item = " + item);
					if (ignoreFirstItem) {
						ignoreFirstItem = false;
						return false;
					} else {
						if (item != null && !item.isCollection() && item.getMediaType().equals(SimpleList.IMAGE_MEDIA_TYPE))
							images.add(item);
						fileItemList.add(item);
						return true;
					}
				}
			});
			Collections.sort(fileItemList, FILE_ITEM_COMPARATOR);
			exception = null;
		} catch (CancelledPropfindException ex) {
			return fileItemList;
		} catch (WebdavException | IOException ex) {
			ex.printStackTrace();
			exception = ex;
		} finally {
			TransportClient.shutdown(client);
		}
		return fileItemList;
	}

	public Exception getException() {
		return exception;
	}

	public List<ListItem> getImages() {
		return images;
	}
}
