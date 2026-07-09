package com.vinayak.bitmapconversion;

/**
**
** Bytes2Activations.java
**
** @author Eric R. Nelson
** April 27, 2026
**
** This code reads in a binary file of byte values (0 to 255) and converts them into an activation file (0.0 to 1.0)
** This code is needed since the C and Java double precision formats are not the same
**
** Search for $$$ to change the data scaling by the program.
**
** javac Bytes2Activations.java
**
** Usage:
** java Bytes2Activations input_byte_file output_activation_file
**
** If two arguments are not passed, then a usage message is presented to the user.
**
**/
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Bytes2Activations
   {
   static final double SCALE  = 255.0; // $$$  double value = byte * SCALE + OFFSET
   static final double OFFSET = 0.0;   // $$$ 

   public static void main(String[] args)
      {
      int byteVal;
      double activationVal;
/*
** args[0] - input bin file
** args[1] - output activation file
**
** The default size for the student photos in IC is 175x232 (width, height)
**
*/
      if (args.length != 2)
         System.out.printf("Usage: java Bytes2Activations input_byte_file output_activation_file\n\n");
      else
         {
         String inFileName  = args[0];
         String outFileName = args[1];

         System.out.printf("Reading file '%s' and writing out '%s'.\n", inFileName, outFileName);

         try // lots of things can go wrong when doing file i/o
            {
            FileInputStream fInStream = new FileInputStream(inFileName); // Open the file that is the second command line parameter
            DataInputStream in = new DataInputStream(fInStream);         // Convert our input stream to a DataInputStream

            try
               {
               FileOutputStream fOutStream = new FileOutputStream(outFileName);
               DataOutputStream out = new DataOutputStream(fOutStream);
/*
** Read in 1 byte values and then write them out as scaled double precision
*/
               boolean EOF = false;
               while (!EOF)
                  {
                  try // an exception is thrown when end of file is reached
                     {
                     byteVal  = in.readUnsignedByte();

                     activationVal = (double)byteVal / SCALE + OFFSET;

                     out.writeDouble(activationVal);
                     } // try
                  catch (Exception e)
                     {
                     EOF = true;
                     }
                  } // while (!EOF)

               out.close();
               fOutStream.close();
               } // try
            catch (Exception e)
               {
               System.err.println("File output error" + e);
               }

            in.close();
            fInStream.close();
            } // try
         catch (Exception e)
            {
            System.err.println("File input error" + e);
            }
         } // if (args.length != 2) ... else
      } // public public void main
   } // public class Bytes2Activations
