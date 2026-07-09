package com.vinayak;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/*
 * Author: Vinayak Sinha
 * Course: ATCS Neural Networks
 * Date: 04/29/2026
 *
 * Description:
 * Utility class for reading control-file parameters and writing binary matrix
 * data.
 *
 * Parser stores control-file entries as String key-value pairs, then exposes a
 * typed lookup method so Network can retrieve configuration values as integers,
 * doubles, booleans, strings, or int arrays.
 *
 * For hiddenSizes, Parser returns a 1-indexed int array: index 0 is left
 * unused, index 1 stores the first hidden-layer size, index 2 stores the
 * second hidden-layer size, and so on. This matches the layer-numbering
 * convention used throughout Network.
 *
 * In addition, Parser can write 2D double arrays to binary files using the
 * same simple row-major format that Network expects when loading training
 * inputs and truth tables.
 *
 * Method Table of Contents:
 *    - public void parseControlFile(String controlFileName)
 *    - public <T> T findVal(String key, Class<T> type)
 *    - public void writeBinary(String filename, double[][] data)
 */
public class Parser
{
/*
 * Raw control-file values indexed by parameter name.
 */
   private HashMap<String, String> configVariables;

/*
 * Parses the given control file and stores each key-value pair in
 * configVariables.
 *
 * Expected format:
 *
 * inputSize=2
 * hiddenSizes=3 4
 * outputSize=1
 * numActivationLayers=4
 * randLowerBound=-1.0
 * randUpperBound=1.0
 * learningRate=0.3
 * maxIterations=100000
 * errorThreshold=0.0002
 * keepAlive=1000
 * training=true
 * runningTestCases=true
 * saveWeights=true
 * weightInputChoice=random
 * trainingDataFileName=inputs.bin
 * truthTableFileName=truth.bin
 * loadFileName=weights.bin
 * saveFileName=weights.bin
 * numTestCases=4
 *
 * In the example above, hiddenSizes=3 4 becomes an internal int[] where
 * hiddenSizes[0] is unused, hiddenSizes[1] == 3, and hiddenSizes[2] == 4.
 * The same value could also be written as hiddenSizes=3,4.
 *
 * keepAlive is the number of completed training iterations between progress
 * messages. A value of 0 disables keep-alive output.
 *
 * Blank lines and lines beginning with # are ignored. Any nonblank line must
 * contain one '=' character separating the parameter name from its value.
 */
   public void parseControlFile(String controlFileName)
   {
      configVariables = new HashMap<>();
      try (BufferedReader reader = new BufferedReader(new FileReader(controlFileName)))
      {
         String line;

         while ((line = reader.readLine()) != null)
         {
            line = line.trim();

            if (line.isEmpty() || line.startsWith("#"))
            {
               continue;
            }

            int equalsIndex = line.indexOf('=');
            if (equalsIndex < 0)
            {
               throw new IllegalArgumentException("Invalid control file line (missing '='): " + line);
            }

            String key = line.substring(0, equalsIndex).trim();
            String value = line.substring(equalsIndex + 1).trim();

            configVariables.put(key, value);
        } // while ((line = reader.readLine()) != null)
      } // try (BufferedReader reader = new BufferedReader(new FileReader(controlFileName)))
      catch (IOException e)
      {
         throw new RuntimeException(
            "Failed to read control file: " + controlFileName, e);
      }
   } // public void parseControlFile(String controlFileName)

/*
 * Retrieves the value associated with a given key from the configuration
 * and converts it to the specified type.
 *
 * The method looks up the key in the configVariables map and parses the
 * corresponding string value into the requested type. Supported types
 * include Integer, Double, Boolean, String, and int[].
 *
 * If the key does not exist in the configuration, an exception is thrown.
 * If the requested type is not supported, an exception is also thrown.
 *
 * For int[].class, the stored value is treated as a comma-separated or
 * whitespace-separated list of integers such as "3,4,5" or "3 4 5". The
 * returned array is intentionally 1-indexed, so index 0 is unused and the
 * first parsed size is stored at index 1. This means the returned array length
 * is one greater than the number of hidden layers listed in the control file.
 *
 * Parameters:
 *    key  - the name of the configuration variable to retrieve
 *    type - the Class object representing the desired return type
 *
 * Returns:
 *    The value associated with the key, converted to the specified type.
 *
 * Throws:
 *    - RuntimeException if the key is not found in the configuration
 *    - RuntimeException if the requested type is not supported
 */
   public <T> T findVal(String key, Class<T> type)
   {
      String value = configVariables.get(key);

      if (value == null)
      {
         throw new RuntimeException("Key not found: " + key);
      }

      Object result;
      if (type == Integer.class)
      {
         result = Integer.parseInt(value);
      }
      else if (type == Double.class)
      {
         result = Double.parseDouble(value);
      }
      else if (type == Boolean.class)
      {
         result = Boolean.parseBoolean(value);
      }
      else if (type == int[].class)
      {
         String[] parts = value.trim().split("[,\\s]+");
         int[] array = new int[parts.length + 1];
         for (int i = 0; i < parts.length; i++)
         {
            array[i + 1] = Integer.parseInt(parts[i].trim());
         }
         result = array;
      }
      else if (type == String.class)
      {
         result = value;
      }
      else
      {
         throw new RuntimeException("Unsupported type: " + type);
      }

      return type.cast(result);
   } // public <T> T findVal(String key, Class<T> type)

/*
 * Writes a 2D array of data values to a binary file.
 *
 * The method writes both metadata and data values to the file specified
 * by filename. The metadata allows the file to be validated and correctly
 * interpreted when loading the data back into the network.
 *
 * The file is written in the following order:
 *
 *   1. numTestCases (number of rows in the array)
 *   2. size (number of columns in each row)
 *   3. All data values in row-major order
 *
 * This format is compatible with corresponding load methods that expect
 * the same structure.
 *
 * The method does not perform validation on the input array and assumes
 * that the array is non-empty and rectangular (all rows have equal length).
 *
 * Throws:
 *   - RuntimeException if an I/O error occurs while writing the file.
 */
   public void writeBinary(String filename, double[][] data)
   {
      try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename))))
      {
         int numTestCases = data.length;
         int size = data[0].length;

         out.writeInt(numTestCases);
         out.writeInt(size);

         for (int t = 0; t < numTestCases; t++)
         {
            for (int k = 0; k < size; k++)
            {
               out.writeDouble(data[t][k]);
            }
         }
      } // try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename))))
      catch (IOException e)
      {
         throw new RuntimeException("Failed to save binary data to file: " + filename, e);
      }
   } // public void writeBinary(String filename, double[][] data)
} // public class Parser
