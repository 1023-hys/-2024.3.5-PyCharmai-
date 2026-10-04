package dev.localai.pycharmassistant.actions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class GenerateDocumentationAction : AiEditorAction() {
    override fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ) {
        val context = codeContext(virtualFile, selection, fileText)
            ?: return showMessage(project, "Select code or open a file to generate documentation for.")
        run(project, editor, "Generated Documentation", "${BuiltInPrompts.GENERATE_DOCUMENTATION}\n\n$context")
    }
}

class SuggestNamesAction : AiEditorAction() {
    override fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ) {
        val context = codeContext(virtualFile, selection, fileText)
            ?: return showMessage(project, "Select code or open a file to suggest names for.")
        run(project, editor, "Name Suggestions", "${BuiltInPrompts.SUGGEST_NAMES}\n\n$context")
    }
}

class SuggestRefactoringAction : AiEditorAction() {
    override fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ) {
        val context = codeContext(virtualFile, selection, fileText)
            ?: return showMessage(project, "Select code or open a file to get refactoring suggestions for.")
        run(project, editor, "Refactoring Suggestions", "${BuiltInPrompts.SUGGEST_REFACTORING}\n\n$context")
    }
}

class FindBugsAction : AiEditorAction() {
    override fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ) {
        val context = codeContext(virtualFile, selection, fileText)
            ?: return showMessage(project, "Select code or open a file to review for bugs.")
        run(project, editor, "Bug Review", "${BuiltInPrompts.FIND_BUGS}\n\n$context")
    }
}

class GenerateTestsAction : AiEditorAction() {
    override fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ) {
        val context = codeContext(virtualFile, selection, fileText)
            ?: return showMessage(project, "Select code or open a file to generate tests for.")
        run(project, editor, "Generated Tests", "${BuiltInPrompts.GENERATE_TESTS}\n\n$context", codeOutput = true)
    }
}

class ConvertLanguageAction : AiEditorAction() {
    override fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ) {
        val context = codeContext(virtualFile, selection, fileText)
            ?: return showMessage(project, "Select code or open a file to convert.")
        val target = askInput(
            project,
            "Convert to Another Language",
            "Target programming language:"
        )?.trim()
        if (target.isNullOrEmpty()) return
        run(
            project,
            editor,
            "Converted to $target",
            "${BuiltInPrompts.convertToLanguage(target)}\n\n$context",
            codeOutput = true
        )
    }
}

class GenerateCodeFromDescriptionAction : AiEditorAction() {
    override fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ) {
        val description = askInput(
            project,
            "Generate Code from Description",
            "Describe the code you want generated:"
        )?.trim()
        if (description.isNullOrEmpty()) return
        val context = codeContext(virtualFile, selection, fileText)
        val prompt = if (context != null) {
            "${BuiltInPrompts.generateCodeFromDescription(description)}\n\nFor reference, the surrounding code:\n$context"
        } else {
            BuiltInPrompts.generateCodeFromDescription(description)
        }
        run(project, editor, "Generated Code", prompt, codeOutput = true)
    }
}

class ExplainRuntimeErrorAction : AiEditorAction() {
    override fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ) {
        val trace = askInput(
            project,
            "Explain Runtime Error",
            "Paste the error message or stack trace:",
            initial = selection.orEmpty()
        )?.trim()
        if (trace.isNullOrEmpty()) return
        run(
            project,
            editor,
            "Runtime Error Explanation",
            "${BuiltInPrompts.EXPLAIN_RUNTIME_ERROR}\n\nError or stack trace:\n$trace"
        )
    }
}
