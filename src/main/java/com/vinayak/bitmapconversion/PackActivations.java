package com.vinayak.bitmapconversion;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Packs a directory of activation files into one network training-data file.
 *
 * Output format:
 *   1. numTestCases as int
 *   2. inputSize as int
 *   3. all activation values in row-major order as doubles
 */
public class PackActivations
   {
   public static void main(String[] args)
      {
      if (args.length != 2)
         {
         System.out.printf("Usage: java PackActivations input_activation_directory output_training_data_file%n%n");
         return;
         }

      Path inputDir = Paths.get(args[0]);
      Path outputFile = Paths.get(args[1]);

      try
         {
         List<Path> activationFiles = getActivationFiles(inputDir);
         if (activationFiles.isEmpty())
            {
            throw new IllegalArgumentException("No activation files found in " + inputDir);
            }

         int inputSize = (int) (Files.size(activationFiles.get(0)) / Double.BYTES);

         try (DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(outputFile.toFile()))))
            {
            out.writeInt(activationFiles.size());
            out.writeInt(inputSize);

            for (Path activationFile : activationFiles)
               {
               int currentInputSize = (int) (Files.size(activationFile) / Double.BYTES);
               if (currentInputSize != inputSize)
                  {
                  throw new IllegalStateException(
                     "Activation file size mismatch for " + activationFile.getFileName() +
                     ". Expected " + inputSize + " doubles but found " + currentInputSize + ".");
                  }

               try (DataInputStream in = new DataInputStream(
                  new BufferedInputStream(new FileInputStream(activationFile.toFile()))))
                  {
                  for (int i = 0; i < inputSize; i++)
                     {
                     out.writeDouble(in.readDouble());
                     }
                  }
               }
            }

         System.out.printf(
            "Packed %d activation files from '%s' into '%s' with inputSize=%d.%n",
            activationFiles.size(), inputDir, outputFile, inputSize);
         }
      catch (Exception e)
         {
         System.err.println("Activation packing error " + e);
         }
      }

   private static List<Path> getActivationFiles(Path inputDir) throws IOException
      {
      List<Path> activationFiles = new ArrayList<>();

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir, "*.act"))
         {
         for (Path path : stream)
            {
            activationFiles.add(path);
            }
         }

      Collections.sort(activationFiles);
      return activationFiles;
      }
   }
