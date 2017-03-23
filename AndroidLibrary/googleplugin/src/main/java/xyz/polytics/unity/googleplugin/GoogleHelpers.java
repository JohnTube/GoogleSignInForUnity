package xyz.polytics.unity.googleplugin;

import android.app.Activity;
import android.app.Fragment;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;

import com.google.android.gms.common.api.Status;
import com.unity3d.player.UnityPlayer;

/**
 * Created by Hamza on 2/16/2016.
 */
public class GoogleHelpers extends Fragment implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "GoogleHelpers";
    public static GoogleHelpers Instance;

    private static final int RC_SIGN_IN = 9001;
    private static final int RC_GET_TOKEN = 9002;
    private static final int RC_GET_AUTH_CODE = 9003;
    private static final int RC_RESOLUTION = 9004;
    private GoogleApiClient mGoogleApiClient;
    private GoogleSignInAccount acct;
    public static Activity mainUnityPlayerActivity = null;

    public static String ClientId = "";

    public static String UnityObjectName = "_GoogleHelpersGO";
    public static String OnTokenV3ReceivedCallback = "OnGoogleGetTokenV3Success";
    public static String OnGetTokenV3ErrorCallback = "OnGoogleGetTokenV3Error";
    public static String OnSignInSuccessCallback = "OnGoogleSignInSuccess";
    public static String OnSignInErrorCallback = "OnGoogleSignInError";
    public static String OnSignOutSuccessCallback = "OnGoogleSignOutSuccess";
    public static String OnSignOutErrorCallback = "OnGoogleSignOutError";
    public static String OnAccessRevokedCallback = "OnGoogleAccessRevoked";
    public static String OnServerTokenReceivedCallback = "OnGoogleServerTokenSuccess";
    public static String OnGetServerTokenErrorCallback = "OnGoogleGetServerTokenError";
    public static String OnAccessRevokeErrorCallback = "OnGoogleAccessRevokeError";
    public static String OnGetPhotoCallback = "OnGooglePhotoAsync";
    public static String OnGetEmailCallback = "OnGoogleEmailAsync";
    public static String OnGetNameCallback = "OnGoogleNameAsync";

    private int asyncAction = 0;
    private static final int GET_PHOTO_ACTION = 1;
    private static final int GET_EMAIL_ACTION = 2;
    private static final int GET_NAME_ACTION = 3;

    private void buildGoogleApiClient() {
        //Log.d(TAG, "buildGoogleApiClient");
        GoogleSignInOptions.Builder builderSignIn;
        if (validateServerClientID(ClientId)){
            builderSignIn = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    //.requestIdToken(ClientId)
                    .requestProfile()
                    //.requestScopes(new Scope(Scopes.PLUS_ME))
                    //.requestScopes(new Scope(Scopes.PLUS_LOGIN))
                    .requestServerAuthCode(ClientId)
                    .requestProfile();
        } else {
            Log.w(TAG, "Getting token v3 will not be possible b/c ClientId is not set properly.");
            return;
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
        if (Instance.mGoogleApiClient == null){
            Log.d(TAG, "Start: mGoogleApiClient == null");
            Instance.buildGoogleApiClient();
        }
        Log.d(TAG, "Start");
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); // Retain between configuration changes (like device rotation)
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
            case RC_GET_AUTH_CODE:
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
                return;

        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            if (IsConnected() && acct != null) {
                if (!GetEmail().equalsIgnoreCase(result.getSignInAccount().getEmail())) {
                    Log.w(TAG, "Accounts mismatch: just connected: "+
                            result.getSignInAccount().getEmail()+", previously connected: "+
                            GetEmail());
                }
            } else {
                mGoogleApiClient.connect();
                // Signed in successfully, show authenticated UI.
                acct = result.getSignInAccount();
                SendUnityMessage(OnSignInSuccessCallback, GetGoogleId());
                Log.d(TAG, "handleSignInResult success");
            }
            switch (asyncAction){
                case GET_PHOTO_ACTION:
                    SendUnityMessage(OnGetPhotoCallback, GetPhotoUrl());
                    break;
                case GET_EMAIL_ACTION:
                    SendUnityMessage(OnGetEmailCallback, GetEmail());
                    break;
                case GET_NAME_ACTION:
                    SendUnityMessage(OnGetNameCallback, GetDisplayName());
                    break;
            }
        } else if (result.getStatus().getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED){
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(signInIntent, RC_SIGN_IN);
            return;
        } else if (result.getStatus().hasResolution()) {
            try {
                result.getStatus().startResolutionForResult(mainUnityPlayerActivity, RC_RESOLUTION);
                return;
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
        asyncAction = 0;
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
        Log.d(TAG, "trying silentSignIn");
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
                    Log.d(TAG, "SilentSignInAsyncCallback");
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }

    public void SilentSignIn(){
        if (!IsConnected()){
            silentSignIn();
        } else {
            Log.d(TAG, "SilentSignIn cant be called:" + GetEmail()+ " ("+ GetDisplayName() + ") already connected");
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

    public void GetServerToken() {
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

    // TODO: check if already connected
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

    public Boolean IsConnected(){
        if (mGoogleApiClient == null) {
            Log.d(TAG, "IsConnected: mGoogleApiClient == null");
            return false;
        }
        return mGoogleApiClient.isConnected();
    }

    public Boolean IsConnectedB(){
        return IsConnected();
    }

    public String IsConnectedS(){
        if (IsConnectedB()){
            return "true";
        }
        return "false";
    }

    public int IsConnectedI(){
        if (IsConnectedB()){
            return 1;
        }
        return 0;
    }

    public void IsConnectedV(){
        SendUnityMessage("IsGoogleConnectedAsync", GetGoogleId());
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
            asyncAction = GET_PHOTO_ACTION;
            SignIn();
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
            asyncAction = GET_EMAIL_ACTION;
            SignIn();
            return null;
        }
        return acct.getEmail();
    }

    @Nullable
    public String GetDisplayName(){
        if (acct == null) {
            Log.d(TAG, "GetDisplayName: acct == null");
            asyncAction = GET_NAME_ACTION;
            SignIn();
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
        if (mainUnityPlayerActivity == null){
            mainUnityPlayerActivity = UnityPlayer.currentActivity;
        }
        Toast.makeText(mainUnityPlayerActivity, message, Toast.LENGTH_SHORT).show();
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
}
