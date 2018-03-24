package com.softartdev.poder.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.RawRes
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatDelegate
import com.softartdev.poder.R

object ViewUtil {
/*
    fun getBitmapFromVectorDrawable(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.apply {
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
        }
        return bitmap
    }
*/
    fun getDrawableFromVector(context: Context, drawableRes: Int): Drawable? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        ContextCompat.getDrawable(context, drawableRes)?.let { DrawableCompat.wrap(it).mutate() }
    } else {
        context.getDrawable(drawableRes)
    }

    fun getDefaultAlbumArt(context: Context): Bitmap {
        val opts = BitmapFactory.Options()
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888
        @RawRes val artRes = R.drawable.albumart_mp_unknown
        return BitmapFactory.decodeStream(context.resources.openRawResource(artRes), null, opts)
    }

}
