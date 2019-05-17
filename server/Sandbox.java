package server;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Timer;
import util.geom.*;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.ColorModel;
import java.awt.Color;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.ObjectOutputStream;
import java.io.IOException;

import client.ClientSettings;
import blocks.*;
/**
 * Eine virtuelle Umgebung aus Blöcken
 * 
 * @Content:
 *  1. Methoden zum Erstellen der Sandbox
 *  2. Methoden für Blöcke (setBlock(),...)
 *  3. Methoden für Subsandboxes und Raketenstart
 *  4. Methoden für Ansicht und Grafikausgabe
 */
public abstract class Sandbox implements Serializable
{
    public transient Block[][]map;
    public Meta[][]meta;
    // Sandboxen können Sandboxen enthalten (Kompositum). z.B.: Schiff auf Planet
    protected transient ArrayList<Sandbox> subsandboxes = new ArrayList<Sandbox>();
    protected transient Timer spaceTimer; //nur eine Referenz

    /***********************************************************************************************************************************************************
    /*********1. Methoden zum Erstellen der Sandbox*************************************************************************************************************
    /***********************************************************************************************************************************************************

    /**
     * erstellt eine neue Sandbox
     * @param: Vektor size: gibt die größe der Sandbox an (Bereich in dem Blöcke sein können)
     */
    public Sandbox(VektorI size, Timer spaceTimer){
        map = new Block[size.x][size.y];
        meta = new Meta[size.x][size.y];
        this.spaceTimer=spaceTimer;
        this.spaceTimerSetup();
    }

    public Sandbox(Block[][] map, ArrayList<Sandbox> subsandboxes, Timer spaceTimer){
        this.map=map;
        
        this.subsandboxes=subsandboxes;
        this.spaceTimer=spaceTimer;
        this.spaceTimerSetup();
    }

    public void setSpaceTimer(Timer t){
        this.spaceTimer=t;
        this.spaceTimerSetup();
    }

    protected abstract void spaceTimerSetup();
    //Nur hier können neue TimerTasks hinzugefügt werden.

    /**
     * gibt die Größe der Sandbox zurück
     */
    public VektorI getSize(){
        return new VektorI(map.length, map[0].length);
    }

    /**
     * Ersetzt die Map mit einer anderen
     */
    public void setMap(Block[][]map){
        if(map!= null)this.map = map;
    }

    /**
     * Fügt eine Sandbox hinzu
     */
    public void addSandbox(Sandbox sbNeu){
        if(sbNeu!=null)subsandboxes.add(sbNeu);
    }

    /**
     * Löscht eine Sandbox
     */
    public void removeSandbox(Sandbox sbR){
        if(sbR!=null)subsandboxes.remove(sbR);
    }

    public ArrayList<Sandbox> getSubsandboxes(){
        return subsandboxes;
    }

    /***********************************************************************************************************************************************************
    /*********2. Methoden für Blöcke (setBlock(),...)***********************************************************************************************************
    /***********************************************************************************************************************************************************

    /**
     * Rechtsklick auf einen Block in der Welt:
     *  wenn an der Stelle kein Block => plaziert Block
     *  wenn an der Stelle ein Block => führt (wenn möglich) das onRightclick Event im Block aus
     *  
     *  @param:
     *  * VektorI pos: Position des Blocks
     *  * Integer playerID
     * Request-Funktion
     */
    public Boolean rightclickBlock(Integer playerID, Boolean onPlanet, Integer sandboxIndex, VektorI pos){
        Boolean success=new Boolean(false);
        try{
            if (map[pos.x][pos.y] == null){
                placeBlock(Blocks.blocks.get(104), pos, playerID);
            }else{
                ((SBlock)map[pos.x][pos.y]).onRightclick(this, pos, playerID);
                System.out.println("Block at "+pos.toString()+" rightclicked by Player "+playerID+"!");
            }
        }catch(Exception e){ //block außerhalb der Map oder kein Special Block => kein rightclick möglich
        }
        success=new Boolean(true);
        return success;
    }

    /**
     * Linksklick auf einen Block in der Welt
     *  wenn an der Stelle ein Block => baut den Block ab
     *  
     *  @param:
     *  * VektorI pos: Position des Blocks
     *  * Integer playerID
     * Request-Funktion
     */
    public Boolean leftclickBlock(Integer playerID, Boolean onPlanet, Integer sandboxIndex,  VektorI pos){
        Boolean success=new Boolean(false);
        try{
            if (map[pos.x][pos.y] == null){
                return success;
            }else{
                breakBlock(pos, playerID);
                System.out.println("Block at "+pos.toString()+" leftclicked by Player "+playerID+"!");
            }
        }catch(Exception e){ //block außerhalb der Map 
        }
        success=new Boolean(true);
        return success;
    }

    /**
     * Spieler platziert einen Block, aber nur wenn das onPlace Event true zurückgibt
     * 
     * @param:
     *  * Block block: Block der plaziert werden soll
     *  * VektorI pos: Position des Blocks
     *  * int playerID
     */
    public void placeBlock(Block block, VektorI pos, int playerID){
        try{
            if(!((SBlock)block).onPlace(this, pos, playerID))return;  // ruft onPlace auf, wenn es ein Special Block ist. Wenn es nicht erfolgreich plaziert wurde => Abbruch
        }catch(Exception e){} // => kein SpecialBlock => kann immer plaziert werden
        setBlock(block, pos);
        System.out.println("Block at "+pos.toString()+" placed by Player "+playerID+"!");
    }

