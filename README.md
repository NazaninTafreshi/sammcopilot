# SAMM Copilot

[![Thesis PDF](https://img.shields.io/badge/Thesis-Read%20PDF-blue)](https://github.com/NazaninTafreshi/sammcopilot/blob/main/CS-MA-1072_Nazanin_Mashhaditafreshi___Master_Thesis___419282.pdf)
[![Presentation PPTX](https://img.shields.io/badge/Presentation-View%20PPTX-orange)](https://github.com/NazaninTafreshi/sammcopilot/blob/main/ThesisFinalPresentation.pptx)
[![Explanation Video](https://img.shields.io/badge/Video-Watch%20Explanation-red)](https://github.com/NazaninTafreshi/sammcopilot/raw/refs/heads/main/Nazanin%20Mashhaditafreshi_Thesis_SAMM%20Copilot.mp4)

This repository contains all artifacts related to the Master's thesis "SAMM Copilot," completed at Bosch Connected Industry. The project focuses on creating Aspect Models (based on Semantic Aspect Meta Model (SAMM)) using Large Language Models (LLMs). Instead of using natural language text as input, this work leverages structured data in JSON to create the semantic model.

**Key Resources:**

*   **Full Thesis:** [CS-MA-1072_Nazanin_Mashhaditafreshi___Master_Thesis___419282.pdf](https://github.com/NazaninTafreshi/sammcopilot/blob/main/CS-MA-1072_Nazanin_Mashhaditafreshi___Master_Thesis___419282.pdf)
*   **Presentation:** [ThesisFinalPresentation.pptx](https://github.com/NazaninTafreshi/sammcopilot/blob/main/ThesisFinalPresentation.pptx)
*   **Short Explanation Video:** [Nazanin Mashhaditafreshi_Thesis_SAMM Copilot.mp4](https://github.com/NazaninTafreshi/sammcopilot/raw/refs/heads/main/Nazanin%20Mashhaditafreshi_Thesis_SAMM%20Copilot.mp4)

---

## Dataset for Fine-tuning

The dataset used to fine-tune the primary OpenAI model described in the thesis is located in the `dataset/original_cleaned_data/` directory. These files are provided in the required `.jsonl` format:

*   **Training Data:** [`dataset/original_cleaned_data/output_dataset_train_2024-09-18.jsonl`](https://github.com/NazaninTafreshi/sammcopilot/blob/main/dataset/original_cleaned_data/output_dataset_train_2024-09-18.jsonl)
*   **Validation Data:** [`dataset/original_cleaned_data/output_dataset_validation_2024-09-18.jsonl`](https://github.com/NazaninTafreshi/sammcopilot/blob/main/dataset/original_cleaned_data/output_dataset_validation_2024-09-18.jsonl)
*   **Test Data:** [`dataset/original_cleaned_data/output_dataset_test_2024-09-18.jsonl`](https://github.com/NazaninTafreshi/sammcopilot/blob/main/dataset/original_cleaned_data/output_dataset_test_2024-09-18.jsonl)

This data can be directly uploaded to the OpenAI platform for fine-tuning purposes.

---

## Training a Model

You can replicate the model training process using the provided data or experiment with alternative methods:

### 1. OpenAI Fine-tuning (Replicating Thesis Model)

1.  Use the `.jsonl` files provided in the [Dataset](#dataset-for-fine-tuning) section.
2.  Upload these files to the OpenAI fine-tuning platform.
3.  The exact seed number and configuration used during the thesis work are detailed in the [full thesis text](https://github.com/NazaninTafreshi/sammcopilot/blob/main/CS-MA-1072_Nazanin_Mashhaditafreshi___Master_Thesis___419282.pdf). However, the default fine-tuning settings suggested by OpenAI are generally effective.

### 2. Alternative Fine-tuning using Unsloth/Qwen

If you wish to experiment with fine-tuning a different model architecture (e.g., Qwen 2.5 Coder) using the Unsloth library, a Jupyter notebook is provided:

*   **Notebook:** [`notebooks/Qwen_2_5_Coder_SAMM_Fine_Tune.ipynb`](https://github.com/NazaninTafreshi/sammcopilot/blob/main/notebooks/Qwen_2_5_Coder_SAMM_Fine_Tune.ipynb)

---

## Inference and Reproducing Results

You can perform inference using the fine-tuned model in the following ways:

### 1. Java Inference Code (Recommended for Reproducibility)

The Java code located in the [`inference/`](https://github.com/NazaninTafreshi/sammcopilot/tree/main/inference) directory is designed to evaluate the test set and reproduce the results reported in the thesis.

*   **Functionality:** This code automatically implements the iterative prompting strategy described in the thesis.
*   **Dependencies:** Project dependencies are managed using Apache Maven. Refer to the `pom.xml` file within the `inference` directory.
*   **Augmented Data:** The inference process utilizes augmented data (SAMM Aspect Model). This data is provided as zip files within the `dataset/augmented_data/` directory. You will need to extract these files before running the inference code.
*   **Input Format:** Requires an example JSON input. Examples can be found within the human evaluation section or detailed descriptions in the [thesis text](https://github.com/NazaninTafreshi/sammcopilot/blob/main/CS-MA-1072_Nazanin_Mashhaditafreshi___Master_Thesis___419282.pdf).

### 2. GPT Interface (e.g., LibreChat, OpenAI Playground)

Alternatively, you can interact with your fine-tuned model using standard GPT interfaces. You will need to provide appropriate prompts, potentially including few-shot examples based on the task format described in the thesis.

---

## Utilities

*   **Result Aggregation:** A Jupyter notebook is available to help aggregate and process the structured results generated by the Java inference code: [`notebooks/Master_Thesis_dataCleaning.ipynb`](https://github.com/NazaninTafreshi/sammcopilot/blob/main/notebooks/Master_Thesis_dataCleaning.ipynb).

---

## Additional Artifacts

*   **Drawings:** Diagrams and illustrations created for the thesis are available in the [`drawings/`](https://github.com/NazaninTafreshi/sammcopilot/tree/main/drawings) folder. See [Citation](#citation) section for attribution.
*   **Experimental Data Generation:** Code related to earlier data augmentation and generation experiments (which were found to be less effective) can be found in the [`data-generator/`](https://github.com/NazaninTafreshi/sammcopilot/tree/main/data-generator) folder.

---
## Citation

If you use the work or artifacts from this repository in your research or projects, please cite the Master's thesis:

```bibtex
TBD
```
