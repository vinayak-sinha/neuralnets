package com.vinayak;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/*
 * Author: Vinayak Sinha
 * Course: ATCS Neural Networks
 * Date: 05/08/2026
 *
 * Description:
 * This program implements a fully connected feedforward neural network with one
 * input layer, one or more hidden layers, and one output layer. The network
 * shape and runtime options are loaded from a control file.
 *
 * The program supports:
 *
 *  - Forward propagation
 *  - Training with backpropagation and gradient-descent weight updates
 *  - Running a fixed set of test cases
 *  - Optional random, manual, or load-from-file weight initialization
 *  - Optional keep-alive progress messages during training
 *  - Optional input-table printing during result reporting
 *
 * When training is enabled, the network repeatedly processes the configured
 * training set until the average error falls below the target threshold or the
 * maximum iteration count is reached. After training, the network can run the
 * configured test cases and print both summary and per-case results.
 *
 * Method Table of Contents:
 *    - public void setConfigParams(String[] controlFile)
 *    - public void echoConfigParams()
 *    - public void allocateMemory()
 *    - public void populateArrays()
 *    - private void setWeights()
 *    - private void setRandomWeights()
 *    - private double randomize(double low, double high)
 *    - private void setManualWeights()
 *    - private void loadWeights()
 *    - private boolean weightsMatch(int fileInputSize, int[] fileHiddenSizes, int fileOutputSize)
 *    - public void saveWeights()
 *    - private void setTrainingData()
 *    - private void setTruthTable()
 *    - private double activationFunction(double x)
 *    - private double activationDerivative(double x)
 *    - private double sigmoid(double x)
 *    - private double sigmoidDerivative(double x)
 *    - private double tanh(double x)
 *    - private double tanhDerivative(double x)
 *    - public void train()
 *    - public double runForTraining(int testCase)
 *    - private void updateWeights()
 *    - public void run()
 *    - public void reportTrainingResults()
 *    - private void setActivations(double[] input)
 *    - public double[][] runTestCases()
 *    - public void reportRunResults(double[][] outputs)
 *    - public static void main(String[] args)
 */
