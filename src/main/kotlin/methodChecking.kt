import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr


/**
 * Обрабатывает все вызовы методов (в пределах одного класса) и кладет результат обработки в StaticCommentsCollector.
 * ***/
fun List<MethodDeclaration>.checkMethodByCalling() {
    val result = mutableMapOf<Int, String>()
    val methodsMap = this.toMethodsMap()
    for (method in this) {
        method.body.get().findAll(MethodCallExpr::class.java).forEach { callExpr ->

            if (callExpr.childNodes[0]::class.java == NameExpr::class.java ||
                (callExpr.childNodes[0]::class.java == FieldAccessExpr::class
                        && callExpr.childNodes[0].childNodes[0]::class.java == NameExpr::class)
            ) {
                val name: NameExpr =
                    if (callExpr.childNodes[0]::class.java == NameExpr::class.java)
                        callExpr.childNodes[0] as NameExpr
                    else callExpr.childNodes[0].childNodes[0] as NameExpr
                val resultOfChecking = name.checkVariable()
                if (resultOfChecking.isDanger) {
                    result.addStringByKey(
                        callExpr.end.get().line,
                        "[Variable ${name.name} -- ${resultOfChecking.getInfo()}]"
                    )
                }
            } else {
                for ((index, argument) in callExpr.arguments.withIndex()) {
                    if (methodsMap.containsKey(callExpr.name.toString())) {
                        val parameters = methodsMap[callExpr.name.toString()]!!.parameters
                        if (parameters.size > index && parameters[index].annotations.map { it.toString() }
                                .contains("@NotNull")) {
                            //проверяем только в том случае если аннотации аргумента содержат NotNull
                            val name = argument.asNameExpr()
                            val resultOfChecking =
                                name.checkVariable(isNotNullParameter = true) //передается методу как NotNull
                            if (resultOfChecking.isDanger) {
                                result.addStringByKey(
                                    callExpr.end.get().line,
                                    "[Variable ${name.name}  -- ${resultOfChecking.getInfo()}]"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    StaticCommentsCollector.addComments(result)
}

/**
 * Проверяет обращение к полям переменной, и записывает результат проверки.
 */

fun List<MethodDeclaration>.checkMethodsByFieldAccess() {
    val result = mutableMapOf<Int, String>()
    val methodsMap = this.toMethodsMap()
    for (method in this) {
        method.body.get().findAll(FieldAccessExpr::class.java).forEach { fieldAccessExpr ->
            if (fieldAccessExpr.childNodes[0]::class.java == NameExpr::class.java) {
                val resultOfChecking =
                    (fieldAccessExpr.childNodes[0] as NameExpr).checkVariable(
                        method.parameters.map { it.name.toString() }
                            .contains((fieldAccessExpr.childNodes[0] as NameExpr).name.toString())
                                &&
                                method.parameters
                                    .find { it.name.toString() == (fieldAccessExpr.childNodes[0] as NameExpr).name.toString() }!!
                                    .annotations.map { it.toString() }
                                    .contains("@Nullable")

                    )

                if (resultOfChecking.isDanger) {
                    result.addStringByKey(
                        fieldAccessExpr.end.get().line,
                        "[Variable: ${fieldAccessExpr.childNodes[0]} -- ${resultOfChecking.getInfo()}]"
                    )
                }
            }
        }
    }
    StaticCommentsCollector.addComments(result)
}