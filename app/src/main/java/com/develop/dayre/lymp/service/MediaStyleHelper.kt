package com.develop.dayre.lymp.service

import android.content.Context
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat

fun MediaStyleHelperFrom(
    context: Context,
    mediaSession: MediaSessionCompat
): NotificationCompat.Builder {
    val controller = mediaSession.controller
    val mediaMetadata = controller.metadata
    val description = mediaMetadata.description

    val builder = NotificationCompat.Builder(context)
    builder
        .setContentTitle(description.title)
        .setContentText(description.subtitle)
        .setSubText(description.description)
        .setLargeIcon(description.iconBitmap)
        .setContentIntent(controller.sessionActivity)
        .setDeleteIntent(
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_STOP
            )
        )
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    return builder
}