package ru.mirea.depvis;

import java.util.List;
import java.util.Set;

/**
 * Интерфейс получения прямых зависимостей пакета из источника данных.
 */
public interface DependencyResolver {

    List<String> getDirectDependencies(String packageName);

    Set<String> getAllKnownPackages();
}
