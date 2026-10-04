package dev.localai.pycharmassistant.actions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class AskProgrammingQuestionAction : AiEditorAction() {
    override fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ) {
        val question = askInput(
            project,
            "Ask a Programming Question",
            "Your programming question:"
        )?.trim()
        if (question.isNullOrEmpty()) return
        run(
            project,
            editor,
            "Answer",
            "${BuiltInPrompts.ASK_PROGRAMMING}\n\nQuestion:\n$question"
        )
    }
}

class AskProjectCodeQuestionAction : AiEditorAction() {
    override fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ) {
        val question = askInput(
            project,
            "Ask About Project Code",
            "Your question about the current file or selection:"
        )?.trim()
        if (question.isNullOrEmpty()) return
        val context = codeContext(virtualFile, selection, fileText)
        val prompt = if (context != null) {
            "${BuiltInPrompts.ASK_PROJECT_CODE}\n\n$context\n\nQuestion:\n$question"
        } else {
            "${BuiltInPrompts.ASK_PROJECT_CODE}\n\nNo file is open, so answer from general knowledge.\n\nQuestion:\n$question"
        }
        run(project, editor, "Project Answer", prompt)
    }
}
