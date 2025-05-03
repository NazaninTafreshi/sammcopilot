package io.github.nazaninmtafreshi;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.bedrock.BedrockAnthropicMessageChatModel;
import dev.langchain4j.model.bedrock.BedrockLlamaChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import io.github.nazaninmtafreshi.prompts.FewShotPromptTemplate;
import io.github.nazaninmtafreshi.prompts.PromptTemplate;
import io.github.nazaninmtafreshi.prompts.ZeroShotPromptTemplate;
import org.apache.xmlbeans.impl.xb.xsdschema.Attribute;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;


import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O;

@Command(name = "inference", mixinStandardHelpOptions = true, version = "inference 1.0",
        description = "t")
public class Inference implements Callable<Integer> {
    private ChatLanguageModel chatModel;

    private PromptTemplate promptTemplate;

    @Option(names = {"--ollama-endpoint"}, description = "Ollama endpoint address", defaultValue = "http://127.0.0.1:11434/")
    private String ollamaEndpoint;

    //augmented gpt model: ft:gpt-4o-mini-2024-07-18:personal:augmented-rename-drop-02-05-v1:A8x9uqo0
    //original model: ft:gpt-4o-mini-2024-07-18:personal:original-v0:A8wrwtyN
    //azure:gpt-4o-mini-2024-07-18-SemanticAspectMetaModelV1
    //gpt-4o-mini-2024-07-18
    //local:
    //qwen2.5-coder:7b
    //llama3.2:3b
    //codellama:7b
    //llama3.1:8b
    @Option(names = {"--model"}, description = "Model name in ollama, or azure: , or openai: , or bedrock:", defaultValue = "qwen2.5-coder-samm-v2:latest")
    private String model;
    //azure:gpt-4o-2024-08-06-SemanticAspectMetaModelV1
    //bedrock:SemanticAspectMetaModelV1
    //qwen2.5-coder-samm:latest
    //openai:ft:gpt-4o-mini-2024-07-18:personal:samm-augmented-v1:A4F6icYJ
    //openai:ft:gpt-4o-mini-2024-07-18:personal:original-v0:A8wrwtyN

    @Option(names = {"--seed"}, description = "Seed", defaultValue = "0")
    private Integer seed;

    @Option(names = {"--max-tokens"}, description = "Maximum number of output tokens.", defaultValue = "4096")
    private Integer maxTokens;

    @Option(names = {"--input-base-path"}, description = "Base path pointing to the catena-x dataset.", defaultValue = "D:/NMT Thesis/Dataset/sldt-semantic-models")
    private String inputBasePath;
    @Option(names = {"--output-base-path"}, description = "Base path pointing to the output folder.", defaultValue = "D:/NMT Thesis/Results")
    private String outputBasePath;
    @Option(names = {"--csv-path"}, description = "Path to the test csv.", defaultValue = "D:/NMT Thesis/Dataset/augmented_2024_09_18_v1_drop_rename_02_05/test_2024-09-18.csv")
    private String csvPath;

    @Option(names = {"--experiment-name"}, description = "Name of experiment.", defaultValue = "T07-FineTunedQwenv2")
    private String experimentName;

    @Option(names = {"--iterative-prompting"}, description = "Use past mistakes as a part of prompt", defaultValue = "false")
    private Boolean iterativePrompting;

    @Option(names = {"--examples-path"}, description = "Comma-separated list of examples path.", split = ",")
    private List<String> examplesPath;
    //io.catenax.shared.secondary_material_content/1.0.0/SecondaryMaterialContent.ttl
    //

    @Option(names = {"--temperature"}, description = "Temperature", defaultValue = "0.7")
    private Double temperature;

