package ru.mirea.depvis;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Точка входа CLI-приложения визуализации графа зависимостей (Вариант №27).
 */
public class Main {

    private static final String DEFAULT_CONFIG = "config.csv";

    /**
     * Запускает полный цикл анализа и визуализации зависимостей.
     *
     * @param args аргументы командной строки (первый аргумент — путь к CSV-конфигурации)
     */
    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG;
        try {
            runApplication(configPath);
        } catch (RuntimeException e) {
            System.err.println("Ошибка выполнения: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runApplication(String configPath) {
        Config config = Config.fromCsv(configPath);
        printConfiguration(config);

        DependencyResolver resolver = config.testMode()
                ? new TestRepositoryResolver(config.repoUrl())
                : new MavenRepositoryResolver(config.repoUrl());

        GraphEngine engine = new GraphEngine(resolver);
        Map<String, List<String>> graph = engine.buildTransitiveGraph(config.packageName());

        GraphVisualizer visualizer = new GraphVisualizer();
        String dot = visualizer.toGraphvizDot(graph);
        System.out.println("\n=== Представление Graphviz (DOT) ===");
        System.out.println(dot);
        visualizer.renderAndDisplay(dot, "dependency_graph");

        if (config.asciiMode()) {
            System.out.println("=== ASCII-дерево зависимостей ===");
            System.out.println(visualizer.toAsciiTree(config.packageName(), graph));
        }

        Set<String> reverseDeps = engine.findReverseDependencies(config.packageName());
        System.out.println("=== Обратные зависимости для " + config.packageName() + " ===");
        System.out.println(reverseDeps.isEmpty() ? "Не найдены" : String.join(", ", reverseDeps));
    }

    private static void printConfiguration(Config config) {
        System.out.println("package_name=" + config.packageName());
        System.out.println("repo_url=" + config.repoUrl());
        System.out.println("test_mode=" + config.testMode());
        System.out.println("ascii_mode=" + config.asciiMode());
    }
}
