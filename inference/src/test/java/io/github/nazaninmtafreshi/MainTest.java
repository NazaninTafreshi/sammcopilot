package io.github.nazaninmtafreshi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nazaninmtafreshi.SAMMUtil;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.FileSystemStrategy;
import org.eclipse.esmf.aspectmodel.resolver.ResolutionStrategy;
import org.eclipse.esmf.metamodel.AspectModel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MainTest {
    @Test
    public void testValidTurtle() {
        String content = """
                    @prefix ex: <http://example.com/> .
                    ex:subject ex:predicate ex:object .
                """;
        assertTrue(SAMMUtil.isValidTurtle(content));
    }

    @Test
    public void testInvalidTurtle() {
        String content = """
                    @prefix ex: <http://example.com/> .
                    ex:subject invalid:predicate ex:object .
                """;
        assertFalse(SAMMUtil.isValidTurtle(content));
    }

    @Test
    public void testValidSAMM() throws Exception {
        String content = """
                    @prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.1.0#>.
                    @prefix samm-c: <urn:samm:org.eclipse.esmf.samm:characteristic:2.1.0#>.
                    @prefix samm-e: <urn:samm:org.eclipse.esmf.samm:entity:2.1.0#>.
                    @prefix unit: <urn:samm:org.eclipse.esmf.samm:unit:2.1.0#>.
                    @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.
                    @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
                    @prefix xsd: <http://www.w3.org/2001/XMLSchema#>.
                    @prefix : <urn:samm:io.catenax.shared.uuid:2.0.0#>.
                                
                    :Uuid a samm:Aspect;
                        samm:preferredName "Shared Aspect for UUIDs v4"@en;
                        samm:description "This is a shared aspect for UUIDs with a regex."@en;
                        samm:properties (:uuidV4Property);
                        samm:operations ();
                        samm:events ().
                    :uuidV4Property a samm:Property;
                        samm:preferredName "UUID v4 Property"@en;
                        samm:description "Property based on a UUID v4."@en;
                        samm:characteristic :UuidV4Trait;
                        samm:exampleValue "urn:uuid:48878d48-6f1d-47f5-8ded-a441d0d879df".
                    :UuidV4Trait a samm-c:Trait;
                        samm:preferredName "Trait for UUIDs v4"@en;
                        samm:description "Trait to ensure UUID v4 data format."@en;
                        samm-c:baseCharacteristic :Uuidv4Characteristic;
                        samm-c:constraint :Uuidv4RegularExpression.
                    :Uuidv4Characteristic a samm:Characteristic;
                        samm:preferredName "UUID v4"@en;
                        samm:description "A version 4 UUID is a universally unique identifier that is generated using random 32 hexadecimal characters."@en;
                        samm:dataType xsd:string;
                        samm:see <https://tools.ietf.org/html/rfc4122>.
                    :Uuidv4RegularExpression a samm-c:RegularExpressionConstraint;
                        samm:preferredName "UUID v4 Regular Expression"@en;
                        samm:description "The provided regular expression ensures that the UUID is composed of five groups of characters separated by hyphens, in the form 8-4-4-4-12 for a total of 36 characters (32 hexadecimal characters and 4 hyphens), optionally prefixed by \\"urn:uuid:\\" to make it an IRI."@en;
                        samm:see <https://datatracker.ietf.org/doc/html/rfc4122>;
                        samm:value "(^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$)|(^urn:uuid:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$)".
                """;
        assertTrue(SAMMUtil.isValidTurtle(content));
        assertTrue(SAMMUtil.isValidSAMM(content));
    }

    @Test
    public void testInvalidSAMM() throws Exception {
        String content = """
                @prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.1.0#>.
                @prefix samm-c: <urn:samm:org.eclipse.esmf.samm:characteristic:2.1.0#>.
                @prefix samm-e: <urn:samm:org.eclipse.esmf.samm:entity:2.1.0#>.
                @prefix unit: <urn:samm:org.eclipse.esmf.samm:unit:2.1.0#>.
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#>.
                @prefix : <urn:samm:io.catenax.shared.uuid:2.0.0#>.
                            
                :Uuid a samm:Aspect;
                    samm:preferredName "Shared Aspect for UUIDs v4"@en;
                    samm:description "This is a shared aspect for UUIDs with a regex."@en;
                    samm:properties (:uuidV4Property);
                    samm:operations ();
                    samm:events ().
                :uuidV4Property a samm:Property;
                    samm:preferredName "UUID v4 Property"@en;
                    samm:description "Property based on a UUID v4."@en;
                    samm:exampleValue "urn:uuid:48878d48-6f1d-47f5-8ded-a441d0d879df".
                """;
        assertTrue(SAMMUtil.isValidTurtle(content));
        assertFalse(SAMMUtil.isValidSAMM(content));
    }


    @Test
    public void testCompareSimilarJson() throws IOException {
        // Different key orders and different values
        String json1 = "{\"name\":\"John\", \"age\":30, \"address\":{\"city\":\"New York\", \"postalCode\":\"10001\"}}";
        String json2 = "{\"age\":31, \"name\":\"John\", \"address\":{\"postalCode\":\"10001\", \"city\":\"New York\"}}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node1 = mapper.readTree(json1);
        JsonNode node2 = mapper.readTree(json2);
        assertTrue(SAMMUtil.compareStructure(node1, node2));
    }

    @Test
    public void testCompareNotSimilarJson() throws IOException {
        // Change key
        String json1 = "{\"name\":\"John\", \"age\":30, \"address\":{\"city\":\"New York\", \"postalCode\":\"10001\"}}";
        String json2 = "{\"age\":30, \"name\":\"John\", \"address\":{\"postalCodeChanged\":\"10001\", \"city\":\"New York\"}}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node1 = mapper.readTree(json1);
        JsonNode node2 = mapper.readTree(json2);
        assertFalse(SAMMUtil.compareStructure(node1, node2));
    }
    @Test
    public void testdummy(){
        SAMMUtil.loadAspectModelFromString("");
    }

    @Test
    public void testCompareSimilarJsonWithArray() throws IOException {

        String jsonArray1 = """
                [
                  {
                    "team": "Team C",
                    "members": [
                      {
                        "id": 3,
                        "name": "Adam",
                        "roles": ["Project Lead"]
                      }
                    ]
                  },
                  {
                    "team": "Team A",
                    "members": [
                      {
                        "id": 1,
                        "name": "Alice",
                        "roles": ["Developer", "Tester"]
                      },
                      {
                        "id": 2,
                        "name": "Bob",
                        "roles": ["Manager"]
                      }
                    ]
                  }
                ]
                """;
        String jsonArray2 = """
                [
                  {
                    "team": "Team B",
                    "members": [
                      {
                        "name": "Charlie",
                        "roles": ["Developer"],
                        "id": 3
                      }
                    ]
                  },
                  {
                    "team": "Team A",
                    "members": [
                      {
                        "roles": ["Tester", "Developer"],
                        "id": 1,
                        "name": "Alice"
                      },
                      {
                        "name": "Pascal",
                        "roles": ["Manager"],
                        "id": 2
                      }
                    ]
                  }
                ]
                """;

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node1 = mapper.readTree(jsonArray1);
        JsonNode node2 = mapper.readTree(jsonArray2);
        assertTrue(SAMMUtil.compareStructure(node1, node2));
    }

    @Test
    public void testCompareNotSimilarJsonWithArray() throws IOException {

        String jsonArray1 = "[{\"team\": \"Team A\", \"members\": [{\"id\": 1, \"name\": \"Alice\", \"roles\": [\"Developer\", \"Tester\"]}, {\"id\": 2, \"name\": \"Bob\", \"roles\": [\"Manager\"]}]}, {\"team\": \"Team B\", \"members\": [{\"id\": 3, \"name\": \"Charlie\", \"roles\": [\"Developer\"]}]}]";
        String jsonArray2 = "[{\"team\": \"Team C\", \"members\": [{\"name\": \"Charlie\", \"role\": [\"Developer\"], \"id\": 3}]}, {\"team\": \"Team A\", \"members\": [{\"roles\": [\"Tester\", \"Developer\"], \"id\": 1, \"name\": \"Alice\"}, {\"name\": \"Bob\", \"roles\": [\"Manager\"], \"id\": 2}]}]";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node1 = mapper.readTree(jsonArray1);
        JsonNode node2 = mapper.readTree(jsonArray2);
        assertFalse(SAMMUtil.compareStructure(node1, node2));
    }

    @Test
    public void testLoadDependentAspect(){
       
        String BASE_DIR = "D:\\NMT Thesis\\Dataset\\sldt-semantic-models";
        ResolutionStrategy FILE_SYSTEM_STRATEGY = new FileSystemStrategy(Path.of(BASE_DIR));
        String sammContent = "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.1.0#> .\n" +
                             "@prefix samm-c: <urn:samm:org.eclipse.esmf.samm:characteristic:2.1.0#> .\n" +
                             "@prefix samm-e: <urn:samm:org.eclipse.esmf.samm:entity:2.1.0#> .\n" +
                             "@prefix unit: <urn:samm:org.eclipse.esmf.samm:unit:2.1.0#> .\n" +
                             "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                             "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                             "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                             "@prefix : <urn:samm:io.catenax.reuse_certificate:3.0.0#> .\n" +
                             "@prefix ext-certificate: <urn:samm:io.catenax.shared.recycling_strategy_certificate:3.0.0#> .\n" +
                             "@prefix ext-part: <urn:samm:io.catenax.serial_part:3.0.0#> .\n" +
                             "\n" +
                             ":ReuseCertificate a samm:Aspect ;\n" +
                             "   samm:preferredName \"Reuse certificate\"@en ;\n" +
                             "   samm:description \"The Reuse-Certificate marks the point when an asset enters a new life by using parts or components of end-of-life vehicles for the same purpose for which they were conceived, as defined by the EU proposal 2023/0284.\"@en ;\n" +
                             "   samm:properties ( :certificate :partInstanceId ) ;\n" +
                             "   samm:operations ( ) ;\n" +
                             "   samm:events ( ) .\n" +
                             "\n" +
                             ":certificate a samm:Property ;\n" +
                             "   samm:preferredName \"Certificate\"@en ;\n" +
                             "   samm:description \"A property of the model, named certificate.\"@en ;\n" +
                             "   samm:characteristic ext-certificate:RecyclingStrategyCertificateCharacteristic .\n" +
                             "\n" +
                             ":partInstanceId a samm:Property ;\n" +
                             "   samm:preferredName \"Part Instance Id\"@en ;\n" +
                             "   samm:description \"The serial number of the part from the manufacturer\"@en ;\n" +
                             "   samm:characteristic ext-part:LocalIdentifierCharacteristic .";


        String regex = "@prefix\\s*:\\s*<[^>]+>\\s*.";



        // Compile the pattern
        Pattern pattern = Pattern.compile(regex);

        // Create matcher object
        Matcher matcher = pattern.matcher(sammContent);

        // Find the prefix declaration
        if (matcher.find()) {
            String defaultPrefix = matcher.group(0);
            defaultPrefix = defaultPrefix.substring(defaultPrefix.indexOf("<") + 1, defaultPrefix.indexOf(">"));
            System.out.println("Default Prefix: " + defaultPrefix);
        } else {
            System.out.println("No default prefix found.");
        }
        InputStream inputStream = new ByteArrayInputStream(sammContent.getBytes(StandardCharsets.UTF_8));

        AspectModel aspectModel = new AspectModelLoader(FILE_SYSTEM_STRATEGY).load(inputStream);

    }
}
