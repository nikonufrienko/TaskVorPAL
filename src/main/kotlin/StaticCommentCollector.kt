import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors


class StaticCommentsCollector {
    companion object {
        private val comments = mutableMapOf<Int, String>()
        fun addComment(line: Int, comment: String) {
            if (comments.containsKey(line))
                comments[line] += " [$comment]"
            else comments[line] = "[$comment]"
        }

        fun addComments(comments: Map<Int, String>) {
            comments.forEach {
                    (key, value) ->
                addComment(key, value)
            }
        }

        private fun String.addCommentsAfterLines(comments: Map<Int, String>): String {
            val currComments = comments.toMutableMap()
            val lines = this.lines().toMutableList()
            val builder = StringBuilder()
            for (i in lines.indices) {
                if (currComments.keys.contains(i + 1)) {
                    lines[i] = lines[i] + (if (!lines[i].contains("//")) "//[${i + 1}]" else " ") + currComments[i + 1]
                }
                builder.append(lines[i] + "\n")
            }
            return builder.toString()
        }

        fun writeCommentedSourceCode(pathName: String, sourceCode: String){
            Files.write(File(pathName).toPath(), sourceCode.addCommentsAfterLines(comments).toByteArray())
        }

        fun writeJSONFile(pathName: String){
            val json = "{\n" + comments.toSortedMap().entries.stream()
                .map { e ->
                    "\"" + e.key.toString() + "\":\"" + e.value + "\""
                }
                .collect(Collectors.joining(", \n")).toString() + "\n}"
            Files.write(File(pathName).toPath(), json.toByteArray())
        }

        fun writeXMLFile(pathName: String){
            val stringXML = "<errorMap>\n" + comments.toSortedMap().entries.stream()
                .map { e ->
                    "\t<error>\n" +
                    "\t\t<line>" + e.key.toString() + "</line>\n" +
                    "\t\t<description>" + e.value + "</description>\n" +
                    "\t</error>\n"
                }
                .collect(Collectors.joining()).toString() + "</errorMap>\n"
            Files.write(File(pathName).toPath(), stringXML.toByteArray())
        }

        fun writeYAMLFile(pathName: String) {
            val stringYAML = "errorMap:\n" + comments.toSortedMap().entries.stream()
                .map { e ->
                    "\t" + e.key.toString() + " : \"" + e.value + "\""

                }.collect(Collectors.joining("\n")).toString()
            Files.write(File(pathName).toPath(), stringYAML.toByteArray())
        }
    }
}
