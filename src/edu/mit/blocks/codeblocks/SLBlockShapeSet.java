package edu.mit.blocks.codeblocks;

import java.awt.geom.Point2D;
import edu.mit.blocks.codeblocks.CustomBlockShapeSet;


/**
 * A CustomBlockShapeSet is a set of custom specified block shapes that override the default
 * shape given to a block.  These shapes are specified by four points which make up the corners of
 * the block.  The possible shapes are decendents of trapeziums -- more specifically parallelograms,
 * trapazoids, and rectangles.
 */


public class SLBlockShapeSet extends CustomBlockShapeSet {
	
	/**
	 * Add CustomBlockShapes upon construction.
	 */
	public SLBlockShapeSet() {
	
		//add CustomBlockShape using method from super
		addCustomBlockShape(new ProcedureBlock());
	}
	
	
	
	/*
	 * Example CustomBlockShape with four points specified.  One of the left side points must
	 * have an x-coordinate with value 0F.
	 */
	class ProcedureBlock extends CustomBlockShape {
		public ProcedureBlock(){
			genusName = "procedure";
			topLeftCorner = new Point2D.Float(0F,0F); topRightCorner = new Point2D.Float(60F,0F);
            //TODO warning: for some if you set the y-coordinate of the bottom points to about 10F
            //slcodeblocks crashes randomly whenever you drag out a procedure block.  it crashes right at 
            //RenderableBlock.updateBuffImg(), where we determine the bevel image size...
			botLeftCorner = new Point2D.Float(10F,16F); botRightCorner = new Point2D.Float(50F,16F);
		}
	}
}


	


