package com.vinayak.bitmapconversion;

import com.vinayak.pelarray.PelArray;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NormalizeOneByteImages
   {
   private static final int CROP_SIZE = 1000;
   private static final int FIVE_FINGER_SOURCE_CROP_SIZE = 1150;
   private static final int COM_THRESHOLD = 20;
   private static final int EDGE_MIN_COUNT = 50;

   public static void main(String[] args)
      {
      Path oneByteDir = Paths.get("onebyteimages");
      Path bmpDir = Paths.get("bmpimages");
      Path outDir = Paths.get("cropped1000");
      Path outBmpDir = outDir.resolve("bmp");

      try
         {
         Files.createDirectories(outDir);
         Files.createDirectories(outBmpDir);
         }
      catch (IOException e)
         {
         System.err.println("Unable to create output directories: " + e.getMessage());
         return;
         }

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(oneByteDir, "*.bin"))
         {
         for (Path binPath : stream)
            {
            String baseName = binPath.getFileName().toString().replaceFirst("\\.bin$", "");
            Path bmpPath = bmpDir.resolve(baseName + ".bmp");
            if (!Files.exists(bmpPath))
               {
               System.err.printf("Skipping %s: matching BMP not found.%n", baseName);
               continue;
               }

            int[] dimensions = readBmpDimensions(bmpPath);
            if (dimensions == null)
               {
               System.err.printf("Skipping %s: cannot read BMP dimensions.%n", baseName);
               continue;
               }

            int width = dimensions[0];
            int height = dimensions[1];
            byte[] pixels = Files.readAllBytes(binPath);
            if (pixels.length != width * height)
               {
               System.err.printf("Skipping %s: expected %dx%d bytes but found %d.%n", baseName, width, height, pixels.length);
               continue;
               }

            int[][] array = new int[height][width];
            for (int i = 0; i < pixels.length; ++i)
               {
               int gray = pixels[i] & 0xFF;
               array[i / width][i % width] = (gray << 16) | (gray << 8) | gray;
               }

            PelArray image = new PelArray(array).onesComplimentImage();
            System.out.printf("Inverting %s to place the hand on a dark background.%n", baseName);

            PelArray analysisImage = image.forceMin(COM_THRESHOLD, PelArray.BLACK);
            int comX = analysisImage.getXcom();
            int comY = analysisImage.getYcom();

            // Use the center of mass as the primary crop anchor, then shift only
            // as needed to keep the detected hand content inside the crop window.
            int[] edges = analysisImage.edgeDetect(COM_THRESHOLD, EDGE_MIN_COUNT);
            int contentLeft = (edges[PelArray.LEFT_EDGE] > 0) ? edges[PelArray.LEFT_EDGE] : 0;
            int contentRight = (edges[PelArray.RIGHT_EDGE] > 0) ? edges[PelArray.RIGHT_EDGE] : width - 1;
            int contentTop = (edges[PelArray.TOP_EDGE] > 0) ? edges[PelArray.TOP_EDGE] : 0;
            int contentBottom = (edges[PelArray.BOTTOM_EDGE] > 0) ? edges[PelArray.BOTTOM_EDGE] : height - 1;

            int sourceCropSize = getSourceCropSize(baseName, width, height);
            int x0 = comX - (sourceCropSize / 2);
            int y0 = comY - (sourceCropSize / 2);

            if (contentRight - contentLeft + 1 <= sourceCropSize)
               {
               x0 = clamp(x0, contentRight - sourceCropSize + 1, contentLeft);
               }

            if (contentBottom - contentTop + 1 <= sourceCropSize)
               {
               y0 = clamp(y0, contentBottom - sourceCropSize + 1, contentTop);
               }

            x0 = clamp(x0, 0, Math.max(0, width - sourceCropSize));
            y0 = clamp(y0, 0, Math.max(0, height - sourceCropSize));
            int x1 = x0 + sourceCropSize - 1;
            int y1 = y0 + sourceCropSize - 1;

            System.out.printf(
               "%s COM=(%d,%d) sourceCrop=%d crop=(%d,%d)-(%d,%d) content=(%d,%d)-(%d,%d)%n",
               baseName, comX, comY, sourceCropSize, x0, y0, x1, y1, contentLeft, contentTop, contentRight, contentBottom);

            PelArray cropped = image.crop(x0, y0, x1, y1);
            if (sourceCropSize != CROP_SIZE)
               {
               cropped = cropped.scale(CROP_SIZE, CROP_SIZE);
               }
            Path outBin = outDir.resolve(baseName + ".bin");
            writeGrayBytes(cropped.getPelArray(), outBin);
            Path outBmp = outBmpDir.resolve(baseName + ".bmp");
            BGR2BMP.main(new String[]{"gray", String.valueOf(CROP_SIZE), String.valueOf(CROP_SIZE), outBin.toString(), outBmp.toString()});
            }
         }
      catch (IOException e)
         {
         System.err.println("Image processing error: " + e.getMessage());
         }
      }

   private static int clamp(int value, int min, int max)
      {
      if (min > max) return value;
      return Math.max(min, Math.min(value, max));
      }

   private static int getSourceCropSize(String baseName, int width, int height)
      {
      int requested = baseName.startsWith("5_") ? FIVE_FINGER_SOURCE_CROP_SIZE : CROP_SIZE;
      return Math.min(requested, Math.min(width, height));
      }

   private static int[] readBmpDimensions(Path bmpPath)
      {
      try (DataInputStream in = new DataInputStream(new FileInputStream(bmpPath.toFile())))
         {
         in.skipBytes(14);
         int biSize = Integer.reverseBytes(in.readInt());
         int width = Integer.reverseBytes(in.readInt());
         int height = Integer.reverseBytes(in.readInt());
         return new int[]{width, Math.abs(height)};
         }
      catch (IOException e)
         {
         return null;
         }
      }
   private static void writeGrayBytes(int[][] array, Path outPath) throws IOException
      {
      try (DataOutputStream out = new DataOutputStream(new FileOutputStream(outPath.toFile())))
         {
         for (int[] row : array)
            {
            for (int pel : row)
               {
               out.writeByte(pel & 0xFF);
               }
            }
         }
      }
   }
