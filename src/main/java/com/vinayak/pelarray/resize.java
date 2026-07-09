package com.vinayak.pelarray;

/*
 * Eric R. Nelson
 * 03/16/2026
 * 
 * Resize a simple image byte file (one byte per pel)
 *
 * javac resize.java PelArray.java
 *
 * usage: java resize width height inputfile width height outputfile
 *
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class resize
   {
   public static void main(String[] args)
      {
      if (args.length != 6) // must have six command line arguments (see usage above)
         {
         System.out.printf("usage: java resize width height inputfile width height outputfile\n");
         }
      else
         {
         int infileWidth  = Integer.parseInt(args[0]);
         int infileHeight = Integer.parseInt(args[1]);

         String inputFileName = args[2];

         int outfileWidth  = Integer.parseInt(args[3]);
         int outfileHeight = Integer.parseInt(args[4]);

         String outputFileName = args[5];


         int[][] image = new int[infileHeight][infileWidth];

         try // lots of things can go wrong when doing file i/o
            {
            FileInputStream fistream = new FileInputStream(inputFileName);

            DataInputStream in = new DataInputStream(fistream); // Convert our input stream to a DataInputStream

            for (int i = 0; i < infileHeight; ++i)
               for (int j = 0; j < infileWidth; ++j)
                  image[i][j] = in.readUnsignedByte();

            in.close();
            fistream.close();

            PelArray pixels = new PelArray(image);

            PelArray smallPelArray = pixels.scale(outfileWidth, outfileHeight);
            int[][] smallImage = smallPelArray.getPelArray();

            try
               {
               FileOutputStream fostream = new FileOutputStream(outputFileName);

               DataOutputStream out = new DataOutputStream(fostream); // Convert our output stream to a DataOutputStream

               for (int i = 0; i < outfileHeight; ++i)
                  for (int j = 0; j < outfileWidth; ++j)
                     out.writeByte(smallImage[i][j]);

               out.close();
               fostream.close();
               } // try
            catch (Exception e)
               {
               System.err.println("File output error" + e);
               }
            } // try
         catch (Exception e)
            {
            System.err.println("File input error" + e);
            }
         } // if (agrs.length != 6) ... else
      } // public static void main(String[] args)
   } // public class resize
