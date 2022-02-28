import com.github.javaparser.Position
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.IfStmt

enum class State { NONE, ASSIGNED, DECLARED }
data class CheckingResult(val isDanger: Boolean, val description: String) {
    fun getInfo(): String = (if (isDanger) " [ERROR!] " else " [OK] ") + description
}

data class AssignOrDeclaredValue(
    var state: State,
    val declarator: VariableDeclarator?,
    val assignExpr: AssignExpr?,
    val nameExpr: NameExpr
) {
    fun getAnaliseNullByDeclarationOrAssign(isNotNullParameter: Boolean = false): CheckingResult {
        var description = "" //TODO: StringBuilder()!!!
        val isDanger: Boolean

        when (this.state) {
            State.DECLARED -> {
                if (this.declarator!!.type.isPrimitiveType) {
                    description += "[PRIMITIVE TYPE]"
                    isDanger = false
                } else {
                    description += " [Declarator: ${this.declarator}]"
                    isDanger = !(this.declarator.childNodes.size >= 3
                            && this.declarator.childNodes[2].toString() != "null")
                }
            }
            State.ASSIGNED -> {
                description += " [Assigner: ${this.assignExpr}]"
                isDanger = this.assignExpr!!.value.toString() == "null"
            }
            else -> {
                var currNode: Node = nameExpr
                while (currNode.javaClass != MethodDeclaration::class.java) {
                    if (!currNode.hasParentNode()) {
                        description += "[This scope located not in method!]"
                        return CheckingResult(isNotNullParameter, description)
                    }
                    currNode = currNode.parentNode.get()
                }
                val method: MethodDeclaration = currNode as MethodDeclaration
                if (method.parameters.map { it.name }.contains(nameExpr.name)) {//
                    val annotations =
                        method.parameters.find { it.name == nameExpr.name }!!.annotations.map { it.name.toString() }
                    description += if (annotations.isNotEmpty()) " [Declared as method parameter with annotations: ${annotations.joinToString { it -> " ${it} " }}!]"
                    else "[Declared as method parameter!]"
                    return CheckingResult(
                        annotations.contains("Nullable") ||
                                (isNotNullParameter && (!annotations.contains("NotNull"))),
                        description
                    )
                } else {
                    description += "[External variable!!]"
                    return CheckingResult(isNotNullParameter, description) // переменная рассматривается как notNull
                }
            }
        }
        return CheckingResult(isDanger, description)
    }

    fun getConditions(): Pair<List<Expression>, List<Expression>> {
        val startPos: Position
        when (state) {
            State.ASSIGNED -> {
                startPos = assignExpr!!.end.get()
            }
            State.DECLARED -> {
                startPos = declarator!!.end.get()
            }
            else -> {
                var currNode: Node = nameExpr
                while (currNode.javaClass != MethodDeclaration::class.java && currNode.hasParentNode()) {
                    currNode = currNode.parentNode.get()
                }
                startPos = currNode.begin.get()
            }
        }
        var currNode: Node = nameExpr
        var prevNode: Node
        val thenCondStack = mutableListOf<Expression>()
        val elseCondStack = mutableListOf<Expression>()
        while (currNode.hasParentNode() && currNode::class.java != MethodDeclaration::class.java) {
            prevNode = currNode
            currNode = currNode.parentNode.get()
            if ((currNode::class.java == IfStmt::class.java) && (currNode as IfStmt).begin.get().isAfter(startPos)) {
                if (prevNode == currNode.thenStmt) {
                    thenCondStack.add(currNode.condition)
                } else if (prevNode == currNode.elseStmt) {
                    elseCondStack.add(currNode.condition)
                }
            }
        }
        return Pair(thenCondStack, elseCondStack)
    }
}

fun NameExpr.findVariableAssignOrDeclaration(): AssignOrDeclaredValue {
    val name = this.name.asString()
    var currExpr = this.parentNode.get()
    while (currExpr.hasParentNode()) {
        val declarationList = mutableListOf<Pair<VariableDeclarator, Int>>()
        val assignList = mutableListOf<Pair<AssignExpr, Int>>()
        currExpr.findAll(VariableDeclarator::class.java).forEach { declarator ->
            if (declarator.end.get().isBefore(this.begin.get()) &&
                declarator.name.asString() == name &&
                declarator.parentNode.get().parentNode.get().parentNode.get() == currExpr
            ) {
                declarationList.add(Pair(declarator, declarator.begin.get().line))
            }
        }
        currExpr.findAll(AssignExpr::class.java).forEach { assignExpr ->
            if (assignExpr.end.get().isBefore(this.begin.get()) &&
                assignExpr.target.asNameExpr().name.asString() == name &&
                assignExpr.parentNode.get().parentNode.get() == currExpr
            ) {
                assignList.add(Pair(assignExpr, assignExpr.begin.get().line))
            }
        }
        when {
            declarationList.isNotEmpty() && assignList.isNotEmpty() -> {
                return if (declarationList.last().second > assignList.last().second) {
                    AssignOrDeclaredValue(State.DECLARED, declarationList.last().first, null, this)
                } else {
                    AssignOrDeclaredValue(State.ASSIGNED, null, assignList.last().first, this)
                }
            }
            declarationList.isNotEmpty() ->
                return AssignOrDeclaredValue(State.DECLARED, declarationList.last().first, null, this)
            assignList.isNotEmpty() ->
                return AssignOrDeclaredValue(State.ASSIGNED, null, assignList.last().first, this)
        }
        currExpr = currExpr.parentNode.get()
    }
    return AssignOrDeclaredValue(State.NONE, null, null, this)
}