    /**
     * Ein Block wird ausnahmelos gesetzt. Die Metadaten werden aber überschrieben und das onConstruct Event aufgerufen
     * 
     * @param:
     *  * Block block: Block der gesetzt werden soll
     *  * VektorI pos: Position des Blocks
     */
    public void setBlock(Block block, VektorI pos){
        swapBlock(block, pos);
        removeMeta(pos);
        try{
            ((SBlock)block).onConstruct(this, pos);  // ruft onConstruct auf, wenn es ein Special Block ist. 
        }catch(Exception e){} // => kein SpecialBlock
    }

    /**
     * Ein Block wird ausnahmelos gesetzt. 
     * !!! Die Metadaten bleiben aber erhalten !!!
     * das onConstruct Event wird NICHT aubgerufen  
     * 
     * @param:
     *  * Block block: Block der gesetzt werden soll
     *  * VektorI pos: Position des Blocks
     */
    public void swapBlock(Block block, VektorI pos){
        map[pos.x][pos.y]= block; 
    }

    /**
     * Spieler baut einen Block in die Welt ab, wenn das onBreak() Event true zurückgibt und löscht die Metadaten
     * @param:
     *  * VektorI pos: Position des Blocks
     *  * int playerID
     */
    public void breakBlock(VektorI pos, int playerID){
        if (map[pos.x][pos.y] == null) return;
        try{
            if (((SBlock)map[pos.x][pos.y]).onBreak(this, pos, playerID)){
                breakBlock(pos);
                System.out.println("Block at "+pos.toString()+" breaked by Player "+playerID+"!");
            }
        }catch(Exception e){breakBlock(pos);}
    }

    /**
     * Entfernt einen Block ausnahmelos in der Welt. Entfernt die Metadaten und ruft das onDestruct() Event auf.
     * 
     * @param:
     *  * VektorI pos: Position des Blocks
     */
    public void breakBlock(VektorI pos){
        map[pos.x][pos.y] = null;
        try{
            ((SBlock)map[pos.x][pos.y]).onDestruct(this, pos);
        }catch(Exception e){}
        removeMeta(pos);
    }

    /**
     * Gibt das Block-Object zurück
     */
    public Block getBlock(VektorI pos){
        try{
            return map[pos.x][pos.y];
        }catch(Exception e){ return null; }  // Außerhalb des Map-Arrays
    }

    /**
     * gibt das Metadaten Object zurück
     */
    public Meta getMeta(VektorI pos){
        try{
            return this.meta[pos.x][pos.y];
        }catch(Exception e){ return null; }  // Außerhalb des Map-Arrays
    }

    /**
     * setzt das Metadaten Object
     */
    public void setMeta(VektorI pos, Meta meta){
        try{
            this.meta[pos.x][pos.y] = meta;
        }catch(Exception e){ return; }  // Außerhalb des Map-Arrays
    }

    /**
     * entfernt das Metadaten Object
     */
    public void removeMeta(VektorI pos){
        try{
            this.meta[pos.x][pos.y] = null;
        }catch(Exception e){ return; }  // Außerhalb des Map-Arrays
    }

    /***********************************************************************************************************************************************************
    /*********3. Methoden für Subsandboxes und Raketenstart*****************************************************************************************************
    /***********************************************************************************************************************************************************

    /***********************************************************************************************************************************************************
    /*********4. Methoden für Ansicht und Grafikausgabe*********************************************************************************************************
    /***********************************************************************************************************************************************************

    /**
     * Gibt die obere rechte Ecke (int Blöcken) der Spieleransicht an
     * @param: pos: Position des Spielers relativ zur oberen rechten Ecke der Sandbox
     * 
     * @Benny:
     * Das hat Linus programmiert. Die Bilder aller Blöcke werden zuerst zusammengeführt in ein großes Bild und dann nur dieses Bild "gezeichnet". 
     * Das ist deutlich schneller als jedes Bild einzeln zu zeichen. Bitte setz dich mit Linus (König der Kommentare) in Verbindung um das zu verstehen
     * und zu verbessern. Man kann z.B. zur Zeit nur ganze Koordianten darstellen...
     */
    public VektorD getUpperLeftCorner(VektorD pos){
        return pos.add(ClientSettings.PLAYERC_FIELD_OF_VIEW.toDouble().multiply(-0.5) ).add(new VektorD(0.5,0.5));
    }
    
    /**
     * Request-Funktion
     */
    public int[][] getMapIDs(Integer playerID, Boolean onPlanet, Integer sandboxIndex, VektorI upperLeftCorner, VektorI bottomRightCorner){
        int[][] ret=new int[bottomRightCorner.x-upperLeftCorner.x+1][bottomRightCorner.y-upperLeftCorner.y+1];
        for (int x=upperLeftCorner.x;x<=bottomRightCorner.x;x++){
            for (int y=upperLeftCorner.y;y<=bottomRightCorner.y;y++){
                int i=x-upperLeftCorner.x;
                int j=y-upperLeftCorner.y;
                if (x>=0 && y>=0 && x<map.length && y<map[0].length && map[x][y]!=null)
                    ret[i][j]=map[x][y].getID();
                else
                    ret[i][j]=-1; //Luft
            }
        }
        return ret;
    }
}