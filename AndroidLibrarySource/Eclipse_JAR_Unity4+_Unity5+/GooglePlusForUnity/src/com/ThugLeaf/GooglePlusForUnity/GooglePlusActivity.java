package com.ThugLeaf.GooglePlusForUnity;


import java.io.IOException;
import java.util.Random;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.People.LoadPeopleResult;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.Person.Image;
import com.google.android.gms.plus.model.people.PersonBuffer;
import com.unity3d.player.UnityPlayer;

/**
 * Created by Hamza on 1/16/2015.
 */


public class GooglePlusActivity extends Activity 
		implements ConnectionCallbacks, 
					OnConnectionFailedListener,
					ResultCallback<People.LoadPeopleResult> {
	
	
	private static final String TAG = GooglePlusActivity.class.getSimpleName();

	private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
	private static final int REQUEST_CODE_PICK_ACCOUNT = 1002;
	private static final int REQUEST_CODE_AUTH_CONSENT = 1003;
	private static final int REQUEST_CODE_SIGN_IN = 1004;
	
	public static Context mainUnityPlayerActivity = null;
	
	public static final int SIGN_IN_REASON = 1;
	public static final int GET_TOKEN_REASON = 2;
	public static final int SIGN_OUT_REASON = 3;
	public static final int LOAD_CIRCLES_REASON = 4;
	public static final int REVOKE_ACCESS_REASON = 5;
	public static final int INVALIDATE_TOKEN_REASON = 6;
	
	public static int reason = 0;
	
	public static String ClientId = "";
	
	public static String UnityObjectName = "GooglePlusGO";
	public static String OnTokenReceivedCallback = "OnTokenReceived";
	public static String OnCirclesLoadedCallback = "OnCirclesLoaded";
	public static String OnSignInSuccessCallback = "OnSignInSuccess";
	public static String ConnectionSuspendedCallbackName = "OnConnectionSuspended";
	public static String OnAccessRevokedCallback = "OnAccessRevoked";
	public static String OnTokenInvalidatedCallback = "OnTokenInvalidated";
	
	private static String accountName;
 
	/* A flag indicating that a PendingIntent is in progress and prevents
	   * us from starting further intents.
	   */
  	private static boolean mIntentInProgress;
		
	private static ConnectionResult lastConnectionResult;
	
	private static String token;

    private static GoogleApiClient mGoogleApiClient = null;
	
	public static void Start(int reasonToStart){
		reason = reasonToStart;
		if (mainUnityPlayerActivity==null) {
			mainUnityPlayerActivity = UnityPlayer.currentActivity;
		}
		Intent intent = new Intent(mainUnityPlayerActivity, GooglePlusActivity.class);
		//intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		mainUnityPlayerActivity.startActivity(intent);
		Log.i(TAG, "StartingActivity "+reason);
	}
	    
	private GoogleApiClient buildGoogleApiClient() {
        // When we build the GoogleApiClient we specify where connected and
        // connection failed callbacks should be returned, which Google APIs our
        // app uses and which OAuth 2.0 scopes our app requests.
        return new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
    }
	
	private void GoBackToUnityActivity(){
		Log.i(TAG, "GoBackToUnityActivity "+reason);
		reason = 0;
		Intent intent = new Intent(GooglePlusActivity.this, mainUnityPlayerActivity.getClass());
		//intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
		//finish();
	}

	@Override
	protected void onResume(){
		super.onResume();
		Log.i(TAG, "onResume "+reason);
		if (reason == 0){
			GoBackToUnityActivity();
		}/*else if (reason == SIGN_IN_REASON){
			SignIn();
		}*/
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (mGoogleApiClient==null) {
			mGoogleApiClient = buildGoogleApiClient();
		}
		Log.i(TAG, "onCreate "+reason);
	}

	@Override
	protected void onStart(){
		super.onStart();
		Log.i(TAG, "onStart "+reason);
		switch(reason){
			case SIGN_IN_REASON:
				SignIn();
				break;
			case GET_TOKEN_REASON:
				GetToken();
				break;
			case LOAD_CIRCLES_REASON:
				LoadCircles();
				break;
			case SIGN_OUT_REASON:
				SignOut();
				break;
			case REVOKE_ACCESS_REASON:
				RevokeAccess();
				break;
			case INVALIDATE_TOKEN_REASON:
				InvalidateToken();
			default:
				GoBackToUnityActivity();
				break;
		}
	}
	
	@Override
	protected void onStop(){
		super.onStop();
		/*if (mGoogleApiClient.isConnected()) {
		      mGoogleApiClient.disconnect();
	    }*/
		Log.i(TAG, "onStop "+reason);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult reqCode="+requestCode+" resCode="+resultCode);
		switch (requestCode) {
			case REQUEST_CODE_SIGN_IN:
				mIntentInProgress = false;
				if (resultCode == RESULT_OK){
					mGoogleApiClient.connect();
				} else if (resultCode == RESULT_CANCELED){
					showErrorToast("This application requires a Google account.");
				}
				else {
					Log.i(TAG, "REQUEST_CODE_SIGN_IN : " + resultCode);
				}
				return;
			case REQUEST_CODE_AUTH_CONSENT:
				if (resultCode == RESULT_OK) {
					GetToken();
				} else if (resultCode == RESULT_CANCELED) {
					showErrorToast("This application requires your consent before signing you in.");
				} else {
					Log.i(TAG, "REQUEST_CODE_AUTH_CONSENT : " + resultCode);
				}
				return;
			case REQUEST_CODE_RECOVER_PLAY_SERVICES:
				if (resultCode == RESULT_OK) {
					if (reason == SIGN_IN_REASON){
						mGoogleApiClient.connect();
					}else {
						GetToken();
					}
				} else if (resultCode == RESULT_CANCELED) {
					showErrorToast("Google Play Services must be installed.");
				} else {
					Log.i(TAG, "REQUEST_CODE_RECOVER_PLAY_SERVICES : " + resultCode);
				}
				return;
			case REQUEST_CODE_PICK_ACCOUNT:
				if (resultCode == RESULT_OK) {
					accountName = data
							.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
					
					GetToken();
				} else if (resultCode == RESULT_CANCELED) {
					showErrorToast("This application requires a Google account.");
				} else {
					Log.i(TAG, "REQUEST_CODE_PICK_ACCOUNT : " + resultCode);
				}
				return;
		}
		super.onActivityResult(requestCode, resultCode, data);
		Log.i(TAG, "onActivityResult = 0");
		reason = 0;
	}

	private String getToken(String accountName) {

		String code = null;
		try {
			String scopes = "oauth2:";
			if (ClientId == "" || ClientId == null || ClientId.isEmpty()){
				scopes += Scopes.PLUS_LOGIN;
				code = GoogleAuthUtil.getToken(this, // Context context
						accountName, // String accountName
						scopes // String scope
						);
				Log.i(TAG, scopes);
			}else {
				//scopes += "server:client_id:" + ClientId + ":api_scope:" + Scopes.PLUS_LOGIN;
				scopes = "audience:server:client_id:" + ClientId; // Not the app's client ID.
				Log.i(TAG, scopes);
				Bundle appActivities = new Bundle();
				appActivities.putString(GoogleAuthUtil.KEY_REQUEST_VISIBLE_ACTIVITIES, 
						"http://schemas.google.com/AddActivity http://schemas.google.com/BuyActivity");
				code = GoogleAuthUtil.getToken(this, // Context context
						accountName, // String accountName
						scopes, // String scope
						appActivities
						);
			}
			
			

		} catch (GooglePlayServicesAvailabilityException playEx) {
			showErrorDialog(playEx.getConnectionStatusCode());
			Log.e(TAG,
					"GooglePlayServicesAvailabilityException : "
							+ playEx.getMessage(), playEx);
		} catch (IOException transientEx) {
			// network or server error, the call is expected to succeed if you
			// try again later.
			// Don't attempt to call again immediately - the request is likely
			// to
			// fail, you'll hit quotas or back-off.
			Log.e(TAG,
					"transient error encountered : " + transientEx.getMessage(),
					transientEx);
			doExponentialBackoff();
			// return;
		} catch (UserRecoverableAuthException e) {
			// Requesting an authorization code will always throw
			// UserRecoverableAuthException on the first call to
			// GoogleAuthUtil.getToken
			// because the user must consent to offline access to their data.
			// After
			// consent is granted control is returned to your activity in
			// onActivityResult
			// and the second call to GoogleAuthUtil.getToken will succeed.

			startActivityForResult(e.getIntent(), REQUEST_CODE_AUTH_CONSENT);
			// return;
		} catch (GoogleAuthException authEx) {
			// Failure. The call is not expected to ever succeed so it should
			// not be
			// retried.
			Log.w(TAG,
					"Unrecoverable authentication exception : "
							+ authEx.getMessage(), authEx);

			// return;
		} catch (Exception e) {
			Log.w(TAG, "Unhandled exception : " + e.getMessage(), e);
		}
		return code;
	}

	private void doExponentialBackoff() {
		Backoff backoff = new Backoff();
		// Something is stressed out; the auth servers are by definition
		// high-traffic and you can't count on
		// 100% success. But it would be bad to retry instantly, so back off
		if (backoff.shouldRetry()) {
			backoff.backoff();
		}
	}

	private void showErrorDialog(int code) {
		GooglePlayServicesUtil.getErrorDialog(code, this,
				REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
	}

	private void showErrorToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
		GoBackToUnityActivity();
	}

	public boolean checkPlayServices() {
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (status != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
				showErrorDialog(status);
			} else {
				showErrorToast("This device is not supported.");
			}
			return false;
		}
		return true;
	}

	private void getTokenAsync(String accountName) {
		AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... account) {
				return getToken(account[0]);
			}

			@Override
			protected void onPostExecute(String code) {
				token = code;
				// Log.i(TAG, "Access token retrieved:" + code);
				UnityPlayer.UnitySendMessage(UnityObjectName,
						OnTokenReceivedCallback, code);
				GoBackToUnityActivity();
			}

		};
		task.execute(accountName);
	}

	private void invalidateTokenAsync() {
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void...params) {
				try {
					GoogleAuthUtil.clearToken(GooglePlusActivity.this, token);
				} catch (GoogleAuthException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void x) {
				token = null;
				UnityPlayer.UnitySendMessage(UnityObjectName, OnTokenInvalidatedCallback, "");
				if (reason == INVALIDATE_TOKEN_REASON){
					GoBackToUnityActivity();
				}
			}

		};
		task.execute();
	}
	
	public void GetToken() {
		if (accountName == null) {
			showAccountPicker();
		} else {
			getTokenAsync(accountName);
		}
	}

	public void InvalidateToken(){
		if (token == null){
			return;
		}
		invalidateTokenAsync();
		token = null;
	}
	
	public void SignIn(){
		/*if (isConnected()){
			GoBackToUnityActivity();
		}else {*/
			Log.i(TAG, "GooglePlusSignIn");
	 		startResolution();
		//}
    }
	
	public void SignOut(){
		accountName = null;
		if (isConnected()){
			// We clear the default account on sign out so that Google Play
	        // services will not return an onConnected callback without user
	        // interaction.
	        Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
	        InvalidateToken();
	        //mGoogleApiClient.disconnect();
	        //mGoogleApiClient.connect();
	        mGoogleApiClient.reconnect();
		}
		GoBackToUnityActivity();	
	}

	public void RevokeAccess(){
		// Prior to disconnecting, run clearDefaultAccount().
		if (isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<Status>() {
                    	@Override
                        public void onResult(Status status) {
                        	// mGoogleApiClient is now disconnected and access has been revoked.
                    		// Trigger app logic to comply with the developer policies
                            Log.e(TAG, "User access revoked!");
                            UnityPlayer.UnitySendMessage(UnityObjectName, OnAccessRevokedCallback, "");
                            mGoogleApiClient = buildGoogleApiClient();
                            mGoogleApiClient.connect();
                        }
                    });
        }else {
        	GoBackToUnityActivity();
        }
	}
		
	private void showAccountPicker() {
		Intent pickAccountIntent = AccountPicker.newChooseAccountIntent(null,
				null, new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE },
				true, null, null, null, null);
		startActivityForResult(pickAccountIntent, REQUEST_CODE_PICK_ACCOUNT);
	}

    public static String GetProfilePictureUrl(){
    	if (isConnected()){
	    	Person person = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
	    	if (person != null && person.hasImage()) {
	    		Image image = person.getImage();
	    		if (image.hasUrl()){
	    			return person.getImage().getUrl();
	    		}
	    	}
    	}
		return null;
    }
    
    public static String GetDisplayName(){
    	if (isConnected()){
	    	Person person = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
	    	if (person != null && person.hasDisplayName()) {
	    		return person.getDisplayName();
	    	}
    	}
    	return null;
    	
    }
 
    public static Boolean isConnected(){
    	return mGoogleApiClient!=null && mGoogleApiClient.isConnected();
    }
 
    private static void connect(){
    	if (mGoogleApiClient!=null && !mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()){
    		mGoogleApiClient.connect();
    	}
    }
    
    public void Post(String content){
    	if (isConnected()) {
            /*Intent shareIntent = PlusShare.Builder
                    .from(this)
                    .setText(
                            "Check out: http://example.com/cheesecake/lemon")
                    .setType("text/plain")
                    .setContent(
                            Uri.parse("http://example.com/cheesecake/lemon"))
                    .getIntent();
            startActivity(shareIntent);*/
        } else {
            GoBackToUnityActivity();
        }
    }
     
    public void LoadCircles(){
    	if (isConnected()){
    		Log.i(TAG, "LoadCircles");
    		Plus.PeopleApi.loadVisible(mGoogleApiClient, null)
    		.setResultCallback(this);
    	} else {
    		Log.i(TAG, "LoadCircles 0");
    		GoBackToUnityActivity();
    	}
    }
    
    @Override
    public void onResult(LoadPeopleResult peopleData) {
      if (peopleData.getStatus().getStatusCode() == CommonStatusCodes.SUCCESS) {
        String circles = "";
        String contact;
        PersonBuffer personBuffer = peopleData.getPersonBuffer();
        try {
            int count = personBuffer.getCount();
            Log.i(TAG, "TOTAL CONTACTS = " + count);
            for (int i = 0; i < count; i++) {
            	Person person = personBuffer.get(i);
            	if (person!=null){
	            	contact = person.getId()+"|"+person.getDisplayName()+"|";
	            	if (person.getImage().hasUrl()){
	            		contact+=person.getImage().getUrl();
	            	}
	            	circles += contact + '\n';
            	}
            }
        } finally {
            personBuffer.release();
        }
        UnityPlayer.UnitySendMessage(UnityObjectName, OnCirclesLoadedCallback, circles);
      } else {
        Log.e(TAG, "Error requesting visible circles: " + peopleData.getStatus());
      }
      GoBackToUnityActivity();
    }
    
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		int errorCode = result.getErrorCode();
        Log.i(TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + errorCode);
        switch (errorCode){
	        case ConnectionResult.API_UNAVAILABLE:
	        case ConnectionResult.INTERNAL_ERROR:
	        case ConnectionResult.CANCELED:
	        case ConnectionResult.DEVELOPER_ERROR:
	        case ConnectionResult.INTERRUPTED:
	        case ConnectionResult.SERVICE_MISSING:
	        case ConnectionResult.SERVICE_DISABLED:
	        case ConnectionResult.SERVICE_INVALID:
	        case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
	        case ConnectionResult.NETWORK_ERROR:
	        case ConnectionResult.LICENSE_CHECK_FAILED:
	        case ConnectionResult.INVALID_ACCOUNT:
	        	if (GooglePlayServicesUtil.isUserRecoverableError(errorCode)) {
	        		showErrorDialog(errorCode);
	        	}else {
					showErrorToast("This device is not supported.");
				}
	        	break;
	        case ConnectionResult.SIGN_IN_REQUIRED:
	        case ConnectionResult.RESOLUTION_REQUIRED:
        		lastConnectionResult = result;
        		if (reason == SIGN_IN_REASON) startResolution();
        		else {
        			Log.i(TAG, "onConnectionFailed = 0");
        			GoBackToUnityActivity();
        		}
	        	break;
        }
	}

	private void startResolution() {
		if (lastConnectionResult!=null && 
				lastConnectionResult.hasResolution() &&
				!mIntentInProgress){
			Log.i(TAG, "RESOLVING");
			PendingIntent mSignInIntent = lastConnectionResult.getResolution();
			try {
				mIntentInProgress = true;
				startIntentSenderForResult(mSignInIntent.getIntentSender(),
	        		REQUEST_CODE_SIGN_IN, null, 0, 0, 0);
			} catch (SendIntentException e) {
		        Log.i(TAG, "Sign in intent could not be sent: "
		            + e.getMessage());
		        mIntentInProgress = false;
		        //SignIn();
		        Log.i(TAG, "startResolution 1");
		        reason = 0;
			}
		}else {
			mIntentInProgress = false;
			connect();
			//SignIn();
			Log.i(TAG, "startResolution 2");
			//reason = 0;
		}
	}
	
	@Override
	public void onConnected(Bundle connectionHint) {
		Log.i(TAG, "onConnected");
		accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
		UnityPlayer.UnitySendMessage(UnityObjectName,
				OnSignInSuccessCallback, Plus.PeopleApi.getCurrentPerson(mGoogleApiClient).getId());
		GoBackToUnityActivity();
	}
	
	@Override
	public void onConnectionSuspended(int cause) {
		Log.i(TAG, "onConnectionSuspended, cause : " + cause);
    	switch (cause){
	    	case ConnectionCallbacks.CAUSE_NETWORK_LOST:
	    		break;
	    	case ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED:
	    		break;
    	}
    	UnityPlayer.UnitySendMessage(UnityObjectName,
    			ConnectionSuspendedCallbackName, ""+cause);
    	connect();
	}
	
	 static class Backoff {

	        private static final long INITIAL_WAIT = 1000 + new Random().nextInt(1000);
	        private static final long MAX_BACKOFF = 1800 * 1000;

	        private long mWaitInterval = INITIAL_WAIT;
	        private boolean mBackingOff = true;

	        public boolean shouldRetry() {
	            return mBackingOff;
	        }

	        private void noRetry() {
	            mBackingOff = false;
	        }

	        public void backoff() {
	            if (mWaitInterval > MAX_BACKOFF) {
	                noRetry();
	            } else if (mWaitInterval > 0) {
	                try {
	                    Thread.sleep(mWaitInterval);
	                } catch (InterruptedException e) {
	                    // life's a bitch, then you die
	                }
	            }

	            mWaitInterval = (mWaitInterval == 0) ? INITIAL_WAIT : mWaitInterval * 2;
	        }
	    }

	
	
}

