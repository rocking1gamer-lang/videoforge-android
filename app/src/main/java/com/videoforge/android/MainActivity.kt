package com.videoforge.android

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var store: ProjectStore
    private lateinit var projectName: EditText
    private lateinit var prompt: EditText
    private lateinit var status: TextView
    private lateinit var sceneList: LinearLayout
    private lateinit var preview: PreviewView
    private var project = defaultProject()
    private var selectedScene = 0
    private var tts: TextToSpeech? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ProjectStore(this)
        project = store.load() ?: defaultProject()
        buildUi()
        tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale.US }
        refresh()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(9, 11, 14)) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(20, 14, 20, 10) }
        val brand = TextView(this).apply { text = "VideoForge"; textSize = 23f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD }
        header.addView(brand, LinearLayout.LayoutParams(0, 58, 1f))
        status = TextView(this).apply { text = "Ready"; textSize = 13f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER_VERTICAL }
        header.addView(status, LinearLayout.LayoutParams(180, 58)); root.addView(header)

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 0, 16, 20) }
        projectName = edit("Project name")
        body.addView(label("PROJECT")); body.addView(projectName)
        prompt = EditText(this).apply {
            hint = "Describe the video you want to create…"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.rgb(23, 27, 32)); setPadding(18, 16, 18, 16); gravity = Gravity.TOP; minLines = 4
        }
        body.addView(label("PROMPT")); body.addView(prompt)
        val generate = button("Generate scenes"); generate.setOnClickListener { generateFromPrompt() }
        body.addView(generate, marginTop = 10)

        preview = PreviewView(this)
        body.addView(label("PREVIEW")); body.addView(preview, LinearLayout.LayoutParams(-1, 430).apply { topMargin = 12 })
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val prev = button("←"); val play = button("▶ Preview"); val next = button("→")
        controls.addView(prev, LinearLayout.LayoutParams(0, 54, 1f)); controls.addView(play, LinearLayout.LayoutParams(0, 54, 2f)); controls.addView(next, LinearLayout.LayoutParams(0, 54, 1f))
        prev.setOnClickListener { select((selectedScene - 1 + project.scenes.size) % project.scenes.size) }
        next.setOnClickListener { select((selectedScene + 1) % project.scenes.size) }
        play.setOnClickListener { preview.play(project.scenes, selectedScene) }
        body.addView(controls, marginTop = 10)

        body.addView(label("SCENES")); sceneList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; body.addView(sceneList)
        val add = button("+ Add scene"); add.setOnClickListener {
            project = project.copy(scenes = project.scenes + Scene("New scene", "Add a short explanation.", 0)); select(project.scenes.lastIndex); save()
        }; body.addView(add, marginTop = 8)
        val speak = button("Speak narration"); speak.setOnClickListener { speakCurrent() }
        val save = button("Save project"); save.setOnClickListener { save() }
        val export = button("Export MP4"); export.setOnClickListener { export() }
        body.addView(speak, marginTop = 14); body.addView(save, marginTop = 8); body.addView(export, marginTop = 8)
        scroll.addView(body); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
    }

    private fun refresh() {
        projectName.setText(project.name)
        if (prompt.text.isBlank()) prompt.setText("Create a beginner explainer about inflation with a ₹100 example, causes, CPI, and a recap.")
        sceneList.removeAllViews()
        project.scenes.forEachIndexed { i, scene ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(12, 8, 8, 8); setBackgroundColor(if (i == selectedScene) Color.rgb(35, 30, 58) else Color.rgb(23, 27, 32)) }
            val text = TextView(this).apply { setTextColor(Color.WHITE); textSize = 15f; text = "${i + 1}. ${scene.title}\n${scene.durationMs / 1000}s" }
            row.addView(text, LinearLayout.LayoutParams(0, 70, 1f))
            val edit = button("Edit"); edit.setOnClickListener { editScene(i) }; row.addView(edit, LinearLayout.LayoutParams(90, 52)); row.setOnClickListener { select(i) }
            sceneList.addView(row, LinearLayout.LayoutParams(-1, 78).apply { topMargin = 6 })
        }
        if (project.scenes.isNotEmpty()) preview.scene = project.scenes[selectedScene]
        status.text = "Scene ${selectedScene + 1}/${project.scenes.size} • ${project.width}×${project.height}"
    }

    private fun generateFromPrompt() {
        val p = prompt.text.toString().trim(); val topic = Regex("about ([^.]+)", RegexOption.IGNORE_CASE).find(p)?.groupValues?.get(1) ?: "your topic"
        project = project.copy(name = projectName.text.toString().ifBlank { "VideoForge Project" }, scenes = listOf(
            Scene("What is $topic?", "Start with the simplest definition.", 0, 5000),
            Scene("A simple example", "Turn the idea into an everyday visual example.", 2, 6000),
            Scene("Why does it happen?", "Break the main causes into clear visual ideas.", 3, 6000),
            Scene("How do we measure it?", "Introduce the key measure or indicator.", 4, 6000),
            Scene("Who is affected?", "Show the practical effect on people and businesses.", 2, 5000),
            Scene("Quick recap", "Three short points to remember.", 1, 5000)
        ))
        selectedScene = 0; save(); refresh(); status.text = "Generated ${project.scenes.size} editable scenes"
    }

    private fun editScene(index: Int) {
        val s = project.scenes[index]
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 8) }
        val title = edit("Scene title").apply { setText(s.title) }
        val sub = edit("Scene subtitle").apply { setText(s.subtitle); minLines = 3 }
        val seconds = edit("Duration (seconds)").apply { setText((s.durationMs / 1000).toString()); inputType = 2 }
        box.addView(title); box.addView(sub, LinearLayout.LayoutParams(-1, 120).apply { topMargin = 8 }); box.addView(seconds, LinearLayout.LayoutParams(-1, 60).apply { topMargin = 8 })
        AlertDialog.Builder(this).setTitle("Edit scene ${index + 1}").setView(box).setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
            val duration = (seconds.text.toString().toLongOrNull() ?: 5L).coerceIn(1L, 120L) * 1000L
            val updated = s.copy(title = title.text.toString().ifBlank { "Untitled scene" }, subtitle = sub.text.toString(), durationMs = duration)
            project = project.copy(scenes = project.scenes.toMutableList().also { it[index] = updated }); selectedScene = index; save(); refresh()
        }.show()
    }

    private fun select(i: Int) { if (project.scenes.isEmpty()) return; selectedScene = i.coerceIn(0, project.scenes.lastIndex); refresh() }

    private fun save() {
        project = project.copy(name = projectName.text.toString().ifBlank { "Untitled Project" }); store.save(project); status.text = "Saved • ${project.scenes.size} scenes"
    }

    private fun export() {
        save(); status.text = "Rendering MP4…"
        executor.execute {
            try {
                val file = VideoExporter(this).render(project)
                main.post { status.text = "Exported: ${file.name}"; Toast.makeText(this, "MP4 saved in the app Movies folder", Toast.LENGTH_LONG).show() }
            } catch (e: Exception) { main.post { status.text = "Export failed: ${e.message ?: "unknown error"}" } }
        }
    }

    private fun speakCurrent() {
        val text = project.scenes.joinToString(" ") { "${it.title}. ${it.subtitle}" }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "videoforge-project"); status.text = "Speaking narration…"
    }

    private fun label(s: String) = TextView(this).apply { text = s; textSize = 12f; setTextColor(Color.LTGRAY); typeface = Typeface.DEFAULT_BOLD; setPadding(4, 18, 4, 7) }
    private fun edit(hintText: String) = EditText(this).apply { hint = hintText; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setBackgroundColor(Color.rgb(23, 27, 32)); setPadding(16, 10, 16, 10) }
    private fun button(s: String) = Button(this).apply { text = s; isAllCaps = false; setTextColor(Color.WHITE) }
    private fun <T : View> LinearLayout.addView(v: T, marginTop: Int) { addView(v, LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = marginTop }) }
    override fun onDestroy() { tts?.shutdown(); executor.shutdownNow(); super.onDestroy() }

    private fun defaultProject() = VideoProject(name = "Inflation for Beginners", scenes = listOf(
        Scene("Why does ₹100 buy less?", "A simple introduction to inflation.", 0),
        Scene("Inflation = prices rising", "The general price level rises over time.", 1),
        Scene("Your ₹100 basket", "The same money buys fewer goods.", 2),
        Scene("Why do prices rise?", "Demand-pull and cost-push pressures.", 3),
        Scene("Measure → understand → respond", "CPI tracks the cost of a representative basket.", 4)
    ))
}

class PreviewView(context: android.content.Context) : View(context) {
    private val renderer = SceneRenderer(); var scene: Scene? = null; set(value) { field = value; invalidate() }
    private var start = 0L; private var playing = false
    fun play(scenes: List<Scene>, index: Int) { scene = scenes.getOrNull(index); start = android.os.SystemClock.uptimeMillis(); playing = true; invalidate() }
    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas); val s = scene ?: return; val elapsed = android.os.SystemClock.uptimeMillis() - start
        val progress = if (playing) (elapsed.toFloat() / s.durationMs).coerceIn(0f, 1f) else 0.35f
        renderer.render(canvas, s, width.coerceAtLeast(1), (width / 16f * 9f).toInt().coerceAtLeast(1), progress)
        if (playing && progress < 1f) postInvalidateDelayed(16) else playing = false
    }
}