public class Network
{
/*
 * Configuration parameters
 */
   private final static String DEFAULT_CONTROL_FILE = "control.txt";
   private String controlFileName;
   private int inputSize;
   private int[] hiddenSizes;    // 1-indexed by hidden layer number, with hiddenSizes[0] unused
   private int outputSize;
   private double randLowerBound;
   private double randUpperBound;
   private double learningRate;
   private int maxIterations;
   private double errorThreshold;
   private int keepAlive;
   private String loadFileName;
   private String saveFileName;
   private String trainingDataFileName;
   private String truthTableFileName;

/*
 * Layer-count metadata and helper indices.
 */
   private int numActivationLayers;
   private int numConnectivityLayers;
   private static final int INPUT_LAYER = 0;
   private static final int FIRST_HIDDEN_LAYER = 1;
   private int lastHiddenLayer;
   private int outputLayer;

/*
 * Network activation values stored by activation layer.
 *
 * a[INPUT_LAYER] stores the current input vector.
 * a[n] for FIRST_HIDDEN_LAYER <= n <= lastHiddenLayer stores hidden activations.
 * a[outputLayer] stores the current output activations.
 */
   private double[][] a;

/*
 * Network weights stored by connectivity layer.
 *
 * Each matrix is indexed as w[layer][sourceNeuron][destinationNeuron].
 */
   private double[][][] w;

/*
 * Weighted sums used during training forward passes.
 *
 * theta[n][k] stores the pre-activation weighted sum for neuron k in hidden
 * layer n. The input layer does not use theta, and the output-layer weighted
 * sums are kept local inside runForTraining().
 */
   private double[][] theta;

/*
 * Backpropagation psi values.
 *
 * psi[outputLayer] stores output-layer error terms. For hidden layers beyond
 * the first hidden layer, psi[n] stores the hidden-layer error terms needed to
 * continue backpropagation toward the input layer.
 */
   private double[][] psi;

/*
 * Input cases and expected output values
 */
   private int numTestCases;
   private double[][] trainingData;
   private double[][] truthTable;

/*
 * Control flags
 */
   private boolean training;
   private String weightInputChoice;
   private boolean runningTestCases;
   private boolean saveWeights;
   private boolean printInputs;

/*
 * Training results tracking
 */
   private boolean hitErrorThreshold;
   private boolean hitMaxIterations;
   private int iterationsReached;
   private double averageErrorReached;
   private Duration timeTaken;

/*
 * Parses the selected control file and initializes network dimensions,
 * hyperparameters, filenames, and runtime flags.
 *
 * If no control-file argument is provided, DEFAULT_CONTROL_FILE is used.
 * If more than one command-line argument is provided, an exception is thrown
 * because the control file name is the only supported command-line option.
 * This method must be called before allocateMemory().
 *
 * The hiddenSizes array is treated as 1-indexed by hidden-layer number, with
 * hiddenSizes[0] unused.
 *
 * Throws IllegalArgumentException if the hidden-layer count does not match
 * numActivationLayers.
 */
   public void setConfigParams(String[] controlFile)
   {
      if (controlFile == null || controlFile.length == 0)
      {
         controlFile = new String[]{DEFAULT_CONTROL_FILE};
      }
      else if (controlFile.length > 1)
      {
         throw new IllegalArgumentException(
            "Expected zero or one command-line argument for the control file name, received " + controlFile.length + ".");
      }
      this.controlFileName = controlFile[0];
      Parser parser = new Parser();
      parser.parseControlFile(controlFileName);

      this.inputSize = parser.findVal("inputSize", Integer.class);
      this.hiddenSizes = parser.findVal("hiddenSizes", int[].class);
      this.outputSize = parser.findVal("outputSize", Integer.class);
      this.randLowerBound = parser.findVal("randLowerBound", Double.class);
      this.randUpperBound = parser.findVal("randUpperBound", Double.class);
      this.learningRate = parser.findVal("learningRate", Double.class);
      this.maxIterations = parser.findVal("maxIterations", Integer.class);
      this.errorThreshold = parser.findVal("errorThreshold", Double.class);
      this.loadFileName = parser.findVal("loadFileName", String.class);
      this.saveFileName = parser.findVal("saveFileName", String.class);
      this.trainingDataFileName = parser.findVal("trainingDataFileName", String.class);

      this.numActivationLayers = parser.findVal("numActivationLayers", Integer.class);
      this.numConnectivityLayers = numActivationLayers - 1;
      this.lastHiddenLayer = numActivationLayers - 2;
      this.outputLayer = numActivationLayers - 1;

      if (hiddenSizes.length - 1 != numActivationLayers - 2)
      {
         throw new IllegalArgumentException("Number of hidden sizes provided does not match expected number of hidden layers. " +
            "Expected: " + (numActivationLayers - 2) + ", Provided: " + (hiddenSizes.length - 1));
      }

      this.training = parser.findVal("training", Boolean.class);
      this.weightInputChoice = parser.findVal("weightInputChoice", String.class);
      this.runningTestCases = parser.findVal("runningTestCases", Boolean.class);
      this.saveWeights = parser.findVal("saveWeights", Boolean.class);
      this.printInputs = parser.findVal("printInputs", Boolean.class);
      this.keepAlive = 0;

      if (training)
      {
         this.keepAlive = parser.findVal("keepAlive", Integer.class);
      }

      this.numTestCases = parser.findVal("numTestCases", Integer.class);

      if (training)
      {
         this.truthTableFileName = parser.findVal("truthTableFileName", String.class);
      }
   } // public void setConfigParams(String[] controlFile)

/*
 * Prints the loaded network topology and the main runtime settings.
 *
 * Training-only parameters are shown only when training is enabled, and the
 * weights filename is shown only when weights are being loaded from disk.
 *
 * Because hiddenSizes is 1-indexed, the printed hidden-layer sizes come from
 * hiddenSizes[FIRST_HIDDEN_LAYER] through hiddenSizes[lastHiddenLayer]. When
 * training is enabled, the keepAlive setting is echoed as part of the
 * user-visible run configuration.
 */
   public void echoConfigParams()
   {
      System.out.println("\nReading configuration parameters from: " + controlFileName);
      System.out.println("\n=====Network Configuration Parameters=====");
      System.out.print("Network configuration: " + inputSize + "-");
      for (int n = FIRST_HIDDEN_LAYER; n <= lastHiddenLayer; n++)
      {
         System.out.print(hiddenSizes[n] + "-");
      }
      System.out.println(outputSize);

      if (training)
      {
         System.out.println("randLowerBound: " + randLowerBound);
         System.out.println("randUpperBound: " + randUpperBound);
         System.out.println("learningRate: " + learningRate);
         System.out.println("maxIterations: " + maxIterations);
         System.out.println("errorThreshold: " + errorThreshold);
         System.out.println("keepAlive: " + keepAlive);
         System.out.println("Reading training data from: " + trainingDataFileName);
         System.out.println("Reading truth table from: " + truthTableFileName);
      }

      System.out.println("printInputs: " + printInputs);

      if (weightInputChoice.equalsIgnoreCase("load"))
      {
         System.out.println("Reading weights from: " + loadFileName);
      }
   } // public void echoConfigParams()

/*
 * Allocates memory for all network arrays based on the loaded configuration.
 *
 * setConfigParams() must be called first.
 *
 * The hidden-layer activations and hidden-layer theta arrays are allocated
 * using the 1-indexed hiddenSizes convention, so hiddenSizes[n] corresponds to
 * hidden layer n.
 *
 * Allocates:
 *  - One activation array per network layer
 *  - One weight matrix per connectivity layer
 *  - Input data storage when training or running test cases
 *  - Training helpers such as theta, psi, and truthTable when training
 */
   public void allocateMemory()
   {
      a = new double[numActivationLayers][];    //
      a[INPUT_LAYER] = new double[inputSize];
      for (int n = FIRST_HIDDEN_LAYER; n <= lastHiddenLayer; n++)
      {
         a[n] = new double[hiddenSizes[n]];
      }
      a[outputLayer] = new double[outputSize];

      w = new double[numConnectivityLayers][][];
      for (int n = 0; n < numConnectivityLayers; n++)
      {
         w[n] = new double[a[n].length][a[n + 1].length];
      }

      if (training || runningTestCases)
      {
         trainingData = new double[numTestCases][inputSize];
      }

      if (training)
      {
         theta = new double[numActivationLayers - 1][];
         for (int n = FIRST_HIDDEN_LAYER; n <= lastHiddenLayer; n++)
         {
            theta[n] = new double[hiddenSizes[n]];
         }

         psi = new double[numActivationLayers][];
         for (int n = outputLayer; n > FIRST_HIDDEN_LAYER; n--)
         {
            psi[n] = new double[a[n].length];
         }

         truthTable = new double[numTestCases][outputSize];
      }
   } // public void allocateMemory()

/*
 * Loads the configured binary datasets and initializes the weights.
 *
 * Training inputs are loaded whenever training or test-case execution is
 * enabled. Truth-table outputs are loaded only when training is enabled.
 * This method must be called after allocateMemory().
 */
   public void populateArrays()
   {
      if (training || runningTestCases) // Populate input data for both training and test cases
      {                                 // since they use the same input vectors
         setTrainingData();
      }

      if (training)
      {
         setTruthTable();
      }

      setWeights();
   } // public void populateArrays()

/*
 * Initializes weights according to weightInputChoice.
 *
 * Supported choices:
 *  - random: fills all weights with random values
 *  - manual: uses the hardcoded values in setManualWeights()
 *  - load: reads weights from loadFileName
 *
 * Invalid choices default to random initialization.
 */
   private void setWeights()
   {
      if (weightInputChoice.equalsIgnoreCase("random"))
      {
         setRandomWeights();
      }
      else if (weightInputChoice.equalsIgnoreCase("manual"))
      {
         setManualWeights();
      }
      else if (weightInputChoice.equalsIgnoreCase("load"))
      {
         loadWeights();
      }
      else
      {
         System.out.println("Invalid weightInputChoice: " + weightInputChoice);
         System.out.println("Defaulting to random weight initialization.");
         setRandomWeights();
      }
   } // private void setWeights()

/*
 * Initializes every weight matrix with random values in [randLowerBound, randUpperBound].
 */
   private void setRandomWeights()
   {
      for (int n = 0; n < numConnectivityLayers; n++)
      {
         for (int j = 0; j < a[n].length; j++)
         {
            for (int k = 0; k < a[n + 1].length; k++)
            {
               w[n][j][k] = randomize(randLowerBound, randUpperBound);
            }
         }
      }
   } // private void setRandomWeights()

/*
 * Returns a uniformly distributed random double in [low, high].
 */
   private double randomize(double low, double high)
   {
      return (Math.random() * (high - low) + low);
   } // private double randomize(double low, double high)

/*
 * Initializes selected weights to hardcoded values for manual experiments.
 *
 * These assignments are only valid for the specific small architecture they
 * were written for, so weightInputChoice should be set to "manual" only when
 * the configured layer sizes match those assumptions.
 */
   private void setManualWeights()
   {
      w[0][0][0] = 0.3;
      w[0][1][0] = 0.45;
      w[1][0][0] = 0.05;
      w[1][1][0] = 0.6;

      w[2][0][0] = 0.29851;
      w[2][1][0] = 0.55;
   } // private void setManualWeights()

/*
 * Loads network weights from a binary file.
 *
 * The method reads the network configuration and weight values from the
 * file specified by loadFileName. Before loading the weights, it verifies
 * that the layer sizes stored in the file match the current network.
 *
 * The file is expected to contain data in the following order:
 *
 *   1. inputSize
 *   2. Each hidden layer size, in order
 *   3. outputSize
 *   4. All connectivity-layer weights in order from input to output
 *
 * If the stored network dimensions do not match the current configuration,
 * the method throws an exception to prevent loading incompatible weights.
 *
 * This method overwrites the current values stored in w.
 *
 * Throws:
 *   - IllegalStateException if the file format or network configuration
 *     does not match the current network.
 *   - RuntimeException if an I/O error occurs while reading the file.
 */
   private void loadWeights()
   {
      try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(loadFileName))))
      {
         int fileInputSize = in.readInt();
         int[] fileHiddenSizes = new int[hiddenSizes.length];
         for (int n = FIRST_HIDDEN_LAYER; n <= lastHiddenLayer; n++)
         {
            fileHiddenSizes[n] = in.readInt();
         }
         int fileOutputSize = in.readInt();

         if (!weightsMatch(fileInputSize, fileHiddenSizes, fileOutputSize))
         {
            String fileConfig = fileInputSize + "-";
            String currentConfig = inputSize + "-";

            for (int n = FIRST_HIDDEN_LAYER; n <= lastHiddenLayer; n++)
            {
               fileConfig += (fileHiddenSizes[n]) + "-";
               currentConfig += (hiddenSizes[n]) + "-";
            }

            fileConfig += fileOutputSize;
            currentConfig += outputSize;

            throw new IllegalStateException(
               "Weights file network configuration does not match current configuration. " +
               "File: " + fileConfig +
               ", Current: " + currentConfig
            );
         }

         for (int n = 0; n < numConnectivityLayers; n++)
         {
            for (int j = 0; j < w[n].length; j++)
            {
               for (int k = 0; k < w[n][j].length; k++)
               {
                  w[n][j][k] = in.readDouble();
               }
            }
         }

         if (in.available() != 0)
         {
            throw new IllegalStateException("Weights file contains extra data after all expected weights were loaded.");
         }
      } // try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(loadFileName))))
      catch (IOException e)
      {
         throw new RuntimeException("Failed to load weights from file: " + loadFileName, e);
      }
   } // private void loadWeights()

