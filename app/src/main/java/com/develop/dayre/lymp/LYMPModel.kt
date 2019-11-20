package com.develop.dayre.lymp

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.databinding.BaseObservable
import io.reactivex.Observable
import kotlin.collections.ArrayList
import android.media.AudioManager
import android.support.v4.media.session.MediaControllerCompat
import androidx.media.session.MediaButtonReceiver
import androidx.databinding.ObservableField
import java.text.SimpleDateFormat
import java.util.*


enum class RepeatState { All, One, Stop }
enum class PlayState { Play, Stop, Pause }
enum class SortState { ByName, ByAdded, ByListened }
enum class AndOrState { Or, And }

class LYMPModel(private val audioManager: AudioManager) : BaseObservable() {
    var callBackAwaited: Boolean = false
    private var mediaController: MediaControllerCompat? = null
    private val TAG = "$APP_TAG/model"
    private var currentSong: Song? =
        Song() //У нас бывают ситуации, когда текущий трек не в текущем листе.
    private var currentSongsList = ArrayList<Song>()
    private var currentSongsShuffledListNumber = ArrayList<Int>()
    private var currentSongsAditionListNumber = ArrayList<Int>()

    var helper: RealmHelper = App.realmHelper
    private var searchTags = ";"
    private var antiSearchTags = ";"
    private var searchName = ""
    private var shuffleStatus: Boolean = false
    private var repeatStatus = RepeatState.All
    private var sortStatus = SortState.ByName
    private var andOrStatus = AndOrState.Or

    private var searchMinRating = 0

    private var currentSongPositionInList: Int = 0
        set(value) {
            if (value < currentSongsList.size) {
                field = value
                currentSong = currentSongsList[currentSongPositionInList]
            }
        }
    lateinit var mediaSession: MediaSessionCompat
    private lateinit var exoPlayer: MediaPlayer
    val duration = ObservableField<Int>()
    var isPlaying = PlayState.Stop
    var allTags = ""
    lateinit var context: Context

    init {
        Log.d(TAG,"model init")
    }

    //Методы для обсерверов
    fun getShuffleStatus(): Boolean {
        return shuffleStatus
    }
    fun getRepeatStatus(): RepeatState {
        return repeatStatus
    }
    fun getSortStatus(): SortState {
        return sortStatus
    }
    fun getAndOrStatus(): AndOrState {
        return andOrStatus
    }
    fun getPlayState(): PlayState { return isPlaying}
    fun getCurrentSong(): Observable<SongOrNull> {
        return if (currentSongPositionInList < currentSongsList.size && currentSongPositionInList >= 0)
            Observable.just(SongOrNull(currentSongsList[currentSongPositionInList]))

        //In Rx2 I can't do Observable.just(null)!
        else Observable.just(SongOrNull(Song(), true))
    }
    fun getCurrentSearchTags(): Observable<String> {
        return Observable.just(searchTags)
    }
    fun getCurrentAntiSearchTags(): Observable<String> {
        return Observable.just(antiSearchTags)
    }

    //Действия
    fun browseFolderForFiles(): Observable<Boolean> {
        var newSongFound = 0
        var songsRestored = 0
        var deletedSong = 0
        val dateTime = SimpleDateFormat("dd/M/yyyy HH:mm").format(Date())
        //Получаем список всех файлов в папке и подпапках
        //TODO Поменять путь на недефолтный при загрузке из настроек.
        val allFiles = getFilesListInFolderAndSubFolder(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "mp3"
        )
        Log.i(TAG, "/browse - File found ${allFiles.size}")
        val allRecord = helper.getAllSongsFileExist()
        Log.i(TAG, "/browse - Record with isExist found ${allRecord.size}")
        //Проверяем, каких файлов больше физически нет и помечаем их как отсутствующие, из базы не удаляем.
        for (r in allRecord) {
            if (!allFiles.contains(r.path)) {
                deletedSong++
                val w = r.copy()
                w.isFileExist = false
                helper.writeSong(w)
            }
        }
        //Проверяем, каких нет в базе или они помечены isFileExist=false и добавляем их.
        for (f in allFiles) {
            val s = helper.getSongByID(getHashFromNameAndSize(f.getNameFromPath(), getFileSize(f)))
            if (s == null) {
                newSongFound++
                helper.writeSong(
                    Song(
                        ID = getHashFromNameAndSize(
                            f.getNameFromPath(),
                            getFileSize(f)
                        ),
                        name = f.getNameFromPath(),
                        path = f,
                        isFileExist = true,
                        added = dateTime,
                        lenght = getAudioFileDuration(f)
                    )
                )
            }
            if (s != null && !s.isFileExist) {
                songsRestored++
                s.isFileExist = true
                s.path = f
                s.added = dateTime
                helper.writeSong(s)
            }
        }
        //И уведомление с результатами
        if (songsRestored!=0 || newSongFound!=0 || deletedSong!=0)
            context
                .toast("Новых песен найдено - $newSongFound \r\nУдалено песен - $deletedSong \r\nВосстановленно удаленных - $songsRestored")
        Log.i(
            TAG,
            "Новых песен найдено - $newSongFound \r\nУдалено песен - $deletedSong \r\nВосстановленно удаленных - $songsRestored"
        )
        return Observable.just(true)
    }

