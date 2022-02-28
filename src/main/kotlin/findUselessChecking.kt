import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.NullLiteralExpr
import com.github.javaparser.ast.stmt.IfStmt

fun findUselessChecking(method: MethodDeclaration): Map<Int, String> {
    val resultComments = mutableMapOf<Int, String>()
    method.findAll(IfStmt::class.java).forEach { ifStmt ->
        ifStmt.findAll(NullLiteralExpr::class.java).forEach { nullExpr ->
            if (nullExpr.parentNode.get()::class.java == BinaryExpr::class.java) {
                nullExpr.parentNode.get().findAll(NameExpr::class.java).forEach { nameExpr ->
                    if (!nameExpr.checkVariable(commentingEnabled = false).isDanger) {
                        resultComments[nameExpr.end.get().line] = "[useless null checking]"
                    }
                }
            }
        }
    }
    return resultComments
}