/*
 * Returns true when the layer sizes stored in a weights file match the
 * current network configuration.
 */
   private boolean weightsMatch(int fileInputSize, int[] fileHiddenSizes, int fileOutputSize)
   {
      if (fileInputSize != inputSize || fileOutputSize != outputSize
         || fileHiddenSizes.length != hiddenSizes.length)
      {
         return false;
      }

      for (int n = FIRST_HIDDEN_LAYER; n <= lastHiddenLayer; n++)
      {
         if (fileHiddenSizes[n] != hiddenSizes[n])
         {
            return false;
         }
      }

      return true;
   } // private boolean weightsMatch(int fileInputSize, int[] fileHiddenSizes, int fileOutputSize)

/*
 * Saves the current network weights to a binary file.
 *
 * The file contains both the network configuration and the weights so that
 * the file can be validated when loading. This prevents loading weights later
 * on that do not match the current network structure.
 *
 * The following information is written to the file in this exact order:
 *
 *   1. inputSize
 *   2. Each hidden layer size, in order
 *   3. outputSize
 *   4. All connectivity-layer weights in order from input to output
 *
 * The method does not modify the network state. It simply serializes the
 * current topology and weight values into a binary file specified by
 * saveFileName.
 *
 * Throws a RuntimeException if an I/O error occurs while writing the file.
 */
   public void saveWeights()
   {
      try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(saveFileName))))
      {
         out.writeInt(inputSize);
         for (int n = FIRST_HIDDEN_LAYER; n <= lastHiddenLayer; n++)
         {
            out.writeInt(hiddenSizes[n]);
         }
         out.writeInt(outputSize);

         for (int n = 0; n < numConnectivityLayers; n++)
         {
            for (int j = 0; j < a[n].length; j++)
            {
               for (int k = 0; k < a[n + 1].length; k++)
               {
                  out.writeDouble(w[n][j][k]);
               }
            }
         }

         System.out.println("Saved weights to: " + saveFileName);
      } // try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(saveFileName))))
      catch (IOException e)
      {
         throw new RuntimeException("Failed to save weights to file: " + saveFileName, e);
      }
   } // public void saveWeights()

