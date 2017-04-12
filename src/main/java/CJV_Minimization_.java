import numericalMethods.calculus.*;
//import gov.nih.mipav.view.Preferences;
import ij.*;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.gui.GenericDialog;
import ij.gui.DialogListener;
import ij.process.*;

import java.awt.*;

//public class EntropyMinimizationPlugin_ implements ExtendedPlugInFilter, DialogListener, RealFunctionOfSeveralVariables {
public class CJV_Minimization_ implements ExtendedPlugInFilter, DialogListener, RealFunctionOfSeveralVariables {
	
    /** Choose the imaging model */
    private String shadeComponent;
    
    /** whether output should be a fit (instead of subtracting it) */
    private static boolean outputFit = false;
    private static boolean corrImgInteger = true;
    private static boolean corrImgFloat = true;
    
    
    /** the flags specifying the capabilities and needs of the filter */
    private final int flags = DOES_ALL|CONVERT_TO_FLOAT|FINAL_PROCESSING|KEEP_PREVIEW|PARALLELIZE_STACKS;
    
    /** the ImagePlus of the setup call */
    private ImagePlus imp = null;
    
    //~ Instance fields ------------------------------------------------------------------------------------------------
    /** Buffer holding calculation for noisy image N(x,y) */
    private double[] buffer;

    /** Buffer holding calculation for noiseless U(x,y). */
    private double[] idealBuffer;
    
    /** Numbering of shading correction parameters. */
    private int nParams;

    /** Tolerance passed to Powell's algorithm. */
    private double powellTolerance = 1.0e-6;

    /** Image width */
    private int xDim;
    /** Image height */
    private int yDim;
    private int xcenter;
    private int ycenter;
    
    /** Image size: xDim * yDim. */
    private int sliceSize;
    
    /** Input and Output Images  */
    ImageProcessor srcImage; 
    FloatProcessor corrFloatImage;
    ImageProcessor corrImage;
    FloatProcessor smBcgImage;
    FloatProcessor saBcgImage;
    
    /** Array used to restore multiplicative shading components **/
    private double[] smImgVal;
    private double[] saImgVal;
    
    /** Image mask required **/
    byte[] mask;
    
    /** Image type **/
    private int imageType;
    
    /** Neutralization parameters in background expression **/
    private double xScale;
    private double yScale;
    /** original image maximum. */
    private double maximum;
   
    
	@Override
	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("final") && !(outputFit)) {
            return DONE;
        } else {
            if (IJ.versionLessThan("1.38u")) // generates an error message for older versions
                return DONE;
            this.imp = imp;
            return flags;
        }
	}
	  
	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr)  {
		// TODO Auto-generated method stub
		GenericDialog gd = new GenericDialog("CJV-based Minimization");
        String[] labels1 = {"Multiplicative and Additive","Multiplicative","Additive"};
        gd.addRadioButtonGroup("Shading Components", labels1, 3, 1, "Multiplicative and Additive");       
        gd.addCheckbox("Corrected Image (Integer)", corrImgInteger);
        gd.addCheckbox("Corrected Image (Float)", corrImgFloat);
        gd.addCheckbox("Output Fit For Shading Components", outputFit);
        gd.addDialogListener(this);
        gd.showDialog();                    //input by the user (or macro) happens here
        if (gd.wasCanceled()) return DONE;
        IJ.register(this.getClass());       //protect static class variables (filter parameters) from garbage collection
        return IJ.setupDialog(imp, flags);  //ask whether to process all slices of stack (if a stack)
	}
	
	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		// TODO Auto-generated method stub
		shadeComponent = gd.getNextRadioButton();
        corrImgInteger = gd.getNextBoolean();
        corrImgFloat = gd.getNextBoolean();
        outputFit = gd.getNextBoolean();
		return true;
	}

	
