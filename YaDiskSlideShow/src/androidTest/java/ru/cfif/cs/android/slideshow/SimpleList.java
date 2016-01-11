package ru.cfif.cs.android.slideshow;


import java.util.ArrayList;
import java.util.List;

import android.content.*;
import android.os.Bundle;
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

	static final String LIST_KEY = "listImagesKey";
	static final String CREDENTIALS_KEY = "CredentialsKey";
	static final String START_ITEM_KEY = "StartItemKey";

	private Credentials credentials;
	private String currentDir;

	private final ArrayList<ListItem> items = new ArrayList<>();

	private SimpleListAdapter adapter;

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
		ListItem item = getItems(menuInfo);
		if (item.isCollection() || !item.getMediaType().equals(IMAGE_MEDIA_TYPE))
			return;

		menu.setHeaderTitle(item.getDisplayName());
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.example_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ListItem listItem = getItems(item.getMenuInfo());
		int imageId = items.indexOf(listItem);
		switch (item.getItemId()) {
		case R.id.context_slideshow:
			if (!this.items.isEmpty())
				runSlideShow(imageId);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	private void runSlideShow(final int imagesId) {
		Intent intent = new Intent(getContext(), SlideShowActivity.class);
		intent.putExtra(CREDENTIALS_KEY, credentials);
		intent.putParcelableArrayListExtra(LIST_KEY, items);
		intent.putExtra(START_ITEM_KEY, imagesId);
		startActivity(intent);
	}

	private ListItem getItems(ContextMenu.ContextMenuInfo menuInfo) {
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
			items.addAll(((SimpleListLoader)loader).getImages());
			adapter.setData(data);
		}
	}

	@Override
	public void onLoaderReset(Loader<List<ListItem>> loader) {
		adapter.setData(null);
		items.clear();
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		ListItem item = (ListItem)getListAdapter().getItem(position);
		System.out.println("onListItemClick(): " + item);
		if (item.isCollection()) {
			changeDir(item.getFullPath());
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
