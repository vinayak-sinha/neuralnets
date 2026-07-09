# Neural Networks from First Principles

Java implementation work from **ATCS Neural Networks** at The Harker School, taught by Dr. Eric R. Nelson in Spring 2026.

This project builds a neural-network framework from the ground up rather than relying on a machine-learning library. It starts with small Boolean classification experiments such as XOR, then extends the same feed-forward/backpropagation framework to image-recognition data prepared from BMP/hand-image inputs.

## What I Built

- A configurable fully connected feed-forward neural network in Java.
- Backpropagation-based supervised training with configurable layer sizes, learning rate, iteration cap, error threshold, and weight initialization/loading behavior.
- Binary input, truth-table, and weight file formats for repeatable training and testing runs.
- XOR and Boolean-classifier control files for validating the general network framework on small problems.
- Image preprocessing utilities for converting BMP images into activation vectors, normalizing/cropping image data, packing examples into binary datasets, and generating truth-table files.
- Training and testing datasets for a five-class image-recognition task, plus saved trained weights for repeatable inference.

## Course Context

Course: **ATCS Neural Networks**

School: **The Harker School**

Instructor: **Dr. Eric R. Nelson**

Term: **Spring 2026**

This course covered parallel distributed processing, perceptrons, nonlinear classifiers, gradient descent, backpropagation, fully connected feed-forward networks, and image pattern recognition. This repository reflects that progression through XOR experiments, a generalized neural-network framework, BMP/image preprocessing tools, and a final image-recognition workflow.

## Project Structure

```text
src/main/java/com/vinayak/
  Network.java                         # configurable neural network and training/inference loop
  Parser.java                          # control-file parser and binary matrix writer
  bitmapconversion/                    # BMP, grayscale, activation, packing, and truth-table utilities
  pelarray/                            # pixel-array image transforms used by preprocessing tools

control.txt                            # sample Boolean network run
XORControl.txt                         # XOR experiment configuration
imageTrainingControl.txt               # image-recognition training configuration
imageTestingControl.txt                # image-recognition testing configuration

trainingImages/, testingImages/        # normalized image examples
trainingImages.bin, testingImages.bin  # packed binary datasets
imageTruth.bin                         # image-class truth table
imageWeights.bin                       # saved trained image-model weights
```

## Skills Demonstrated

- Implementing neural-network training mechanics without framework abstractions.
- Translating mathematical learning rules into explicit Java data structures and loops.
- Designing a reusable configuration system for different network topologies and experiments.
- Working with binary file I/O for datasets, labels, and learned weights.
- Building image-processing utilities around BMP layout, grayscale conversion, normalization, cropping, and activation-vector packing.
- Validating a generalized implementation against small, inspectable problems before scaling to image recognition.
- Managing practical training concerns such as convergence thresholds, learning rates, iteration limits, random initialization, and saved weight reuse.

## Running

This is a Maven project targeting Java 21.

```bash
mvn test
```

Run the main network entry point with a control file:

```bash
mvn exec:java -Dexec.mainClass="com.vinayak.Network" -Dexec.args="control.txt"
```

If the Maven exec plugin is not configured locally, compile and run directly:

```bash
mvn compile
java -cp target/classes com.vinayak.Network control.txt
```

Use `imageTrainingControl.txt` to train with the packed image dataset and `imageTestingControl.txt` to run inference from saved weights.

## Creating New Experiments

The network is driven by plain-text control files, so new experiments can be created without changing Java code. A control file defines:

- the network shape: `inputSize`, `hiddenSizes`, `outputSize`, and `numActivationLayers`
- training settings: learning rate, random weight bounds, maximum iterations, error threshold, and progress interval
- runtime behavior: whether to train, run test cases, print inputs, save weights, or load existing weights
- file paths for binary inputs, truth tables, loaded weights, and saved weights
- the number of test/training cases to read from the dataset

To create a new run, copy an existing file such as `control.txt` or `imageTestingControl.txt`, rename it, and adjust the values for the dataset and architecture you want to test.

Example:

```bash
cp control.txt myExperimentControl.txt
mvn compile
java -cp target/classes com.vinayak.Network myExperimentControl.txt
```

For small Boolean experiments, create matching binary input and truth-table files in the same format used by `basicBinaryInputs.bin` and `basicBinaryTruthAOX.bin`. For image experiments, use the utilities in `src/main/java/com/vinayak/bitmapconversion/` to convert images into activation data, pack examples into a dataset, and generate the corresponding truth table.

## Notes

- The project intentionally includes small binary datasets and saved weights so reviewers can see the full course pipeline, not only the Java source.
- `trainingHistory.txt` is ignored because it is a large generated run log.
- No external neural-network or image-processing libraries are used for the core implementation.
