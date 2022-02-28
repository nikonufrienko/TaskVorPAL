import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.IfStmt
import org.python.util.PythonInterpreter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.io.File
import java.io.OutputStream

data class ResultOfConditionChecking(
    val availableStatus: AvailableStatus,
    val indexesOfUselessChecking: List<Int>? = null,
    val blockingIfStmt: IfStmt? = null
)

fun checkConditionStack(
    conditionStack: List<Expression>,
    isElseBranch: Boolean,
    currName: NameExpr,
    commentingEnable: Boolean = true
): ResultOfConditionChecking {
    var availableCounter = 0
    val listOfWrongCheckingIndexes = mutableListOf<Int>()
    for (conditionSource in conditionStack.reversed()) {
        var isActive = false //если здесь обнаружиться сравнение переменной с null
        val listForIgnore = mutableListOf<Expression>() //эти узлы будем игнорировать
        var conditionForChanging: Node =
            StaticJavaParser.parseExpression(conditionSource.toString())//это мы будем менять
        if (conditionForChanging.contain(NullLiteralExpr::class.java)) {
            conditionForChanging.findAll(NullLiteralExpr::class.java).forEach { nullExpr ->
                if (nullExpr.parentNode.get()::class.java == BinaryExpr::class.java) {
                    val conditionWithNull = nullExpr.parentNode.get() as BinaryExpr
                    if (conditionWithNull.left.toString() == currName.toString() ||
                        conditionWithNull.right.toString() == currName.toString()
                    ) {
                        isActive = true
                        if (conditionWithNull.operator == BinaryExpr.Operator.EQUALS) {
                            conditionForChanging = conditionForChanging.getCopyWithReplaced(conditionWithNull,
                                BooleanLiteralExpr().setValue(true).also { listForIgnore.add(it) })
                        } else if (conditionWithNull.operator == BinaryExpr.Operator.NOT_EQUALS) {
                            conditionForChanging = conditionForChanging.getCopyWithReplaced(conditionWithNull,
                                BooleanLiteralExpr().setValue(false).also { listForIgnore.add(it) })
                        }
                    }
                }
            }
        }
        if (isActive) { // если было сравнение переменной с null

            conditionForChanging.findAll(Expression::class.java).forEach { expression ->
                var forSimplify = true
                listForIgnore.forEach {
                    if (expression.contain(it)) {
                        forSimplify = false
                    }
                }
                if (forSimplify) {
                    expression.replace(NameExpr().setName("X"))
                }
            }

            val conditionStatus =
                StaticSolver.solvePythonScript((if (!isElseBranch) "" else "!(") + conditionForChanging.toString() + (if (!isElseBranch) "" else ")"))

            when (conditionStatus) {
                ConditionStatus.ALWAYS_FALSE -> {
                    //=> проверяемое выражение всегда является недостижимым при variable == null

                    if (!isElseBranch && commentingEnable) StaticCommentsCollector.addComment(
                        conditionSource.end.get().line,
                        "Always false when ${currName.name} == null"
                    )

                    return ResultOfConditionChecking(
                        AvailableStatus.NEVER_AVAILABLE,
                        listOfWrongCheckingIndexes,
                        conditionSource.parentNode.get() as IfStmt //TODO: возможно ли здесь исключение?
                    )
                }
                ConditionStatus.ALWAYS_TRUE -> {
                    //=> проверяемое выражение всегда является достижимым при variable == null
                    availableCounter++
                    listOfWrongCheckingIndexes.add(conditionSource.end.get().line)
                }
                else -> {
                    if (!isElseBranch && commentingEnable) StaticCommentsCollector.addComment(
                        conditionSource.end.get().line,
                        "may be true or false when ${currName.name} == null"
                    )
                    listOfWrongCheckingIndexes.add(conditionSource.end.get().line)
                }
            }
        }
    }
    return ResultOfConditionChecking(AvailableStatus.MAYBE_AVAILABLE, listOfWrongCheckingIndexes)
}

enum class ConditionStatus { ALWAYS_TRUE, ALWAYS_FALSE, MAYBE_TRUE_OR_FALSE }
enum class AvailableStatus { MAYBE_AVAILABLE, NEVER_AVAILABLE } //TODO: переделай это

class StaticSolver {
    companion object {
        private var useJythonSolver = false
        fun initJythonSolver() {
            useJythonSolver = true
        }

        fun solvePythonScript(input: String): ConditionStatus {
            val result: String
            if (!useJythonSolver) {
                if (!Files.exists(File("solver.py").toPath())) {
                    Files.write(File("solver.py").toPath(), javaClass.getResource("solver.py")!!.readBytes());
                }
                val processBuilder = ProcessBuilder("python3", "solver.py")
                processBuilder.redirectErrorStream(true)
                val process = processBuilder.start()
                process.outputStream.write((input + "\n").toByteArray())
                process.outputStream.flush()
                result = process.inputStream.bufferedReader().readLine()
            } else {
                val jythonSolver = JythonSolver()
                jythonSolver.runPythonScript(input)
                result = jythonSolver.inputStream.bufferedReader().readLine()
            }
            if (result.isEmpty()) error("python script error")
            return when (result) {
                "may be true or false" -> ConditionStatus.MAYBE_TRUE_OR_FALSE
                "only false" -> ConditionStatus.ALWAYS_FALSE
                else -> ConditionStatus.ALWAYS_TRUE
            }
        }
    }
}

/*Решатель использующий Jython (опция на случай если не установлен Python3)*/
class JythonSolver() {
    companion object {
        const val pythonSolverScript =
            "line = line.replace(\"!\", \" not \").replace(\"&&\", \" and \").replace(\"||\", \" or \")" +
                    ".replace(\"|\", \" or \").replace(\"&\", \" and \").replace(\"false\", \" False \")" +
                    ".replace(\"true\", \" True \")\n" +
                    "n = line.count(\"X\")\n" +
                    "arr = []\n" +
                    "for i in range(2 ** n):\n" +
                    "    line2 = line\n" +
                    "    value = bin(i)[2:]\n" +
                    "    value = ((n - len(value)) * \"0\") + value\n" +
                    "    for char in value:\n" +
                    "        if char == \"1\":\n" +
                    "            line2 = line2.replace(\"X\", \" True \", 1)\n" +
                    "        else:\n" +
                    "            line2 = line2.replace(\"X\", \" False \", 1)\n" +
                    "    arr.append(eval(line2))\n" +
                    "if True in arr and False in arr:\n" +
                    "    print(\"may be true or false\")\n" +
                    "elif True in arr:\n" +
                    "    print(\"only true\")\n" +
                    "elif False in arr:\n" +
                    "    print(\"only false\")"
    }

    private val pyInterp = PythonInterpreter()
    val inputStream = ByteArrayInputStream(ByteArray(1024))

    init {
        pyInterp.setIn(inputStream)
        pyInterp.setOut(ByteArrayOutputStream())
    }

    fun runPythonScript(valueToSolve: String) {
        pyInterp.exec("line = \"${valueToSolve}\" \n" + pythonSolverScript)
    }
}
