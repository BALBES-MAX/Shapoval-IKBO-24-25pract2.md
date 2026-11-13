package ru.mirea.depvis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Конфигурация приложения, загружаемая из CSV-файла.
 *
 * @param packageName имя анализируемого пакета (groupId:artifactId или буква в тестовом режиме)
 * @param repoUrl     URL Maven-репозитория или путь к файлу тестового репозитория
 * @param testMode    флаг режима работы с тестовым репозиторием
 * @param asciiMode   флаг вывода зависимостей в виде ASCII-дерева
 */
public record Config(String packageName, String repoUrl, boolean testMode, boolean asciiMode) {

    private static final int EXPECTED_COLUMNS = 2;

    /**
     * Загружает и валидирует параметры конфигурации из CSV-файла формата key,value.
     *
     * @param csvPath путь к CSV-файлу конфигурации
     * @return валидный объект конфигурации
     * @throws IllegalArgumentException при ошибках чтения или некорректных значениях параметров
     */
    public static Config fromCsv(String csvPath) {
        List<String> lines = readLines(csvPath);
        Map<String, String> values = parseKeyValueLines(lines);
        String pkg = requireNonEmpty(values, "package_name");
        String repo = requireNonEmpty(values, "repo_url");
        boolean isTest = parseStrictBoolean(values, "test_mode");
        boolean isAscii = parseStrictBoolean(values, "ascii_mode");
        validateRepoSource(repo, isTest);
        return new Config(pkg, repo, isTest, isAscii);
    }

    private static List<String> readLines(String csvPath) {
        if (csvPath == null || csvPath.isBlank()) {
            throw new IllegalArgumentException("Путь к конфигурационному файлу не задан.");
        }
        Path path = Path.of(csvPath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Файл конфигурации не найден: " + csvPath);
        }
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            throw new IllegalArgumentException("Ошибка чтения файла конфигурации: " + e.getMessage(), e);
        }
    }

    private static Map<String, String> parseKeyValueLines(List<String> lines) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] parts = trimmed.split(",", EXPECTED_COLUMNS);
            if (parts.length != EXPECTED_COLUMNS) {
                throw new IllegalArgumentException("Некорректный формат CSV в строке " + (i + 1));
            }
            String key = parts[0].trim();
            if (i == 0 && "key".equalsIgnoreCase(key)) {
                continue;
            }
            map.put(key, parts[1].trim());
        }
        return map;
    }

    private static String requireNonEmpty(Map<String, String> map, String key) {
        String val = map.get(key);
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException("Отсутствует или пуст обязательный параметр: " + key);
        }
        return val;
    }

    private static boolean parseStrictBoolean(Map<String, String> map, String key) {
        String val = requireNonEmpty(map, key);
        if ("true".equalsIgnoreCase(val)) {
            return true;
        }
        if ("false".equalsIgnoreCase(val)) {
            return false;
        }
        throw new IllegalArgumentException("Параметр " + key + " должен быть true или false, получено: " + val);
    }

    private static void validateRepoSource(String repo, boolean isTest) {
        if (isTest) {
            Path path = Path.of(repo);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                throw new IllegalArgumentException("Файл тестового репозитория не найден: " + repo);
            }
        } else if (!repo.startsWith("http://") && !repo.startsWith("https://")) {
            throw new IllegalArgumentException("URL репозитория должен начинаться с http:// или https://");
        }
    }
}
