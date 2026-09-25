package com.videoforge.android

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.view.Surface
import java.io.File

class VideoExporter(private val context: Context) {
    fun render(project: VideoProject): File {
        require(project.scenes.isNotEmpty()) { "Add at least one scene before exporting." }
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "exports").apply { mkdirs() }
        val safeName = project.name.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifBlank { "videoforge" }
        val out = File(dir, "$safeName.mp4")

        val format = MediaFormat.createVideoFormat("video/avc", project.width, project.height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, project.fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }
        val codec = MediaCodec.createEncoderByType("video/avc")
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface = codec.createInputSurface()
        codec.start()
        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val renderer = SceneRenderer()
        val info = MediaCodec.BufferInfo()
        var track = -1
        var muxerStarted = false

        fun drain(endOfStream: Boolean = false) {
            if (endOfStream) codec.signalEndOfInputStream()
            while (true) {
                when (val index = codec.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> if (!endOfStream) return
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        check(!muxerStarted) { "Encoder changed format twice" }
                        track = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    else -> if (index >= 0) {
                        val buffer = codec.getOutputBuffer(index)
                        if (buffer != null && info.size > 0 && muxerStarted && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            muxer.writeSampleData(track, buffer, info)
                        }
                        val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(index, false)
                        if (eos) return
                    }
                }
            }
        }

        try {
            for (scene in project.scenes) {
                val frames = ((scene.durationMs / 1000.0) * project.fps).toInt().coerceAtLeast(1)
                for (frame in 0 until frames) {
                    val progress = if (frames == 1) 1f else frame.toFloat() / (frames - 1).toFloat()
                    val canvas = surface.lockCanvas(null)
                    try {
                        renderer.render(canvas, scene, project.width, project.height, progress)
                    } finally {
                        surface.unlockCanvasAndPost(canvas)
                    }
                    drain()
                }
            }
            drain(endOfStream = true)
        } finally {
            if (muxerStarted) muxer.stop()
            muxer.release()
            codec.stop()
            codec.release()
            surface.release()
        }
        return out
    }
}
