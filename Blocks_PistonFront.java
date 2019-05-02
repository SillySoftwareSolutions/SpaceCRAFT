import geom.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.io.ObjectStreamException;
public class Blocks_PistonFront extends SBlock
{
    public Blocks_PistonFront(){
        super("Piston Front", "blocks_pistonFront");
        
    }
    
    
    public boolean onPlace(Player p, VektorI pos){
        return false;
    }
    
    public void onConstruct(int dir){
       
    }
    

    public boolean onBreak(Player p){
        return false;  // kann nicht abgebaut werden!!
    }
}
