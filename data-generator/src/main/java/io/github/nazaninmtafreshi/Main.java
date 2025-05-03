package io.github.nazaninmtafreshi;

import io.github.nazaninmtafreshi.core.Util;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.esmf.metamodel.vocabulary.SAMM;
import org.eclipse.esmf.metamodel.vocabulary.SAMMC;
import org.eclipse.esmf.metamodel.vocabulary.SAMME;
import org.eclipse.esmf.samm.KnownVersion;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import static io.github.nazaninmtafreshi.ComplexityUtil.calculateComplexity;
import static io.github.nazaninmtafreshi.DataManipulator.*;
import static io.github.nazaninmtafreshi.core.Util.*;

@CommandLine.Command(name = "data-generator", mixinStandardHelpOptions = true, version = "data-generator 1.0",
        description = "Data Generator randomly performs operations on Aspect Model to generate more data.")
public class Main implements Callable<Integer> {
    static final Logger logger = LogManager.getLogger();
    @CommandLine.Option(names = {"--input-base-path"}, description = "Base path pointing to the catena-x dataset.", defaultValue = "D:/NMT Thesis/Dataset/sldt-semantic-models/")
    private String inputBasePath;
    @CommandLine.Option(names = {"--change-rate"}, description = "Chance of change which is a value between 0-1.", defaultValue = "0.5")
    private Double changeRate;

    @CommandLine.Option(names = {"--csv-path"}, description = "Perform change on the list of files which are in a csv.", defaultValue = "D:/NMT Thesis/Dataset/augmented_2024_09_18_v1_drop_rename_02_05/validation_2024-09-18.csv")
    private String csvPath;
    //D:/NMT Thesis/Dataset/file_path_2024-09-04.csv
    @CommandLine.Option(names = {"--file-path"}, description = ".ttl path if you want to apply change on one file.", defaultValue = "")
    private String filePath;
    //io.catenax.shared.contact_information/4.0.0/ContactInformation.ttl
    //io.catenax.pcf/7.0.0/Pcf.ttl
    //io.catenax.single_level_bom_as_built\2.0.0\SingleLevelBomAsBuilt.ttl
    @CommandLine.Option(names = {"--mode"}, description = "What kind of changes to apply. It should be a string with values 'drop','rename','remove-example'.", defaultValue = "drop,rename")
    private String mode;

    @CommandLine.Option(names = {"--augmentData"}, description = "Set to true to augment Data, or false.", defaultValue = "false", arity = "0..1")
    private Boolean augmentData;

    @CommandLine.Option(names = {"--calculateComplexity"}, description = "Set to true to calculate complexity, or false.", defaultValue = "true", arity = "0..1")
    private Boolean calculateComplexity;



    @Override
    public Integer call() throws Exception {
        if (filePath != null && !filePath.isEmpty()) {
            System.out.println("In FILE PATH MODE");
            processModel(filePath);
        } else if (csvPath != null && !csvPath.isEmpty()) {
            System.out.println("In CSV PATH MODE");
            String line = "";
            String csvSplitBy = ",";
            int filePathColumnIndex = 1; // ttl file paths are in second column of csv

            try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
                // Skip the header row
                String header = br.readLine();

                while ((line = br.readLine()) != null) {
                    String[] columns = line.split(csvSplitBy);
                    if (columns.length > filePathColumnIndex) {
                        try {
                            String ttlFilePath = columns[filePathColumnIndex];
                            if (calculateComplexity){
                                String fileName = Paths.get(csvPath).getFileName().toString();
                                String parentDir = Paths.get(csvPath).getParent().toString();
                                saveDictionaryToCSV(calculateComplexity(inputBasePath, ttlFilePath),
                                        Paths.get(parentDir, "complexity_csv_" + fileName).toString());
                            }
                            if (augmentData){
                                processModel(ttlFilePath);
                            }

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("End of Process");
        return 0;
    }

    private void processModel(String filePath) {
        Model model = loadModel(inputBasePath, filePath);
        String sammPrefix = model.getNsPrefixMap().get("samm");
        //        System.out.println("sammPrefix = " + sammPrefix);
        String sammVersion = sammPrefix.substring(sammPrefix.lastIndexOf(":") + 1, sammPrefix.length() - 1);
        //        System.out.println("sammVersion = " + sammVersion);
        SAMM samm = new SAMM(KnownVersion.fromVersionString(sammVersion).get());
        if (mode.contains("drop")) {
            System.out.println("Drop");
            dropProperty(model, samm, changeRate);
          //  RDFDataMgr.write(System.out, model, Lang.TURTLE);
        }
        if (mode.contains("rename")) {
            System.out.println("Rename");
            changeName(model, samm, changeRate);
        }
        if (mode.contains("remove-example")) {
            System.out.println("Change Rate");
            removeExampleValue(model, samm, changeRate);
        }

        Resource aspectAsResource = model.listStatements(null, RDF.type, samm.Aspect()).nextStatement().getSubject();
//        System.out.println(aspectAsResource);
        // Find all nodes connected to the root node
        Set<Resource> connectedNodes = findConnectedNodes(model, aspectAsResource);
//        System.out.println(connectedNodes);
        // Remove unconnected components
        removeUnconnectedComponents(model, connectedNodes);
//        RDFDataMgr.write(System.out, model, Lang.TURTLE);

        String basePrefix = model.getNsPrefixURI("");
//        System.out.println("basePrefix = " + basePrefix);
        String modelVersion = basePrefix.substring(basePrefix.lastIndexOf(":") + 1, basePrefix.lastIndexOf("#"));
        String modelRootFolder = Paths.get(filePath).subpath(0, 1).toString();
//        System.out.println("modelRootFolder = " + modelRootFolder);
        Path path = Paths.get(inputBasePath, modelRootFolder, modelVersion);
        while (Files.exists(path)) {
            modelVersion = incrementVersion(modelVersion);
            path = Paths.get(inputBasePath, modelRootFolder, modelVersion);
        }
//        System.out.println("modelVersion = " + modelVersion);


        String fileName = Paths.get(filePath).getFileName().toString();
        String outputFilePath = path.resolve(fileName).toString();
//        System.out.println("outputFilePath = " + outputFilePath);
        String newURI = basePrefix.substring(0, basePrefix.lastIndexOf(":") + 1) + modelVersion + "#";
//        System.out.println("newURI = " + newURI);
        String newPrefix = "@prefix : <" + newURI + "> .";
//        System.out.println("newPrefix = " + newPrefix);

        String modelAsString = saveModelAsString(basePrefix, inputBasePath, model).replaceAll("@prefix\\s*:\\s*<" + Pattern.quote(basePrefix) + ">\\s*.", newPrefix);
        String jsonPayload = generateJsonPayloadPretty(basePrefix, loadAspectModelFromString(inputBasePath, modelAsString));
        Util.saveToFile(jsonPayload, Paths.get(path.toString(), "gen", fileName.replace("ttl", "json")).toString());
        Util.saveToFile(modelAsString, outputFilePath);

    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }


}