/*
 * Loads input cases from a binary file into the trainingData array.
 *
 * The method reads metadata and data values from the file specified by
 * trainingDataFileName. The metadata is used to verify that the file
 * configuration matches the current network configuration before loading
 * the data.
 *
 * The file is expected to be in the following format:
 *
 *   1. numTestCases (number of input cases)
 *   2. inputSize (number of input features per example)
 *   3. All input values in row-major order`
 *
 * If the metadata in the file does not match the network's current
 * numTestCases or inputSize, an exception is thrown to prevent
 * inconsistent or invalid data from being loaded.
 *
 * The trainingData array is filled such that:
 *   trainingData[t][k] corresponds to the k-th input of test case t.
 *
 * Throws:
 *   - IllegalStateException if file metadata does not match configuration
 *   - RuntimeException if an I/O error occurs while reading the file
 */
   private void setTrainingData()
   {
      try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(trainingDataFileName))))
      {
         int fileNumTestCases = in.readInt();
         int fileInputSize = in.readInt();

         if (fileNumTestCases != numTestCases || fileInputSize != inputSize)
         {
            throw new IllegalStateException(
               "Training data file configuration does not match current configuration. " +
               "File: numTestCases=" + fileNumTestCases + ", inputSize=" + fileInputSize +
               ", Current: numTestCases=" + numTestCases + ", inputSize=" + inputSize
            );
         }

         for (int t = 0; t < numTestCases; t++)
         {
            for (int k = 0; k < inputSize; k++)
            {
               trainingData[t][k] = in.readDouble();
            }
         }
      } // try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(trainingDataFileName))))
      catch (IOException e)
      {
         throw new RuntimeException("Failed to load training data from file: " + trainingDataFileName, e);
      }
   } // private void setTrainingData()