    fun setCurrentSongByID(songID: String) {
        val s = helper.getSongByID(songID)
        if (s != null && currentSongsList.contains(s)) {
            currentSong = s
            currentSongPositionInList = currentSongsList.indexOf(s)
        }
        prepareTrackForPlayer()
    }
    fun setPositionInList(position: Int) {
        currentSongPositionInList = position
        if (isPlaying == PlayState.Play) doWithMedia("next")
    }

    fun clearTag() {
        val song = currentSong?.copy()
        if (song != null) {
            song.tags = ";"
            helper.writeSong(song)
            createCurrentList() //После удаления тегов трека он может пропасть/появиться в листе.
        }
    }

    fun newSearch(tags: String = "", antiTags: String = "", searchName: String = "", fromAdditionList:Boolean = false): Observable<ArrayList<Song>> {
        Log.i(TAG, "new search")
        searchTags = tags
        antiSearchTags = antiTags
        this.searchName = searchName
        if (!fromAdditionList) clearAdditionList()
        createCurrentList()
        return Observable.just(currentSongsList)
    }

    fun saveSongToDB(song: Song) {
        helper.writeSong(song)
        Log.i(TAG, "Song with name ${song.name}, tags ${song.tags} and rating ${song.rating} saved to DB")
    }

    fun nextSong(doNotIncreaseListenedTimes: Boolean = false) {
        if (currentSongsList.isNotEmpty()) {
            if (!doNotIncreaseListenedTimes && currentSong != null && (exoPlayer.currentPosition * 2) > exoPlayer.duration) {
                val cs = currentSong!!.copy()
                cs.listenedTimes += 0.5
                helper.writeSong(cs)
            }

            currentSongPositionInList = getNextPositionInList(
                currentSongsShuffledListNumber, currentSongsAditionListNumber,
                currentSongPositionInList,
                shuffleStatus
            )
            if (currentSongsAditionListNumber.isNotEmpty()) {
                for (position in currentSongsAditionListNumber) {
                    val s = currentSongsList[position].copy()
                    s.order = s.order - 1
                    helper.writeSong(s)
                }
                currentSongsAditionListNumber.removeAt(0)
            }

            Log.i(
                TAG, "Next track ${currentSong?.name} / ${currentSong?.tags}"
            )
            doWithMedia("next")
        }
    }
    fun prevSong() {
        if (currentSongsList.isNotEmpty()) {
            if (currentSong != null && (exoPlayer.currentPosition * 2) > exoPlayer.duration) {
                val cs = currentSong!!.copy()
                cs.listenedTimes += 0.5
                helper.writeSong(cs)
            }
            currentSongPositionInList = getPrevPositionInList(
                currentSongsShuffledListNumber,
                currentSongPositionInList,
                shuffleStatus
            )
            Log.i(
                TAG, "New track ${currentSong?.name} / ${currentSong?.tags}"
            )

            doWithMedia("next")
        }
    }

