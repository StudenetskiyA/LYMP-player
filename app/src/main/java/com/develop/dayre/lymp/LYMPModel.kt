package com.develop.dayre.lymp

import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.databinding.BaseObservable
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

enum class RepeatState { All, One, Stop }
enum class PlayState { Play, Stop, Pause }
enum class SortState { ByName, ByAdded, ByListened }
enum class TagsFlag { Or, And }

interface ILYMPModel {
    fun initialize()

    fun newSearch(tags: String): Observable<ArrayList<Song>>

    fun saveSongToDB(song: Song)
    fun nextSong()
    fun prevSong()
    fun changeShuffle()
    fun changeRepeat()
    fun clearTag()

    fun testAction()
    fun getCurrentSong(): Observable<SongOrNull>
    fun getCurrentSongsList(): ArrayList<Song>
    fun getAllTags(): String
    fun getShuffleStatus(): Boolean
    fun getRepeatStatus(): RepeatState
    fun getCurrentSearchTags(): Observable<String>
}

class LYMPModel : ILYMPModel, BaseObservable() {
    private val TAG = "$APP_TAG/model"
    private var currentSong: Song? =
        Song() //У нас бывают ситуации, когда текущий трек не в текущем листе.
    private var currentSongsList = ArrayList<Song>()
    private var currentSongsShuffledListNumber = ArrayList<Int>()
    private var helper: RealmHelper = RealmHelper(MainActivity.applicationContext())
    private var searchTags = ";"
    private var shuffleStatus: Boolean = false
    private var repeatStatus = RepeatState.All
    private var currentSongPositionInList: Int = 0
        set(value) {
            if (value < currentSongsList.size) {
                field = value
                currentSong = currentSongsList[currentSongPositionInList]
            }
        }
    lateinit var mediaSession: MediaSessionCompat
    private lateinit var exoPlayer: MediaPlayer

    //Методы для обсерверов
    override fun getShuffleStatus(): Boolean {
        return shuffleStatus
    }
    override fun getRepeatStatus(): RepeatState {
        return repeatStatus
    }


    override fun getAllTags(): String {
        return "rock; pop; techno; jazz; superjazz; technojazz; вскрытие души; оптимально для суицида; осень; дорога"
    }

    override fun getCurrentSongsList(): ArrayList<Song> {
        return currentSongsList
    }

    override fun getCurrentSong(): Observable<SongOrNull> {
        return if (currentSongPositionInList < currentSongsList.size && currentSongPositionInList >= 0)
            Observable.just(SongOrNull(currentSongsList[currentSongPositionInList]))

        //In Rx2 I can't do Observable.just(null)!
        else Observable.just(SongOrNull(Song(), true))
    }

    override fun getCurrentSearchTags(): Observable<String> {
        return Observable.just(searchTags)
    }

