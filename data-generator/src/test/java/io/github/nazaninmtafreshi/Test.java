package io.github.nazaninmtafreshi;


import io.github.nazaninmtafreshi.core.Util;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.eclipse.esmf.aspectmodel.resolver.FileSystemStrategy;
import org.eclipse.esmf.aspectmodel.resolver.ResolutionStrategy;

import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.serializer.AspectSerializer;
import org.eclipse.esmf.metamodel.AspectModel;
import org.eclipse.esmf.metamodel.vocabulary.SAMM;
import org.eclipse.esmf.metamodel.vocabulary.SAMMC;
import org.eclipse.esmf.metamodel.vocabulary.SAMME;
import org.eclipse.esmf.samm.KnownVersion;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.regex.Pattern;

import static io.github.nazaninmtafreshi.ComplexityUtil.calculateComplexity;
import static io.github.nazaninmtafreshi.DataManipulator.*;
import static io.github.nazaninmtafreshi.core.Util.incrementVersion;
import static io.github.nazaninmtafreshi.core.Util.loadModel;


public class Test {

    public static String BASE_DIR = "D:\\NMT Thesis\\Dataset\\content\\sldt-semantic-models";
    private static final ResolutionStrategy FILE_SYSTEM_STRATEGY = new FileSystemStrategy(Path.of(BASE_DIR));

    @org.junit.jupiter.api.Test
    public void testLoadModel() {

        String ROOT_PATH = "D:\\NMT Thesis\\Dataset\\content\\sldt-semantic-models\\";
        String rdfFilePath = "io.catenax.idconversion\\2.0.0\\IdConversion.ttl";
        String sammNamespace = "urn:samm:org.eclipse.esmf.samm:meta-model:2.1.0#";
        Model model = loadModel(ROOT_PATH, rdfFilePath);
        String prefixSamm = model.getNsPrefixMap().get("samm");
        String sammVersion = prefixSamm.substring(prefixSamm.lastIndexOf(":") + 1, prefixSamm.length() - 1);
        SAMM samm = new SAMM(KnownVersion.fromVersionString(sammVersion).get());
//        removeOperationEvent(model, samm);

    }

    @org.junit.jupiter.api.Test
    public void testDropEntity() {
        String ROOT_PATH = "D:\\NMT Thesis\\Dataset\\content\\sldt-semantic-models\\";
        String rdfFilePath = "io.catenax.shared.contact_information\\4.0.0\\ContactInformation.ttl";

//        extracted(ROOT_PATH, rdfFilePath);
    }


    @org.junit.jupiter.api.Test
    public void testChangeName() {
        String ROOT_PATH = "D:\\NMT Thesis\\Dataset\\content\\sldt-semantic-models\\";
        String rdfFilePath = "io.catenax.shared.contact_information\\4.0.0\\ContactInformation.ttl";
//        applyChangeName(ROOT_PATH, rdfFilePath);

    }



}

