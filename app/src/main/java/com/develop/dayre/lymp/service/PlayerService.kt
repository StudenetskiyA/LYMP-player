package com.develop.dayre.lymp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import com.develop.dayre.lymp.*
import com.develop.dayre.lymp.App.Companion.exoPlayer

import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util

class PlayerService : Service() {
    private val NOTIFICATION_ID = 404
    private val NOTIFICATION_DEFAULT_CHANNEL_ID = "default_channel"

    private val metadataBuilder = MediaMetadataCompat.Builder()

    private val stateBuilder = PlaybackStateCompat.Builder().setActions(
        PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
    )

    private var mediaSession: MediaSessionCompat? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusRequested = false
    private val musicRepository = MusicRepository()

    private val mediaSessionCallback: MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {
            private var currentUri: Uri? = null
            var currentState = PlaybackStateCompat.STATE_STOPPED

            override fun onPlay() {
                if (!exoPlayer.playWhenReady) {
                    startService(Intent(applicationContext, PlayerService::class.java))

                    val track = musicRepository.getCurrentSong() ?: return

                    updateMetadataFromTrack(track)
                    prepareToPlay(track.path)

                    if (!audioFocusRequested) {
                        audioFocusRequested = true

                        val audioFocusResult: Int =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                audioManager!!.requestAudioFocus(audioFocusRequest!!)
                            } else {
                                audioManager!!.requestAudioFocus(
                                    audioFocusChangeListener,
                                    AudioManager.STREAM_MUSIC,
                                    AudioManager.AUDIOFOCUS_GAIN
                                )
                            }
                        if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                            return
                    }

                    mediaSession?.isActive = true // Сразу после получения фокуса

                    registerReceiver(
                        becomingNoisyReceiver,
                        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                    )

                    exoPlayer?.playWhenReady = true
                }

                mediaSession?.setPlaybackState(
                    stateBuilder.setState(
                        PlaybackStateCompat.STATE_PLAYING,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1f
                    ).build()
                )
                currentState = PlaybackStateCompat.STATE_PLAYING

                refreshNotificationAndForegroundStatus(currentState)
            }

            override fun onPause() {
                if (exoPlayer!!.playWhenReady) {
                    exoPlayer!!.playWhenReady = false
                    unregisterReceiver(becomingNoisyReceiver)
                }

                mediaSession!!.setPlaybackState(
                    stateBuilder.setState(
                        PlaybackStateCompat.STATE_PAUSED,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1f
                    ).build()
                )
                currentState = PlaybackStateCompat.STATE_PAUSED

                refreshNotificationAndForegroundStatus(currentState)
            }

