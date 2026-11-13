package ru.mirea.depvis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Движок построения транзитивного графа зависимостей и поиска обратных зависимостей.
 * Использует рекурсивный обход в глубину (DFS) с защитой от циклов.
 */
public class GraphEngine {

    private final DependencyResolver resolver;

    /**
     * Создает движок анализа графа зависимостей.
     *
     * @param resolver источник прямых зависимостей
     */
    public GraphEngine(DependencyResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Строит транзитивный граф зависимостей для корневого пакета с помощью рекурсивного DFS.
     *
     * @param rootPackage имя стартового пакета
     * @return карта смежности (пакет -> список прямых зависимостей)
     */
    public Map<String, List<String>> buildTransitiveGraph(String rootPackage) {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        dfsBuild(rootPackage, graph, visiting, visited);
        return graph;
    }

    private void dfsBuild(
            String current,
            Map<String, List<String>> graph,
            Set<String> visiting,
            Set<String> visited) {
        if (visited.contains(current) || visiting.contains(current)) {
            return;
        }
        visiting.add(current);
        List<String> directDeps = safeGetDependencies(current);
        graph.put(current, directDeps);
        for (String dep : directDeps) {
            dfsBuild(dep, graph, visiting, visited);
        }
        visiting.remove(current);
        visited.add(current);
    }

    private List<String> safeGetDependencies(String pkg) {
        try {
            return resolver.getDirectDependencies(pkg);
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Находит все пакеты, которые прямо или транзитивно зависят от целевого пакета,
     * используя рекурсивный алгоритм DFS по инвертированному графу.
     *
     * @param targetPackage целевой пакет
     * @return упорядоченное множество обратных зависимостей
     */
    public Set<String> findReverseDependencies(String targetPackage) {
        Map<String, List<String>> reverseGraph = buildReverseAdjacency();
        Set<String> result = new LinkedHashSet<>();
        Set<String> visiting = new HashSet<>();
        dfsReverse(targetPackage, targetPackage, reverseGraph, visiting, result);
        return result;
    }

    private Map<String, List<String>> buildReverseAdjacency() {
        Map<String, List<String>> reverse = new LinkedHashMap<>();
        for (String pkg : resolver.getAllKnownPackages()) {
            reverse.putIfAbsent(pkg, new ArrayList<>());
            for (String dep : safeGetDependencies(pkg)) {
                reverse.computeIfAbsent(dep, k -> new ArrayList<>()).add(pkg);
            }
        }
        return reverse;
    }

    private void dfsReverse(
            String current,
            String rootTarget,
            Map<String, List<String>> reverseGraph,
            Set<String> visiting,
            Set<String> result) {
        if (visiting.contains(current)) {
            return;
        }
        visiting.add(current);
        for (String dependent : reverseGraph.getOrDefault(current, Collections.emptyList())) {
            if (!dependent.equals(rootTarget) && result.add(dependent)) {
                dfsReverse(dependent, rootTarget, reverseGraph, visiting, result);
            }
        }
        visiting.remove(current);
    }
}
