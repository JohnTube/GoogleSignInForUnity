using System.Collections;
using UnityEngine;

public class GooglePlusWrapper : MonoBehaviour {
#if !UNITY_EDITOR && UNITY_ANDROID
    private const string className = "com.ThugLeaf.GooglePlusForUnity.GooglePlusActivity";

    public static int SIGN_IN_REASON = 1;
    public static int GET_TOKEN_REASON = 2;
    public static int SIGN_OUT_REASON = 3;
    public static int LOAD_CIRCLES_REASON = 4;
    public static int REVOKE_ACCESS_REASON = 5;
    public static int INVALIDATE_TOKEN_REASON = 6;

    private static AndroidJavaClass activityClass;

    private static string ClientId = ""; // put your ClientID here

    private void Init() {
        //Debug.Log("GooglePlusWrapper Init");
        AndroidJNI.AttachCurrentThread();
        if (activityClass == null) {
            activityClass = new AndroidJavaClass(className);
        }
        SetUnityObjectName(this.gameObject.name);
        activityClass.SetStatic<string>("ClientId", GooglePlusWrapper.ClientId);
    }

    public void StartActivityForReason(int reason) {
        activityClass.CallStatic("Start", new object[] { reason });
    }

    public void SetUnityObjectName(string objectName) {
        activityClass.SetStatic<string>("UnityObjectName", objectName);
    }

    private void Awake() {
        //Debug.Log("GooglePlusWrapper Awake");
        Init();
    }

    private void OnDestroy() {
        if (activityClass != null) {
            activityClass.Dispose();
        }
    }

    public void GetToken() {
        StartActivityForReason(GET_TOKEN_REASON);
    }

    public void SignIn() {
        StartActivityForReason(SIGN_IN_REASON);
    }

    public void SignOut() {
        StartActivityForReason(SIGN_OUT_REASON);
    }

    public void LoadCircles() {
        StartActivityForReason(LOAD_CIRCLES_REASON);
    }

    public void RevokeAccess() {
        StartActivityForReason(REVOKE_ACCESS_REASON);
    }

    public void InvalidateToken() {
        StartActivityForReason(INVALIDATE_TOKEN_REASON);
    }

    public string GetProfilePictureUrl() {
        return activityClass.CallStatic<string>("GetProfilePictureUrl");
    }

    public string GetDisplayName() {
        return activityClass.CallStatic<string>("GetDisplayName");
    }

    public bool IsSignedIn() {
        return activityClass.CallStatic<bool>("isConnected");
    }

#endif
}