//	@Override
    public void setNPasses(int nPasses) {
//		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void run(ImageProcessor ip) {
		// TODO Auto-generated method stub
		/* Get the image mask first */
		ip = imp.getProcessor();
		mask = ip.getMaskArray();
		imageType = imp.getBitDepth();
		ImageStatistics stats = imp.getStatistics();
        maximum = stats.max;
		
        String title = imp.getTitle();
		if(ip instanceof ColorProcessor){
		   IJ.error("Cannot process color image");
		}else if(mask==null){
		   IJ.error("Please get the image mask first");
			
		}else{
			if (imp.getNDimensions() == 2){
				if (shadeComponent == "Multiplicative and Additive") {
					outputFit = false;
					nParams = 10;
	                run2DMulAdd();
	                if(corrImgInteger){
	                	ImagePlus imp1 = new ImagePlus("CJV(MulAdd) CorrectedImg(Integer) of "+ title, corrImage);
		                imp1.show();
	                }
	                if(corrImgFloat){
	                	ImagePlus imp11 = new ImagePlus("CJV(MulAdd) CorrectedImg(Float) of "+title, corrFloatImage);
		                imp11.show();
	                }	                
	                
	            } else if (shadeComponent=="Multiplicative") {
	            	outputFit = true;
	            	nParams = 5;
	                run2DMul();
	                if(corrImgInteger){
	                	ImagePlus imp2 = new ImagePlus("CJV(Mul) CorrectedImg(Integer) of "+ title, corrImage);
		                imp2.show();	
	                }
	                if(corrImgFloat){
	                	ImagePlus imp22 = new ImagePlus("CJV(Mul) CorrectedImg(Float) of "+ title, corrFloatImage);
		                imp22.show();
	                }
	                ImagePlus imp3 = new ImagePlus("ShadingField(Mul) of " + title, smBcgImage);
	                imp3.show();
			} else if(shadeComponent=="Additive"){
				 outputFit = true;
				 nParams = 5;
	             run2DAdd();
	             if(corrImgInteger){
	            	 ImagePlus imp4 = new ImagePlus("CJV(Add) CorrectedImg(Integer) of "+ title, corrImage);
		             imp4.show();
	             }
	             if(corrImgFloat){
	            	 ImagePlus imp44 = new ImagePlus("CJV(Add) CorrectedImg(Float) of "+ title, corrFloatImage);
		             imp44.show();
	             }
	             ImagePlus imp5 = new ImagePlus("ShadingField(Add) of " + title, saBcgImage);
	             imp5.show();
			}
			
		}else{
			IJ.error("Cannot process 3D image");
		}
			
		}
}
	