    //Действия
    fun browseFolderForFiles() : Observable<Boolean> {
        var newSongFound = 0
        var songsRestored = 0
        var deletedSong = 0
        //Получаем список всех файлов в папке и подпапках
        //TODO Поменять путь на недефолтный при загрузке из настроек.
        val allFiles = getFilesListInFolderAndSubFolder(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "mp3"
        )
        val allRecord = helper.getAllSongsFileExist()
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
            val s = helper.getSongByPath(f)
            if (s == null) {
                newSongFound++
                helper.writeSong(Song(name = f.getNameFromPath(), path = f))
            }
            if (s != null && !s.isFileExist) {
                songsRestored++
                s.isFileExist = true
                helper.writeSong(s)
            }
        }
        //И уведомление с результатами
        //Вообще, наверное, не правильно в модели использовать контекст активити. Но пока сделаю так.
        // if (songsRestored!=0 || newSongFound!=0 || deletedSong!=0)
        MainActivity.applicationContext()
            .toast("Новых песен найдено - $newSongFound \r\nУдалено песен - $deletedSong \r\nВосстановленно удаленных - $songsRestored")
      //  return Observable.just(true).delay(5, TimeUnit.SECONDS)
        return Observable.just(true)
    }

    fun setCurrentSong(songName: String) {
        val s = helper.getSongByName(songName)
        if (s != null && currentSongsList.contains(s)) {
            currentSong = s
            currentSongPositionInList = currentSongsList.indexOf(s)
        }
    }

    fun setCurrentSongByID(songID: Long) {
        val s = helper.getSongByID(songID)
        if (s != null && currentSongsList.contains(s)) {
            currentSong = s
            currentSongPositionInList = currentSongsList.indexOf(s)
        }
    }

    fun setPositionInList(position: Int) {
        currentSongPositionInList = position
    }

    override fun clearTag() {
        val song = currentSong?.copy()
        if (song != null) {
            song.tags = ";"
            helper.writeSong(song)
            createCurrentList() //После удаления тегов трека он может пропасть/появиться в листе.
        }
    }

    override fun newSearch(tags: String): Observable<ArrayList<Song>> {
        Log.i(TAG, "new search")
        searchTags = tags
        createCurrentList()
        //  return Observable.just(currentSongsList).delay(5, TimeUnit.SECONDS)
        return Observable.just(currentSongsList)
    }

    override fun saveSongToDB(song: Song) {
        helper.writeSong(song)
        Log.i(TAG, "Song with name ${song.name} and tags ${song.tags} saved to DB")
    }

    override fun nextSong() {
        if (currentSongsList.isNotEmpty()) {
            currentSongPositionInList = getNextPositionInList(
                currentSongsShuffledListNumber,
                currentSongPositionInList,
                shuffleStatus
            )
            Log.i(
                TAG, "Next track ${currentSong?.name} / " +
                        "${currentSong?.tags}"
            )
        }
    }

    override fun prevSong() {
        if (currentSongsList.isNotEmpty()) {
            currentSongPositionInList = getPrevPositionInList(
                currentSongsShuffledListNumber,
                currentSongPositionInList,
                shuffleStatus
            )
            Log.i(TAG, "Next track name ${currentSong?.name}")
        }
    }

    fun play() {
        // Заполняем данные о треке
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

        // Указываем, что наше приложение теперь активный плеер и кнопки
        // на окне блокировки должны управлять именно нами
        mediaSession.isActive = true

        // Сообщаем новое состояние
        mediaSession.setPlaybackState(
            stateBuilder.setState(
                PlaybackStateCompat.STATE_PLAYING,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f
            ).build()
        )

        // Загружаем URL аудио-файла в Player
        exoPlayer = MediaPlayer.create(MainActivity.applicationContext(), Uri.parse(currentSong?.path))
        // Запускаем воспроизведение
        exoPlayer.start()
    }

    fun pause() {
        // Останавливаем воспроизведение
        exoPlayer.pause()

        // Сообщаем новое состояние
        mediaSession.setPlaybackState(
            stateBuilder.setState(
                PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f
            ).build()
        )
    }

    fun stop() {
        // Останавливаем воспроизведение
        exoPlayer.stop()

        // Все, больше мы не "главный" плеер, уходим со сцены
        mediaSession.isActive = false

        // Сообщаем новое состояние
        mediaSession.setPlaybackState(
            stateBuilder.setState(
                PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f
            ).build()
        )
    }

    override fun changeShuffle() {
        shuffleStatus = !shuffleStatus
        currentSongsShuffledListNumber = getShuffledListOfInt(currentSongsList.size)
        Log.i(TAG, "Now shuffle is $shuffleStatus")
    }
    override fun changeRepeat() {
        when (repeatStatus) {
            RepeatState.All -> repeatStatus=RepeatState.One
            RepeatState.One -> repeatStatus=RepeatState.Stop
            RepeatState.Stop -> repeatStatus=RepeatState.All
        }
        Log.i(TAG, "Now repeat is $repeatStatus")
    }

    override fun testAction() {
        Log.i(TAG, "testAction")
    }

    fun getMediaSessionToken(): MediaSessionCompat.Token {
        return mediaSession.sessionToken
    }
//    fun getMediaSession(): MediaSessionCompat {
//        return mediaSession
//    }

    fun setMediaSessonCallback(mediaSessionCallback : MediaSessionCompat.Callback) {
        mediaSession.setCallback(mediaSessionCallback)
    }

    override fun initialize() {
        Log.i(TAG, "initialization")

        mediaSession = MediaSessionCompat(MainActivity.applicationContext(), "LYMPService")
        //exoPlayer = MediaPlayer.create(MainActivity.applicationContext(), Uri.parse(currentSong?.path))
        // FLAG_HANDLES_MEDIA_BUTTONS - хотим получать события от аппаратных кнопок
        // (например, гарнитуры)
        // FLAG_HANDLES_TRANSPORT_CONTROLS - хотим получать события от кнопок
        // на окне блокировки
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)



        // Укажем activity, которую запустит система, если пользователь
        // заинтересуется подробностями данной сессии
        mediaSession.setSessionActivity(
            PendingIntent.getActivity(
                MainActivity.applicationContext(),
                0,
                Intent(MainActivity.applicationContext(), MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
            //browseFolderForFiles()
//        helper.clearDataBase()
//        helper.writeSong(Song(name = "1 track"))
//        helper.writeSong(Song(name = "2 track"))
//        helper.writeSong(Song(name = "3 track"))
//        helper.writeSong(Song(path = "subfolder",name = "1 track"))
//        helper.writeSong(Song(name = "Русский track"))
//        helper.writeSong(Song(name = "Еще один track"))
//        helper.writeSong(Song(name = "Трек с очень-очень длинным именем, прям куда деваться"))
//        helper.writeSong(Song(name = "Без тегов"))
//        helper.writeSong(Song(name = "Все теги"))
        createCurrentList()
    }

    //Private
    private fun createCurrentList() {
        currentSongsList =
            ArrayList(helper.getSongsFromDBToCurrentSongsList(getListFromString(searchTags)))
        currentSongsShuffledListNumber = getShuffledListOfInt(currentSongsList.size)

        //currentSongPositionInList = 0 - при поиске мы не меняем трек, играет/редактируется тот же, что и был.
    }



    // Закешируем билдеры

    // ...метаданных трека
    private val metadataBuilder = MediaMetadataCompat.Builder()

    // ...состояния плеера
    // Здесь мы указываем действия, которые собираемся обрабатывать в коллбэках.
    // Например, если мы не укажем ACTION_PAUSE,
    // то нажатие на паузу не вызовет onPause.
    // ACTION_PLAY_PAUSE обязателен, иначе не будет работать
    // управление с Android Wear!
    val stateBuilder: PlaybackStateCompat.Builder = PlaybackStateCompat.Builder()
        .setActions(
            PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_STOP
                    or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )


}