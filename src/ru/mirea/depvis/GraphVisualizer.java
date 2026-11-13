package ru.mirea.depvis;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Генератор представлений графа зависимостей в форматах Graphviz (DOT) и ASCII-дерева.
 */
public class GraphVisualizer {

    private static final int EXIT_SUCCESS = 0;

    /**
     * Формирует текстовое описание направленного графа на языке Graphviz DOT.
     *
     * @param graph карта смежности графа
     * @return исходный код диаграммы на языке DOT
     */
    public String toGraphvizDot(Map<String, List<String>> graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph Dependencies {\n");
        sb.append("    rankdir=LR;\n");
        sb.append("    node [shape=box, style=rounded];\n");
        for (Map.Entry<String, List<String>> entry : graph.entrySet()) {
            appendDotEdges(sb, entry.getKey(), entry.getValue());
        }
        sb.append("}\n");
        return sb.toString();
    }

    private void appendDotEdges(StringBuilder sb, String from, List<String> targets) {
        if (targets.isEmpty()) {
            sb.append(String.format("    \"%s\";%n", from));
            return;
        }
        for (String to : targets) {
            sb.append(String.format("    \"%s\" -> \"%s\";%n", from, to));
        }
    }

    /**
     * Сохраняет DOT-файл, пытается отрендерить PNG через утилиту dot и открыть изображение на экране.
     *
     * @param dotContent текст на языке Graphviz
     * @param baseName   базовое имя выходных файлов
     */
    public void renderAndDisplay(String dotContent, String baseName) {
        Path dotPath = Path.of(baseName + ".dot");
        Path pngPath = Path.of(baseName + ".png");
        try {
            Files.writeString(dotPath, dotContent);
            Process process = new ProcessBuilder("dot", "-Tpng", dotPath.toString(), "-o", pngPath.toString())
                    .start();
            if (process.waitFor() == EXIT_SUCCESS && Files.exists(pngPath)) {
                openImageIfSupported(pngPath.toFile());
            } else {
                System.out.println("Утилита Graphviz (dot) недоступна; сохранен файл: " + dotPath);
            }
        } catch (Exception e) {
            System.out.println("Не удалось вызвать внешний рендер Graphviz: " + e.getMessage());
        }
    }

    private void openImageIfSupported(File imageFile) throws IOException {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(imageFile);
        }
    }

    /**
     * Формирует строковое представление зависимостей в виде ASCII-дерева с защитой от циклов.
     *
     * @param root  корневой пакет
     * @param graph карта смежности графа
     * @return отформатированное ASCII-дерево
     */
    public String toAsciiTree(String root, Map<String, List<String>> graph) {
        StringBuilder sb = new StringBuilder();
        sb.append(root).append("\n");
        Set<String> pathStack = new HashSet<>();
        pathStack.add(root);
        List<String> children = graph.getOrDefault(root, List.of());
        for (int i = 0; i < children.size(); i++) {
            boolean isLast = (i == children.size() - 1);
            buildAsciiNode(sb, children.get(i), "", isLast, graph, pathStack);
        }
        return sb.toString();
    }

    private void buildAsciiNode(
            StringBuilder sb,
            String current,
            String prefix,
            boolean isLast,
            Map<String, List<String>> graph,
            Set<String> pathStack) {
        String connector = isLast ? "\\-- " : "|-- ";
        if (pathStack.contains(current)) {
            sb.append(prefix).append(connector).append(current).append(" (cycle)\n");
            return;
        }
        sb.append(prefix).append(connector).append(current).append("\n");
        pathStack.add(current);
        List<String> children = graph.getOrDefault(current, List.of());
        String childPrefix = prefix + (isLast ? "    " : "|   ");
        for (int i = 0; i < children.size(); i++) {
            buildAsciiNode(sb, children.get(i), childPrefix, i == children.size() - 1, graph, pathStack);
        }
        pathStack.remove(current);
    }
}
