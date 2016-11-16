using UnityEngine;

[DisallowMultipleComponent]
public class GoogleHelpersWrapper : MonoBehaviour {

    private const string className = "xyz.polytics.unity.googleplugin.GoogleHelpers";

    private static AndroidJavaClass activityClass;
    private static AndroidJavaObject instance { get { return activityClass.GetStatic<AndroidJavaObject>("Instance"); } } // TODO: cleanup, dispose?

    private static string ClientId = "<PASTE_CLIENT_ID_HERE>.apps.googleusercontent.com"; // WebApplication credentials

    private void Init() {
#if UNITY_EDITOR
        return;
#elif UNITY_ANDROID
        Debug.Log("GoogleHelpersWrapper Init");
        //AndroidJNI.AttachCurrentThread();
        activityClass = new AndroidJavaClass(className);
        setUnityObjectName(gameObject.name);
        setClientId(ClientId);
        activityClass.CallStatic("Start");
#endif
    }

    private void setUnityObjectName(string objectName) {
#if UNITY_EDITOR
        return;
#elif UNITY_ANDROID
        activityClass.SetStatic("UnityObjectName", objectName);
#endif
    }

    private void setClientId(string clientId) {
#if UNITY_EDITOR
        return;
#elif UNITY_ANDROID
        activityClass.SetStatic("ClientId", clientId);
#endif
    }

    private void Awake() {
        Debug.Log("GoogleHelpersWrapper Awake");
        Init();
    }

    private void OnDestroy() {
        if (activityClass != null) {
            activityClass.Dispose();
        }
    }

    public void GetV3Token() {
#if UNITY_ANDROID && !UNITY_EDITOR
        instance.Call("GetNewToken");
#endif
    }

    public void GetV2Token()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        instance.Call("GetV2Token");
#endif
    }

    //    public void GetServerToken() {
    //#if UNITY_ANDROID && !UNITY_EDITOR
    //        instance.Call("GetServerToken");
    //#endif
    //    }

    public void SignIn() {
#if UNITY_ANDROID && !UNITY_EDITOR
        instance.Call("SignIn");
#endif
    }

    public void SignOut() {
#if UNITY_ANDROID && !UNITY_EDITOR
        instance.Call("SignOut");
#endif
    }

    public void RevokeAccess() {
#if UNITY_ANDROID && !UNITY_EDITOR
        instance.Call("RevokeAccess");
#endif
    }

    public string GetPhotoUrl() {
#if UNITY_EDITOR
        return string.Empty;
#elif UNITY_ANDROID
        return instance.Call<string>("GetPhotoUrl");
#endif
    }

    public string GetDisplayName() {
#if UNITY_EDITOR
        return string.Empty;
#elif UNITY_ANDROID
        return instance.Call<string>("GetDisplayName");
#endif
    }

    public bool IsSignedIn() {
#if UNITY_EDITOR
        return false;
#elif UNITY_ANDROID
        return instance.Call<bool>("IsConnected");
#endif
    }

    public string GetEmail()
    { 
#if UNITY_EDITOR
        return string.Empty;
#elif UNITY_ANDROID
        return instance.Call<string>("GetEmail");
#endif
    }
}