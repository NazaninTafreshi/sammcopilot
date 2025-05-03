package io.github.nazaninmtafreshi.prompts;

public class ZeroShotPromptTemplate implements PromptTemplate {


    @Override
    public String prompt(String input) {
        return "You are a bot to help people create Semantic Aspect Meta Model (SAMM) from given JSON data. Create SAMM model based on the following JSON:\n" +
               "JSON:\n" +
               input + "\n" +
               "Provide only the SAMM model without any extra explanation. Make sure you always give a valid RDF turtle as the SAMM model.\n";
    }


}