/* Surface function from a paper based on Entropy Minimization */
	 private double cjvFunction(double[] p) {        
		        double a1, a2, a3, a4, a5;
		        double m1, m2, m3, m4, m5;
		        int x;
		        int y;
		        int xx;
		        int yy;
		        int i;
		        int j;
		        double say;
		        double smy;
		        double sa;
		        double sm;
		        
		        /** Find the foreground and background in an image using the following parameters **/
		        double cjv;
		        int m = 0;
		        int n = 0;
		        double stdInCell;
		        double stdInBcg;
		        double meanInCell;
		        double meanInBcg;
		        double[] inCellVal;
		        double[] inBcgVal;
		        int InCellNum = 0;
		        int InBcgNum = 0;

        		if (shadeComponent=="Multiplicative and Additive") {
        			a1 = p[0];
	                a2 = p[1];
	                a3 = p[2];
	                a4 = p[3];
	                a5 = p[4];
	                m1 = p[5];
	                m2 = p[6];
	                m3 = p[7];
	                m4 = p[8];
	                m5 = p[9];	

	                for (y = 0; y < yDim; y ++) {
	                    j = y * xDim;
	                    yy = y - ycenter; 
	                    say = a2 * yy  + a5 * (yy*yy - yScale) ;
    	                smy = 1.0 + m2 * yy + m5 * (yy*yy - yScale);
	               
	                    for (x = 0; x < xDim; x++) {
	                        i = j + x;
	                        xx = x - xcenter;
	                        sa = say + a1 * xx + a3 * xx* yy + a4 * (xx * xx - xScale) ;
                            sm = smy + m1 * xx + m3 * xx* yy + m4 * (xx * xx - xScale);
	            
	                        if (sa < (-maximum / 2.0)) {
	                            sa = -maximum / 2.0;
	                        } else if (sa > (maximum / 2.0)) {
	                            sa = maximum / 2.0;
	                        }
	                        
	                        if (sm < 0.05) {
	                            sm = 0.05;
	                        } else if (sm > 20.0) {
	                            sm = 20.0;
	                        }

                            idealBuffer[i] = (buffer[i] - sa) / sm;

	                    } // for (x = 0; x < xDim; x += pixelIncrement)
	                } // for (y = 0; y < yDim; y += pixelIncrement)
	                
	            } // if (noiseType == NOISE_MA2)
	            else if (shadeComponent=="Multiplicative") {
	            	 m1 = p[0];
		             m2 = p[1];
		             m3 = p[2];
		             m4 = p[3];
		             m5 = p[4];
		           
	                for (y = 0; y < yDim; y ++) {
	                    j = y * xDim;
	                    yy = y - ycenter;   
	                    smy = 1.0 + m2 * yy + m5 * (yy*yy - yScale);
	                    
	                    for (x = 0; x < xDim; x ++) {
	                        i = j + x;
	                        xx = x - xcenter;
	                        sm = smy + m1 * xx + m3 * xx * yy + m4 * (xx * xx - xScale);
	                        
	                        if (sm < 0.05) {
	                            sm = 0.05;
	                        }
	                        else if (sm > 20.0) {
	                            sm = 20.0;
	                        }                        
                            idealBuffer[i] = buffer[i] / sm;
                            
	                    } // for (x = 0; x < xDim; x += pixelIncrement)
	                } // for (y = 0; y < yDim; y += pixelIncrement)	                
	            } // else if (noiseType == NOISE_M2)
        		
	            else if(shadeComponent=="Additive"){
                    a1 = p[0];
	                a2 = p[1];
	                a3 = p[2];
	                a4 = p[3];
	                a5 = p[4];
                    
	                for (y = 0; y < yDim; y ++) {
	                    j = y * xDim;
                        yy = y - ycenter;   	                    
                        say = a2 * yy  + a5 * ( yy * yy - yScale) ;
	          
	                    for (x = 0; x < xDim; x ++) {
	                        i = j + x;
	                        xx = x - xcenter;	                        
	                        sa = say + a1 * xx + a3 * xx * yy + a4 * (xx * xx - xScale);	  
                            
	                        if (sa < (-maximum / 2.0)) {
	                            sa = -maximum / 2.0;
	                        } else if (sa > (maximum / 2.0)) {
	                            sa = maximum / 2.0;
	                        }
	                        
                            idealBuffer[i] = buffer[i] - sa;
                            saImgVal[i] = sa;

	                    } // for (x = 0; x < xDim; x += pixelIncrement)
	                } // for (y = 0; y < yDim; y += pixelIncrement)
	            }
	                
        		 for (y = 0; y < yDim; y ++) {
                     j = y * xDim;
                     for (x = 0; x < xDim; x ++) {
                    	  i = j + x;
                    	 if(mask[i]==0){
                    		 InCellNum++;
                    	 }else{
                    		 InBcgNum++;
                    	 }	
                     }
        		 }
        	
	        	inCellVal = new double[InCellNum];
                inBcgVal = new double[InBcgNum];  
                for (y = 0; y < yDim; y ++) {
                    j = y * xDim;

                    for (x = 0; x < xDim; x ++) {
                    	i = j + x;
                    	if(mask[i]==0){
                    		inCellVal[m] = idealBuffer[i];
                    		m++;
                    	}else{
                    		inBcgVal[n] = idealBuffer[i];
                    		n++;
                    	}
                    	
                    }
                }
                
	            Statistics imgInBcgStatis = new Statistics(inBcgVal);
	            stdInBcg = imgInBcgStatis.getStdDev();
	            meanInBcg = imgInBcgStatis.getMean();
	            
	            Statistics imgInCellStatis = new Statistics(inCellVal);
	            stdInCell = imgInCellStatis.getStdDev();
	            meanInCell = imgInCellStatis.getMean();
	            
	            cjv = (double) (stdInBcg + stdInCell)/Math.abs(meanInBcg - meanInCell);
	            
		        if(cjv!=Double.NaN){
		        	System.out.println("cjv Value"+ ":" + cjv);
	            }
		        return cjv; 
	 }
		 
		 private void run2DMulAdd() {
		        int i;
		        double[] p = new double[nParams];
		        double[][] xi = new double[nParams][nParams];
		        double a1, a2, a3, a4, a5;
		        double m1, m2, m3, m4, m5;
		        int x;
		        int y;
		        int xx;
		        int yy;
		        int j;
		        double say;
		        double smy;
		        double sa;
		        double sm;
		        
//		        Change here: class ImageProcessor different from AlgorithmEntropyMinimization
		        srcImage = imp.getProcessor();
		        xDim = srcImage.getWidth();
		        yDim = srcImage.getHeight();
		        corrFloatImage = new FloatProcessor(xDim, yDim);
		        corrImage = srcImage.createProcessor(xDim, yDim);
		        sliceSize = xDim * yDim;
		        xcenter = xDim/2;
		        ycenter = yDim/2;
		        
		        buffer = new double[sliceSize];
	            idealBuffer = new double[sliceSize];
		        yScale = (double) ((yDim * yDim)/12);
		        xScale = (double) ((xDim * xDim)/12);
		        
		        smImgVal = new double[sliceSize];
		        saImgVal = new double[sliceSize];

                for (y = 0; y < yDim; y++) {
                    j = y * xDim;

                    for (x = 0; x < xDim; x++) {
                        i = j + x;

                        buffer[i] = srcImage.getPixel(x, y);
                    } // for (x = 0; x < xDim; x++)
                } // for (y = 0; y < yDim; y++)

                for (i = 0; i < nParams; i++) {
                    xi[i][i] = 1.0;
                }

                powell(p, xi, powellTolerance);
                
                System.out.println("Optimized parameters:");
                for(double k:p){
                	System.out.print(k + ",");
                }                
                a1 = p[0];
                a2 = p[1];
                a3 = p[2];
                a4 = p[3];
                a5 = p[4];               
                m1 = p[5];
                m2 = p[6];
                m3 = p[7];
                m4 = p[8];
                m5 = p[9];
                              
                for (y = 0; y < yDim; y++) {
                    j = y * xDim;
                    yy = y - ycenter; 
                    say = a2 * yy  + a5 * (yy*yy - yScale) ;
	                smy = 1.0 + m2 * yy + m5 * (yy*yy - yScale);
                    
                    for (x = 0; x < xDim; x++) {
                        i = j + x;
                        xx = x - xcenter;
                        sa = say + a1 * xx + a3 * xx* yy + a4 * (xx * xx - xScale) ;
                        sm = smy + m1 * xx + m3 * xx* yy + m4 * (xx * xx - xScale);
                                            
                        if (sa < (-maximum / 2.0)) {
                            sa = -maximum / 2.0;
                        } else if (sa > (maximum / 2.0)) {
                            sa = maximum / 2.0;
                        }
                        
                        if (sm < 0.05) {
                            sm = 0.05;
                        } else if (sm > 20.0) {
                            sm = 20.0;
                        }
                        
                       saImgVal[i] = sa;
                       smImgVal[i] = sm;
                       idealBuffer[i] = (buffer[i] - sa) / sm;
                       idealBuffer[i] = clampImgValRange(imageType, idealBuffer[i]);
                       corrFloatImage.setf(i, (float)idealBuffer[i]);
                       corrImage.putPixelValue(x, y, idealBuffer[i]);
                    }
                    
                    
        }
                System.out.println("***************");
                Statistics saImgStatis = new Statistics(saImgVal);
                double minSaImg = saImgStatis.getMin();
                double maxSaImg = saImgStatis.getMax();
                System.out.print("MinSaImgVal:" + minSaImg + "MaxSaImgVal: "+ maxSaImg);
                
                System.out.println("***************");
                Statistics smImgStatis = new Statistics(smImgVal);
                double minSmImg = smImgStatis.getMin();
                double maxSmImg = smImgStatis.getMax();
                System.out.print("MinSmImgVal:" + minSmImg + "MaxSmImgVal: "+ maxSmImg);
                
                System.out.println("***************");
                ImagePlus corFloatImgImp = new ImagePlus("Corrected FloatImage", corrFloatImage);
                ImageStatistics corrFloatImgStatis  = corFloatImgImp.getStatistics();
                double minCorrFloatImg = corrFloatImgStatis.min;
                double maxCorrFloatImg = corrFloatImgStatis.max;
                System.out.print("MinCorrFloatImgVal:" + minCorrFloatImg + "MaxCorrFloatImgVal: "+ maxCorrFloatImg);
                
                System.out.println("***************");
                ImagePlus corImgImp = new ImagePlus("Corrected Image", corrImage);
                ImageStatistics corrImgStatis  = corImgImp.getStatistics();
                double minCorrImg = corrImgStatis.min;
                double maxCorrImg = corrImgStatis.max;
                System.out.print("MinCorrImgVal:" + minCorrImg + "MaxCorrImgVal: "+ maxCorrImg);
	 
		 }
		 
		 private void run2DMul() {
		        int i;
		        double[] p = new double[nParams];
		        double[][] xi = new double[nParams][nParams];
		        double m1, m2, m3, m4, m5;
		        int x;
		        int y;
		        int xx;
		        int yy;
		        int j;
		        double smy;
		        double sm;
		        
//		        Change here: class ImageProcessor different from AlgorithmEntropyMinimization
		        srcImage = imp.getProcessor();		        
		        xDim = srcImage.getWidth();
		        yDim = srcImage.getHeight();
		        corrImage = srcImage.createProcessor(xDim, yDim);
		        corrFloatImage = new FloatProcessor(xDim, yDim);
		        smBcgImage = new FloatProcessor(xDim, yDim);
		        
		        sliceSize = xDim * yDim;
		        xcenter = xDim/2;
		        ycenter = yDim/2;
    	        buffer = new double[sliceSize];
	            idealBuffer = new double[sliceSize];
		        yScale = (double) ((yDim * yDim)/12);
		        xScale = (double) ((xDim * xDim)/12);
		        smImgVal = new double[sliceSize];

                for (y = 0; y < yDim; y++) {
                    j = y * xDim;

                    for (x = 0; x < xDim; x++) {
                        i = j + x;
                        buffer[i] = srcImage.getPixel(x, y);
                    } // for (x = 0; x < xDim; x++)
                } // for (y = 0; y < yDim; y++)

                for (i = 0; i < nParams; i++) {
                    xi[i][i] = 1.0;
                }

                powell(p, xi, powellTolerance);
                
                System.out.println("Optimized parameters:");
                for(double k:p){
                	System.out.print(k + ",");
                }
                m1 = p[0];
                m2 = p[1];
                m3 = p[2];
                m4 = p[3];
                m5 = p[4];
                
                for (y = 0; y < yDim; y++) {
                    j = y * xDim;
                    yy = y - ycenter;
                    smy = 1.0 + m2 * yy + m5 * (yy * yy - yScale) ;
                    
                    for (x = 0; x < xDim; x++) {
                        i = j + x;
                        xx = x - xcenter;
                        sm = smy + m1 * xx + m3 * xx * yy + m4 * (xx * xx - xScale) ;                       
                        
                        if (sm < 0.05) {
                            sm = 0.05;
                        } 
                        else if (sm > 20.0) {
                            sm = 20.0;
                        }

                       idealBuffer[i] = (double) (buffer[i] / sm);
                       smImgVal[i] = sm;

                       idealBuffer[i] = clampImgValRange(imageType, idealBuffer[i]);
                       corrFloatImage.setf(i, (float) idealBuffer[i]);
                       corrImage.putPixelValue(x, y, idealBuffer[i]);
                       smBcgImage.setf(i, (float) smImgVal[i]);
                    }       
                
     }
                System.out.println("***************");
                Statistics smImgStatis = new Statistics(smImgVal);
                double minSmImg = smImgStatis.getMin();
                double maxSmImg = smImgStatis.getMax();
                System.out.print("MinSmImgVal:" + minSmImg + "MaxSmImgVal: "+ maxSmImg);
                
                System.out.println("***************");
                ImagePlus corFloatImgImp = new ImagePlus("Corrected FloatImage", corrFloatImage);
                ImageStatistics corrFloatImgStatis  = corFloatImgImp.getStatistics();
                double minCorrFloatImg = corrFloatImgStatis.min;
                double maxCorrFloatImg = corrFloatImgStatis.max;
                System.out.print("MinCorrFloatImgVal:" + minCorrFloatImg + "MaxCorrFloatImgVal: "+ maxCorrFloatImg);
                
                System.out.println("***************");
                ImagePlus corImgImp = new ImagePlus("Corrected Image", corrImage);
                ImageStatistics corrImgStatis  = corImgImp.getStatistics();
                double minCorrImg = corrImgStatis.min;
                double maxCorrImg = corrImgStatis.max;
                System.out.print("MinCorrImgVal:" + minCorrImg + "MaxCorrImgVal: "+ maxCorrImg);
		 }
	
		 private void run2DAdd() {
		        int i;
		        double[] p = new double[nParams];
		        double[][] xi = new double[nParams][nParams];
		        double a1, a2, a3, a4, a5;
		        int x;
		        int y;
		        int xx;
		        int yy;
		        int j;
		        double say;
		        double sa;
		        
//		        Change here: class ImageProcessor different from AlgorithmEntropyMinimization
		        srcImage = imp.getProcessor();		        
		        xDim = srcImage.getWidth();
		        yDim = srcImage.getHeight();
		        xcenter = xDim/2;
		        ycenter = yDim/2;
		        corrImage = srcImage.createProcessor(xDim, yDim);
		        corrFloatImage = new FloatProcessor(xDim, yDim);
		        saBcgImage = new FloatProcessor(xDim, yDim);
		        
		        sliceSize = xDim * yDim;
		        buffer = new double[sliceSize];
	            idealBuffer = new double[sliceSize];
	            saImgVal = new double[sliceSize];
		        yScale = (double) ((yDim * yDim)/12);
		        xScale = (double) ((xDim * xDim)/12);

             for (y = 0; y < yDim; y++) {
                 j = y * xDim;

                 for (x = 0; x < xDim; x++) {
                     i = j + x;

                     buffer[i] = srcImage.getPixel(x, y);
                 } // for (x = 0; x < xDim; x++)
             } // for (y = 0; y < yDim; y++)

             for (i = 0; i < nParams; i++) {
                 xi[i][i] = 1.0;
             }

             powell(p, xi, powellTolerance);
             a1 = p[0];
             a2 = p[1];
             a3 = p[2];
             a4 = p[3];
             a5 = p[4];             
             
             for (y = 0; y < yDim; y++) {
                 j = y * xDim;
                 yy = y - ycenter;
                 say = a2 * yy  + a5 * (yy * yy - yScale) ;
                 
                 for (x = 0; x < xDim; x++) {
                     i = j + x;
                     xx = x - xcenter;
                     sa = say + a1 * xx + a3 * xx * yy + a4 * (xx * xx - xScale) ;                    
                     
                     if (sa < (-maximum / 2.0)) {
                         sa = -maximum / 2.0;
                     } else if (sa > (maximum / 2.0)) {
                         sa = maximum / 2.0;
                     }

                    idealBuffer[i] = (float) (buffer[i] - sa);
                    idealBuffer[i] = clampImgValRange(imageType, idealBuffer[i]);
                    saImgVal[i] = sa;
                    corrFloatImage.setf(i, (float) idealBuffer[i]);
                    corrImage.putPixelValue(x, y, idealBuffer[i]);
                    saBcgImage.setf(i, (float) saImgVal[i]);                   
                 }
                 
     }
             System.out.println("***************");
             Statistics smImgStatis = new Statistics(saImgVal);
             double minSaImg = smImgStatis.getMin();
             double maxSaImg = smImgStatis.getMax();
             System.out.print("MinSaImgVal:" + minSaImg + "MaxSaImgVal: "+ maxSaImg);
             
             System.out.println("***************");
             ImagePlus corFloatImgImp = new ImagePlus("Corrected FloatImage", corrFloatImage);
             ImageStatistics corrFloatImgStatis  = corFloatImgImp.getStatistics();
             double minCorrFloatImg = corrFloatImgStatis.min;
             double maxCorrFloatImg = corrFloatImgStatis.max;
             System.out.print("MinCorrFloatImgVal:" + minCorrFloatImg + "MaxCorrFloatImgVal: "+ maxCorrFloatImg);
             
             System.out.println("***************");
             ImagePlus corImgImp = new ImagePlus("Corrected Image", corrImage);
             ImageStatistics corrImgStatis  = corImgImp.getStatistics();
             double minCorrImg = corrImgStatis.min;
             double maxCorrImg = corrImgStatis.max;
             System.out.print("MinCorrImgVal:" + minCorrImg + "MaxCorrImgVal: "+ maxCorrImg);
}
		 
//	 Minimization optimization based on Powell's algorithm
		 
	 private void powell(double[] p, double[][] xi, double ftol) {
	        int ITMAX = 2000000000;
	        Powell.search( p, xi, ftol, this, ITMAX, null );
	    }
	 
//	 Implementing methods in RealFunctionOfSeveralVariables
	 @Override
	    public double eval(double[] x) {
	        return cjvFunction( x );
	    }


	    @Override
	    public int getNumberOfVariables() {
	        return nParams;
	    }
	    
//		 Bounding the image to the specified range
	    public double clampImgValRange(int imgType, double imgVal){
	    	switch (imgType) {
            case 8: 
           	 if (imgVal < 0.0f) {
           		imgVal = 0.0f;
                } else if (imgVal > 255.0f) {
                	imgVal = 255.0f;
                }

                break;
            case 16:
           	 if (imgVal < 0.0f) {
           		imgVal = 0.0f;
                } else if (imgVal > 65535.0f) {
                	imgVal = 65535.0f;
                }
           	 
           	 break;
           	 
            }
	    	
	    	return imgVal;
	    }

	 }
