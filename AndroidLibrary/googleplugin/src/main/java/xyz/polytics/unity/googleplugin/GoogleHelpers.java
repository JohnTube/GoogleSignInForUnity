package xyz.polytics.unity.googleplugin;

/*import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.accounts.AccountManager;
import android.content.pm.PackageManager;
import android.app.ProgressDialog;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;
import java.util.jar.Manifest;*/
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.unity3d.player.UnityPlayer;

import java.io.IOException;

/**
 * Created by Hamza on 2/16/2016.
 */
public class GoogleHelpers extends Fragment implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "GoogleHelpers";
    public static GoogleHelpers Instance;

    private static final int RC_REQUEST_PERMISSION = 2001;
    private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private static final int REQUEST_CODE_PICK_ACCOUNT = 1002;
    private static final int REQUEST_CODE_AUTH_CONSENT = 1003;

    private static final int RC_SIGN_IN = 9001;
    private static final int RC_GET_TOKEN = 9002;
    //private static final int RC_GET_AUTH_CODE = 9003;
    private static final int RC_RESOLUTION = 9004;
    private GoogleApiClient mGoogleApiClient;
    private GoogleSignInAccount acct;
    private String accountName;
    public static Activity mainUnityPlayerActivity = null;

    private final static String scopes = Scopes.PROFILE;

    public static String ClientId = "";

    public static String UnityObjectName = "_GoogleHelpersGO";
    public static String OnTokenV3ReceivedCallback = "OnGoogleGetTokenV3Success";
    public static String OnGetTokenV3ErrorCallback = "OnGoogleGetTokenV3Error";
    public static String OnTokenV2ReceivedCallback = "OnGoogleGetTokenV2Success";
    public static String OnGetTokenV2ErrorCallback = "OnGoogleGetTokenV2Error";
    public static String OnSignInSuccessCallback = "OnGoogleSignInSuccess";
    public static String OnSignInErrorCallback = "OnGoogleSignInError";
    public static String OnSignOutSuccessCallback = "OnGoogleSignOutSuccess";
    public static String OnSignOutErrorCallback = "OnGoogleSignOutError";
    //public static String OnAccessRevokedCallback = "OnGoogleAccessRevoked";
    //public static String OnServerTokenReceivedCallback = "OnGoogleServerTokenReceived";
    //public static String OnGetServerTokenErrorCallback = "OnGoogleGetServerTokenError";
    //public static String OnAccessRevokeErrorCallback = "OnGoogleAccessRevokeError";

    private void buildGoogleApiClient() {
        //Log.d(TAG, "buildGoogleApiClient");
        GoogleSignInOptions.Builder builderSignIn;
        if (validateServerClientID(ClientId)){
            builderSignIn = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(ClientId)
                    .requestProfile()
                    //.requestScopes(new Scope(Scopes.PLUS_ME))
                    //.requestScopes(new Scope(Scopes.PLUS_LOGIN))
                    //.requestServerAuthCode(ClientId)
                    .requestProfile();
        } else {
            Log.w(TAG, "Getting token v3 will not be possible b/c ClientId is not set properly.");
            builderSignIn = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestProfile()
                    //.requestScopes(new Scope(Scopes.PLUS_ME))
                    //.requestScopes(new Scope(Scopes.PLUS_LOGIN))
                    //.requestServerAuthCode(ClientId)
                    .requestProfile();
        }
        mGoogleApiClient = new GoogleApiClient.Builder(mainUnityPlayerActivity)
                .addApi(Auth.GOOGLE_SIGN_IN_API, builderSignIn.build())
                //.enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .build();
        registerCallbacks();
    }

    public static void Start()
    {
        // Instantiate and add to Unity Player Activity.
        Instance = new GoogleHelpers();
        mainUnityPlayerActivity = UnityPlayer.currentActivity;
        mainUnityPlayerActivity.getFragmentManager().beginTransaction().add(Instance, GoogleHelpers.TAG).commit();
        Log.d(TAG, "Start");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); // Retain between configuration changes (like device rotation)
        buildGoogleApiClient();
        Log.d(TAG, "onCreate");
        silentSignIn();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
        if (result == null){
            Log.d(TAG, "onActivityResult: result == null");
        }
        else if (result.getStatus() != null && !result.getStatus().isSuccess()) {
            Log.d(TAG, "GoogleSignInResult:" + CommonStatusCodes.getStatusCodeString(result.getStatus().getStatusCode()) +
                    "(" + result.getStatus().getStatusCode() + ") resolvable?:" + result.getStatus().hasResolution());
        }
        switch (requestCode) {
            case RC_RESOLUTION:
            case RC_SIGN_IN:
                handleSignInResult(result);
                return;
            case RC_GET_TOKEN:
                // [START get_id_token]
                handleGetTokenResult(result);
                // [END get_id_token]
                return;
            case REQUEST_CODE_AUTH_CONSENT:
                if (resultCode == Activity.RESULT_OK) {
                    GetV2Token();
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    showErrorToast("This application requires your consent before signing you in.");
                } else {
                    showErrorToast("An internal error has occurred.");
                }
                return;
            case REQUEST_CODE_RECOVER_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    GetV2Token();
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    showErrorToast("Google Play Services must be installed.");
                } else {
                    Log.i(TAG, "REQUEST_CODE_RECOVER_PLAY_SERVICES : " + resultCode);
                    showErrorToast("Google Play Services Error.");
                }
                return;
            case REQUEST_CODE_PICK_ACCOUNT:
                if (resultCode == Activity.RESULT_OK) {
                    accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    Log.d(TAG, "accountTyp="+data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)+" accountName="+
                            accountName);
                    GetV2Token();
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    showErrorToast("This application requires a Google account.");
                } else {
                    Log.i(TAG, "REQUEST_CODE_PICK_ACCOUNT : " + resultCode);
                    showErrorToast("Error getting Google Account.");
                }
                return;
            /*case RC_GET_AUTH_CODE:
                if (result.isSuccess()) {
                    // [START get_auth_code]
                    acct = result.getSignInAccount();
                    String authCode = acct.getServerAuthCode();
                    SendUnityMessage(OnServerTokenReceivedCallback, authCode);
                    // Show signed-in UI.
                    Log.d(TAG, "authToken:" + authCode);
                    // [END get_auth_code]
                } else {
                    // Show signed-out UI.
                    SendUnityMessage(OnGetServerTokenErrorCallback, "");
                    showErrorToast("Failed to get server auth token");
                }
                return;*/

        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        //isSignedIn = result.isSuccess();
        if (result.isSuccess()) {
            mGoogleApiClient.connect();
            // Signed in successfully, show authenticated UI.
            acct = result.getSignInAccount();
            accountName = GetEmail();
            SendUnityMessage(OnSignInSuccessCallback, acct.getId());
            Log.d(TAG, "handleSignInResult success");
        } else if (result.getStatus().getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED){
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(signInIntent, RC_SIGN_IN);
        } else if (result.getStatus().hasResolution()) {
            try {
                result.getStatus().startResolutionForResult(mainUnityPlayerActivity, RC_RESOLUTION);
            } catch (IntentSender.SendIntentException e) {
                String msg = "Exception when resolving sign in" + e.getMessage();
                Log.e(TAG, msg, e);
                SendUnityMessage(OnSignInErrorCallback, msg);
                showErrorToast("Failed to sign in.");
            }
        } else if (result.getStatus().getStatusCode() == CommonStatusCodes.CANCELED) {
            SendUnityMessage(OnSignInErrorCallback, "Canceled by user.");
            showErrorToast("Google login canceled.");
        } else {
            // Signed out.
            SendUnityMessage(OnSignInErrorCallback, CommonStatusCodes.getStatusCodeString(result.getStatus().getStatusCode()));
            showErrorToast("Failed to sign in.");
        }
    }

    private void handleGetTokenResult(GoogleSignInResult result){
        if (result.isSuccess()) {
            acct = result.getSignInAccount();
            String idToken = acct.getIdToken();
            SendUnityMessage(OnTokenV3ReceivedCallback, idToken);
            // Show signed-in UI.
            Log.d(TAG, "idToken:" + idToken);
        } else {
            // Show signed-out UI.
            SendUnityMessage(OnGetTokenV3ErrorCallback, CommonStatusCodes.getStatusCodeString(result.getStatus().getStatusCode()));
            showErrorToast("Failed to get token.");
        }
    }

    private void silentSignIn() {
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d(TAG, "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    Log.d(TAG, "SilentSignAsyncInCallback");
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }

    public void GetIdToken() {
        Log.d(TAG, "getIdToken");
        // Show an account picker to let the user choose a Google account from the device.
        // If the GoogleSignInOptions only asks for IDToken and/or profile and/or email then no
        // consent screen will be shown here.
        try {
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(signInIntent, RC_GET_TOKEN);
        } catch (Exception ex) {
            Log.e(TAG, "getIdTokenException", ex);
        }
    }

    /*
    public void GetAuthCode() {
        // Start the retrieval process for a server auth code.  If requested, ask for a refresh
        // token.  Otherwise, only get an access token if a refresh token has been previously
        // retrieved.  Getting a new access token for an existing grant does not require
        // user consent.
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_GET_AUTH_CODE);
    }

    public void RevokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()){
                            acct = null;
                            //isSignedIn = false;
                            unregisterCallbacks();
                            SendUnityMessage(OnAccessRevokedCallback, status.getStatusMessage());
                        } else {
                            SendUnityMessage(OnAccessRevokeErrorCallback, status.getStatusMessage());
                            showErrorToast("Failed to revoke token access");
                        }
                    }
                });
    }
    */

    private String getToken(String accountName) {
        String code = null;
        try {
            String fullScopes;
            //if (ClientId == "" || ClientId == null || ClientId.isEmpty()) {
            fullScopes = "oauth2:" + scopes;
            code = GoogleAuthUtil.getToken(mainUnityPlayerActivity, // Context context
                    accountName, // String accountName
                    fullScopes // String scope
            );
            /*} else {
                //scopes += "server:client_id:" + ClientId + ":api_scope:" + Scopes.PLUS_LOGIN;
                fullScopes = "audience:server:client_id:" + ClientId + ":api_scopes:" + scopes; // Not the app's client ID.
                code = GoogleAuthUtil.getToken(mainUnityPlayerActivity, // Context context
                        accountName, // String accountName
                        fullScopes // String scope
                );
            }*/
            Log.i(TAG, "requested scopes=" + fullScopes);
        } catch (Exception e) {
            handleException(e);
        }
        return code;
    }

    private void getTokenAsync() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (accountName != null) {
                    String token = getToken(accountName);
                    if (token != null) {
                        Log.i(TAG, "Access token retrieved:" + token);
                        UnityPlayer.UnitySendMessage(UnityObjectName,
                                OnTokenV2ReceivedCallback, token);
                    }
                } else if (IsConnected()) {
                    accountName = GetEmail();
                } else if (grantPermissionAndShowAccountPicker()) {
                    showAccountPicker();
                } else {
                    String msg = "Failed to get token: account is null";
                    Log.w(TAG, msg);
                    UnityPlayer.UnitySendMessage(UnityObjectName, OnGetTokenV2ErrorCallback, msg);
                    //showErrorToast("Internal error has occurred");
                }
                return null;
            }
        };
        task.execute();
    }

    /** begin Manual GoogleClient life cycle handling **/

    // this is important to be able to do sign out

    private void registerCallbacks(){
        if (mGoogleApiClient == null) {
            Log.d(TAG, "registerCallbacks: mGoogleApiClient == null");
            return;
        }
        if (!mGoogleApiClient.isConnectionCallbacksRegistered(this)){
            Log.d(TAG, "registerConnectionCallbacks");
            mGoogleApiClient.registerConnectionCallbacks(this);
        }
        if (!mGoogleApiClient.isConnectionFailedListenerRegistered(this)){
            Log.d(TAG, "registerConnectionFailedListener");
            mGoogleApiClient.registerConnectionFailedListener(this);
        }
    }

    private void unregisterCallbacks(){
        if (mGoogleApiClient == null) {
            Log.d(TAG, "unregisterCallbacks: mGoogleApiClient == null");
            return;
        }
        if (mGoogleApiClient.isConnectionCallbacksRegistered(this)){
            Log.d(TAG, "unregisterConnectionCallbacks");
            mGoogleApiClient.unregisterConnectionCallbacks(this);
        }
        if (mGoogleApiClient.isConnectionFailedListenerRegistered(this)){
            Log.d(TAG, "unregisterConnectionFailedListener");
            mGoogleApiClient.unregisterConnectionFailedListener(this);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        String message = "onConnected:";
        if (bundle != null) message += bundle.toString();
        Log.d(TAG, message);
    }

    @Override
    public void onConnectionSuspended(int i) {
        String message = "onConnectionSuspended:" + i;
        Log.d(TAG, message);
        SendUnityMessage(OnSignInErrorCallback, message);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        String message = "onConnectionFailed:" + connectionResult;
        Log.d(TAG, message);
        SendUnityMessage(OnSignInErrorCallback, message);
        //showErrorToast(message);
    }

    /** end Manual GoogleClient life cycle handling **/

    /** begin Unity interface **/

    public void SignIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void SignOut() {
        if (IsConnected()){
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                mGoogleApiClient.disconnect();
                                acct = null;
                                accountName = null;
                                unregisterCallbacks();
                                SendUnityMessage(OnSignOutSuccessCallback, "");
                            } else {
                                String msg = "StatusCode=" + CommonStatusCodes.getStatusCodeString(status.getStatusCode());
                                SendUnityMessage(OnSignOutErrorCallback, msg);
                                Log.d(TAG, "Sign out failed: " + msg);
                                showErrorToast("Failed to sign out");
                            }
                        }
                    });
        } else {
            SendUnityMessage(OnSignOutErrorCallback, "Cannot sign out when not signed in.");
            //showErrorToast("Failed to sign out");
        }
    }

    public void GetToken() {
        GetNewToken();
    }

    public void GetNewToken() {
        GetV3Token();
    }

    public void GetV3Token(){
        GetIdToken();
    }

    public void GetOldToken(){
        GetV2Token();
    }

    public void GetV2Token(){
        getTokenAsync();
    }

    public Boolean IsConnected(){
        if (mGoogleApiClient == null) {
            Log.d(TAG, "IsConnected: mGoogleApiClient == null");
            return false;
        }
        return mGoogleApiClient.isConnected();
    }

    @Nullable
    public String GetGoogleId() {
        if (acct == null) {
            Log.d(TAG, "GetGoogleId: acct == null");
            return null;
        }
        return acct.getId();
    }

    @Nullable
    public String GetPhotoUrl(){
        if (acct == null) {
            Log.d(TAG, "GetPhotoUrl: acct == null");
            return null;
        }
        if (acct.getPhotoUrl() == null) {
            Log.d(TAG, "GetPhotoUrl: acct.getPhotoUrl() == null");
            return null;
        }
        return acct.getPhotoUrl().toString();
    }

    @Nullable
    public String GetEmail(){
        if (acct == null) {
            Log.d(TAG, "GetEmail: acct == null");
            return null;
        }
        return acct.getEmail();
    }

    @Nullable
    public String GetDisplayName(){
        if (acct == null) {
            Log.d(TAG, "GetDisplayName: acct == null");
            return null;
        }
        return acct.getDisplayName();
    }

    /** end Unity interface **/

    /*** being Helpers ***/

    private void SendUnityMessage(String methodName, String parameter)
    {
        Log.d(TAG, "SendUnityMessage(`"+methodName+"`, `"+parameter+"`)");
        UnityPlayer.UnitySendMessage(UnityObjectName, methodName, parameter);
    }

    private void showErrorToast(String message) {
        Toast.makeText(mainUnityPlayerActivity, message, Toast.LENGTH_SHORT).show();
    }

    private void showErrorDialog(int code) {
        GooglePlayServicesUtil.getErrorDialog(code, mainUnityPlayerActivity,
                REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
    }

    private boolean validateServerClientID(String clientId) {
        String suffix = ".apps.googleusercontent.com";
        if (!clientId.trim().endsWith(suffix)) {
            String message = "Invalid server client ID ("+ clientId +"), must end with " + suffix;
            Log.w(TAG, message);
            showErrorToast(message);
            return false;
        }
        return true;
    }

    private void showAccountPicker() {
        Intent pickAccountIntent = AccountPicker.newChooseAccountIntent(null,
                null, new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE },
                true, null, null, null, null);
        startActivityForResult(pickAccountIntent, REQUEST_CODE_PICK_ACCOUNT);
    }

    public boolean grantPermissionAndShowAccountPicker() {
        Log.d(TAG, "checking permission \"android.permission.GET_ACCOUNTS\"");
        if (ContextCompat.checkSelfPermission(mainUnityPlayerActivity,
                "android.permission.GET_ACCOUNTS") == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        // Should we show an explanation?
        /*else if (ActivityCompat.shouldShowRequestPermissionRationale(mainUnityPlayerActivity,
                "android.permission.GET_ACCOUNTS")) {

            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            return false;
        }*/ else {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(mainUnityPlayerActivity,
                    new String[]{"android.permission.GET_ACCOUNTS"}, RC_REQUEST_PERMISSION);
            return false;
        }
    }

    // Once user allow or deny permissions this method is called
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode != RC_REQUEST_PERMISSION)
        {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted, yay! Do the
            // contacts-related task you need to do.
            Log.d(TAG, "\"android.permission.GET_ACCOUNTS\" PERMISSION_GRANTED");
            showAccountPicker();
        } else {
            // permission denied, boo! Disable the
            // functionality that depends on this permission.
            Log.d(TAG, "\"android.permission.GET_ACCOUNTS\" PERMISSION_DENIED");
            showErrorToast("Google Login Aborted");
        }
    }

    /**
     * This method is a hook for background threads and async tasks that need to
     * provide the user a response UI when an exception occurs.
     */
    public void handleException(final Exception e) {
        // Because this call comes from the AsyncTask, we must ensure that the following
        // code instead executes on the UI thread.
        mainUnityPlayerActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException)e)
                            .getConnectionStatusCode();
                    //showErrorDialog(statusCode);*/
                    String msg = "GooglePlayServicesAvailabilityException connectionStatusCode: " + statusCode;
                    Log.w(TAG, msg);
                    UnityPlayer.UnitySendMessage(UnityObjectName, OnGetTokenV2ErrorCallback, msg);
                    showErrorToast("Internal error has occurred");
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException)e).getIntent();
                    startActivityForResult(intent, REQUEST_CODE_AUTH_CONSENT);
                } else if (e instanceof GoogleAuthException) {
                    // Failure. The call is not expected to ever succeed so it should
                    // not be
                    // retried.
                    String msg = "Unrecoverable authentication exception : " + e.getMessage();
                    Log.e(TAG, msg, e);
                    UnityPlayer.UnitySendMessage(UnityObjectName, OnGetTokenV2ErrorCallback, msg);
                    showErrorToast("Internal error has occurred");
                } else if (e instanceof IOException) {
                    // network or server error, the call is expected to succeed if you
                    // try again later.
                    // Don't attempt to call again immediately - the request is likely
                    // to
                    // fail, you'll hit quotas or back-off.
                    String msg = "transient error encountered : " + e.getMessage();
                    UnityPlayer.UnitySendMessage(UnityObjectName, OnGetTokenV2ErrorCallback, msg);
                    Log.e(TAG, msg, e);
                    //doExponentialBackoff();
                    showErrorToast("Internal error has occurred. Please try again later!");
                } else {
                    String msg = "Unhandled exception : " + e.getMessage();
                    Log.w(TAG, msg, e);
                    UnityPlayer.UnitySendMessage(UnityObjectName, OnGetTokenV2ErrorCallback, msg);
                    showErrorToast("Internal error has occurred");
                }
            }
        });
    }

    /*** end Helpers ***/

}
