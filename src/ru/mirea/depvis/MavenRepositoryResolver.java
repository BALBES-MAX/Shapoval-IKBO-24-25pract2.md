package ru.mirea.depvis;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Резолвер прямых зависимостей пакетов Maven по HTTP без сторонних библиотек.
 */
public class MavenRepositoryResolver implements DependencyResolver {

    private static final int HTTP_OK = 200;
    private static final int TIMEOUT_SECONDS = 10;
    private static final int COORD_PARTS = 2;

    private final String baseUrl;
    private final HttpClient httpClient;
    private final Map<String, List<String>> cache = new HashMap<>();

    /**
     * Создает Maven-резолвер для указанного базового URL репозитория.
     *
     * @param repoUrl базовый URL репозитория (например, https://repo1.maven.org/maven2)
     */
    public MavenRepositoryResolver(String repoUrl) {
        this.baseUrl = repoUrl.endsWith("/") ? repoUrl.substring(0, repoUrl.length() - 1) : repoUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public List<String> getDirectDependencies(String packageName) {
        if (cache.containsKey(packageName)) {
            return cache.get(packageName);
        }
        String[] coords = splitCoordinates(packageName);
        String groupPath = coords[0].replace('.', '/');
        String artifactId = coords[1];
        String latestVersion = fetchLatestVersion(groupPath, artifactId);
        String pomXml = fetchPomXml(groupPath, artifactId, latestVersion);
        List<String> dependencies = parsePomDependencies(pomXml);
        cache.put(packageName, dependencies);
        return dependencies;
    }

    @Override
    public Set<String> getAllKnownPackages() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    private String[] splitCoordinates(String packageName) {
        String[] parts = packageName.split(":");
        if (parts.length != COORD_PARTS || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException(
                    "Имя Maven-пакета должно иметь формат groupId:artifactId, получено: " + packageName);
        }
        return parts;
    }

    private String fetchLatestVersion(String groupPath, String artifactId) {
        String metadataUrl = baseUrl + "/" + groupPath + "/" + artifactId + "/maven-metadata.xml";
        String xml = downloadText(metadataUrl);
        Document doc = parseXml(xml);
        String latest = extractFirstTagText(doc, "latest");
        if (latest != null && !latest.isBlank()) {
            return latest;
        }
        String release = extractFirstTagText(doc, "release");
        if (release != null && !release.isBlank()) {
            return release;
        }
        throw new IllegalStateException("Не удалось определить версию для " + artifactId);
    }

    private String fetchPomXml(String groupPath, String artifactId, String version) {
        String pomUrl = String.format("%s/%s/%s/%s/%s-%s.pom",
                baseUrl, groupPath, artifactId, version, artifactId, version);
        return downloadText(pomUrl);
    }

    private String downloadText(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HTTP_OK) {
                throw new IllegalStateException("HTTP " + response.statusCode() + " при запросе " + url);
            }
            return response.body();
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка загрузки данных по URL " + url + ": " + e.getMessage(), e);
        }
    }

    private Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            return factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка разбора XML: " + e.getMessage(), e);
        }
    }

    private String extractFirstTagText(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }

    private List<String> parsePomDependencies(String pomXml) {
        Document doc = parseXml(pomXml);
        Set<String> result = new LinkedHashSet<>();
        NodeList depsBlocks = doc.getElementsByTagName("dependencies");
        for (int i = 0; i < depsBlocks.getLength(); i++) {
            Node parent = depsBlocks.item(i).getParentNode();
            if (parent != null && "project".equals(parent.getNodeName())) {
                collectFromDependenciesNode((Element) depsBlocks.item(i), result);
            }
        }
        return new ArrayList<>(result);
    }

    private void collectFromDependenciesNode(Element dependenciesElement, Set<String> result) {
        NodeList depList = dependenciesElement.getElementsByTagName("dependency");
        for (int i = 0; i < depList.getLength(); i++) {
            Element dep = (Element) depList.item(i);
            String scope = getChildText(dep, "scope");
            String optional = getChildText(dep, "optional");
            if (isRuntimeDependency(scope, optional)) {
                String groupId = getChildText(dep, "groupId");
                String artifactId = getChildText(dep, "artifactId");
                if (isValidCoordinate(groupId, artifactId)) {
                    result.add(groupId + ":" + artifactId);
                }
            }
        }
    }

    private boolean isRuntimeDependency(String scope, String optional) {
        boolean badScope = "test".equalsIgnoreCase(scope) || "provided".equalsIgnoreCase(scope);
        boolean isOpt = "true".equalsIgnoreCase(optional);
        return !badScope && !isOpt;
    }

    private boolean isValidCoordinate(String groupId, String artifactId) {
        return groupId != null && artifactId != null
                && !groupId.contains("${") && !artifactId.contains("${");
    }

    private String getChildText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return null;
    }
}