/*
 * Loads the truth table from a binary file into the truthTable array.
 *
 * The method reads metadata and data values from the file specified by
 * truthTableFileName. The metadata is used to verify that the file
 * configuration matches the current network configuration before loading
 * the data.
 *
 * The file is expected to be in the following format:
 *
 *   1. numTestCases (number of expected output rows)
 *   2. outputSize (number of output features per example)
 *   3. All truth table values in row-major order
 *
 * If the metadata in the file does not match the network's current
 * numTestCases or outputSize, an exception is thrown to prevent
 * inconsistent or invalid data from being loaded.
 *
 * The truthTable array is filled such that:
 *   truthTable[t][i] corresponds to the i-th output of test case t.
 *
 * Throws:
 *   - IllegalStateException if file metadata does not match configuration
 *   - RuntimeException if an I/O error occurs while reading the file
 */
   private void setTruthTable()
   {
      try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(truthTableFileName))))
      {
         int fileNumTestCases = in.readInt();
         int fileOutputSize = in.readInt();

         if (fileNumTestCases != numTestCases || fileOutputSize != outputSize)
         {
            throw new IllegalStateException(
               "Truth table file configuration does not match current configuration. " +
               "File: numTestCases=" + fileNumTestCases + ", outputSize=" + fileOutputSize +
               ", Current: numTestCases=" + numTestCases + ", outputSize=" + outputSize
            );
         }

         for (int t = 0; t < numTestCases; t++)
         {
            for (int i = 0; i < outputSize; i++)
            {
               truthTable[t][i] = in.readDouble();
            }
         }
      } // try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(truthTableFileName))))
      catch (IOException e)
      {
         throw new RuntimeException("Failed to load truth table from file: " + truthTableFileName, e);
      }
   } // private void setTruthTable()


