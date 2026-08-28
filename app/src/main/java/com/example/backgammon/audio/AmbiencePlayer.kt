package com.example.backgammon.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.annotation.RawRes
import com.example.backgammon.ui.game.Ambience

internal fun loopResumeMs(savedMs: Int, durationMs: Int): Int {
  if (durationMs <= 0 || savedMs <= 0) return 0
  return savedMs % durationMs
}

class AmbiencePlayer(context: Context) {
  private val app = context.applicationContext
  private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
  private val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private val attrs =
    AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_MEDIA)
      .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
      .build()
  private var focusRequest: AudioFocusRequest? = null
  private var player: MediaPlayer? = null
  private var current: Ambience = Ambience.OFF
  private var started = false
  private val positions =
    Ambience.entries
      .filter { it.rawRes != null }
      .associateWith { prefs.getInt(posKey(it), 0) }
      .toMutableMap()

  fun setAmbience(track: Ambience) {
    if (track == current) {
      if (started) play()
      return
    }
    capturePosition()
    current = track
    rebuild()
  }

  fun onStart() {
    started = true
    play()
  }

  fun onStop() {
    started = false
    capturePosition()
    try {
      player?.takeIf { it.isPlaying }?.pause()
    } catch (_: Exception) {}
    abandonFocus()
  }

  fun release() {
    started = false
    capturePosition()
    abandonFocus()
    player?.release()
    player = null
    current = Ambience.OFF
  }

  private fun rebuild() {
    player?.release()
    player = null
    val res = current.rawRes ?: run {
      abandonFocus()
      return
    }
    player = create(res)?.also { seekToSaved(it) }
    if (started) play()
  }

  private fun play() {
    val mp = player ?: return
    requestFocus()
    try {
      if (!mp.isPlaying) mp.start()
    } catch (_: Exception) {
      val res = current.rawRes ?: return
      player?.release()
      player = create(res)?.also { seekToSaved(it) }
      try {
        player?.start()
      } catch (_: Exception) {}
    }
  }

  private fun create(@RawRes res: Int): MediaPlayer? {
    return try {
      val fd = app.resources.openRawResourceFd(res) ?: return null
      MediaPlayer().apply {
        setAudioAttributes(attrs)
        setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        fd.close()
        isLooping = true
        setVolume(0.55f, 0.55f)
        prepare()
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun capturePosition() {
    val track = current
    if (track.rawRes == null) return
    val pos =
      try {
        player?.currentPosition ?: return
      } catch (_: Exception) {
        return
      }
    positions[track] = pos
    prefs.edit().putInt(posKey(track), pos).apply()
  }

  private fun seekToSaved(mp: MediaPlayer) {
    val saved = positions[current] ?: prefs.getInt(posKey(current), 0)
    val seek = loopResumeMs(saved, mp.duration)
    if (seek <= 0) return
    try {
      mp.seekTo(seek)
    } catch (_: Exception) {}
  }

  private fun requestFocus(): Boolean {
    val result =
      if (Build.VERSION.SDK_INT >= 26) {
        val request =
          focusRequest
            ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
              .setAudioAttributes(attrs)
              .setAcceptsDelayedFocusGain(false)
              .build()
              .also { focusRequest = it }
        audio.requestAudioFocus(request)
      } else {
        @Suppress("DEPRECATION")
        audio.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
      }
    return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
  }

  private fun abandonFocus() {
    if (Build.VERSION.SDK_INT >= 26) {
      focusRequest?.let { audio.abandonAudioFocusRequest(it) }
    } else {
      @Suppress("DEPRECATION")
      audio.abandonAudioFocus(null)
    }
  }

  private companion object {
    const val PREFS = "backgammon_game"

    fun posKey(track: Ambience) = "ambience_pos_${track.name}"
  }
}
