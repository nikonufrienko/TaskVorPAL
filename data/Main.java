import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class Main {
    void foo(@NotNull Point p) {
    }

    void bar(Point p) {
    }

    void sample(
            @NotNull Point nonNullP, // definitely not null
            @Nullable Point nullP, // may be null and need checks
            Point defaultP // may be null but we don't care
    ) {
        Integer s1 = new Integer(1), s2 = new Integer(2);
        if (nonNullP != null) {
            int x = nonNullP.x;
        }

        foo(nullP);
        if (something()) {

        } else {
            if (nullP != null && something()) {
                foo(nullP);
            }
        }

        if (nullP == null) {
            System.out.println("nullP is null");
        } else {
            System.out.println(nullP);
        }

        int z = nullP.x;

        bar(nullP);

        if (defaultP != null) {
            defaultP = new Point(0, 0);
            foo(defaultP);
        }

        Valera a = null;

        if ((((!(defaultP == null) & defaultP != null & zerg == null) && ((1 & 1) == 1)
                && (foo() && bar())) || something()) ^ something()) {
            int b = defaultP.x;
        }
        bar(defaultP);
        defaultP.doSomthing();
        foo(defaultP);
        foo(s1);
        foo(z);

        switch (a) {
            case 1 -> {if(a!= null) a.foo();}
        }

        Integer b = null;
        b.foo();
        this.somthing.foo();
        if (variable != null || "zzz".equals("xxx")) {
            variable.foo();
        }
        int a = 1;
        int b = 1;
        if (variable != null || a != b) {
            variable.foo();
        }

        if (variable == null || 1 != 0) {
            variable.foo();
        }
        switch (nullP) {
            case 1 -> nullP.x;
        }
    }
}