    private fun prepareTrackForPlayer() {
        val metadata = metadataBuilder
//            .putBitmap(
//                MediaMetadataCompat.METADATA_KEY_ART,
//                BitmapFactory.decodeResource(resources, track.bitmapResId)
//            )
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong?.name)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentSong?.name)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong?.name)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentSong?.lenght!!.toLong())
            .build()
        mediaSession.setMetadata(metadata)
        //Берем аудиофокус
        val audioFocusResult = audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            return

        // Указываем, что наше приложение теперь активный плеер и кнопки
        // на окне блокировки должны управлять именно нами
        mediaSession.isActive = true

        // Загружаем URL аудио-файла в Player
        if (currentSong != null && currentSong?.path != "") {
            exoPlayer =
                MediaPlayer.create(context, Uri.parse(currentSong?.path))
            duration.set(exoPlayer.duration)
            exoPlayer.setOnCompletionListener {
                Log.i(TAG, "Track complete")
                if (currentSong != null) {
                    val cs = currentSong!!.copy()
                    cs.listenedTimes++
                    helper.writeSong(cs)
                }

                //TODO Модель не должна знать о вью-модели!
                when (repeatStatus) {
                    RepeatState.All -> {
                        nextSong(true)
                        App.viewModel.setCurrentSong()
                    }
                    RepeatState.One -> {
                        stop()
                        play()
                        App.viewModel.setCurrentSong()
                    }
                    RepeatState.Stop -> {
                        stop()
                        App.viewModel.setCurrentSong()
                    }
                }
            }
        }
    }
    private fun doWithMedia(s: String) {
        callBackAwaited = true
        when (s) {
            "play" -> {
                if (isPlaying != PlayState.Pause)
                    prepareTrackForPlayer()
                //  prepareTrackLoadToPlayer()
                // Запускаем воспроизведение
                exoPlayer.start()
                mediaController?.transportControls?.play()
                // Сообщаем новое состояние
                mediaSession.setPlaybackState(
                    stateBuilder.setState(
                        PlaybackStateCompat.STATE_PLAYING,
                        exoPlayer.currentPosition.toLong(), 1f
                    ).build()
                )
                isPlaying = PlayState.Play
            }
            "pause" -> {
                exoPlayer.pause()
                mediaController?.transportControls?.pause()
                // Сообщаем новое состояние
                mediaSession.setPlaybackState(
                    stateBuilder.setState(
                        PlaybackStateCompat.STATE_PAUSED,
                        exoPlayer.currentPosition.toLong(), 1f
                    ).build()
                )
                isPlaying = PlayState.Pause
            }
            "next" -> {
                exoPlayer.stop()
                if (isPlaying == PlayState.Pause || isPlaying == PlayState.Stop) {
                    Log.i(TAG, "next/here")
                    //Новый файл не загружаем в плеер,
                    prepareTrackForPlayer()
                    //Если на паузе мы меняем трек - новый не запускается, прогресс старого сбрасывается.
                    isPlaying = PlayState.Stop
                    // Сообщаем новое состояние
                    mediaController?.transportControls?.skipToNext()
                    mediaSession.setPlaybackState(
                        stateBuilder.setState(
                            PlaybackStateCompat.STATE_SKIPPING_TO_NEXT,
                            0, 1f
                        ).build()
                    )
                    //mediaSession.isActive = false
                } else {
                    isPlaying = PlayState.Stop
                    doWithMedia("play")
                }
            }
            "stop" -> {
                // Останавливаем воспроизведение
                exoPlayer.stop()
                isPlaying = PlayState.Stop
                // Все, больше мы не "главный" плеер, уходим со сцены
                mediaSession.isActive = false
                // Сообщаем новое состояние
                mediaSession.setPlaybackState(
                    stateBuilder.setState(
                        PlaybackStateCompat.STATE_STOPPED,
                        0, 1f
                    ).build()
                )
            }
        }
    }

    fun play()  {
        Log.i(TAG, "Play, isPlaying =  $isPlaying")
        if (isPlaying == PlayState.Play) {
            doWithMedia("pause")
        } else {
            doWithMedia("play")
        }
    }

    fun stop() {
        if (isPlaying != PlayState.Stop) doWithMedia("stop")
    }

    fun changeShuffle() {
        shuffleStatus = !shuffleStatus
        currentSongsShuffledListNumber = getShuffledListOfInt(currentSongsList.size)
        App.appSettings.edit()
            .putBoolean(APP_PREFERENCES_SHUFFLE, shuffleStatus)
            .apply()
        Log.i(TAG, "Now shuffle is $shuffleStatus")
    }
    fun changeRepeat() {
        //TODO
        var rs = 0
        when (repeatStatus) {
            RepeatState.All -> {
                repeatStatus = RepeatState.One
                rs = 1
            }
            RepeatState.One -> {
                repeatStatus = RepeatState.Stop
                rs = 2
            }
            RepeatState.Stop -> {
                repeatStatus = RepeatState.All
            }
        }
        App.appSettings.edit()
            .putInt(APP_PREFERENCES_REPEAT, rs)
            .apply()
        Log.i(TAG, "Now repeat is $repeatStatus")
    }
    fun changeSort() {
        //TODO
        var ss = 0
        when (sortStatus) {
            SortState.ByName -> {
                sortStatus = SortState.ByListened
                ss = 1
            }
            SortState.ByListened -> {
                sortStatus = SortState.ByAdded
                ss = 2
            }
            SortState.ByAdded -> {
                sortStatus = SortState.ByName
            }
        }
        App.appSettings.edit()
            .putInt(APP_PREFERENCES_SORT, ss)
            .apply()
        createCurrentList()
        Log.i(TAG, "Now sortStatus is $sortStatus")
    }
    fun changeAndOr() {
        andOrStatus =
        when (andOrStatus) {
            AndOrState.Or -> {
                 AndOrState.And
            }
            AndOrState.And -> {
                AndOrState.Or
            }
        }
        App.appSettings.edit()
            .putInt(APP_PREFERENCES_ANDOR, andOrStatus.ordinal)
            .apply()
        createCurrentList()
        Log.i(TAG, "Now andOrStatus is $andOrStatus")
    }
    fun setShuffleStatus(newShuffleStatus: Boolean) {
        shuffleStatus = newShuffleStatus
    }
    fun setRepeatStatus(newRepeatStatus: RepeatState) {
        repeatStatus = newRepeatStatus
    }
    fun setSortStatus(newSortStatus: SortState) {
        sortStatus = newSortStatus
        createCurrentList()
    }
    fun setAndOrStatus(newAndOrStatus: AndOrState) {
        andOrStatus = newAndOrStatus
        createCurrentList()
    }
    fun setSearchRating(rating: Int, withoutNewSearch: Boolean = false) {
        searchMinRating = rating
        App.appSettings.edit()
            .putInt(APP_PREFERENCES_SEARCH_MIN_RATING, searchMinRating)
            .apply()
        if (!withoutNewSearch) createCurrentList()
    }


    fun testAction() {
        Log.i(TAG, "testAction")
    }

    fun getMediaSessionToken(): MediaSessionCompat.Token {
        return mediaSession.sessionToken
    }

    fun setMediaSessonCallback(mediaSessionCallback: MediaSessionCompat.Callback) {
        mediaSession.setCallback(mediaSessionCallback)
    }

    fun setMediaControllerCallback(mediaControllerCallback: MediaControllerCompat.Callback) {
        mediaController?.registerCallback(mediaControllerCallback)
    }

    fun initialize(context: Context) {
        Log.i(TAG, "initialization")

        this.context = context
        mediaSession = MediaSessionCompat(context, "LYMPService")
        // FLAG_HANDLES_MEDIA_BUTTONS - хотим получать события от аппаратных кнопок
        // (например, гарнитуры)
        // FLAG_HANDLES_TRANSPORT_CONTROLS - хотим получать события от кнопок
        // на окне блокировки
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        val mediaButtonIntent = Intent(
            Intent.ACTION_MEDIA_BUTTON,
            null,
            context,
            MediaButtonReceiver::class.java
        )
        mediaSession.setMediaButtonReceiver(
            PendingIntent.getBroadcast(context, 0, mediaButtonIntent, 0)
        )
        // Укажем activity, которую запустит система, если пользователь
        // заинтересуется подробностями данной сессии
        mediaSession.setSessionActivity(
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        mediaController = MediaControllerCompat(context, getMediaSessionToken()
        )
    }

    private fun clearAdditionList() {
        for (position in currentSongsAditionListNumber) {
            val s = currentSongsList[position].copy()
            s.order=0
            helper.writeSong(s)
        }
        currentSongsAditionListNumber = ArrayList()
    }

    //Private
    private fun createCurrentList() {
        currentSongsList =
            ArrayList(
                helper.getSongsFromDBToCurrentSongsList(
                    getListFromString(searchTags), getListFromString(antiSearchTags), searchName = searchName,
                    sort = sortStatus, minRating = searchMinRating, andOrFlag = andOrStatus
                )
            )
        currentSongsShuffledListNumber = getShuffledListOfInt(currentSongsList.size)
    }

    fun setAllTagsFromSettings(t: String) {
        allTags = t
    }

    fun jumpToPosition(position: Int) {
        Log.i(TAG, "Seek to position $position")
        exoPlayer.seekTo(position)
    }

    fun addSongToAdditionList(position: Int) {
        if (currentSongPositionInList!=position) {
            val s = currentSongsList[position].copy()
            if (!currentSongsAditionListNumber.contains(position)) {
                currentSongsAditionListNumber.add(position)
                s.order = currentSongsAditionListNumber.size
            } else {
                currentSongsAditionListNumber.remove(position)
                s.order = 0
            }
            helper.writeSong(s)
        }
    }

    private val metadataBuilder = MediaMetadataCompat.Builder()
    private val stateBuilder: PlaybackStateCompat.Builder = PlaybackStateCompat.Builder()
        .setActions(
            PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_STOP
                    or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )

    private val audioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // Фокус предоставлен.
                    // Например, был входящий звонок и фокус у нас отняли.
                    // Звонок закончился, фокус выдали опять
                    // и мы продолжили воспроизведение.
                    //  mediaSessionCallback.onPlay()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    // Фокус отняли, потому что какому-то приложению надо
                    // коротко "крякнуть".
                    // Например, проиграть звук уведомления или навигатору сказать
                    // "Через 50 метров поворот направо".
                    // В этой ситуации нам разрешено не останавливать вопроизведение,
                    // но надо снизить громкость.
                    // Приложение не обязано именно снижать громкость,
                    // можно встать на паузу, что мы здесь и делаем.
                    // mediaSessionCallback.onPause()
                }
                else -> {
                    // Фокус совсем отняли.
                    // mediaSessionCallback.onPause()

                }
            }
        }
}