/*
 * Returns the activation function used by the network.
 *
 * The current implementation uses sigmoid(). The tanh() helper remains
 * available if the project is later switched to a different activation.
 */
   private double activationFunction(double x)
   {
      return sigmoid(x);
   } // private double activationFunction(double x)

/*
 * Returns the derivative of the currently selected activation function.
 */
   private double activationDerivative(double x)
   {
      return sigmoidDerivative(x);
   } // private double activationDerivative(double x)

/*
 * Standard sigmoid function.
 *
 * f(x) = 1 / (1 + e^-x)
 */
   private double sigmoid(double x)
   {
      return 1.0 / (1.0 + Math.exp(-x));
   } // private double sigmoid(double x)

/*
 * Derivative of the sigmoid function.
 *
 * f'(x) = f(x) * (1 - f(x))
 * where f(x) is the sigmoid function applied to x.
 */
   private double sigmoidDerivative(double x)
   {
      double s = sigmoid(x);
      return s * (1.0 - s);
   } // private double sigmoidDerivative(double x)

/*
 * Standard hyperbolic tangent function.
 *
 * f(x) = (e^x - e^(-x)) / (e^x + e^(-x))
 */
   private double tanh(double x)
   {
      double epsilon = (x < 0.0) ? 1.0 : -1.0;
      double exponential = Math.exp(epsilon * 2.0 * x);
      return epsilon * (exponential - 1.0) / (exponential + 1.0);
   } // private double tanh(double x)

/*
 * Derivative of the hyperbolic tangent function.
 * 
 * f'(x) = 1 - (f(x))^2
 * where f(x) is the hyperbolic tangent function applied to x.
 */
   private double tanhDerivative(double x)
   {
      double t = tanh(x);
      return 1.0 - t * t;
   } // private double tanhDerivative(double x)

/*
 * Trains the network with repeated forward passes and backpropagation updates.
 *
 * Stops when:
 *   - average error <= errorThreshold
 *            OR
 *   - maxIterations reached
 *
 * Each iteration processes every configured training case once and applies a
 * weight update immediately after that case's training forward pass. If
 * keepAlive is nonzero, a progress message is printed after each completed
 * multiple of that many iterations.
 *
 */
   public void train()
   {
      hitErrorThreshold = false;
      hitMaxIterations = false;
      iterationsReached = 0;
      averageErrorReached = Double.POSITIVE_INFINITY;

      while (iterationsReached < maxIterations && averageErrorReached > errorThreshold)
      {
         double totalTwiceError = 0.0;
         for (int t = 0; t < numTestCases; t++) // Loop through each training case
         {
            setActivations(trainingData[t]);
            totalTwiceError += runForTraining(t);
            updateWeights();
         } // for (int t = 0; t < numTestCases; t++)
         averageErrorReached = totalTwiceError / ((double) numTestCases * 2.0);

         iterationsReached++;

         if (keepAlive != 0 && iterationsReached % keepAlive == 0)
         {
            if (iterationsReached == keepAlive)
            {
               System.out.println();
            }
            System.out.printf("Iteration %d, Error = %f\n", iterationsReached, averageErrorReached);
         }
      } // while (iterationsReached < maxIterations && averageErrorReached > errorThreshold)

      if (averageErrorReached <= errorThreshold)
      {
         hitErrorThreshold = true;
      }

      if (iterationsReached >= maxIterations)
      {
         hitMaxIterations = true;
      }
   } // public void train()

/*
 * Performs a forward pass for one training case and computes output-layer
 * error terms.
 *
 * This method assumes that a[INPUT_LAYER] already stores the current input
 * vector. It fills all hidden-layer activations, all hidden-layer theta
 * values, the output activations, and psi[outputLayer].
 *
 * The returned value is the sum of squared output errors for the selected
 * training case before the outer training loop divides by 2.0.
 */
   public double runForTraining(int testCase)
   {
      for (int n = FIRST_HIDDEN_LAYER; n <= lastHiddenLayer; n++)
      {
         for (int k = 0; k < a[n].length; k++)
         {
            theta[n][k] = 0.0;
            for (int j = 0; j < a[n - 1].length; j++)
            {
               theta[n][k] += a[n - 1][j] * w[n - 1][j][k];
            }
            a[n][k] = activationFunction(theta[n][k]);
         }
      }

      double twiceError = 0.0;
      for (int k = 0; k < outputSize; k++) // Compute output layer activations
      {
         double outputTheta = 0.0;
         for (int j = 0; j < a[lastHiddenLayer].length; j++)
         {
            outputTheta += a[lastHiddenLayer][j] * w[lastHiddenLayer][j][k];
         }
         a[outputLayer][k] = activationFunction(outputTheta);

         double omega_k = truthTable[testCase][k] - a[outputLayer][k];
         twiceError += omega_k * omega_k;
         psi[outputLayer][k] = omega_k * activationDerivative(outputTheta);
      } // for (int k = 0; k < outputSize; k++)
      return twiceError;
   } // public double runForTraining(int testCase)

