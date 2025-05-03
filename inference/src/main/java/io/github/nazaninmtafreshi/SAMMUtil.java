package io.github.nazaninmtafreshi;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.eclipse.esmf.aspectmodel.generator.json.AspectModelJsonPayloadGenerator;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.aspectmodel.validation.services.DetailedViolationFormatter;
import org.eclipse.esmf.aspectmodel.validation.services.ViolationFormatter;
import org.eclipse.esmf.metamodel.Aspect;
import org.eclipse.esmf.metamodel.AspectModel;
import org.eclipse.esmf.aspectmodel.resolver.FileSystemStrategy;
import org.eclipse.esmf.aspectmodel.resolver.ResolutionStrategy;

import java.io.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SAMMUtil {

    public static String BASE_DIR = "D:\\NMT Thesis\\Dataset\\augmented_2024_09_18_v1_drop_rename_02_05\\sldt-semantic-models";
    private static final ResolutionStrategy FILE_SYSTEM_STRATEGY = new FileSystemStrategy(Path.of(BASE_DIR));

    public static boolean isValidTurtle(String sammContent) {
        InputStream inputStream = new ByteArrayInputStream(sammContent.getBytes(StandardCharsets.UTF_8));

        // Create an empty Jena Model
        Model model = ModelFactory.createDefaultModel();

        model.read(inputStream, null, "TTL");
        return true;
    }

    public static boolean isValidSAMM(String sammContent) throws Exception {
        if(sammContent.contains("urn:samm:org.eclipse.esmf.samm:meta-model")==false){
            throw new Exception("SAMM Namespace (urn:samm:org.eclipse.esmf.samm:meta-model) is not utilized!");
        }
        AspectModel aspectModel = SAMMUtil.loadAspectModelFromString(sammContent);
        return SAMMUtil.sammValidation(aspectModel);
    }

    public static boolean isValidJson(String groundTruthJson, String generatedJson) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node1 = mapper.readTree(groundTruthJson);
        JsonNode node2 = mapper.readTree(generatedJson);
        boolean isSimilar = false;
        try {
            isSimilar = compareStructure(node1, node2);

        }catch(Exception ex){
            throw new Exception("JSONs are not similar:\n"+ex.getMessage()+"\nRequired Ground truth JSON should be like:\n" + groundTruthJson +
                    "\nHowever, the generated Aspect Model corresponds to the following JSON structure:\n" +
                    generatedJson);
        }
        if (isSimilar == false) {
            throw new Exception("JSONs are not similar:\nRequired JSON should be like:\n" + groundTruthJson +
                    "\nHowever, the generated Aspect Model corresponds to the following JSON structure:\n" +
                    generatedJson);
        }
        return isSimilar;
    }


    public static String generateJsonPayload(String sammContent) {
        try {
            AspectModel aspectModel = loadAspectModelFromString(sammContent);
            // Regex for default prefix
            String regex = "@prefix\\s*:\\s*<[^>]+>\\s*.";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(sammContent);

            // Find the default prefix
            if (matcher.find()) {
                String defaultPrefix = matcher.group(0);
                defaultPrefix = defaultPrefix.substring(defaultPrefix.indexOf("<") + 1, defaultPrefix.indexOf(">"));
//                System.out.println("Default Prefix: " + defaultPrefix);
                Aspect mainModel = null;
                for (int i = 0; i < aspectModel.aspects().size(); i++) {
                    String urn = aspectModel.aspects().get(i).urn().getUrnPrefix();
                    if (urn.startsWith(defaultPrefix)) {
                        mainModel = aspectModel.aspects().get(i);
                    }
                }
                final AspectModelJsonPayloadGenerator generator = new AspectModelJsonPayloadGenerator(mainModel);
                return generator.generateJson();
            }

        } catch (Exception e) {
//            e.printStackTrace();
        }
        return "{}";
    }

    static AspectModel loadAspectModelFromString(String sammContent) {
        InputStream inputStream = new ByteArrayInputStream(sammContent.getBytes(StandardCharsets.UTF_8));
        AspectModel aspectModel = new AspectModelLoader(FILE_SYSTEM_STRATEGY).load(inputStream);
        return aspectModel;
    }

    private static boolean sammValidation(AspectModel aspectModel) throws Exception {
        final List<Violation> violations = new AspectModelValidator().validateModel(aspectModel);
        final String validationReport = new ViolationFormatter().apply(violations);
        final String detailedReport = new DetailedViolationFormatter().apply(violations);

        if (violations.isEmpty()) {

            return true;
        }
//        System.out.println("Validation Report: " + validationReport);
//        System.out.println("Detailed Validation Report = " + detailedReport);
        throw new Exception(validationReport);
    }


    protected static boolean compareStructure(JsonNode inputNode1, JsonNode inputNode2) {
        JsonNode node1 = sortJsonNode(inputNode1);
        JsonNode node2 = sortJsonNode(inputNode2);
        // Check if both are objects
        if (node1.isObject() && node2.isObject()) {
            Iterator<String> fieldNames1 = node1.fieldNames();
            Iterator<String> fieldNames2 = node2.fieldNames();

            // Collect keys from both objects
            while (fieldNames1.hasNext()) {
                String fieldName1 = fieldNames1.next();
                if (!node2.has(fieldName1) ) {
                    throw new RuntimeException("JSONs are not similar: ground truth has key \""+fieldName1+"\" but it is missing in the other JSON.\n" +
                            "Add a SAMM \""+fieldName1+"\" as a Property or Entity to the Aspect Model.");

                }
                if(!compareStructure(node1.get(fieldName1), node2.get(fieldName1))){
                    throw new RuntimeException("JSONs are not similar: for key \""+fieldName1+"\" the structure of value is different. Try to use a corrected Entity.\n" +
                            "Groud truth structure is \n"+node1.get(fieldName1)+"\nBut the generated json is:\n"+node2.get(fieldName1)+""
                    );
                }
            }
            // Check if node2 has extra fields not present in node1
            while (fieldNames2.hasNext()) {
                String fieldName2 = fieldNames2.next();
                if (!node1.has(fieldName2)) {
                    throw new RuntimeException("JSONs are not similar: \""+fieldName2+"\" is not a key in ground truth. It is probably extra and you can remove it.");
                }
            }
            return true;
        }
        // If both are arrays, compare their structure recursively
        else if (node1.isArray() && node2.isArray()) {
            if (node1.size() != node2.size()) return false;
            for (int i = 0; i < node1.size(); i++) {
                if (!compareStructure(node1.get(i), node2.get(i))) {
                    return false;
                }
            }
            return true;
        }
        // Otherwise, treat them as leaves (they're structurally the same if they're both non-containers)
        return node1.isValueNode() && node2.isValueNode();
    }

    private static JsonNode sortJsonNode(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            // Use a TreeMap to sort the keys
            ObjectNode sortedObjectNode = JsonNodeFactory.instance.objectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
            TreeMap<String, JsonNode> sortedFields = new TreeMap<>();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                sortedFields.put(field.getKey(), sortJsonNode(field.getValue()));
            }

            sortedFields.forEach(sortedObjectNode::set);
            return sortedObjectNode;
        } else if (jsonNode.isArray()) {
            ArrayNode sortedArrayNode = JsonNodeFactory.instance.arrayNode();
            for (JsonNode element : jsonNode) {
                sortedArrayNode.add(sortJsonNode(element)); // Recursively sort if element is an object
            }
            return sortedArrayNode;
        }
        // Return leaf nodes as is (e.g., strings, numbers)
        return jsonNode;
    }


}






