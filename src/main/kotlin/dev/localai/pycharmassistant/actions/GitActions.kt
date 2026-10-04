package dev.localai.pycharmassistant.actions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.concurrent.TimeUnit

internal object GitRunner {
    fun run(basePath: String, vararg args: String, timeoutSeconds: Long = 30): String? {
        return try {
            val process = ProcessBuilder(listOf("git") + args)
                .directory(File(basePath))
                .redirectErrorStream(true)
                .start()
            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                null
            } else {
                val output = process.inputStream.bufferedReader().use { it.readText() }
                if (process.exitValue() == 0) output else null
            }
        } catch (_: Exception) {
            null
        }
    }
}

class GenerateCommitMessageAction : AiEditorAction() {
    override fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ) {
        val basePath = project.basePath
            ?: return showMessage(project, "Cannot locate the project directory.")
        ApplicationManager.getApplication().executeOnPooledThread {
            val status = GitRunner.run(basePath, "status", "--short").orEmpty()
            val staged = GitRunner.run(basePath, "diff", "--cached")?.takeIf { it.isNotBlank() }
            val unstaged = if (staged == null) {
                GitRunner.run(basePath, "diff")?.takeIf { it.isNotBlank() }
            } else {
                null
            }
            val diff = (staged ?: unstaged ?: "").take(MAX_DIFF_CHARS)
            ApplicationManager.getApplication().invokeLater {
                if (status.isBlank() && diff.isBlank()) {
                    showMessage(project, "No staged or unstaged changes detected by git.")
                    return@invokeLater
                }
                val prompt = buildString {
                    append(BuiltInPrompts.GENERATE_COMMIT_MESSAGE)
                    if (status.isNotBlank()) append("\n\nGit status:\n").append(status)
                    if (diff.isNotBlank()) append("\n\nDiff:\n").append(diff)
                }
                run(project, editor, "Generated Commit Message", prompt)
            }
        }
    }

    private companion object {
        const val MAX_DIFF_CHARS = 15_000
    }
}

class ExplainCommitAction : AiEditorAction() {
    override fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ) {
        val basePath = project.basePath
            ?: return showMessage(project, "Cannot locate the project directory.")
        val commitRef = askInput(
            project,
            "Explain Commit",
            "Commit hash or reference (for example HEAD, a1b2c3d):",
            initial = "HEAD"
        )?.trim()
        if (commitRef.isNullOrEmpty()) return
        ApplicationManager.getApplication().executeOnPooledThread {
            val detail = GitRunner.run(
                basePath,
                "show",
                "--stat",
                "--patch",
                "--format=fuller",
                commitRef,
                timeoutSeconds = 60
            )?.take(MAX_COMMIT_CHARS)
            ApplicationManager.getApplication().invokeLater {
                if (detail.isNullOrBlank()) {
                    showMessage(project, "Could not read commit '$commitRef'. Check the reference and that git is available.")
                    return@invokeLater
                }
                run(
                    project,
                    editor,
                    "Commit Explanation: $commitRef",
                    "${BuiltInPrompts.EXPLAIN_COMMIT}\n\nCommit:\n$detail"
                )
            }
        }
    }

    private companion object {
        const val MAX_COMMIT_CHARS = 15_000
    }
}

class AskVcsQuestionAction : AiEditorAction() {
    override fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ) {
        val basePath = project.basePath
            ?: return showMessage(project, "Cannot locate the project directory.")
        val question = askInput(
            project,
            "Ask About VCS",
            "Your question about this repository, branch, or history:"
        )?.trim()
        if (question.isNullOrEmpty()) return
        ApplicationManager.getApplication().executeOnPooledThread {
            val status = GitRunner.run(basePath, "status", "--short").orEmpty()
            val branch = GitRunner.run(basePath, "branch", "--show-current").orEmpty().trim()
            val log = GitRunner.run(basePath, "log", "--oneline", "-20").orEmpty()
            ApplicationManager.getApplication().invokeLater {
                if (status.isBlank() && log.isBlank()) {
                    showMessage(project, "This project does not appear to be a git repository, or git is unavailable.")
                    return@invokeLater
                }
                val prompt = buildString {
                    append(BuiltInPrompts.ASK_VCS)
                    if (branch.isNotBlank()) append("\n\nCurrent branch: ").append(branch)
                    append("\n\nGit status:\n").append(status.ifBlank { "(clean)" })
                    append("\n\nRecent commits:\n").append(log)
                    append("\n\nQuestion:\n").append(question)
                }
                run(project, editor, "VCS Answer", prompt)
            }
        }
    }
}
