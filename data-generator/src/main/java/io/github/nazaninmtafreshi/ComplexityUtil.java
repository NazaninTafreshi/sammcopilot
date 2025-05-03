package io.github.nazaninmtafreshi;

import org.apache.jena.rdf.model.*;
import org.eclipse.esmf.metamodel.vocabulary.SAMM;
import org.eclipse.esmf.metamodel.vocabulary.SAMMC;
import org.eclipse.esmf.metamodel.vocabulary.SAMME;
import org.eclipse.esmf.samm.KnownVersion;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.github.nazaninmtafreshi.core.Util.loadModel;


public class ComplexityUtil {

    private static int countStatements(Model model, Property predicate, RDFNode object) {
        return (int) model.listStatements(null, predicate, object).toList().size();
    }

    public static Map<String, Object> calculateComplexity(String inputBasePath, String modelPath) {
        Model model = loadModel(inputBasePath, modelPath);
        String sammPrefix = model.getNsPrefixMap().get("samm");
        String sammVersion = sammPrefix.substring(sammPrefix.lastIndexOf(":") + 1, sammPrefix.length() - 1);
        KnownVersion metaModelVersion = KnownVersion.fromVersionString(sammVersion).get();
        SAMM samm = new SAMM(metaModelVersion);
        SAMMC sammc = new SAMMC(metaModelVersion);
        SAMME samme = new SAMME(metaModelVersion, samm);


        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("FilePath", modelPath);
        metrics.put("#triples", model.size());
// Objects
        metrics.put("Property", countStatements(model, null, samm.Property()));
        metrics.put("Aspect",countStatements(model, null, samm.Aspect()));
        metrics.put("Characteristic",countStatements(model, null, samm.Characteristic()));
        metrics.put("Entity",countStatements(model, null, samm.Entity()));
        metrics.put("Constraint",countStatements(model, null, samm.Constraint()));
        metrics.put("AbstractEntity",countStatements(model, null, samm.AbstractEntity()));
        metrics.put("AbstractProperty",countStatements(model, null, samm.AbstractProperty()));
        //sammc
        metrics.put("Collection",countStatements(model, null, sammc.Collection()));
        metrics.put("Trait",countStatements(model, null, sammc.Trait()));
        metrics.put("Set",countStatements(model, null, sammc.Set()));
        metrics.put("Quantifiable",countStatements(model, null, sammc.Quantifiable()));
        metrics.put("RangeConstraint",countStatements(model, null, sammc.RangeConstraint()));
        metrics.put("RegularExpressionConstraint",countStatements(model, null, sammc.RegularExpressionConstraint()));
// Properties
        metrics.put("description",countStatements(model, samm.description(), null));
        metrics.put("exampleValue",countStatements(model, samm.exampleValue(), null));
        metrics.put("see",countStatements(model, samm.see(), null));
        metrics.put("preferredName",countStatements(model, samm.preferredName(), null));
        //sammc
        metrics.put("unit",countStatements(model, sammc.unit(), null));
        metrics.put("maxValue",countStatements(model, sammc.maxValue(), null));
        metrics.put("minValue",countStatements(model, sammc.minValue(), null));
        metrics.put("upperBoundDefinition",countStatements(model, sammc.upperBoundDefinition(), null));
        metrics.put("lowerBoundDefinition",countStatements(model, sammc.lowerBoundDefinition(), null));
        metrics.put("constraint",countStatements(model, sammc.constraint(), null));
        metrics.put("baseCharacteristic",countStatements(model, sammc.baseCharacteristic(), null));
        //samme
        metrics.put("value",countStatements(model, samme.value(), null));

        return metrics;



    }
}
