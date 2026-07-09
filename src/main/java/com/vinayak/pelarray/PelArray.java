package com.vinayak.pelarray;

/*
 * The PelArray class is used to manipulate two dimensional integer arrays that are
 * interpreted to be picture elements (32 bit color). The Java int type is 4 bytes
 * long, which is more than sufficient for 32 bit color represented as 0x00RRGGBB.
 *
 * javac PelArray.java
 *
 * Most calculations are done using double types to prevent overflows.
 *
 * (0,0) is in the upper left corner.
 *
 * Some of the methods currently return a fixed value which I am using as a place holder 
 * for the future when it would return a success or failure flag of some sort.
 * 
 * Methods in the PelArray class are
 *  PelArray(int [][])    - constructor that sets the initial array of picture elements
 *  setPelArray(int [][]) - set the current array of pels
 *  getPelArray()         - get the current array of pels
 *
 *  getWidth()   - Return the width of the image array
 *  getHeight()  - Return the height of the image array
 *
 *  getXcom() - return the x coordinate of the center of mass (method may need to call calcCOM() to determine the value). Zero indexed.
 *  getYcom() - return the y coordinate of the center of mass (method may need to call calcCOM() to determine the value). Zero indexed.
 *  calcCOM() - calculates the x and y center of mass values.
 * 
 *  offset(int, int)         - offset the image array by the specified x,y amount
 *
 *  rotateCCW90()            - Rotate image 90 degrees counter clockwise
 *  rotateCW90()             - Rotate image 90 degrees clockwise
 *  flipHorizontal()         - Flip the image about the horizontal central line (top becomes bottom)
 *  flipVertical()           - Flip the image about the vertical central line (left becomes right) 
 *
 *  edgeDetect(int, int)     - Find edges in the image array based on a simple threshold count criteria.
 *  crop(int, int, int, int) - crop the image array given the x,y upper left and x,y lower right coordinates.
 *  scale(int, int)          - scale the image array to an array with the new specified width and height.
 *
 *  dump() - dumps the pelArray as hex values to the console
 *
 *  grayScalePel(int)     - Takes a single RGB pel and returns a gray scale pel value
 *  grayScaleImage()      - Converts all the pels in an image array to gray scale
 *  onesComplimentImage() - Take the ones complement of all the pels in an image array
 *  oneColorPel(int, int) - Masks off a single color in a single pel
 *  oneColorImage(int)    - Masks off a single color in all the pels of an image array
 *
 *  forceMin(int, int)    - Any value below the first argument is set to the second argument
 *  forceMax(int, int)    - Any value above the first argument is set to the second argument
 *
 *  offsetColors(int redOffset, int greenOffset, int blueOffset) - Add a bounded offset to individual colors in the image array (arrayOfPels[][]).
 *  blur() - Blurs an image
 *
 *  min(int a, int b) - Return the minimum value of two integers
 *  max(int a, int b) - Return the maximum value of two integers
 *
 * November 17, 2023 - Added the getWidth() and getHeight() methods
 * December 6, 2023  - Added forceMin() and forceMax()
 * April 24, 2024    - Added offsetColors(), min() and max() and fixed a bug in onesComplimentImage(). Agul G. pointed out that I neglected to mask off the most significant byte.
 * October 23, 2025  - Added blur()
 */

/**
 * @author Eric R. Nelson
 * February 25, 2009
 *
 */

/*
 * A class to help manage the manipulation of the pels in an array that is interpreted as an RGB image.
 */
