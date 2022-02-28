/**
 * -xml -- вывод результата в <input>.xml
 * -json -- вывод результата в <input>.json
 * -yaml -- вывод результата в <input>.yml
 * */

public class Main {
    public static void main(String[] args) {
        try {
            MainKt.main(args);
        } catch (IllegalArgumentException e){
            System.out.println(e.getMessage());
        }
    }
}

