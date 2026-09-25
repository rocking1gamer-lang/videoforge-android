package com.videoforge.android

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.min

class SceneRenderer {
    private val text = Paint(Paint.ANTI_ALIAS_FLAG)
    private val muted = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(124, 92, 255) }

    fun render(canvas: Canvas, scene: Scene, width: Int, height: Int, progress: Float) {
        val sx = width / 1920f
        val sy = height / 1080f
        val scale = min(sx, sy)
        canvas.drawColor(Color.rgb(11, 13, 15))
        canvas.save()
        canvas.scale(scale, scale)
        val w = width / scale
        val h = height / scale
        val p = progress.coerceIn(0f, 1f)
        val eased = 1f - (1f - p) * (1f - p)

        text.color = Color.WHITE
        text.typeface = android.graphics.Typeface.DEFAULT_BOLD
        text.textSize = 76f
        text.alpha = (255 * min(1f, p * 4f)).toInt()
        canvas.drawText(scene.title, 110f, 170f + (1f - eased) * 34f, text)

        muted.color = Color.rgb(190, 195, 205)
        muted.textSize = 34f
        muted.alpha = 255
        canvas.drawText(scene.subtitle, 110f, 235f + (1f - eased) * 34f, muted)

        when (scene.kind) {
            0 -> hook(canvas)
            1 -> line(canvas, p)
            2 -> basket(canvas, p)
            3 -> pressure(canvas)
            else -> cpi(canvas)
        }
        canvas.restore()
    }

    private fun hook(c: Canvas) {
        text.color = Color.WHITE; text.textSize = 180f
        c.drawText("₹100", 130f, 540f, text)
        c.drawRoundRect(680f, 350f, 1640f, 535f, 36f, 36f, accent)
        text.textSize = 68f; c.drawText("buys less", 850f, 460f, text)
        text.textSize = 32f; c.drawText("over time", 875f, 510f, text)
    }

    private fun line(c: Canvas, p: Float) {
        val path = Path()
        for (i in 0..100) {
            val x = 160f + 1500f * i / 100f
            val y = 720f - 360f * (i / 100f) * (0.25f + 0.75f * p)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent.color; style = Paint.Style.STROKE; strokeWidth = 12f }
        c.drawPath(path, line)
        text.color = Color.WHITE; text.textSize = 40f
        c.drawText("General price level", 160f, 820f, text)
    }

    private fun basket(c: Canvas, p: Float) {
        val labels = arrayOf("Milk", "Rice", "Fuel")
        val base = floatArrayOf(220f, 180f, 130f)
        labels.indices.forEach { i ->
            val x = 150f + i * 550f
            val q = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent.color }
            val h = base[i] * (1f + .65f * p)
            c.drawRoundRect(x, 700f - h, x + 300f, 700f, 24f, 24f, q)
            text.color = Color.WHITE; text.textSize = 32f
            c.drawText(labels[i], x + 28f, 760f, text)
        }
        text.textSize = 42f
        c.drawText("Same ₹100 → smaller basket", 150f, 900f, text)
    }

    private fun pressure(c: Canvas) {
        pill(c, 150f, "DEMAND", "more buyers")
        pill(c, 1010f, "COST", "inputs get dearer")
    }

    private fun pill(c: Canvas, x: Float, a: String, b: String) {
        c.drawRoundRect(x, 420f, x + 700f, 620f, 34f, 34f, accent)
        text.color = Color.WHITE; text.textSize = 42f
        c.drawText(a, x + 45f, 500f, text)
        text.textSize = 30f; c.drawText(b, x + 45f, 555f, text)
    }

    private fun cpi(c: Canvas) {
        val labels = arrayOf("Food", "Housing", "Transport", "Other")
        labels.indices.forEach { i ->
            val x = 120f + i * 450f
            val q = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (i % 2 == 0) accent.color else Color.rgb(55, 60, 70) }
            c.drawRoundRect(x, 420f, x + 360f, 620f, 28f, 28f, q)
            text.color = Color.WHITE; text.textSize = 30f
            c.drawText(labels[i], x + 35f, 535f, text)
        }
        text.textSize = 42f
        c.drawText("CPI = cost of a representative basket", 120f, 820f, text)
    }
}
