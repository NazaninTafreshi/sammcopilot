package io.github.nazaninmtafreshi;

import java.io.*;

import java.util.*;

import io.github.nazaninmtafreshi.core.Util;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import org.apache.jena.vocabulary.RDF;

import java.nio.file.Path;

import org.eclipse.esmf.metamodel.vocabulary.SAMM;


public class DataManipulator {
    private final static long seed = 43;
    public static void changeName(Model model, SAMM samm, double changeRate) {
        Random random = new Random(seed);
        StmtIterator statements = model.listStatements(null, RDF.type, samm.Property());
        List<Resource> resourcesToReplace = new ArrayList<>();

        while (statements.hasNext()) {
            Statement statement = statements.nextStatement();
            Resource toReplace = statement.getSubject();
            resourcesToReplace.add(toReplace);
        }

        int totalResources = resourcesToReplace.size();
//        System.out.println("totalResources = " + totalResources);
        if (totalResources == 0) {
            // No resources to process
            return;
        }
        Collections.shuffle(resourcesToReplace, random);
        int numberOfResourcesToRename = Math.min((int) (totalResources * changeRate) + 1, totalResources);
//        System.out.println("numberOfResourcesToRename = " + numberOfResourcesToRename);
        // Only keep the first half of the shuffled list
        List<Resource> resourcesToRename = resourcesToReplace.subList(0, numberOfResourcesToRename);

        for (Resource toReplace : resourcesToRename) {
//            if(random.nextBoolean()){
//                // Replace all triples where oldResource is the object
//                model.listStatements(null, samm.property(), toReplace).forEachRemaining(stmt -> {
//                    //remove payloadName
//                    model.removeAll(stmt.getSubject(), samm.payloadName(), (RDFNode) null);
//                    model.add(stmt.getSubject(), samm.payloadName(), Util.toSnakeCase(toReplace.getLocalName()));
//                });
//                model.listStatements(null, samm.properties(), toReplace).forEachRemaining(stmt -> {
//                    //remove payloadName
//                    model.removeAll(stmt.getSubject(), samm.properties(), toReplace);
//                    Resource resource = model.createResource();
//                    resource.addProperty(samm.property(),toReplace);
//                    resource.addProperty(samm.payloadName(),Util.toSnakeCase(toReplace.getLocalName()));
//                    model.add(stmt.getSubject(), samm.properties(),resource);
//                });
//            }else {
            String newURI = toReplace.getNameSpace() + toReplace.getLocalName().toUpperCase();
//                System.out.println("newURI = " + newURI);
            // Create Resource objects for the new URIs
            Resource newResource = model.createResource(newURI);
            Property newProperty = model.createProperty(newURI);
            Property oldProperty = model.createProperty(toReplace.getURI());

            // Replace all triples where oldResource is the subject
            model.listStatements(toReplace, null, (RDFNode) null).forEachRemaining(stmt -> {
                model.add(newResource, stmt.getPredicate(), stmt.getObject());
            });

            // Replace all triples where oldResource is the object
            model.listStatements(null, null, toReplace).forEachRemaining(stmt -> {
                model.add(stmt.getSubject(), stmt.getPredicate(), newResource);
            });

            // Replace all triples where oldProperty is the predicate
            model.listStatements(null, oldProperty , (RDFNode) null).forEachRemaining(stmt -> {
                model.add(stmt.getSubject(), newProperty, stmt.getObject());
            });

            //remove payloadName
            model.listStatements(null, samm.property(), newResource).forEachRemaining(stmt -> {
                model.removeAll(stmt.getSubject(), samm.payloadName(), (RDFNode) null);
            });

            model.removeAll(toReplace, null, (RDFNode) null);
            model.removeAll(null, null, toReplace);
            model.removeAll(null, oldProperty, null);
//            }

        }
    }

    public static void removeExampleValue(Model model, SAMM samm, double changeRate) {
        Random random = new Random(seed);
        ResIterator resIterator = model.listSubjectsWithProperty(samm.exampleValue());

        while (resIterator.hasNext()) {
            Resource res = resIterator.nextResource();
            if (random.nextDouble() < changeRate) {
                res.removeAll(samm.exampleValue());
            }
        }
    }

    public static void dropProperty(Model model, SAMM samm, double changeRate) {
        Random random = new Random(seed);
        ResIterator resIterator = model.listSubjectsWithProperty(samm.properties());
        while (resIterator.hasNext()) {
            Resource res = resIterator.nextResource();

            RDFList list = model.getList(res.getPropertyResourceValue(samm.properties()));
            List<RDFNode> javaList = list.asJavaList();
            if (javaList.size() > 1) {
                // Shuffle the list to randomize element order
                Collections.shuffle(javaList, random);
                // Calculate the number of elements to remove (half of the list size)
                int elementsToRemove = Math.min((int) (javaList.size() * changeRate) + 1, javaList.size() - 1);

                for (int i = 0; i < elementsToRemove; i++) {
                    RDFNode nodeToRemove = javaList.remove(0);
//                    System.out.println(nodeToRemove.asResource());
                }

            }
            RDFList remainingList = model.createList(javaList.iterator());
            res.removeAll(samm.properties());
            res.addProperty(samm.properties(), remainingList);

        }

    }
}