using System;
using UnityEngine;
using UnityEngine.UI;

public static class ExtensionMethods {

    public static bool IsNull<T>(this T o) {
        return o == null;
    }
    
    public static bool IsUrl(this string source) {
        if (string.IsNullOrEmpty(source)) {
            return false;
        }
        Uri uriResult;
        return Uri.TryCreate(source, UriKind.Absolute, out uriResult)
            && (uriResult.Scheme == Uri.UriSchemeHttp || uriResult.Scheme == Uri.UriSchemeHttps);
    }

    public static void SetTexture2D(this Image image, Texture2D texture) {
        if (!image.IsNull())
        {
            image.overrideSprite = (texture == null) ? null :
            Sprite.Create(
                texture,
                new Rect(0, 0, texture.width, texture.height),
                new Vector2(0.5F, 0.5F)
            );
        } 
    }
}