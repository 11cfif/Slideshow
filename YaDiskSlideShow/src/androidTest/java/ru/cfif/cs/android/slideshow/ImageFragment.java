package ru.cfif.cs.android.slideshow;

import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;

public class ImageFragment extends Fragment {
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final String ARG_IMAGE_URI = "ImageUri";

	private Uri imageUri;

	public static ImageFragment newInstance(Uri imageURI) {
		ImageFragment fragment = new ImageFragment();
		Bundle args = new Bundle();
		args.putString(ARG_IMAGE_URI, imageURI.toString());
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			imageUri = Uri.parse(getArguments().getString(ARG_IMAGE_URI));
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_image, container, false);
		ImageView view = (ImageView)rootView.findViewById(R.id.imageView);
		if (imageUri != null)
			view.setImageURI(imageUri);
		else
			view.setImageResource(R.drawable.loading_image);
		return rootView;
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}
}