package com.videoforge.android

import android.content.Context
import java.io.File

class ProjectStore(private val context: Context) {
    private val projectFile get() = File(context.filesDir, "project.json")

    fun save(project: VideoProject) {
        val temp = File(context.filesDir, "project.json.tmp")
        temp.writeText(ProjectJson.encode(project))
        if (projectFile.exists() && !projectFile.delete()) error("Could not replace project file")
        if (!temp.renameTo(projectFile)) error("Could not save project")
    }

    fun load(): VideoProject? = runCatching {
        if (!projectFile.exists()) null else ProjectJson.decode(projectFile.readText())
    }.getOrNull()
}
