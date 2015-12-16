package ru.cfif.cs.android.slideshow;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.*;
import android.widget.*;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;

public class SimpleList extends ListFragment implements LoaderManager.LoaderCallbacks<List<ListItem>> {

	private static final String CURRENT_DIR_KEY = "example.current.dir";


	private static final String ROOT = "/";
	static final String IMAGE_MEDIA_TYPE = "image";
	public static final int SLIDESHOW_DELAY = 3 * 1000;

	private Credentials credentials;
	private String currentDir;

	private final ArrayList<ListItem> images = new ArrayList<>();

	private SimpleListAdapter adapter;
	DownloadFilesDialogFragment downloadFragment;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setDefaultEmptyText();

		setHasOptionsMenu(true);

		registerForContextMenu(getListView());

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String username = preferences.getString(MainActivity.USERNAME, null);
		String token = preferences.getString(MainActivity.TOKEN, null);

		credentials = new Credentials(username, token);

		Bundle args = getArguments();
		if (args != null) {
			currentDir = args.getString(CURRENT_DIR_KEY);
		}
		if (currentDir == null) {
			currentDir = ROOT;
		}
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(!ROOT.equals(currentDir));

		adapter = new SimpleListAdapter(getActivity());
		setListAdapter(adapter);
		setListShown(false);
		getLoaderManager().initLoader(0, null, this);
	}

	public void restartLoader() {
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (getListItem(menuInfo).isCollection() || !getListItem(menuInfo).getMediaType().equals(IMAGE_MEDIA_TYPE))
			return;

		menu.setHeaderTitle(getListItem(menuInfo).getDisplayName());
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.example_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ListItem listItem = getListItem(item.getMenuInfo());
		switch (item.getItemId()) {
		case R.id.context_slideshow:
			System.out.println("onContextItemSelected: publish: listItem=" + listItem);

			if (!images.isEmpty())
				runSlideShow(listItem);

			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	private void runSlideShow(ListItem item) {
		if (downloadFragment == null)
			downloadFragment = DownloadFilesDialogFragment.newInstance(getActivity(), credentials, images, item);
		Handler handler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				System.out.println("---------------- 1)" + msg);
				switch (msg.what) {
				case 1:
					System.out.println("---------------- 1)" + msg);
					File file = (File)msg.obj;
					file.setReadable(true, false);
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(file), "Image");
					startActivity(Intent.createChooser(intent, ""));
					break;
				default:
					System.out.println("---------------- def)" + msg);
				}
			}
		};
		downloadFragment.slideshow(item, handler, SLIDESHOW_DELAY);
	}

	private ListItem getListItem(ContextMenu.ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		return (ListItem)getListAdapter().getItem(info.position);
	}

	@Override
	public Loader<List<ListItem>> onCreateLoader(int i, Bundle bundle) {
		return new SimpleListLoader(getActivity(), credentials, currentDir);
	}

	@Override
	public void onLoadFinished(final Loader<List<ListItem>> loader, List<ListItem> data) {
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
		if (data.isEmpty()) {
			Exception ex = ((SimpleListLoader)loader).getException();
			if (ex != null) {
				setEmptyText(((SimpleListLoader)loader).getException().getMessage());
			} else {
				setDefaultEmptyText();
			}
		} else {
			images.addAll(((SimpleListLoader)loader).getImages());
			adapter.setData(data);
		}
	}

	@Override
	public void onLoaderReset(Loader<List<ListItem>> loader) {
		adapter.setData(null);
		images.clear();
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		ListItem item = (ListItem)getListAdapter().getItem(position);
		System.out.println("onListItemClick(): " + item);
		if (item.isCollection()) {
			changeDir(item.getFullPath());
		} else {
//			downloadFile(item);
		}
	}

	protected void changeDir(String dir) {
		Bundle args = new Bundle();
		args.putString(CURRENT_DIR_KEY, dir);

		SimpleList fragment = new SimpleList();
		fragment.setArguments(args);
		getFragmentManager().beginTransaction()
			.replace(android.R.id.content, fragment, MainActivity.FRAGMENT_TAG)
			.addToBackStack(null)
			.commit();
	}

//	private void downloadFile(ListItem item) {
//		DownloadFilesDialogFragment.newInstance(credentials, images, new ArrayList<>(Arrays.asList(item))).show(getFragmentManager(), "download");
//	}

	private void setDefaultEmptyText() {
		setEmptyText(getString(R.string.no_files));
	}

	public static class SimpleListAdapter extends ArrayAdapter<ListItem> {
		private final LayoutInflater inflater;

		public SimpleListAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_2);
			inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public void setData(List<ListItem> data) {
			clear();
			if (data != null) {
				addAll(data);
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;

			if (convertView == null) {
				view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
			} else {
				view = convertView;
			}

			ListItem item = getItem(position);
			((TextView)view.findViewById(android.R.id.text1)).setText(item.getDisplayName());
			((TextView)view.findViewById(android.R.id.text2)).setText(item.isCollection() ? "" : "" + item.getContentLength());

			return view;
		}
	}
}
