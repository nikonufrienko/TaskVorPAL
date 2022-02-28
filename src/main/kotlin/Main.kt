import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import org.jetbrains.annotations.NotNull
import java.io.FileReader
import java.lang.IllegalArgumentException


fun parseGroupedMethods(@NotNull input: String): List<List<MethodDeclaration>> {
    val parsingError:String
    try {
        val cu = StaticJavaParser.parse(input)
        val methodsGroups = mutableListOf<MutableList<MethodDeclaration>>()
        cu.findAll(ClassOrInterfaceDeclaration::class.java).forEach { classNode ->
            methodsGroups.add(mutableListOf())
            classNode.methods.forEach { method ->
                methodsGroups.last().add(method)
            }
        }
        return methodsGroups
    } catch (e: ParseProblemException) {
        parsingError = e.message.toString().lines()[0]
    }
    throw IllegalArgumentException(parsingError)
}

class ArgKeys {
    companion object {
        val argumentActions = mapOf<String, (String) -> Unit>(
            Pair("-yaml", StaticCommentsCollector::writeYAMLFile),
            Pair("-xml", StaticCommentsCollector::writeXMLFile),
            Pair("-json", StaticCommentsCollector::writeJSONFile)
        )
        val modifiers = mapOf(
            Pair("-yaml", ".yml"),
            Pair("-xml", ".xml"),
            Pair("-json", ".json")
        )
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        throw IllegalArgumentException("There are no arguments!")
    }
    val filePath = args[0]
    args.copyOfRange(1, args.size).forEach {
        if (!ArgKeys.argumentActions.keys.contains(it)) {
            throw IllegalArgumentException("Wrong argument: $it!")
        }
    }

    val fileString = try {
        FileReader(filePath).readText()
    } catch (e: Exception) {
        throw IllegalArgumentException("Wrong file name!")
    }

    val methodsGroups = parseGroupedMethods(fileString)
    val comments = mutableMapOf<Int, String>()

    methodsGroups.forEach {
        it.checkMethodByCalling()
    }
    StaticCommentsCollector.addComments(comments)
    val commentsFieldAccess = mutableMapOf<Int, String>()
    methodsGroups.forEach {
        it.checkMethodsByFieldAccess()
    }
    StaticCommentsCollector.addComments(commentsFieldAccess)
    methodsGroups.forEach {
        it.forEach { method ->
            StaticCommentsCollector.addComments(findUselessChecking(method))
        }
    }
    StaticSolver.initJythonSolver()

    args.copyOfRange(1, args.size)
        .forEach {
            ArgKeys.argumentActions[it]!!.invoke(filePath.modifyFilePath(ArgKeys.modifiers[it]!!))
        }
    StaticCommentsCollector.writeCommentedSourceCode(filePath.modifyFilePath("Commented.java"), fileString)
}

fun String.modifyFilePath(modifier: String): String {
    return this.substringBeforeLast(".") + modifier
}

fun List<MethodDeclaration>.toMethodsMap(): Map<String, MethodDeclaration> {
    val result = mutableMapOf<String, MethodDeclaration>()
    this.forEach { result[it.name.toString()] = it }

    return result
}

fun MutableMap<Int, String>.addStringByKey(index: Int, string: String) {
    if (this[index] == null) {
        this[index] = string
    } else {
        this[index] += string
    }
}