package dev.localai.pycharmassistant.actions

internal object BuiltInPrompts {
    const val GENERATE_DOCUMENTATION =
        "You are an expert technical writer. Write clear documentation for the given code " +
            "in the documentation style appropriate for its language (KDoc, Python docstring, JSDoc, etc.). " +
            "Cover purpose, parameters, return values, exceptions, and key behavior. " +
            "Return only the documentation, or the code with the documentation added when appropriate."

    const val SUGGEST_NAMES =
        "You are an expert programmer. Suggest clearer, conventional names for the variables, functions, " +
            "classes, and parameters in the following code. First give a short mapping from old names to new " +
            "names with a one-line reason, then return the complete code with the better names applied."

    const val SUGGEST_REFACTORING =
        "You are an expert refactoring engineer. Analyze the following code and propose concrete refactorings " +
            "that improve readability, maintainability, and performance without changing behavior. " +
            "List the suggested changes briefly, then return the refactored code."

    const val FIND_BUGS =
        "You are a meticulous code reviewer. Find bugs, edge cases, race conditions, and potential runtime " +
            "errors in the following code. For each issue state its severity, explain the problem, and show the " +
            "corrected code. If the code is correct, say so and still list possible improvements."

    const val GENERATE_TESTS =
        "You are a QA expert. Write a thorough unit test file for the following code using the standard testing " +
            "framework for its language (for example pytest for Python, JUnit for Kotlin/Java). Cover the happy " +
            "path, edge cases, and error cases. Return only the test code without extra explanation."

    const val EXPLAIN_RUNTIME_ERROR =
        "You are an expert debugging engineer. Explain the following runtime error or stack trace: identify the " +
            "root cause, point to where it occurs, and give a concrete fix with code. Be concise."

    const val ASK_PROGRAMMING =
        "Answer the following programming question clearly and concisely. Include short, focused code examples " +
            "when they help."

    const val ASK_PROJECT_CODE =
        "You are analyzing this project's source code. Answer the question using the provided code context. " +
            "Reference the relevant functions, classes, or files by name when answering, and be concise."

    const val GENERATE_COMMIT_MESSAGE =
        "You are writing a Git commit message. Summarize the staged changes in Conventional Commits style. " +
            "The first line is an imperative summary of at most 72 characters, followed by a blank line and a few " +
            "short bullet points only when the change needs detail. Do not mention files that did not change. " +
            "Return only the commit message."

    const val EXPLAIN_COMMIT =
        "Explain what the following Git commit changes and why, based solely on its metadata and diff. " +
            "Summarize the intent, list the main behavioral or file changes, and flag anything that looks risky."

    const val ASK_VCS =
        "You are a Git expert helping with this repository. The current git status and recent history are " +
            "provided below. Answer the user's question using that information and general Git knowledge, " +
            "and include concrete commands when useful."

    fun convertToLanguage(targetLanguage: String): String =
        "Convert the following code to $targetLanguage. Produce idiomatic, runnable $targetLanguage code that " +
            "preserves behavior. Return only the converted code without markdown fences or explanation."

    fun generateCodeFromDescription(description: String): String =
        "You are an expert software engineer. Implement code that satisfies this description:\n\n" +
            "$description\n\nReturn only complete, runnable code without markdown fences or extra explanation."

    /** Preset entries shown in the prompt library. */
    val LIBRARY: List<Pair<String, String>> = listOf(
        "Generate Documentation" to GENERATE_DOCUMENTATION,
        "Suggest Better Names" to SUGGEST_NAMES,
        "Suggest Refactoring" to SUGGEST_REFACTORING,
        "Find Bugs and Suggest Fixes" to FIND_BUGS,
        "Generate Unit Tests" to GENERATE_TESTS,
        "Explain Runtime Error" to EXPLAIN_RUNTIME_ERROR,
        "Ask Programming Question" to ASK_PROGRAMMING
    )
}