/*
 * Computes and applies backpropagation weight updates for one training example.
 *
 * This method assumes that runForTraining() has already been executed for the
 * current training example, meaning:
 *
 *   - a contains all layer activations
 *   - theta contains hidden-layer weighted sums
 *   - psi[outputLayer] contains output-layer error terms
 *
 * The method backpropagates error information from the output layer toward the
 * input layer and updates the weight matrices in w in place.
 */
   private void updateWeights()
   {
      for (int n = lastHiddenLayer; n > FIRST_HIDDEN_LAYER; n--)
      {
         for (int j = 0; j < a[n].length; j++)
         {
            double omega_j = 0.0;
            for (int k = 0; k < a[n + 1].length; k++)
            {
               omega_j += psi[n + 1][k] * w[n][j][k];
               w[n][j][k] += learningRate * a[n][j] * psi[n + 1][k];
            }

            psi[n][j] = omega_j * activationDerivative(theta[n][j]);
         } // for (int j = 0; j < a[n].length; j++)
      } // for (int n = lastHiddenLayer; n > FIRST_HIDDEN_LAYER; n--)

      for (int j = 0; j < a[FIRST_HIDDEN_LAYER].length; j++)
      {
         double omega_j = 0.0;
         for (int k = 0; k < a[FIRST_HIDDEN_LAYER + 1].length; k++)
         {
            omega_j += psi[FIRST_HIDDEN_LAYER + 1][k] * w[FIRST_HIDDEN_LAYER][j][k];
            w[FIRST_HIDDEN_LAYER][j][k] += learningRate * a[FIRST_HIDDEN_LAYER][j] * psi[FIRST_HIDDEN_LAYER + 1][k];
         }

         double inputPsi = omega_j * activationDerivative(theta[FIRST_HIDDEN_LAYER][j]);
         for (int i = 0; i < inputSize; i++)
         {
            w[INPUT_LAYER][i][j] += learningRate * a[INPUT_LAYER][i] * inputPsi;
         }
      } // for (int j = 0; j < a[FIRST_HIDDEN_LAYER].length; j++)
   } // private void updateWeights()

/*
 * Performs a forward pass through the network to compute output activations.
 *
 * This method assumes that a[INPUT_LAYER] already stores the current input
 * vector. It fills every later activation layer through a[outputLayer].
 *
 * The method does not compute theta or psi because those values are needed only
 * during training.
 */
   public void run()
   {
      for (int n = FIRST_HIDDEN_LAYER; n <= outputLayer; n++)
      {
         for (int k = 0; k < a[n].length; k++)
         {
            double runTheta = 0.0;
            for (int j = 0; j < a[n - 1].length; j++)
            {
               runTheta += a[n - 1][j] * w[n - 1][j][k];
            }
            a[n][k] = activationFunction(runTheta);
         }
      } // for (int n = FIRST_HIDDEN_LAYER; n <= outputLayer; n++)
   } // public void run()

/*
 * Prints the training exit condition and summary statistics, including:
 *  - Whether the error threshold was reached
 *  - Whether the maximum iterations were reached
 *  - The number of iterations executed
 *  - The average error reached at the end of training
 *
 * This method should be called after the training loop completes to provide a
 * summary of the training outcome.
 *
 */
   public void reportTrainingResults()
   {
      if (!training) return;

      System.out.println("\n=====Training Exit Information=====");
      System.out.println("Reason for end of training:");
      System.out.println("Reached error threshold: " + hitErrorThreshold);
      System.out.println("Reached max iterations: " + hitMaxIterations);
      System.out.println("Iterations reached: " + iterationsReached);
      System.out.printf("Average Error reached: %.4f\n", averageErrorReached);
      System.out.println("Time taken: " + timeTaken.toMillis() + " ms");
   } // public void reportTrainingResults()

