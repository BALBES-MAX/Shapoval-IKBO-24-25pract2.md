package ru.mirea.depvis;

import java.util.List;
import java.util.Set;

/**
 * Интерфейс получения прямых зависимостей пакета из источника данных.
 */
public interface DependencyResolver {

    /**
     * Возвращает список прямых зависимостей для указанного пакета.
     *
     * @param packageName имя пакета
     * @return список имен прямых зависимостей
     */
    List<String> getDirectDependencies(String packageName);

    /**
     * Возвращает множество всех известных пакетов в репозитории (используется для обратных зависимостей).
     *
     * @return множество имен пакетов
     */
    Set<String> getAllKnownPackages();
}
