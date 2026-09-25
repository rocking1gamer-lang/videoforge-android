package com.videoforge.android

import org.json.JSONArray
import org.json.JSONObject

data class Scene(
    val title: String,
    val subtitle: String,
    val kind: Int,
    val durationMs: Long = 5000L
)

data class VideoProject(
    val name: String = "Untitled Project",
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val scenes: List<Scene> = emptyList()
)

object ProjectJson {
    fun encode(project: VideoProject): String = JSONObject().apply {
        put("name", project.name)
        put("width", project.width)
        put("height", project.height)
        put("fps", project.fps)
        put("scenes", JSONArray().apply {
            project.scenes.forEach { s ->
                put(JSONObject().apply {
                    put("title", s.title)
                    put("subtitle", s.subtitle)
                    put("kind", s.kind)
                    put("durationMs", s.durationMs)
                })
            }
        })
    }.toString()

    fun decode(raw: String): VideoProject {
        val o = JSONObject(raw)
        val a = o.optJSONArray("scenes") ?: JSONArray()
        val scenes = buildList {
            for (i in 0 until a.length()) {
                val s = a.getJSONObject(i)
                add(Scene(s.optString("title"), s.optString("subtitle"), s.optInt("kind"), s.optLong("durationMs", 5000L)))
            }
        }
        return VideoProject(o.optString("name", "Untitled Project"), o.optInt("width", 1920), o.optInt("height", 1080), o.optInt("fps", 30), scenes)
    }
}
