package com.vinayak.bitmapconversion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Converts activation values in the range 0.0 to 1.0 back into single-byte
 * grayscale values in the range 0 to 255.
 */
public class Activations2Bytes
   {
   private static final double MIN_ACTIVATION = 0.0;
   private static final double MAX_ACTIVATION = 1.0;

   public static void main(String[] args)
      {
      if (args.length != 2)
         {
         System.out.printf("Usage: java Activations2Bytes input_activation_file output_byte_file%n%n");
         return;
         }

      String inFileName = args[0];
      String outFileName = args[1];

      System.out.printf("Reading activation file '%s' and writing out '%s'.%n", inFileName, outFileName);

      try (DataInputStream in = new DataInputStream(new FileInputStream(inFileName));
           DataOutputStream out = new DataOutputStream(new FileOutputStream(outFileName)))
         {
         while (true)
            {
            try
               {
               double activation = in.readDouble();
               int byteValue = activationToByte(activation);
               out.writeByte(byteValue);
               }
            catch (EOFException eof)
               {
               break;
               }
            }
         }
      catch (Exception e)
         {
         System.err.println("File conversion error " + e);
         }
      }

   private static int activationToByte(double activation)
      {
      double normalized = (activation - MIN_ACTIVATION) / (MAX_ACTIVATION - MIN_ACTIVATION);
      normalized = Math.max(0.0, Math.min(1.0, normalized));
      return (int) Math.round(normalized * 255.0);
      }
   }
