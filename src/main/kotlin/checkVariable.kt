import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.IfStmt


fun NameExpr.checkVariable(
    commentingEnabled: Boolean = true,
    isNotNullParameter: Boolean = false
    // передаётся ли переменная в качестве notNull параметра в метод?
    // если да -- то опасным источником будет считаться аргумент без аннотаций
): CheckingResult {
    this.checkSwitchStatements()
    val analyzer = this.findVariableAssignOrDeclaration()
    val firstAnalyze = analyzer.getAnaliseNullByDeclarationOrAssign(isNotNullParameter)
    val uselessCheckingLinesList = mutableListOf<Int>() // список для бесполезных if-ов
    if (firstAnalyze.isDanger) {
        var secondIsDanger = true //презумпция виновности!
        val switchCheckingResult = this.checkSwitchStatements()
        val thenConditionsAndElseConditions = analyzer.getConditions()
        val ifCondRes =
            checkConditionStack(thenConditionsAndElseConditions.first, false, this, commentingEnabled)
        val elseCondRes =
            checkConditionStack(thenConditionsAndElseConditions.second, true, this, commentingEnabled)
        uselessCheckingLinesList.addAll(ifCondRes.indexesOfUselessChecking!!)
        uselessCheckingLinesList.addAll(elseCondRes.indexesOfUselessChecking!!)
        var ifElseStmt: IfStmt? = null

        if ((elseCondRes.blockingIfStmt == null && ifCondRes.blockingIfStmt != null)
            || ((elseCondRes.blockingIfStmt != null && ifCondRes.blockingIfStmt != null) && ifCondRes.blockingIfStmt.end.get()
                .isBefore(elseCondRes.blockingIfStmt.end.get()))
        ) {
            ifElseStmt = ifCondRes.blockingIfStmt
        } else if (elseCondRes.blockingIfStmt != null) {
            ifElseStmt = elseCondRes.blockingIfStmt
        }
        if (ifCondRes.availableStatus == AvailableStatus.NEVER_AVAILABLE || elseCondRes.availableStatus == AvailableStatus.NEVER_AVAILABLE) {
            secondIsDanger = false
        }

        if ((!switchCheckingResult.first) && (ifElseStmt == null || switchCheckingResult.second.contain(ifElseStmt))) {
            if (commentingEnabled) StaticCommentsCollector.addComment(
                switchCheckingResult.second.begin.get().line,
                "[ERROR: ${this} may be null there!!]"
            )
            secondIsDanger = false
        }
        if (commentingEnabled) {
            uselessCheckingLinesList.forEach {
                StaticCommentsCollector.addComment(it, "[Useless checking for $this]")
            }
        }
        return CheckingResult(secondIsDanger, firstAnalyze.description)
    }
    return CheckingResult(false, firstAnalyze.description)
}

fun <T : Node> Node.contain(javaClass: Class<T>): Boolean = this.findAll(javaClass).isNotEmpty()
fun Node.contain(node: Node): Boolean = this.findAll(node::class.java).contains(node) || this == node
fun Node.getCopyWithReplaced(nodeToReplace: Node, newNode: Node): Node {
    if (nodeToReplace == this) {
        return newNode
    }
    nodeToReplace.replace(newNode)
    return this
}