            override fun onStop() {
                if (exoPlayer.playWhenReady) {
                    exoPlayer.playWhenReady = false
                    exoPlayer.stop()
                    unregisterReceiver(becomingNoisyReceiver)
                }

                if (audioFocusRequested) {
                    audioFocusRequested = false

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)
                    } else {
                        audioManager!!.abandonAudioFocus(audioFocusChangeListener)
                    }
                }

                mediaSession!!.isActive = false

                mediaSession!!.setPlaybackState(
                    stateBuilder.setState(
                        PlaybackStateCompat.STATE_STOPPED,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1f
                    ).build()
                )
                currentState = PlaybackStateCompat.STATE_STOPPED

                refreshNotificationAndForegroundStatus(currentState)

                stopSelf()
            }

            override fun onSkipToNext() {
                if ((exoPlayer!!.currentPosition * 2) > exoPlayer!!.duration)
                    App.viewModel.increaseListenedTimeOfCurrentTrack()

                val track = musicRepository.getNextSong() ?: return
                updateMetadataFromTrack(track)

                refreshNotificationAndForegroundStatus(currentState)

                prepareToPlay(track.path)
            }

            override fun onSkipToPrevious() {
                val track = musicRepository.getPreviousSong() ?: return
                updateMetadataFromTrack(track)

                refreshNotificationAndForegroundStatus(currentState)

                prepareToPlay(track.path)
            }

            private fun prepareToPlay(url: String) {
                val uri = Uri.parse(url)
                if (uri != currentUri || currentState == PlaybackStateCompat.STATE_STOPPED ) {
                    currentUri = uri
                    val dataSourceFactory =  if (url.startsWith("http"))
                        DefaultHttpDataSourceFactory(
                            Util.getUserAgent(
                                applicationContext,
                                "Lymp"
                            )
                        ) else
                        DefaultDataSourceFactory(
                            applicationContext,
                            Util.getUserAgent(applicationContext, "Lymp")
                        )
                    val extractorsFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
                    val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory,extractorsFactory)
                        .createMediaSource(uri)
                    exoPlayer!!.prepare(mediaSource)
                }
            }

            private fun updateMetadataFromTrack(track: Song) {
//            metadataBuilder.putBitmap(
//                MediaMetadataCompat.METADATA_KEY_ART,
//                BitmapFactory.decodeResource(resources, track.bitmapResId)
//            )
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.name)
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.name)
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.name)
                metadataBuilder.putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    track.lenght.toLong()
                )
                mediaSession!!.setMetadata(metadataBuilder.build())
            }
        }

    private val audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> mediaSessionCallback.onPlay()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaSessionCallback.onPause()
                else -> mediaSessionCallback.onPause()
            }
        }

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                mediaSessionCallback.onPause()
            }
        }
    }

    private val exoPlayerListener = object : Player.EventListener {
        override fun onTracksChanged(
            trackGroups: TrackGroupArray?,
            trackSelections: TrackSelectionArray?
        ) {
            App.viewModel.setCurrentSong()
        }

        override fun onLoadingChanged(isLoading: Boolean) {}

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playWhenReady && playbackState == ExoPlayer.STATE_ENDED) {
                App.viewModel.increaseListenedTimeOfCurrentTrack()
                mediaSessionCallback.onSkipToNext()
            }
        }

        override fun onPlayerError(error: ExoPlaybackException?) {}

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_DEFAULT_CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setAcceptsDelayedFocusGain(false)
                //TODO Может делать тише, когда что-то крякает, а не на паузу?
                .setWillPauseWhenDucked(true)
                .setAudioAttributes(audioAttributes)
                .build()
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        mediaSession = MediaSessionCompat(this, "LYMPservice")
        mediaSession?.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession?.setCallback(mediaSessionCallback)

        val activityIntent = Intent(applicationContext, MainActivity::class.java)
        mediaSession?.setSessionActivity(
            PendingIntent.getActivity(
                applicationContext,
                0,
                activityIntent,
                0
            )
        )

        val mediaButtonIntent =
            Intent(Intent.ACTION_MEDIA_BUTTON, null, applicationContext, MediaButtonReceiver::class.java)
        mediaSession!!.setMediaButtonReceiver(
            PendingIntent.getBroadcast(
                applicationContext,
                0,
                mediaButtonIntent,
                0
            )
        )

        exoPlayer = ExoPlayerFactory.newSimpleInstance(
            this,
            DefaultRenderersFactory(this),
            DefaultTrackSelector(),
            DefaultLoadControl()
        )
        exoPlayer!!.addListener(exoPlayerListener)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        exoPlayer?.release()
    }

    override fun onBind(intent: Intent): IBinder? {
        return PlayerServiceBinder()
    }

    inner class PlayerServiceBinder : Binder() {
        val mediaSessionToken: MediaSessionCompat.Token
            get() = mediaSession!!.sessionToken
    }

    private fun refreshNotificationAndForegroundStatus(playbackState: Int) {
        when (playbackState) {
            PlaybackStateCompat.STATE_PLAYING -> {
                startForeground(NOTIFICATION_ID, getNotification(playbackState))
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                NotificationManagerCompat.from(this@PlayerService)
                    .notify(NOTIFICATION_ID, getNotification(playbackState))
                stopForeground(false)
            }
            else -> {
                stopForeground(true)
            }
        }
    }

    private fun getNotification(playbackState: Int): Notification {
        val builder = MediaStyleHelperFrom(this, mediaSession!!)
        builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_previous,
                getString(R.string.previous),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
            )
        )

        if (playbackState == PlaybackStateCompat.STATE_PLAYING)
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause,
                    getString(R.string.pause),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )
        else
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_play,
                    getString(R.string.play),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )

        builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                getString(R.string.next),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
            )
        )
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
                .setMediaSession(mediaSession!!.sessionToken)
        ) // setMediaSession требуется для Android Wear
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setColor(
            ContextCompat.getColor(
                this,
                R.color.colorPrimaryDark
            )
        ) // The whole background (in MediaStyle), not just icon background
        builder.setShowWhen(false)
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        builder.setOnlyAlertOnce(true)
        builder.setChannelId(NOTIFICATION_DEFAULT_CHANNEL_ID)

        return builder.build()
    }
}
