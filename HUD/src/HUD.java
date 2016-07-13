/**
 * RPITank
 * Created by MAWood on 13/07/2016.
 */

import com.sun.j3d.utils.universe.SimpleUniverse;

import com.sun.j3d.utils.geometry.ColorCube;
import javax.media.j3d.BranchGroup;

public class HUD {

    public HUD()

    {
        SimpleUniverse universe = new SimpleUniverse();

        BranchGroup group = new BranchGroup();

        group.addChild(new ColorCube(0.3));

        universe.getViewingPlatform().setNominalViewingTransform();

        universe.addBranchGraph(group);
    }

    public static void main( String[] args ) {

        new HUD();

    }

} // end of class Hello3d