public class PelArray
   {
/*
** Constants used for edge detection array. They are just the meanings behind the index used in an array with four elements.
*/
   public static final int RIGHT_EDGE  = 0;
   public static final int TOP_EDGE    = 1;
   public static final int LEFT_EDGE   = 2;
   public static final int BOTTOM_EDGE = 3;

/*
** Constants for masking colors in an RGB pel
*/
   public static final int RED   = 0x00FF0000;
   public static final int GREEN = 0x0000FF00;
   public static final int BLUE  = 0x000000FF;

/*
** Other useful colors
*/
   public static final int BLACK = 0x00000000;
   public static final int WHITE = 0x00FFFFFF;

/*
** Private Member Variables
*/
   private int[][] arrayOfPels;        // This array holds the picture elements for each instance of this class
   private boolean comKnownFlag;       // If this flag is true then we have a center of mass value.
   private int xComColumnVal;          // x (column) center of mass value
   private int yComRowVal;             // y (row) center of mass value
    
/*
 * If no array is passed to the constructor, then create a 1x1 as a place holder (prevents exceptions)
 */
   public PelArray()
      {
      int[][] array = new int[1][1];
      setPelArray(array);
      }

   public PelArray(int[][] array)
      {
      setPelArray(array);
      }

/*
 * Set the array of pels to whatever is passed. Resets the comKnownFlag to indicate that we
 * no longer know the center of mass values and that they will need to be recalculated from scratch
 * the next time someone asks for them.
 */
   public int[][] setPelArray(int[][] array)
      {
      arrayOfPels = array;
      comKnownFlag = false;
      return arrayOfPels;
      }
    
/*
* A get method to return the array of picture elements maintained by this class     
*/
   public int[][] getPelArray()
      {
      return arrayOfPels;
      }

/*
** Return the width and height of the image array
*/
   public int getWidth()
      {
      return(arrayOfPels[0].length);
      }

   public int getHeight()
      {
      return(arrayOfPels.length);
      }

/*
 * Calculate the center of mass of the array of pels.
 * This method is called by the getXcom() and getYcom() methods
 * if the com values are not known. This method sets the comKnownFlag to true.
 * If the method returns false then something weird happened.
 */
   public boolean calcCOM()
      {
      int iRow, iCol;
      double fRowCom = 0.0, fColCom = 0.0, fMass = 0.0;
     
      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;
        
      for (iRow = 0;  iRow < iRowCount; ++iRow)
         {
         for (iCol = 0; iCol < iColCount; ++iCol)
            {
            fColCom += ((double)iCol) * ((double)arrayOfPels[iRow][iCol]);
            fRowCom += ((double)iRow) * ((double)arrayOfPels[iRow][iCol]);
            fMass += (double)arrayOfPels[iRow][iCol];
            }
          }

      comKnownFlag = true;

      if (fMass > 0.0)    // Just do the division
         {
         xComColumnVal = (int)Math.round(fColCom / fMass);
         yComRowVal = (int)Math.round(fRowCom / fMass);
         }
      else if (fMass == 0.0) // All pels are zero, so just set to the center of the array
         {
         xComColumnVal = iColCount / 2;
         yComRowVal = iRowCount / 2;
         }
      else
         {
         comKnownFlag = false; // Error! This error should never occur.
         xComColumnVal = 0;
         yComRowVal = 0;
         }
        
      return comKnownFlag;
      } // boolean calcCOM()
    
/*
 * Return the x (column) value of the center of mass of the image. Recalculates the com if needed first.     
 */
   public int getXcom()
      {
      if (comKnownFlag == false) calcCOM();
               
      return xComColumnVal;
      }
    
/*
 * Return the y (row) value of the center of mass of the image. Recalculates the com if needed first.     
 */
   public int getYcom()
      {
      if (comKnownFlag == false) calcCOM();
               
      return yComRowVal;
      }
    
/*
 * Perform an arbitrary offset of the image. Fill the empty space with zero.
 * Returns a new PelArray object    
 */
   public PelArray offset(int x, int y)
      {
      int iRow, iCol;
      int iNewRow, iNewCol;
        
      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

      int[][] targetArray = new int[iRowCount][iColCount];
        
      for (iRow = 0;  iRow < iRowCount; ++iRow)
         {
         for (iCol = 0; iCol < iColCount; ++iCol)
            {
            iNewRow = iRow + y;
            iNewCol = iCol + x;
            if ((iNewRow < iRowCount) &&
                (iNewRow >= 0) &&
                (iNewCol < iColCount) &&
                (iNewCol >= 0)) targetArray[iNewRow][iNewCol] = arrayOfPels[iRow][iCol];
            }
         } // for (iRow = 0;  iRow < iRowCount; ++iRow)

      return new PelArray(targetArray);
      } //  PelArray offset(int x, int y)

/*
 * Rotate image 90 degrees counter clockwise
 * Returns a new PelArray object    
 */
  public PelArray rotateCCW90()
      {
      int iRow, iCol;
      int iNewRow, iNewCol;
        
      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

      int[][] targetArray = new int[iColCount][iRowCount]; // Columns become rows with rotation

      for (iRow = 0;  iRow < iRowCount; ++iRow)
         {
         for (iCol = 0; iCol < iColCount; ++iCol)
            {
            iNewRow = iColCount - iCol - 1;
            iNewCol = iRow;
            targetArray[iNewRow][iNewCol] = arrayOfPels[iRow][iCol];
            }
         }

      return new PelArray(targetArray);
      } // PelArray rotateCCW90()

/*
 * Rotate image 90 degrees clockwise
 * Returns a new PelArray object    
 */
  public PelArray rotateCW90()
      {
      int iRow, iCol;
      int iNewRow, iNewCol;
        
      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

      int[][] targetArray = new int[iColCount][iRowCount]; // Columns become rows with rotation

      for (iRow = 0;  iRow < iRowCount; ++iRow)
         {
         for (iCol = 0; iCol < iColCount; ++iCol)
            {
            iNewRow = iCol;
            iNewCol = iRowCount  - iRow - 1;
            targetArray[iNewRow][iNewCol] = arrayOfPels[iRow][iCol];
            }
         }

      return new PelArray(targetArray);
      } // PelArray rotateCW90()

/*
 * Flip the image about the horizontal central line (top becomes bottom)
 * Returns a new PelArray object    
 */
  public PelArray flipHorizontal()
      {
      int iRow, iCol;
      int iNewRow, iNewCol;
        
      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

      int lastRowIndex = iRowCount - 1; // The array index value to the last row element
      int halfWay = iRowCount / 2 + 1;  // We stop swapping elements halfway through, so let's make the loop obvious

      int[][] targetArray = new int[iRowCount][iColCount];

      for (iRow = 0;  iRow < halfWay; ++iRow)
         {
         for (iCol = 0; iCol < iColCount; ++iCol)
            {
            iNewRow = lastRowIndex - iRow;
            iNewCol = iCol;
            targetArray[iNewRow][iNewCol] = arrayOfPels[iRow][iCol];    // Top becomes bottom
            targetArray[iRow][iNewCol]    = arrayOfPels[iNewRow][iCol]; // Bottom becomes top
            }
         }

      return new PelArray(targetArray);
      } // PelArray flipHorizontal()

/*
 * Flip the image about the vertical central line (left becomes right)
 * Returns a new PelArray object    
 */
  public PelArray flipVertical()
      {
      int iRow, iCol;
      int iNewRow, iNewCol;
        
      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

      int lastColIndex = iColCount - 1; // The array index value to the last column element
      int halfWay = iColCount / 2 + 1;  // We stop swapping elements halfway through, so let's make the loop obvious

      int[][] targetArray = new int[iRowCount][iColCount];

      for (iRow = 0;  iRow < iRowCount; ++iRow)
         {
         for (iCol = 0; iCol < halfWay; ++iCol)
            {
            iNewRow = iRow;
            iNewCol = lastColIndex - iCol;
            targetArray[iNewRow][iNewCol] = arrayOfPels[iRow][iCol];      // Left becomes right
            targetArray[iNewRow][iCol]    = arrayOfPels[iRow][iNewCol];   // Right becomes left
            }
         }

      return new PelArray(targetArray);
      } // PelArray flipVertical()

/*
 * Detect edges by looking for a minimum count of pels (inclusive) above a given threshold (inclusive).
 * If the top edge is not found, the bottom edge is not searched. The same goes for left and right.
 * The edges array is intended to be used with the RIGHT_EDGE, LEFT_EDGE, TOP_EDGE, BOTTOM_EDGE constants.
 * Labeled break statements are used within the for loops because this implementation is cleaner than using while loops.
 */
   public int[] edgeDetect(int threshold, int minCount)
      {
      int iRow, iCol;
      int count;
      int[] edges= new int[4]; // right, top, left, bottom
        
      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

/*
** Initialize the returned array of edges.
*/
      for (int i = 0; i < edges.length; ++i) edges[i] = -1; // -1 is not a valid edge. It is used as a flag.
        
/*
** Start with the top row and go down
*/
      count = 0;
TopRow:
      for (iRow = 0; iRow < iRowCount; ++iRow)
         {
         for (iCol = 0; iCol < iColCount; ++iCol)
            {
            if (arrayOfPels[iRow][iCol] >= threshold) ++count;
            if (count >= minCount)
               {
               edges[TOP_EDGE] = iRow;
               break TopRow;
               }
            }
         } // for (iRow = 0; iRow < iRowCount; ++iRow)

/*
** And now from the bottom up
*/
      if (edges[TOP_EDGE] > -1)
         {
         count = 0;
BottomRow:
         for (iRow = iRowCount - 1;  iRow >= 0; --iRow)
            {
            for (iCol = 0; iCol < iColCount; ++iCol)
               {
               if (arrayOfPels[iRow][iCol] >= threshold) ++count;
               if (count >= minCount)
                  {
                  edges[BOTTOM_EDGE] = iRow;
                  break BottomRow;
                  }
               }
            } // for (iRow = iRowCount - 1;  iRow >= 0; --iRow)
         } // if (edges[TOP_EDGE] > -1)

/*
** From left to right
*/
      count = 0;
LeftColumn:
      for (iCol = 0; iCol < iColCount; ++iCol)
         {
         for (iRow = 0;  iRow < iRowCount; ++iRow)
            {
            if (arrayOfPels[iRow][iCol] >= threshold) ++count;
            if (count >= minCount)
               {
               edges[LEFT_EDGE] = iCol;
               break LeftColumn;
               }
            }
         } // for (iCol = 0; iCol < iColCount; ++iCol)

/*
** and finally from right to left
*/
      if (edges[LEFT_EDGE] > -1)
         {
         count = 0;
RightColumn:
         for (iCol = iColCount - 1; iCol >= 0 ; --iCol)
            {
            for (iRow = 0;  iRow < iRowCount; ++iRow)
               {
               if (arrayOfPels[iRow][iCol] >= threshold) ++count;
               if (count >= minCount)
                  {
                  edges[RIGHT_EDGE] = iCol;
                  break RightColumn;
                  }
               }
            } // for (iCol = iColCount - 1; iCol >= 0 ; --iCol)
         } // if (edges[LEFT_EDGE] > -1)

      return edges;
      } // int[] edgeDetect(int threshold, int minCount)

/*
 * Crop the array of pels to the given frame (inclusive)
 * x is columns
 * y is rows 
 * Returns a new PelArray object    
*/
   public PelArray crop(int xUpperLeft, int yUpperLeft, int xLowerRight, int yLowerRight)
      {
      int iRow, iCol;
      int iNewRow = 0, iNewCol;
      int pel;
        
      int iRowCount = yLowerRight - yUpperLeft + 1;
      int iColCount = xLowerRight - xUpperLeft + 1;

      int[][] targetArray = new int[iRowCount][iColCount];
        
      for (iRow = yUpperLeft; iRow <= yLowerRight; ++iRow)
         {
         iNewCol = 0; // Start in column 0 for each new row

         for (iCol = xUpperLeft; iCol <= xLowerRight; ++iCol)
            {
            targetArray[iNewRow][iNewCol] = arrayOfPels[iRow][iCol];
            ++iNewCol;
            }
          ++iNewRow;
          }

      return new PelArray(targetArray);
      } // PelArray crop(int xUpperLeft, int yUpperLeft, int xLowerRight, int yLowerRight)

 /*
 * Scale the array of pels to a new size
 * We iterate over the target to get the location of the source pels. This technique prevents holes in a larger image.
 * We average the values going into each new pel so that we don't get a "last man wins all" result.
 * Returns a new PelArray object    
 */
   public PelArray scale(int newColumnWidth, int newRowHeight)
      {
      int iRow, iCol;
      int iSourceRow, iSourceCol;
      double xColumnRatio, yRowRatio;
        
      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

      int[][] targetArray = new int[newRowHeight][newColumnWidth];
      double[][] sumArray = new double[newRowHeight][newColumnWidth];
      double[][] countArray = new double[newRowHeight][newColumnWidth];

      xColumnRatio = ((double)(iColCount - 1))/((double)(newColumnWidth - 1));
      yRowRatio = ((double)(iRowCount - 1))/((double)(newRowHeight - 1));

      for (iRow = 0; iRow < newRowHeight; ++iRow) // Accumulate values in the target and count how many values were added in each new pel
         {
         iSourceRow = (int)Math.round(((double)iRow) * yRowRatio);

         for (iCol = 0; iCol < newColumnWidth; ++iCol)
            {
            iSourceCol = (int)Math.round(((double)iCol) * xColumnRatio);
            sumArray[iRow][iCol] += (double)arrayOfPels[iSourceRow][iSourceCol];
            ++countArray[iRow][iCol];
            }
         } // for (iRow = 0; iRow < newRowHeight; ++iRow) 

      for (iRow = 0; iRow < newRowHeight; ++iRow) // Replace the accumulated pel values with averages, or zero if there are none
         {
         for (iCol = 0; iCol < newColumnWidth; ++iCol)
            {
            if (countArray[iRow][iCol] > 0.0)
               targetArray[iRow][iCol] = (int)Math.round(sumArray[iRow][iCol] / countArray[iRow][iCol]);
            else
               targetArray[iRow][iCol] = 0;
            }
         } // for (iRow = 0; iRow < newRowHeight; ++iRow)

      return new PelArray(targetArray);
      } // PelArray scale(int newColumnWidth, int newRowHeight)

/*
 * Simple method to dump the pelArray to the console as hex values per pel followed by a space.
 * The default format is "%06X " which can get overridden by passing a new format string.
 * This method is used primarily for debugging.
 */
   public boolean dump(String fmt)
      {
      int iRow, iCol;

      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

      for (iRow = 0;  iRow < iRowCount; ++iRow)
         {
         for (iCol = 0; iCol < iColCount; ++iCol)
            {
            System.out.printf(fmt, arrayOfPels[iRow][iCol]);
            }
         System.out.printf("\n");
         }

      return true;
      } // boolean dump(String fmt)

   boolean dump() // Hex dump with the default format
      {
      return dump("%06X ");
      }

 /*
 * Y = 0.3RED + 0.59GREEN + 0.11Blue
 * The "color-to-Grayscale" method takes a color picture element (pel) and returns the gray scale pel using just one of may possible formulas
 */
   public int grayScalePel(int pel)
      {
      int blue   =  pel        & 0x00FF; // Extract the R, G, B color bytes
      int green  = (pel >> 8)  & 0x00FF;
      int red    = (pel >> 16) & 0x00FF;
    
      int lum = (int)Math.round(0.3 * (double)red + 0.589 * (double)green + 0.11 * (double)blue); // Calculate the gray scale color

      return ((lum & 0x00FF) << 16) | ((lum & 0x00FF) << 8) | (lum & 0x00FF); // Build the RGB pel from the one gray scale color
      }

/*
* Takes the array of pels and returns a gray scale version of the PelArray
* Returns a new PelArray object    
*/
   public PelArray grayScaleImage()
      {
      int iRow, iCol;

      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

      int[][] targetArray = new int[iRowCount][iColCount];

      for (iRow = 0;  iRow < iRowCount; ++iRow)
         {
         for (iCol = 0; iCol < iColCount; ++iCol)
            {
            targetArray[iRow][iCol] = grayScalePel(arrayOfPels[iRow][iCol]);
            }
         }

      return new PelArray(targetArray);
      } // PelArray grayScaleImage()

/*
* Takes the array of pels and returns a ones complement version of the PelArray
* Returns a new PelArray object    
*/
   public PelArray onesComplimentImage()
      {
      int iRow, iCol;

      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

      int[][] targetArray = new int[iRowCount][iColCount];

      for (iRow = 0;  iRow < iRowCount; ++iRow)
         {
         for (iCol = 0; iCol < iColCount; ++iCol)
            {
            targetArray[iRow][iCol] = (~arrayOfPels[iRow][iCol]) & WHITE; // Need to mask off the unused most significant byte
            }
         }

      return new PelArray(targetArray);
      } // PelArray onesComplimentImage()

 /*
 * returns the pel masked with a single color: typically RED, GREEN or BLUE
 */
   public int oneColorPel(int pel, int color)
      {
      return pel & color;
      }


/*
* Takes the array of pels and returns a single color version of the PelArray
* The color argument should be RED, GREEN or BLUE
*
* Returns a new PelArray object    
*/
   public PelArray oneColorImage(int color)
      {
      int iRow, iCol;

      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

      int[][] targetArray = new int[iRowCount][iColCount];

      for (iRow = 0;  iRow < iRowCount; ++iRow)
         {
         for (iCol = 0; iCol < iColCount; ++iCol)
            {
            targetArray[iRow][iCol] = oneColorPel(arrayOfPels[iRow][iCol], color);
            }
         }

      return new PelArray(targetArray);
      } // PelArray oneColorImage(int color)

/*
** Looks at each pel and if it is below "limit" then it is given a value of "forced"
**
** This method is useful for removing noise in a black background in which case forced is BLACK.
**
** Returns a new PelArray object    
*/
   public PelArray forceMin(int limit, int forced)
      {
      int iRow, iCol;

      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

      int[][] targetArray = new int[iRowCount][iColCount];

      for (iRow = 0;  iRow < iRowCount; ++iRow)
         {
         for (iCol = 0; iCol < iColCount; ++iCol)
            {
            targetArray[iRow][iCol] = (arrayOfPels[iRow][iCol] < limit) ? forced : arrayOfPels[iRow][iCol];
            }
         }

      return new PelArray(targetArray);
      } // PelArray forceMin(int limit, int forced)

/*
** Looks at each pel and if it is above "limit" then it is given a value of "forced"
**
** This method is useful for removing noise in a white background in which case forced is WHITE.
**
** Returns a new PelArray object    
*/
   public PelArray forceMax(int limit, int forced)
      {
      int iRow, iCol;

      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

      int[][] targetArray = new int[iRowCount][iColCount];

      for (iRow = 0;  iRow < iRowCount; ++iRow)
         {
         for (iCol = 0; iCol < iColCount; ++iCol)
            {
            targetArray[iRow][iCol] = (arrayOfPels[iRow][iCol] > limit) ? forced : arrayOfPels[iRow][iCol];
            }
         }

      return new PelArray(targetArray);
      } // PelArray forceMax(int limit, int forced)

/*
** Adds the passed offsets (positive or negative) to the red, green and blue values of the PelArray.
** If a color value is less than zero, then it is set to zero.
** If a color value exceeds 255, then it is set to 255.
**
** This method is useful for images with a bright background that you want to be black.
** You can put in the color offsets to bring it down to something close to zero and not change the image contrast.
**
** Returns a new PelArray object    
*/
   public PelArray offsetColors(int redOffset, int greenOffset, int blueOffset)
      {
      int iRow, iCol;

      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

      int[][] targetArray = new int[iRowCount][iColCount];

      for (iRow = 0;  iRow < iRowCount; ++iRow)
         {
         for (iCol = 0; iCol < iColCount; ++iCol)
            {
            int pelRedValue, pelGreenValue, pelBlueValue;
/*
**  Remember that RED = 0x00FF0000, GREEN = 0x0000FF00 and BLUE  = 0x000000FF, so we need to shift red and green to get the byte value between 0 and 255
*/
            pelRedValue   = ((arrayOfPels[iRow][iCol] & RED)   >> 16) + redOffset;   // Mask off the relevant color, make it 0-255 and add the passed color offsets
            pelGreenValue = ((arrayOfPels[iRow][iCol] & GREEN) >>  8) + greenOffset;
            pelBlueValue  = (arrayOfPels[iRow][iCol] & BLUE)          + blueOffset;

/*
** Now make sure that each color stays in the range 0 to 255
*/ 
            pelRedValue = min(pelRedValue, 255);
            pelRedValue = max(pelRedValue, 0);

            pelGreenValue = min(pelGreenValue, 255);
            pelGreenValue = max(pelGreenValue, 0);

            pelBlueValue = min(pelBlueValue, 255);
            pelBlueValue = max(pelBlueValue, 0);

            targetArray[iRow][iCol] = (pelRedValue  << 16) | (pelGreenValue << 8) | pelBlueValue; // Rebuild the picture element with the new color values
            } // for (iCol = 0; iCol < iColCount; ++iCol)
         } // for (iRow = 0;  iRow < iRowCount; ++iRow)

      return new PelArray(targetArray);
      } // PelArray offsetColors(int redOffset, int greenOffset, int blueOffset)

/*
** Blurs an image (tested 10/23/2025)
**
** It makes four diagonally offset versions of the image (by one pel in both x and y) and averages them with the original
**
** Returns a new PelArray object    
*/
   public PelArray blur()
      {
      double val;
      int iRow, iCol;

      int NUM_IMAGES = 5;

      int iRowCount = arrayOfPels.length;
      int iColCount = arrayOfPels[0].length;

      int[][] targetArray = new int[iRowCount][iColCount];

      PelArray p1 = this.offset(-1, -1); 
      int[][] p1Array = p1.getPelArray();

      PelArray p2 = this.offset( 1, -1); 
      int[][] p2Array = p2.getPelArray();

      PelArray p3 = this.offset(-1,  1); 
      int[][] p3Array = p3.getPelArray();

      PelArray p4 = this.offset( 1,  1); 
      int[][] p4Array = p4.getPelArray();

      for (iRow = 0;  iRow < iRowCount; ++iRow)
         {
         for (iCol = 0; iCol < iColCount; ++iCol)
            {
            val = (double)arrayOfPels[iRow][iCol] + (double)p1Array[iRow][iCol] + (double)p2Array[iRow][iCol] + (double)p3Array[iRow][iCol] + (double)p4Array[iRow][iCol];
            targetArray[iRow][iCol] = (int)(val/(double)NUM_IMAGES);
            }
         }

      return new PelArray(targetArray);
      } // PelArray blur()


/*
** A couple of small methods to find the min and max of a couple of integers.
*/
   public int min(int a, int b)
      {
      return((a < b) ? a : b);
      }

   public int max(int a, int b)
      {
      return((a > b) ? a : b);
      }

   } // public class PelArray

