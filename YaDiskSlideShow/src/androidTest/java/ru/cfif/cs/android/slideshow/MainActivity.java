package ru.cfif.cs.android.slideshow;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.accounts.*;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {

	// create your own client id/secret pair with callback url on oauth.yandex.ru
	public static final String CLIENT_ID = "6d91c309a85e4310a7a7e125af827499";
	public static final String CLIENT_SECRET = "bec1f56486b348caa3b8bf49eca57eb7";

	public static final String ACCOUNT_TYPE = "ru.cfif.cs.android";
	public static final String AUTH_URL = "https://oauth.yandex.ru/authorize?response_type=token&client_id="+CLIENT_ID;
	private static final String ACTION_ADD_ACCOUNT = "ru.cfif.cs.android.intent.ADD_ACCOUNT";
	private static final String KEY_CLIENT_SECRET = "clientSecret";
	private static final int GET_ACCOUNT_CREDS_INTENT = 100;

	public static String USERNAME = "yaDisk.username";
	public static String TOKEN = "yaDisk.token";

	public static String FRAGMENT_TAG = "list";


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		System.out.println(" !!!! ---- !!!! ");
		if (getIntent() != null && getIntent().getData() != null)
			onLogin();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String token = preferences.getString(TOKEN, null);
		if (token == null) {
			getToken();
			return;
		}

		if (savedInstanceState == null) {
			startFragment();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == GET_ACCOUNT_CREDS_INTENT) {
			if (resultCode == RESULT_OK) {
				Bundle bundle = data.getExtras();
				String name = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
				String type = bundle.getString(AccountManager.KEY_ACCOUNT_TYPE);
				Log.d("StartActivity", "GET_ACCOUNT_CREDS_INTENT: name="+name+" type="+type);
				Account account = new Account(name, type);
				getAuthToken(account);
			}
		}
	}

	public void reloadContent() {
		SimpleList fragment = (SimpleList) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
		fragment.restartLoader();
	}

	private void onLogin () {
		Uri data = getIntent().getData();
		setIntent(null);
		Pattern pattern = Pattern.compile("access_token=(.*?)(&|$)");
		Matcher matcher = pattern.matcher(data.toString());
		if (matcher.find()) {
			final String token = matcher.group(1);
			if (!TextUtils.isEmpty(token)) {
				System.out.println("onLogin: token: "+token);
				saveToken(token);
			} else {
				System.out.println("onRegistrationSuccess: empty token");
			}
		} else {
			System.out.println("onRegistrationSuccess: token not found in return url");
		}
	}

	private void getToken() {
		AccountManager accountManager = AccountManager.get(getApplicationContext());
		Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
		System.out.println("accounts: "+(accounts != null ? accounts.length : null));

		if (accounts != null && accounts.length > 0) {
			// get the first account, for example (you must show the list and allow user to choose)
			Account account = accounts[0];
			System.out.println("account: "+account);
			getAuthToken(account);
			return;
		}

		System.out.println("No such accounts: "+ACCOUNT_TYPE);
		for (AuthenticatorDescription authDesc : accountManager.getAuthenticatorTypes()) {
			if (ACCOUNT_TYPE.equals(authDesc.type)) {
				System.out.println("Starting "+ACTION_ADD_ACCOUNT);
				Intent intent = new Intent(ACTION_ADD_ACCOUNT);
				startActivityForResult(intent, GET_ACCOUNT_CREDS_INTENT);
				return;
			}
		}

		// no account manager for com.yandex
		new AuthDialogFragment().show(getSupportFragmentManager(), "auth");
	}

	private void getAuthToken(Account account) {
		AccountManager systemAccountManager = AccountManager.get(getApplicationContext());
		Bundle options = new Bundle();
		options.putString(KEY_CLIENT_SECRET, CLIENT_SECRET);
		systemAccountManager.getAuthToken(account, CLIENT_ID, options, this, new GetAuthTokenCallback(), null);
	}

	private void saveToken(String token) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putString(USERNAME, "");
		editor.putString(TOKEN, token);
		editor.commit();
	}

	private void startFragment() {
		getSupportFragmentManager().beginTransaction()
			.replace(android.R.id.content, new SimpleList(), FRAGMENT_TAG)
			.commit();
	}

	private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
		public void run(AccountManagerFuture<Bundle> result) {
			try {
				Bundle bundle = result.getResult();
				System.out.println("bundle: "+bundle);

				String message = (String) bundle.get(AccountManager.KEY_ERROR_MESSAGE);
				if (message != null) {
					Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
				}

				Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
				System.out.println("intent: "+intent);
				if (intent != null) {
					// User input required
					startActivityForResult(intent, GET_ACCOUNT_CREDS_INTENT);
				} else {
					String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
					System.out.println("GetAuthTokenCallback: token="+token);
					saveToken(token);
					startFragment();
				}
			} catch (OperationCanceledException ex) {
				ex.printStackTrace();
				Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
			} catch (AuthenticatorException ex) {
				ex.printStackTrace();
				Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
			} catch (IOException ex) {
				ex.printStackTrace();
				Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}

	public static class AuthDialogFragment extends DialogFragment {

		public AuthDialogFragment () {
			super();
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.auth_title)
				.setMessage(R.string.auth_message)
				.setPositiveButton(R.string.auth_positive_button, new DialogInterface.OnClickListener() {
					@Override
					public void onClick (DialogInterface dialog, int which) {
						dialog.dismiss();
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AUTH_URL)));
					}
				})
				.setNegativeButton(R.string.auth_negative_button, new DialogInterface.OnClickListener() {
					@Override
					public void onClick (DialogInterface dialog, int which) {
						dialog.dismiss();
						getActivity().finish();
					}
				})
				.create();
		}
	}
}
