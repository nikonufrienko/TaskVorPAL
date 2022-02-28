import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.SwitchEntry
import com.github.javaparser.ast.stmt.SwitchStmt

class StaticSwitchCheckedCollector {
    companion object {
        val checkedSwitchSet = mutableSetOf<SwitchStmt>()
    }
}

/** Возвращает Pair(isDanger,indexOfExternalSwitch),
 * если isDanger == true, то switch оперирующий данным NameExpr не был найден.
 * externalSwitch -- это самый внешний switch который оперирует над переменной.
 *  **/
fun NameExpr.checkSwitchStatements(): Pair<Boolean, Node> {
    var isDanger = true
    var currentNode: Node = this
    var prevNode: Node
    var lastSwitch: Node? = null
    while (currentNode.hasParentNode() && currentNode::class.java != MethodDeclaration::class.java) {
        prevNode = currentNode
        currentNode = currentNode.parentNode.get()
        if (currentNode::class.java == SwitchStmt::class.java && prevNode::class.java == SwitchEntry::class.java
            && (currentNode as SwitchStmt).selector.toString() == this.name.toString()
        ) {
            isDanger = false
            lastSwitch = currentNode
        }
    }
    return Pair(isDanger, lastSwitch ?: this)
}
