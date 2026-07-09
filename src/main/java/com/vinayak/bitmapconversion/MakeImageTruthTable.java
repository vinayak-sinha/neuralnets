package com.vinayak.bitmapconversion;

import com.vinayak.Parser;

/**
 * Writes a one-hot truth table for the 25 training hand images:
 * 1_1..1_5 map to class 1,
 * 2_1..2_5 map to class 2,
 * ...
 * 5_1..5_5 map to class 5.
 */
public class MakeImageTruthTable
   {
   public static void main(String[] args)
      {
      if (args.length != 1)
         {
         System.out.printf("Usage: java MakeImageTruthTable output_truth_file%n%n");
         return;
         }

      double[][] truth = new double[25][5];

      int row = 0;
      for (int label = 0; label < 5; label++)
         {
         for (int sample = 0; sample < 5; sample++)
            {
            truth[row][label] = 1.0;
            row++;
            }
         }

      new Parser().writeBinary(args[0], truth);
      System.out.printf("Wrote 25x5 one-hot truth table to '%s'.%n", args[0]);
      }
   }
