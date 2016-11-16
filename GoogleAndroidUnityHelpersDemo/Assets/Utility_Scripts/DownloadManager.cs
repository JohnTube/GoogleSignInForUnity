using System;
using System.Collections;
using UnityEngine;

public class DownloadManager : Singleton<DownloadManager> {

    public void DownloadTexture2dAsync(string url, Action<Texture2D> callback) {
        StartCoroutine(DownloadTexture2D(url, callback));
    }

    private WWW imageDownloader;

    private IEnumerator DownloadTexture2D(string url, Action<Texture2D> callback) {
        if (url.IsUrl()) {
            while (imageDownloader != null) {
                yield return null;
            }
            //Debug.LogFormat("Getting photo from URL = {0}", url);
            imageDownloader = new WWW(url);//WWW.LoadFromCacheOrDownload(url, 1);//
            yield return imageDownloader;
            if (!string.IsNullOrEmpty(imageDownloader.error)) {
                Debug.LogErrorFormat("error : {0} retrieving URL = {1}", imageDownloader.error, url);
            }
            else {
                Texture2D temp = new Texture2D(imageDownloader.texture.width,
                imageDownloader.texture.height, imageDownloader.texture.format, false); //TextureFormat must be DXT5 or DXT1 ??
                //textFb2 = imageDownloader.texture;
                imageDownloader.LoadImageIntoTexture(temp);
                callback(temp);
                //texture = imageDownloader.texture;
            }
            imageDownloader.Dispose();
            imageDownloader = null;
        }
        else {
            Debug.LogErrorFormat("invalid url {0}", url);
        }
    }
}