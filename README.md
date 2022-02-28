# TaskVorPAL
  Запуск из терминала:
    
    java -jar TaskVorPAL-1.0-SNAPSHOT-all.jar <имя обрабатывемого файла> <ключи>
    
  По умолчанию генерируется только файл <имя обрабатывемого файла>Сommented.java с выводом ошибок в комментариях.
  
  Возможные ключи:
        
    -xml -- генерирует на выходе xml-файл.
    -yaml -- генерирует на выходе yml-файл.
    -json -- генерирует на выходе json-файл.
    --use-native-python -- пытается подключить нативный python3 вместо jython.
    
   Пример запуска:
   
       java -jar TaskVorPAL-1.0-SNAPSHOT-all.jar Main.java --use-native-python -xml -json -yaml
