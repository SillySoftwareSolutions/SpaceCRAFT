import java.awt.Graphics;
import java.awt.Color;
import geom.*;
import java.io.Serializable;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.*;  // test
/**
 * ein Spieler in der Space Ansicht
 * test
 */
public class PlayerS implements Serializable
{
    private Player player;
    private VektorD posToMass;
    private double scale=0.05; //eine Einheit im Space => scale Pixel auf dem Frame
    private Mass focussedMass;
    private transient VektorI lastDragPosition = null;
    
    private JPopupMenu popupmenu;
    
    public PlayerS(Player player, VektorD pos, Mass focussedMass)
    {
        this.player = player;
        this.posToMass=pos;
        this.focussedMass = focussedMass;
        
        popupmenu = new JPopupMenu("Edit");   
         JMenuItem cut = new JMenuItem("Cut");  
         JMenuItem copy = new JMenuItem("Copy");  
         JMenuItem paste = new JMenuItem("Paste");  
         popupmenu.add(cut); popupmenu.add(copy); popupmenu.add(paste);        
          player.getFrame().add(popupmenu);
        player.getFrame().setLayout(null);
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
        switch(e.getKeyCode()){
            case Shortcuts.space_focus_current_mass: 
                focussedMass = player.getCurrentMass();
                popupmenu.show(player.getFrame() , 300,300);  
                popupmenu.setVisible(true);
                break;
        }
    }

    /**
     * Maus Event
     * @param:
     *  char type: 'p': pressed
     *             'r': released
     *             'c': clicked
     *             'd': dragged
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
                Mass massAtPos = getMassAtPos(new VektorI(e), player.getScreenSize());
                if(massAtPos != null)focussedMass = massAtPos;
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
            return posToMass.add(focussedMass.pos);
        }
    }
    
    public Mass getMassAtPos(VektorI pos, VektorI screenSize){
        Space sp=player.getSpace();
        for (int i=0;i<sp.masses.size();i++){
            if (sp.masses.get(i)!=null){
                VektorD posRel=sp.masses.get(i).getPos().subtract(getPosToNull());
                posRel=posRel.multiply(scale);
                int r=2;
                if (sp.masses.get(i) instanceof PlanetS){
                    r=(int)Math.round(((double)((PlanetS) sp.masses.get(i)).getRadius()));
                }
                r=(int)(r*scale);
                VektorI posPix = screenSize.toDouble().multiply(0.5).add(posRel).toInt();
                int distance = (int)Math.round(posPix.subtract(pos).getLength());
                if (distance < r+20)return sp.masses.get(i);
            }
        }
        return null;
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
        int acuracy = 100;
        for (int i=0;i<sp.masses.size();i++){
            if (sp.masses.get(i)!=null){
                for (int j=acuracy;j<sp.masses.get(i).o.pos.size();j=j+acuracy){  // ernsthaft?
                    VektorD posDiff1=sp.masses.get(i).o.pos.get(j-acuracy).subtract(posToNull);
                    posDiff1=posDiff1.multiply(scale);
                    VektorD posDiff2=sp.masses.get(i).o.pos.get(j).subtract(posToNull);
                    posDiff2=posDiff2.multiply(scale);
                    g.setColor(Color.WHITE);
                    g.drawLine((int) (screenSize.x/2+posDiff1.x),(int) (screenSize.y/2-posDiff1.y),(int) (screenSize.x/2+posDiff2.x),(int) (screenSize.y/2-posDiff2.y));
                }
                VektorD posDiff=sp.masses.get(i).getPos().subtract(posToNull);
                posDiff=posDiff.multiply(scale);
                int r=2;
                if (sp.masses.get(i) instanceof PlanetS){
                    r=((PlanetS) sp.masses.get(i)).getRadius();
                }
                r=(int)(r*scale);
                if (sp.masses.get(i) == player.getCurrentMass())g.setColor(Color.RED);
                else if(sp.masses.get(i) == focussedMass)g.setColor(Color.CYAN);
                else g.setColor(Color.WHITE);
                g.fillArc((int) (screenSize.x/2+posDiff.x-r),(int) (screenSize.y/2-posDiff.y-r),2*r,2*r,0,360);
            }
        }
        
        //popupmenu.paint(g);
        
    }
}