/*
 * Copies an input vector into the network's input activation layer.
 *
 * This method is called before training and testing forward passes. The input
 * array is expected to contain inputSize values in the same order used by the
 * configured training and test data files.
 */
   private void setActivations(double[] input)
   {
      for (int k = 0; k < inputSize; k++)
      {
         a[INPUT_LAYER][k] = input[k];
      }
   } // private void setActivations(double[] input)

/*
 * Runs the network on every input case stored in trainingData.
 *
 * This method assumes that trainingData has already been loaded and that the
 * weights have already been initialized.
 *
 * For each test case, the method calls run() to compute a[OUTPUT_LAYER] and
 * copies those values into an outputs array.
 * The method then returns the outputs array.
 */
   public double[][] runTestCases()
   {
      double[][] outputs = new double[numTestCases][outputSize];

      for (int t = 0; t < numTestCases; t++) // Run each test case and store the output activations
      {
         setActivations(trainingData[t]);
         run();
         for (int i = 0; i < outputSize; i++)
         {
            outputs[t][i] = a[outputLayer][i];
         }
      }

      return outputs;
   } // public double[][] runTestCases()

/*
 * Prints the input table, truth table (if available), and actual outputs for the test cases.
 *
 * This method should be called after runTestCases() to provide a detailed report of the
 * test case results. It displays:
 *  - The input vectors used for each test case
 *  - The corresponding truth values (if training was enabled)
 *  - The actual output activations computed by the network
 *
 * Parameter:
 *   outputs - a 2D array containing the output activations for each test case
 */
   public void reportRunResults(double[][] outputs)
   {
      if (printInputs)
      {
         System.out.println("\n=====Input Table=====");
         for (int t = 0; t < numTestCases; t++) // Print input vectors for each test case
         {
            System.out.print(t + ": ");
            for (int k = 0; k < inputSize; k++)
            {
               System.out.print(trainingData[t][k]);
               if (k < inputSize - 1) System.out.print(" ");
            }
            System.out.println();
         }
      }

      if (truthTable != null) // Print truth table values if available (i.e., if training was enabled)
      {
         System.out.println("\n=====Truth Table=====");
         for (int t = 0; t < numTestCases; t++)
         {
            System.out.print(t + ": ");
            for (int i = 0; i < outputSize; i++)
            {
               System.out.print(truthTable[t][i]);
               if (i < outputSize - 1) System.out.print(" ");
            }
            System.out.println();
         }
      } // if (truthTable != null)

      System.out.println("\n=====Actual Outputs=====");
      for (int t = 0; t < outputs.length; t++) // Print the actual output activations for each test case
      {
         System.out.print(t + ": ");
         for (int i = 0; i < outputSize; i++)
         {
            System.out.printf("%.4f", outputs[t][i]);
            if (i < outputSize - 1) System.out.print(" ");
         }
         System.out.println();
      }

      System.out.println("\n=====Time Taken=====");
      System.out.println(timeTaken.toMillis() + " ms");
   } // public void reportRunResults(double[][] outputs)

/*
 * Program entry point.
 *
 * The method loads the control file, allocates memory, loads data, initializes
 * weights, optionally trains the network, optionally saves weights, and
 * optionally runs and reports test cases.
 *
 * args[0], when present, is treated as the control-file name. If no argument
 * is provided, DEFAULT_CONTROL_FILE is used instead.
 */
   public static void main(String[] args)
   {
      Network net = new Network();
      net.setConfigParams(args);
      net.echoConfigParams();
      net.allocateMemory();
      net.populateArrays();

      if (net.training)
      {
         Instant startTime = Instant.now();
         net.train();
         Instant endTime = Instant.now();
         net.timeTaken = Duration.between(startTime, endTime);
         net.reportTrainingResults();
      }

      if (net.saveWeights)
      {
         net.saveWeights();
      }

      if (net.runningTestCases)
      {
         Instant startTime = Instant.now();
         double [][] outputs = net.runTestCases();
         Instant endTime = Instant.now();
         net.timeTaken = Duration.between(startTime, endTime);
         net.reportRunResults(outputs);
      }
   } // public static void main(String[] args)
} // public class Network
