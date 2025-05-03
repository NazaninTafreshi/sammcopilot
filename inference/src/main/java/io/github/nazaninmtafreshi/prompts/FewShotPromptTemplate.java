package io.github.nazaninmtafreshi.prompts;

import java.util.List;

public class FewShotPromptTemplate implements PromptTemplate {
    private List<String> sampleSAMM;
    private List<String> sampleJson;


    public FewShotPromptTemplate(List<String> sampleSAMM, List<String> sampleJson) {
        this.sampleSAMM = sampleSAMM;
        this.sampleJson = sampleJson;
    }


    @Override
    public String prompt(String input) {
        int n = sampleJson.size();
        StringBuilder promptContent = new StringBuilder();
        for (int i = 0; i < n; i++) {
            promptContent.append("This is an example SAMM model:\n");
            promptContent.append(sampleSAMM.get(i));
            promptContent.append("\n");
            promptContent.append("This is its corresponding JSON example:\n");
            promptContent.append(sampleJson.get(i));
            promptContent.append("\n");
        }
        promptContent.append("Your task is to create a SAMM model from a JSON Example.\n");
        promptContent.append("Json Example:\n");
        promptContent.append(input);
        promptContent.append("\n");
        promptContent.append("Provide only the SAMM model without any extra explanation. Make sure that the output is a valid RDF Turtle format.\n");
        return promptContent.toString();
    }
}
