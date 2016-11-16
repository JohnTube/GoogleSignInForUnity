public interface IGoogleHelpersCallbacks
{
    void OnGoogleGetTokenV3Success(string v3Token);
    void OnGoogleGetTokenV3Error(string error);
    void OnGoogleGetTokenV2Success(string v2Token);
    void OnGoogleGetTokenV2Error(string error);
    void OnGoogleSignInSuccess(string googleId);
    void OnGoogleSignInError(string error);
    void OnGoogleSignOutSuccess();
    void OnGoogleSignOutError(string error);
}
