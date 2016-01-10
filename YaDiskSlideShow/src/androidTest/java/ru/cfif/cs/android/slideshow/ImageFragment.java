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

//	private OnFragmentInteractionListener mListener;


	public static ImageFragment newInstance(Uri imageURI) {
		ImageFragment fragment = new ImageFragment();
		Bundle args = new Bundle();
		args.putString(ARG_IMAGE_URI, imageURI.toString());
		fragment.setArguments(args);
		return fragment;
	}

//	public ImageFragment() {
//		// Required empty public constructor
//	}

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
		view.setImageURI(imageUri);
		return rootView;
	}

//	// TODO: Rename method, update argument and hook method into UI event
//	public void onButtonPressed(Uri uri) {
//		if (mListener != null) {
//			mListener.onFragmentInteraction(uri);
//		}
//	}

//	@Override
//	public void onAttach(Activity activity) {
//		super.onAttach(activity);
//		try {
//			mListener = (OnFragmentInteractionListener)activity;
//		} catch (ClassCastException e) {
//			throw new ClassCastException(activity.toString()
//				+ " must implement OnFragmentInteractionListener");
//		}
//	}

	@Override
	public void onDetach() {
		super.onDetach();
//		mListener = null;
	}
}

//	/**
//	 * This interface must be implemented by activities that contain this
//	 * fragment to allow an interaction in this fragment to be communicated
//	 * to the activity and potentially other fragments contained in that
//	 * activity.
//	 * <p/>
//	 * See the Android Training lesson <a href=
//	 * "http://developer.android.com/training/basics/fragments/communicating.html"
//	 * >Communicating with Other Fragments</a> for more information.
//	 */
//	public interface OnFragmentInteractionListener {
//		// TODO: Update argument type and name
//		public void onFragmentInteraction(Uri uri);
//	}