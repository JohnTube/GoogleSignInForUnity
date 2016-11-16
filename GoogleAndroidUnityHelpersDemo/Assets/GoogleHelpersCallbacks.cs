using UnityEngine;
using UnityEngine.UI;

public class GoogleHelpersCallbacks : MonoBehaviour, IGoogleHelpersCallbacks
{
    [SerializeField] private GoogleHelpersWrapper googleHelpersWrapper;
    [SerializeField] private Text DisplayNameText;
    [SerializeField] private Text EmailText;
    [SerializeField] private Text TokenText;
    [SerializeField] private Image PhotoImage;

    public void OnGoogleGetTokenV3Success(string v3Token)
    {
        Debug.LogFormat("OnGoogleGetTokenV3Success: {0}", v3Token);
        TokenText.text = string.Format("TokenV3={0}", v3Token);
    }

    public void OnGoogleGetTokenV3Error(string error)
    {
        Debug.LogErrorFormat("OnGoogleGetTokenV3Error: {0}", error);
    }

    public void OnGoogleGetTokenV2Success(string v2Token)
    {
        Debug.LogFormat("OnGoogleGetTokenV2Success: {0}", v2Token);
        TokenText.text = string.Format("TokenV2={0}", v2Token);
    }

    public void OnGoogleGetTokenV2Error(string error)
    {
        Debug.LogErrorFormat("OnGoogleGetTokenV2Error: {0}", error);
    }

    public void OnGoogleSignInSuccess(string googleId)
    {
        Debug.LogFormat("OnGoogleSignInSuccess: googleId={0}", googleId);
        EmailText.text = googleHelpersWrapper.GetEmail();
        DisplayNameText.text = googleHelpersWrapper.GetDisplayName();
        DownloadManager.Instance.DownloadTexture2dAsync(googleHelpersWrapper.GetPhotoUrl(),
            texture => PhotoImage.SetTexture2D(texture));
    }

    public void OnGoogleSignInError(string error)
    {
        Debug.LogErrorFormat("OnGoogleSignInError: {0}", error);
    }

    public void OnGoogleSignOutSuccess()
    {
        Debug.Log("OnGoogleSignOutSuccess");
        DisplayNameText.text = "";
        PhotoImage.overrideSprite = null;
        EmailText.text = "";
        TokenText.text = "";
    }

    public void OnGoogleSignOutError(string error)
    {
        Debug.LogErrorFormat("OnGoogleSignOutError: {0}", error);
    }
}
