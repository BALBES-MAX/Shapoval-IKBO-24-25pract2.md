package ru.mirea.depvis;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Модульные тесты для проверки конфигурации, обхода с циклами и обратных зависимостей.
 */
public class GraphEngineTest {

    /**
     * Запускает набор проверок без внешних тестовых фреймворков.
     *
     * @param args аргументы командной строки
     * @throws Exception при ошибках ввода-вывода
     */
    public static void main(String[] args) throws Exception {
        testCyclicGraphAndReverseDependencies();
        testAsciiTreeCycleProtection();
        System.out.println("Все тесты успешно пройдены!");
    }

    private static void testCyclicGraphAndReverseDependencies() throws Exception {
        Path tempRepo = Files.createTempFile("repo", ".txt");
        Files.writeString(tempRepo, "A: B C\nB: D\nC: D\nD: A\n");
        DependencyResolver resolver = new TestRepositoryResolver(tempRepo.toString());
        GraphEngine engine = new GraphEngine(resolver);

        Map<String, List<String>> graph = engine.buildTransitiveGraph("A");
        assert graph.size() == 4 : "Ожидалось 4 узла в графе";

        Set<String> reverseForD = engine.findReverseDependencies("D");
        assert reverseForD.containsAll(Set.of("B", "C", "A")) : "Неверные обратные зависимости для D";
        Files.deleteIfExists(tempRepo);
    }

    private static void testAsciiTreeCycleProtection() {
        Map<String, List<String>> cyclic = Map.of(
                "A", List.of("B"),
                "B", List.of("A")
        );
        GraphVisualizer visualizer = new GraphVisualizer();
        String tree = visualizer.toAsciiTree("A", cyclic);
        assert tree.contains("A (cycle)") : "Цикл должен быть корректно помечен в ASCII-дереве";
    }
}