    @Option(names = {"--attempt-limit"}, description = "attemptLimit", defaultValue = "1")
    private Integer attemptLimit;
    public static final ChatMessage rdfSystemGuideline = UserMessage.from("""
            You can follow these instructions for Generating a Valid Turtle Document
            Start with Prefixes (Optional but Recommended):
                            
                Declare namespaces using the @prefix or PREFIX keyword followed by a prefix, a colon, and the IRI in angle brackets.
                End the declaration with a period (.).
                Example:
                ```turtle
                @prefix ex: <http://example.org/> .
                ```
            Define a Subject:
                        
                A subject is a resource (IRI or blank node) about which you want to make statements.
            Represent it using:
                An IRI enclosed in angle brackets, e.g., <http://example.org/resource1>, or
                A prefixed name like ex:Resource1.
            Connect the Subject to Properties (Predicates):
                            
                Use predicates (verbs) to describe relationships or attributes of the subject.
                Represent predicates using:
                An IRI in angle brackets, e.g., <http://example.org/property>, or
                A prefixed name like ex:Property.
            Provide Object Values:
                        
                Objects can be:
                    IRIs (as subjects or resources),
                    Literals (data values like strings, numbers, dates),
                    Blank nodes (anonymous resources).
                    Literals should be enclosed in quotes, e.g., "Alice", or formatted as datatype literals, e.g., "42"^^<http://www.w3.org/2001/XMLSchema#integer>.
            End Statements with a Period:
                        
                Each triple (subject-predicate-object) must end with a period (.).
            Group Properties for the Same Subject:
                            
                Use a semicolon (;) to separate multiple predicates for the same subject.
                Example:
                ```turtle
                ex:Resource1 ex:hasName "Alice";
                             ex:hasAge 30 .
                ```
            Use Commas to List Multiple Objects for the Same Predicate:
                        
                Use commas (,) to separate multiple objects for a single predicate.
                Example:
                ```turtle
                ex:Resource1 ex:hasFriend ex:Resource2, ex:Resource3 .
                ```
            Include Comments for Clarity:
                        
                Use the # symbol for comments, which are ignored by parsers.
                Example:
                ```turtle
                # This is a comment
                ```
            Follow RDF Data Model Constraints:
                        
                Ensure that:
                Subjects and predicates are always IRIs or prefixed names.
                Objects can be IRIs, prefixed names, literals, or blank nodes.
                
            Example valid RDF:
            ```turtle
            @prefix : <urn:samm:com.mycompany.myapplication:1.0.0#> .
            @prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.1.0#> .
            @prefix samm-c: <urn:samm:org.eclipse.esmf.samm:characteristic:2.1.0#> .
            @prefix samm-e: <urn:samm:org.eclipse.esmf.samm:entity:2.1.0#> .
            @prefix unit: <urn:samm:org.eclipse.esmf.samm:unit:2.1.0#> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                        
            :MyAspect a samm:Aspect ;
               samm:preferredName "My Aspect"@en ;
               samm:preferredName "Mein Aspekt"@de ;
               samm:description "This Aspect is an example."@en ;
               samm:description "Dieser Aspekt ist ein Beispiel."@de ;
               samm:properties ( :materialNumber ) ;
               samm:operations ( ) ;
               samm:events ( ) .
                        
            :materialNumber a samm:Property ;
               samm:preferredName "Material number"@en ;
               samm:description "A material number"@en ;
               samm:exampleValue "ABC123456-000" ;
               samm:characteristic samm-c:Text .
                        
            ```
            """);
    public static final ChatMessage jsonMappingGuideline = UserMessage.from("""
            * Relation between JSON and SAMM Aspect Model:
            For understanding the construction rules, we define the following terms:
                        
            A Constraint of the Property’s Characteristic defined with a Trait is applied to the characteristic referred in the samm-c:baseCharacteristic .
                        
            A Property’s effective data type means the Property’s Characteristic’s samm:dataType.
                        
            A data type is scalar, if it is one of the allowed data types, but not a samm:Entity.
            
                In order to create JSON payloads that correspond to an Aspect Model, the following rules are applied. The other way round they can also be used to describe a validation algorithm.
                            
                An Aspect Model is always serialized as an unnamed JSON object.
                            
                For each Property:
                            
                If it is marked as optional, it may or may not be included in the payload. If, and only if, the Property is marked as optional and is included in the payload, then its value may be null, which is equivalent to it not being included in the payload.
                            
                If the Property’s effective data type is scalar with any date type other than rdf:langString, the Property is serialized as ${propertyName}: ${value} where ${value} is the JSON serialization of the respective Property’s value, details on mapping of the data types are given in Data type mappings. The value must adhere to the value range defined by the Property’s effective data type and possible Constraints on the Property’s Characteristic.
                            
                If the Property’s effective data type is scalar with the data type rdf:langString, the Property is serialized as a named JSON object (with ${propertyName} being the name of the JSON property), with keys for each available language tag of the Property and the corresponding localized string as the value.
                            
                If the Property’s effective data type is not scalar, it is serialized as a named JSON object (with ${propertyName} being the name of the JSON property), recursively using the same rules.
                            
                If the Property’s effective data type is an Entity which extends another Entity, it is serialized as a named JSON object (with ${propertyName} being the name of the JSON property). The Properties included for the Entity, are the Properties from the Entity itself as well as all Properties from the extended Entities, i.e. all Properties from ?thisEntity samm:extends* [].
                            
                If the Property’s Characteristic is a Collection, List, Set or Sorted Set, it is serialized as a named JSON array (with ${propertyName} being the name of the JSON array property).
                            
                Characteristics defined in the Aspect Model other than the ones mentioned above are not subject to serialization.
                            
                Operations defined in the Aspect Model are not subject to serialization.
                            
                Events defined in the Aspect Model are not subject to serialization.     
                1- Example JSON:
                {
                  "isMoving": true,
                  "speed": 0.5
                }
                1- Expected SAMM Aspect Model:
                @prefix : <urn:samm:com.mycompany.myapplication:1.0.0#> .
                @prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.1.0#> .
                @prefix samm-c: <urn:samm:org.eclipse.esmf.samm:characteristic:2.1.0#> .
                @prefix samm-e: <urn:samm:org.eclipse.esmf.samm:entity:2.1.0#> .
                @prefix unit: <urn:samm:org.eclipse.esmf.samm:unit:2.1.0#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                :Movement a samm:Aspect ;
                   samm:preferredName "movement"@en ;
                   samm:properties ( :isMoving :speed ) ;
                   samm:operations ( ) ;
                   samm:events ( ) .
                
                :isMoving a samm:Property ;
                   samm:preferredName "is moving"@en ;
                   samm:characteristic samm-c:Boolean .
                
                :speed a samm:Property ;
                   samm:preferredName "speed"@en ;
                   samm:characteristic :Speed ;
                   samm:exampleValue "0.5"^^xsd:float .
                
                :Speed a samm-c:Measurement ;
                   samm:preferredName "speed"@en ;
                   samm:dataType xsd:float ;
                   samm-c:unit unit:kilometrePerHour .  
            
                
                3- Example JSON:
                {
                  "categoricalData": "enum1",
                  "property1": 0,
                  "multiLang": {
                    "en": "multilanguage"
                  },
                  "simpleKey": "simpleValue",
                  "complexList": [
                    {
                      "anotherKey": "value"
                    }
                  ],
                  "complex": {
                    "property2": "2024-11-29T02:02:49.228+01:00",
                    "property3": true
                  }
                }
                
                3-Expected SAMM Aspect Model
                @prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.1.0#> .
                @prefix samm-c: <urn:samm:org.eclipse.esmf.samm:characteristic:2.1.0#> .
                @prefix samm-e: <urn:samm:org.eclipse.esmf.samm:entity:2.1.0#> .
                @prefix unit: <urn:samm:org.eclipse.esmf.samm:unit:2.1.0#> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                @prefix : <urn:samm:example.example.contact_property:7.0.0#> .
                
                :ContactProperty a samm:Aspect ;
                   samm:preferredName "Contact Property"@en ;
                   samm:description "The contact property aspect contains essential contact information such as fax number, website, phone number, and email."@en ;
                   samm:properties ( :simpleKey :complexList :complex :categoricalData :multiLang :property1 ) ;
                   samm:operations ( ) ;
                   samm:events ( ) .
                
                :simpleKey a samm:Property ;
                   samm:characteristic samm-c:Text ;
                   samm:exampleValue "simpleValue" .
                
                :complexList a samm:Property ;
                   samm:characteristic :ComplexValueCharacteristic .
                
                :complex a samm:Property ;
                   samm:characteristic :Characteristic1 .
                
                :categoricalData a samm:Property ;
                   samm:characteristic :Characteristic2 .
                
                :multiLang a samm:Property ;
                   samm:characteristic samm-c:MultiLanguageText ;
                   samm:exampleValue "multilanguage"@en .
                
                :property1 a samm:Property ;
                   samm:characteristic :Characteristic3 .
                
                :ComplexValueCharacteristic a samm-c:List ;
                   samm:dataType :DataTypeOfComplexValue .
                
                :Characteristic1 a samm:Characteristic ;
                   samm:dataType :Entity1 .
                
                :Characteristic2 a samm-c:Enumeration ;
                   samm:dataType xsd:string ;
                   samm-c:values ( "enum1" "enum2" ) .
                
                :Characteristic3 a samm-c:Measurement ;
                   samm:dataType xsd:float ;
                   samm-c:unit unit:angstrom .
                
                :DataTypeOfComplexValue a samm:Entity ;
                   samm:properties ( :anotherKey ) .
                
                :Entity1 a samm:Entity ;
                   samm:properties ( :property2 :property3 ) .
                
                :anotherKey a samm:Property ;
                   samm:characteristic :AnotherValueCharacteristic ;
                   samm:exampleValue "value" .
                
                :property2 a samm:Property ;
                   samm:characteristic samm-c:Timestamp .
                
                :property3 a samm:Property ;
                   samm:characteristic samm-c:Boolean .
                
                :AnotherValueCharacteristic a samm:Characteristic ;
                   samm:dataType xsd:string .
                
                
            """);
    public static final ChatMessage systemGuideline = UserMessage.from("""
            Use the following hints for correction:
            * General RDF and SAMM Model Rules:
                        
                Output Format: Always ensure the output is a valid RDF Turtle file. Don't write any explanation.
                Model Validity: Adhere to SAMM standards to create valid Aspect Models.
                 
            * Properties and Characteristics:
                        
                Each Entity or Aspect must have exactly one samm:properties field containing a list of properties.
                Example (Invalid):
                ```turtle
                :Person a samm:Entity ;
                    samm:properties ( :name ) ;
                    samm:properties ( [ samm:property :surname ; samm:payloadName "familyName" ] ) .
                ```
                Example (Correct):
                ```turtle
                :Person a samm:Entity ;
                    samm:properties ( :name [ samm:property :surname ; samm:payloadName "familyName" ] ) .
                ```
                Every samm:Property must have a samm:characteristic.
                A Property and its Characteristic cannot have the same name.
                Ensure the samm:dataType of a property matches the data type of its characteristic.
                
                Example (Invalid):
                ```turtle
                :ToolTemperature a samm-c:Measurement ;
                    samm:dataType xsd:float ;
                    samm-c:unit unit:degreeCelsius .
                            
                :drillHeadTemperature a samm:Property ;
                    samm:characteristic :ToolTemperature ;
                    samm:exampleValue "12"^^xsd:int .
                ```
                
                Example (Correct):
                ```turtle
                :ToolTemperature a samm-c:Measurement ;
                    samm:dataType xsd:float ;
                    samm-c:unit unit:degreeCelsius .
                            
                :drillHeadTemperature a samm:Property ;
                    samm:characteristic :ToolTemperature ;
                    samm:exampleValue "12"^^xsd:float .
                ```
                
            * Error Handling:
                If you encounter errors like "No model file", remove dependencies or references to external namespaces.
                If the example value type is incorrect, either correct it or remove the samm:exampleValue.
                
            * Constraints on Properties:
                        
                A Property must not have both samm:notInPayload true and samm:payloadName.
                If Property name cannot have special characters like "@id" or "@type", you need to use alphanumeric name for Property and use samm:payloadName
                Correct Example:
                ```turtle
                :Person a samm:Entity ;
                    samm:properties ( :name [ samm:property :surname ; samm:payloadName "@SName" ] ) .
                ```
            * Mandatory Fields:
                All model elements must have:
                samm:preferredName: Human-readable, uses normal orthography, no camelCase.
                samm:description: Short, comprehensible, and consistent.
                preferredName and description must not be identical.`
            * Traits and Enumerations:
                A Trait adds constraints to a "base characteristic." Traits inherit the data type from their samm-c:baseCharacteristic and do not define their own samm:dataType.
                For enumerations, list valid literal values with samm-c:values.
                Example:
                ```turtle
                :Status a samm-c:Enumeration ;
                    samm:dataType xsd:string ;
                    samm-c:values ( "Complete" "In Progress" "Created" ) .
                ```
            * Measurements:
                        
                A samm-c:Measurement must include a samm-c:unit.
                Example:
                ```turtle
                :TemperatureMeasurement a samm-c:Measurement ;
                    samm:dataType xsd:float ;
                    samm-c:unit unit:degreeCelsius .
                ```
                
            * Aspect Model Naming:
              Names must be singular unless the aspect contains a Collection, List, or Set as its only property, in which case plural naming is allowed.
                        
            * External Standards:
              Use samm:see to reference external standards.
            * Units:
              Always use units from the SAMM unit catalog when applicable.
            * Constraints:
                        
              Explicitly define known constraints for a use case within the Aspect Model.
                        
            * Example Values:
              All properties with simple types should have an samm:exampleValue that matches the data type of the characteristic.
                        
            Characteristics Classes: Guidelines for Valid Instances
                1- Allowed Instances
                            
                    Only specific instances or subclasses within the samm-c namespace (urn:samm:org.eclipse.esmf.samm:characteristic:2.1.0#) are valid.
                    If invalid instances (e.g., urn:samm:org.eclipse.esmf.samm:characteristic:2.1.0#Map or samm-c:Map) are found, they must be replaced with one of the valid characteristics listed below.
                2- Valid Characteristics and Their Required Attributes
                    Ensure each characteristic includes its mandatory attributes for the model to be valid:
                                
                    samm-c:Trait
                    samm-c:Quantifiable
                    samm-c:Measurement
                        Required attributes: samm-c:unit
                    samm-c:Enumeration
                        Required attributes: samm-c:values
                    samm-c:State
                        Required attributes: samm-c:defaultValue
                    samm-c:Duration
                        Required attributes: samm-c:unit
                        Example values: unit:week, unit:year, unit:month, unit:millisecond
                    samm-c:Collection
                    samm-c:List
                    samm-c:Set
                    samm-c:SortedSet
                    samm-c:TimeSeries
                        Required attributes: samm:dataType
                    samm-c:Code
                3- Discouraged Characteristics
                    While the following characteristics are valid, avoid using them unless necessary:
                    samm-c:Either
                        Required attributes: samm-c:left, samm-c:right
                    samm-c:SingleEntity
                    samm-c:StructuredValue
                        Required attributes: samm-c:deconstructionRule, samm-c:elements
                4- Important Notes
                            
                    Always include the required attributes for any characteristic. Models missing these attributes are invalid.
                
            * Characteristics Instances
            These are already instances of Characteristics. If you see `:ExampleCharacteristic a samm-c:Timestamp` then it is wrong because samm-c:Timestamp is already an instance and you need to correct it.
             - samm-c:Timestamp (Data Type: xsd:dateTime)
             - samm-c:Text (Data Type: xsd:string)
             - samm-c:MultiLanguageText (Data Type: rdf:langString)
                 
             - samm-c:Language
                 Description: Represents a property containing a language as per ISO 639-1 (e.g., "en" for English).
                 Data Type: xsd:string
                 
             - samm-c:Locale
                 Description: Represents a property containing a locale as per IETF BCP 47 (e.g., "en-US").
                 Data Type: xsd:string
                 
             - samm-c:Boolean
                 Description: Represents a property containing a boolean value (true/false).
                 Data Type: xsd:boolean
                 
             - samm-c:ResourcePath
                 Description: Represents a property containing the path to a relative or absolute resource.
                 Data Type: xsd:anyURI
                 
             - samm-c:MimeType
                 Description: Represents a property containing a MIME type as defined by RFC 2046 (e.g., "application/json").
                 Data Type: xsd:string
                 
             - samm-c:UnitReference
                 Description: Represents a property containing a reference to a unit in the Unit catalog.
                 Data Type: samm:curie
                 
             - samm-c:Reference
                 Description: Represents a property containing a reference to a concept such as a resource or model element (e.g., URI or SAMM identifier).
                 Data Type: Based on samm:see reference conventions.
                                
                """);

