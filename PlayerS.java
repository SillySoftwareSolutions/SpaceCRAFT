import java.awt.Graphics;
import java.awt.Color;
import geom.*;
import java.io.Serializable;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
/**
 * ein Spieler in der Space Ansicht
 */
public class PlayerS implements Serializable
{
    private Player player;
    private VektorD posToMass;
    private double scale=1; //eine Einheit im Space => scale Pixel auf dem Frame
    private Mass focussedMass;
    private VektorI lastDragPosition = null;
    
    public PlayerS(Player player, VektorD pos, Mass focussedMass)
    {
        this.player = player;
        this.posToMass=pos;
        this.focussedMass = focussedMass;
    }
    
    public PlayerS(Player player, VektorD pos)
    {
        this(player, pos, null);
    }
    
    /**
     * Tastatur event
     * @param:
     *  char type: 'p': pressed
     *             'r': released
     *             't': typed (nur Unicode Buchstaben)
     */
    public void keyEvent(KeyEvent e, char type) {
        
    }

    /**
     * Maus Event
     * @param:
     *  char type: 'p': pressed
     *             'r': released
     *             'c': clicked
     *             'd': dragged
     * entered und exited wurde nicht implementiert, weil es dafür bisher keine Verwendung gab
     */
    public void mouseEvent(MouseEvent e, char type) {
        switch(type){
            case 'd': 
                if (lastDragPosition != null){
                    VektorI thisDragPosition = new VektorI(e.getX(), e.getY());
                    VektorD diff = lastDragPosition.subtract(thisDragPosition).toDouble().multiply(1/scale);
                    diff.y = -diff.y;   // die Y Achse ist umgedreht
                    this.posToMass = posToMass.add(diff);
                }
                lastDragPosition = new VektorI(e.getX(), e.getY());
            case 'p': lastDragPosition = new VektorI(e.getX(), e.getY());
                break;
            case 'r': lastDragPosition = null;
                break;
        }
    }   
    
    public void mouseWheelMoved(MouseWheelEvent e){
        int amountOfClicks = e.getWheelRotation();
        scale = scale * Math.pow(2,amountOfClicks);
        if (scale == 0)scale = 1;
    }
    
    public VektorD getPosToNull(){
        if(focussedMass == null){
            return posToMass;
        }
        else{
            return posToMass.add(focussedMass.pos.toDouble());
        }
    }
    
    public void setFocussedMass(Mass focussedMass){
        this.focussedMass = focussedMass;
    }
    
    /**
     * Grafik ausgeben
     */
    public void paint(Graphics g, VektorI screenSize){
        VektorD posToNull = getPosToNull();
        
        g.setColor(Color.BLACK);
        g.fillRect(0,0,screenSize.x,screenSize.y); // nice
        Space sp=player.getSpace();
        for (int i=0;i<sp.masses.size();i++){
            if (sp.masses.get(i)!=null){
                for (int j=1;j<sp.masses.get(i).o.pos.size();j++){  // ernsthaft?
                    VektorD posDiff1=sp.masses.get(i).o.pos.get(j-1).toDouble().subtract(posToNull);
                    posDiff1=posDiff1.multiply(scale);
                    VektorD posDiff2=sp.masses.get(i).o.pos.get(j).toDouble().subtract(posToNull);
                    posDiff2=posDiff2.multiply(scale);
                    g.setColor(Color.WHITE);
                    g.drawLine((int) (screenSize.x/2+posDiff1.x),(int) (screenSize.y/2-posDiff1.y),(int) (screenSize.x/2+posDiff2.x),(int) (screenSize.y/2-posDiff2.y));
                }
                VektorD posDiff=sp.masses.get(i).getPos().toDouble().subtract(posToNull);
                posDiff=posDiff.multiply(scale);
                int r=2;
                if (sp.masses.get(i) instanceof PlanetS){
                    r=((PlanetS) sp.masses.get(i)).getRadius();
                }
                r=(int)(r*scale);
                g.setColor(Color.WHITE);
                g.fillArc((int) (screenSize.x/2+posDiff.x-r),(int) (screenSize.y/2-posDiff.y-r),2*r,2*r,0,360);
            }
        }
    }
}