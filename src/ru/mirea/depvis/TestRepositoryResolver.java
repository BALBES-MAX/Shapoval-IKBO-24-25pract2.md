package ru.mirea.depvis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Резолвер зависимостей для тестового режима (чтение графа из текстового файла).
 * Формат файла: "A: B C" (пакет A зависит от B и C).
 */
public class TestRepositoryResolver implements DependencyResolver {

    private static final int KEY_VALUE_PARTS = 2;
    private final Map<String, List<String>> adjacency = new LinkedHashMap<>();

    /**
     * Создает резолвер и загружает описание графа из файла.
     *
     * @param filePath путь к файлу тестового репозитория
     */
    public TestRepositoryResolver(String filePath) {
        List<String> lines = readFile(filePath);
        for (String line : lines) {
            parseLine(line.trim());
        }
    }

    private List<String> readFile(String filePath) {
        try {
            return Files.readAllLines(Path.of(filePath));
        } catch (IOException e) {
            throw new IllegalArgumentException("Не удалось прочитать тестовый репозиторий: " + filePath, e);
        }
    }

    private void parseLine(String trimmed) {
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }
        String[] parts = trimmed.split(":", KEY_VALUE_PARTS);
        String pkg = parts[0].trim();
        validatePackageName(pkg);
        List<String> deps = new ArrayList<>();
        if (parts.length == KEY_VALUE_PARTS && !parts[1].isBlank()) {
            for (String token : parts[1].trim().split("\\s+")) {
                validatePackageName(token);
                deps.add(token);
                adjacency.putIfAbsent(token, new ArrayList<>());
            }
        }
        adjacency.put(pkg, deps);
    }

    private void validatePackageName(String name) {
        if (!name.matches("[A-Z]+")) {
            throw new IllegalArgumentException(
                    "В тестовом режиме имена пакетов должны быть из заглавных латинских букв: " + name);
        }
    }

    @Override
    public List<String> getDirectDependencies(String packageName) {
        if (!adjacency.containsKey(packageName)) {
            throw new IllegalArgumentException("Пакет не найден в тестовом репозитории: " + packageName);
        }
        return Collections.unmodifiableList(adjacency.get(packageName));
    }

    @Override
    public Set<String> getAllKnownPackages() {
        return Collections.unmodifiableSet(adjacency.keySet());
    }
}
