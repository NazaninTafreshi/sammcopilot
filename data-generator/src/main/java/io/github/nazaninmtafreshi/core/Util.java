package io.github.nazaninmtafreshi.core;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.esmf.aspectmodel.generator.json.AspectModelJsonPayloadGenerator;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.FileSystemStrategy;
import org.eclipse.esmf.aspectmodel.resolver.ResolutionStrategy;
import org.eclipse.esmf.aspectmodel.serializer.AspectSerializer;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.metamodel.Aspect;
import org.eclipse.esmf.metamodel.AspectModel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Util {


    public static String incrementVersion(String currentVersion) {
        String[] parts = currentVersion.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);
        minor++;
        return major + "." + minor + "." + patch;
    }

    /**
     * Perform a BFS search
     * @param model model to do BFS
     * @param rootNode root model for BFS
     * @return a set of visited Resources
     */
    public static Set<Resource> findConnectedNodes(Model model, Resource rootNode) {
        Set<Resource> visited = new HashSet<>();
        Queue<Resource> queue = new LinkedList<>();

        // Start BFS from the root node
        queue.add(rootNode);
        visited.add(rootNode);

        while (!queue.isEmpty()) {
            Resource current = queue.poll();
            // Traverse all properties (both directions)
            StmtIterator stmts = model.listStatements(current, null, (RDFNode) null);
            while (stmts.hasNext()) {
                Statement stmt = stmts.nextStatement();
                RDFNode object = stmt.getObject();
                // connection via see link is omitted
                if (stmt.getPredicate().getURI().endsWith("see")) {
                    continue;
                }
                if (stmt.getPredicate().equals(RDF.type)) {
                    continue;
                }
                if (object.isResource()) {
                    Resource neighbor = object.asResource();

                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return visited;
    }

    /**
     * Remove all nodes that are not connected and part of connected Nodes
     * @param model model to perform operation on
     * @param connectedNodes set of connected nodes and should stay in the model graph
     */
    public static void removeUnconnectedComponents(Model model, Set<Resource> connectedNodes) {
        // Get all resources in the model
        ResIterator allNodes = model.listSubjects();
        List<Resource> unconnectedNodes = new ArrayList<>();

        while (allNodes.hasNext()) {
            Resource node = allNodes.nextResource();
            if (!connectedNodes.contains(node)) {
                unconnectedNodes.add(node);
            }
        }

        // Remove unconnected nodes
        for (Resource unconnectedNode : unconnectedNodes) {
//            System.out.println("REMOVING:"+unconnectedNode);
            model.removeAll(unconnectedNode, null, (RDFNode) null);
            model.removeAll(null, null, unconnectedNode);
            if(!unconnectedNode.isAnon()){
                model.removeAll(null, model.createProperty(unconnectedNode.getURI()), null);
            }

        }
    }

    public static String generateJsonPayloadPretty(String basePrefix, AspectModel aspectModel) {
        try {

            Aspect mainModel = getMainModel(aspectModel, basePrefix);
            if (mainModel == null) {
                throw new IllegalStateException("Somehow you lost the main model, something went wrong!");
            }
            final AspectModelJsonPayloadGenerator generator = new AspectModelJsonPayloadGenerator(mainModel);
            OutputStream outputStream = new ByteArrayOutputStream();
            generator.generateJsonPretty(s -> outputStream);
            return outputStream.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }

    }

    private static Aspect getMainModel(AspectModel aspectModel, String basePrefix) {
        String baseUrn = basePrefix.substring(0, basePrefix.lastIndexOf(":"));
        Aspect mainModel = null;
        for (int i = 0; i < aspectModel.aspects().size(); i++) {
            String urn = aspectModel.aspects().get(i).urn().getUrnPrefix();
            if (urn.startsWith(baseUrn)) {
                mainModel = aspectModel.aspects().get(i);
            }
        }
        return mainModel;
    }

    public static AspectModel loadAspectModelFromString(String ROOT_PATH, String modelAsString) {
        InputStream inputStream = new ByteArrayInputStream(modelAsString.getBytes(StandardCharsets.UTF_8));
        ResolutionStrategy FILE_SYSTEM_STRATEGY = new FileSystemStrategy(Path.of(ROOT_PATH));
        AspectModel aspectModel = new AspectModelLoader(FILE_SYSTEM_STRATEGY).load(inputStream);
        return aspectModel;
    }

    public static String saveModelAsString(String basePrefix, String ROOT_PATH, Model model) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        RDFDataMgr.write(outputStream, model, Lang.TURTLE);
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        ResolutionStrategy FILE_SYSTEM_STRATEGY = new FileSystemStrategy(Path.of(ROOT_PATH));
        AspectModel aspectModel = new AspectModelLoader(FILE_SYSTEM_STRATEGY).load(inputStream);
        Aspect mainModel = getMainModel(aspectModel, basePrefix);
        if (mainModel == null) {
            throw new IllegalStateException("Somehow you lost the main model, something went wrong!");
        }
        return AspectSerializer.INSTANCE.aspectToString(mainModel);
    }

    public static void saveToFile(String content, String outputFilePath) {
        Path outputPath = Paths.get(outputFilePath);
        try {
            if (!Files.exists(outputPath.getParent())) {
                Files.createDirectories(outputPath.getParent());
            }
            Files.writeString(outputPath, content);
        } catch (IOException e) {
            System.out.println("An error occurred while creating the folder or writing to the " + outputPath + " file.");
            e.printStackTrace();
        }
    }
    public static String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        String snakeCase = name.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1_$2")
                .toLowerCase();

        return snakeCase;
    }
    public static Model loadModel(String ROOT_PATH, String rdfFilePath) {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream input = new FileInputStream(Path.of(ROOT_PATH, rdfFilePath).toFile())) {
            // Load the RDF model from the file
            RDFDataMgr.read(model, input, Lang.TURTLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return model;
    }


    public static void saveDictionaryToCSV(Map<String, Object> dictionary, String filePath) {
        // Check if the file exists and if it is empty
        boolean isFileEmpty = isFileEmpty(filePath);

        try (FileWriter writer = new FileWriter(filePath, true)) {
            // If the file is empty (first write), write the header (keys)
            if (isFileEmpty) {
                writer.append(String.join(",", dictionary.keySet()) + "\n");
            }

            // Write the values
            writer.append(String.join(",", dictionary.values().stream()
                    .map(String::valueOf)
                    .toArray(String[]::new)) + "\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static boolean isFileEmpty(String filePath) {
        try {
            // Check if the file exists and if it has any content
            return Files.notExists(Paths.get(filePath)) || Files.size(Paths.get(filePath)) == 0;
        } catch (IOException e) {
            e.printStackTrace();
            return true; // In case of an error, assume the file is empty
        }
    }
}