    public void initModels() {
        if (model.toLowerCase().startsWith("openai:")) {
            System.out.println("<USING OPENAI>");
            OpenAiChatModel.OpenAiChatModelBuilder openapiBuilder = OpenAiChatModel.builder()
                    .apiKey(System.getenv("OPENAI_API_KEY")) // Please use your own OpenAI API key
                    .modelName(model.replace("openai:", ""))
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .seed(seed)
                    .timeout(Duration.ofMinutes(20));
            System.out.println("openapiBuilder" + openapiBuilder.toString());
            chatModel = openapiBuilder.build();
        } else if (model.toLowerCase().contains("gemini")) {
            System.out.println("<USING GEMINI>");
//            openAIChatModel = GoogleAiGeminiChatModel.builder()
//                    .apiKey(System.getenv("GEMINI_AI_KEY"))
//                    .modelName("gemini-1.5-flash")
//                    .build();
        } else if (model.toLowerCase().startsWith("azure:")) {
            chatModel = AzureOpenAiChatModel.builder()
                    .apiKey(System.getenv("AZURE_API_KEY"))
                    .deploymentName(model.replace("azure:", ""))
                    .endpoint(System.getenv("AZURE_ENDPOINT"))
                    .timeout(Duration.ofMinutes(20))
                    .maxTokens(maxTokens)
                    .seed(Long.valueOf(seed))
                    .build();
        } else if (model.toLowerCase().startsWith("bedrock:")) {
            System.out.println("<USING AWS BEDROCK>"+System.getenv("AWS_ACCESS_KEY_ID"));
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                    System.getenv("AWS_ACCESS_KEY_ID"),
                    System.getenv("AWS_SECRET_ACCESS_KEY")
            );

            chatModel = BedrockLlamaChatModel
                    .builder()
                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                    .model(model.replace("bedrock:", ""))
                    .region(Region.US_WEST_2)
                    .maxRetries(1)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();
        } else {
            chatModel = OllamaChatModel.builder()
                    .baseUrl(ollamaEndpoint)
                    .modelName(model)
                    .seed(seed)
                    .temperature(temperature)
                    .topK(30)
                    .topP(0.9)
                    .repeatPenalty(1.1)
                    .numCtx(8800)
                    .numPredict(3800)
                    .timeout(Duration.ofMinutes(10))
                    .build();
            System.out.println(chatModel.toString());
        }

    }

    @Override
    public Integer call() throws Exception {
        if (examplesPath == null) {
            examplesPath = new ArrayList<>();
        }
        initModels();
        initPromptTemplate();
        processCsvFile();
        return 0;
    }

    private void initPromptTemplate() {
        int n = examplesPath.size();
//        Pattern pattern = Pattern.compile("^#.*$", Pattern.MULTILINE);

        if (n == 0) {
            promptTemplate = new ZeroShotPromptTemplate();
        }
        if (n >= 1) {
            List<String> exampleSAMMs = new ArrayList<>();
            List<String> exampleJsons = new ArrayList<>();
            for (int i = 0; i < n; i++) {

                // Remove comments from SAMM file
//                exampleSAMMs.add(pattern.matcher(Util.readFileContent(examplesPath.get(i))).replaceAll("").trim());
                exampleSAMMs.add(Util.readFileContent(examplesPath.get(i)));
                String fileName = Paths.get(examplesPath.get(i)).getFileName().toString().replace(".ttl", "");
                String jsonPath = Paths.get(examplesPath.get(i)).getParent().resolve("gen").resolve(fileName + ".json").toString();
                exampleJsons.add(Util.readFileContent(jsonPath));
            }
            promptTemplate = new FewShotPromptTemplate(exampleSAMMs, exampleJsons);
        }
    }

    private void processCsvFile() {
        String fileName = Paths.get(csvPath).toString();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            br.readLine();  // Ignore the first line (header)

            String line;
            while ((line = br.readLine()) != null) {
                // Process the remaining lines
                String[] path = line.split(",");
                //pass json file to process
                System.out.println("<PROCESSING> ->" + path[0]);
                try {
                    promptEngineering(path[0]);
                } catch (Error ex) {
                    ex.printStackTrace();
                }

            }
        } catch (Error e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("<JOB DONE>");
    }


    private void promptEngineering(String jsonFilePath) throws IOException {
        String jsonContent = Util.readFileContent(Paths.get(inputBasePath, jsonFilePath).toString());
        int numExamples = examplesPath.size();
        ChatMessage prompt = UserMessage.from(promptTemplate.prompt(jsonContent));
        Path resultDirectory = Paths.get(outputBasePath,
                jsonFilePath.replace(".json", ""),
                experimentName,
                model.replaceAll("\\W+", ""),
                promptTemplate.getClass().getSimpleName() + numExamples);
        if (Files.exists(resultDirectory.resolve("validsamm.txt"))) {
            System.out.println("SKIP: " + jsonFilePath);
            return;
        }

        String lastAttempt = "0";
        int attempt = 0;
        if (Files.exists(resultDirectory.resolve("attempt.txt"))) {
            lastAttempt = Files.readString(resultDirectory.resolve("attempt.txt"));
        }
        attempt = Integer.parseInt(lastAttempt) + 1;
        if (attempt > attemptLimit) {
            System.out.println("NO FURTHER ATTEMPT ALLOWED: " + jsonFilePath);
            return;
        }
        String result = null;

        if (chatModel == null) {
            throw new IllegalStateException("ChatModel is not initialized properly");
        }
        List<ChatMessage> prompts = new ArrayList<>();
        if (iterativePrompting == true && attempt > 1) {
//
            try {
                String lastException = Files.readString(resultDirectory.resolve((attempt - 1) + "-exception.txt"));
                String lastOutput = Files.readString(resultDirectory.resolve((attempt - 1) + "-result.txt"));
                prompts.add(new UserMessage("In your previous attempt you created this Semantic Aspect Meta Model SAMM Aspect Model\n" + lastOutput));
                prompts.add(new UserMessage("But it has the following error:\n" + lastException +
                        "\nTry to fix the error and generate the whole corrected SAMM Aspect Model without any extra explanation."));
                if (lastException.contains("org.apache.jena")) {
                    prompts.add(rdfSystemGuideline);
                } else if (lastException.contains("JSONs are not similar") == true
                        || lastException.contains("following JSON structure")==true) {
                    // no json related error
                    prompts.add(jsonMappingGuideline);
                }else if(lastException.contains("java.lang.ClassCastException")){
                    prompts.add(UserMessage.from("Try to simplify the previous SAMM Aspect Model to change the error." +
                            "\nAlso Remove any external references and elements like ext-built: ext-uuid: ext-uuid:UuidV4Trait ..."));
                }else if(lastException.contains("java.lang.NullPointerException")){
                    prompts.add(UserMessage.from("Try to simplify the previous SAMM Aspect Model to change the error." +
                            "\nAlso Remove any external references and elements like ext-built: ext-uuid: ext-uuid:UuidV4Trait ..."));
                }else if(lastException.contains("org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException")){
                    prompts.add(UserMessage.from("Try to not reuse any external model when you are creating this Aspect Model"));
                }else{
                    prompts.add(systemGuideline);
                }


            } catch (Exception ex) {
                System.out.println("Enable to resolve previous attempts. " + attempt);
            }
        } else {
            prompts.add(prompt);
        }

        Util.saveToFile(prompts.toString(), resultDirectory.resolve(attempt + "-prompt.txt").toString());

        Response<AiMessage> output = chatModel.generate(prompts);
        result = output.content().text();
        result = result.substring(Math.max(result.indexOf("@prefix"), 0));
        result = result.replace("```turtle", "");
        result = result.replace("\n```", "");
        Util.saveToFile(String.valueOf(attempt), resultDirectory.resolve("attempt.txt").toString());

        Util.saveToFile(result, resultDirectory.resolve(attempt + "-result.txt").toString());

        boolean validTurtle = false;
        boolean validSAMM = false;
        boolean validJson = false;

        try {
            validTurtle = SAMMUtil.isValidTurtle(result);
            validSAMM = SAMMUtil.isValidSAMM(result);
            System.out.println("Aspect model is valid!");
            String generatedJson = SAMMUtil.generateJsonPayload(result);
            Util.saveToFile(generatedJson, resultDirectory.resolve(attempt + "-payload.txt").toString());
            validJson = SAMMUtil.isValidJson(jsonContent, generatedJson);
            System.out.println("json is valid!");
            Util.saveToFile("pass", resultDirectory.resolve("validsamm.txt").toString());
        } catch (Exception | Error ex) {

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            ex.printStackTrace(printWriter);
            printWriter.flush();
            stringWriter.flush();
            System.out.println(stringWriter.toString());
            Util.saveToFile("SAMM Aspect Model is invalid: \n" + stringWriter.toString(),
                    resultDirectory.resolve(attempt + "-stacktrace.txt").toString());
            Util.saveToFile("SAMM Aspect Model is invalid: \n" + ex.toString(),
                    resultDirectory.resolve(attempt + "-exception.txt").toString());
        }
        String outputResultStat = "model,isTurtle,isSAMM,isJSON,passAt" + "\n" +
                jsonFilePath.replace(".json", "") +
                ",%b,%b,%b,%d".formatted(validTurtle, validSAMM, validJson, attempt);
        Util.saveToFile(outputResultStat, resultDirectory.resolve("summary.txt").toString());
        Util.saveToFile(outputResultStat, resultDirectory.resolve(attempt + "-summary.txt").toString());
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new Inference()).execute(args);
        System.exit(exitCode);
    }
}