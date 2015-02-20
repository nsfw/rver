import processing.core.PApplet;
import processing.core.PFont;
import java.awt.Color;
import java.util.*;

class Grid{
    // external control
    public boolean drawRGBcells = false; // draw all as RGB? or just hover
    public boolean drawAllCellNumbers = false;
    public int cellRgbPhase = 0;

    final static int G_OFFSET_X = 32;
    final static int G_OFFSET_Y = 65;
    final static int GRID_STROKE = 64;
    final int LABEL_NUDGE = 5;
    final int CELL_TEXT_FILL = 128;
    final int CELL_STROKE = 200;
    final int CELL_CHANGE_FILL = 32;
    int start_cell = 1; // skip the start code
    final int MAX_CELLS=513;

    int num_cells = 512;
    int cell_stride=1;
    int mousecell;
    PFont label_font;

    ACNFrame previous;  // detect changes
    int[] change_counters;
    Color[] all_cells_rgb;
    Color cell_rgb;

    enum ViewMode{EACH_SLOT,PIXELS}; // pixel means display 170 slots only
    enum DrawMode{CIRCLES,BARS,SPARKLINES};

    PApplet app;
    Config config;
    int numcols,numrows,maxcols;
    int gw,gh,gh_2,gw_2; //size of a cell
    DrawMode cell_mode;
    ViewMode view_mode;

    // RVIP
    final int PANELS_PER_ROW = 4;
    final int ROWS_PER_PANEL = 5;
    final int PIXELS_PER_ROW = 20;
    final int TOTAL_PANELS = PANELS_PER_ROW * 2;

    Grid(PApplet _app,Config _config){
        app = _app;
        config=_config;
        change_counters=new int[MAX_CELLS];
        all_cells_rgb = new Color[MAX_CELLS];

        Arrays.fill(change_counters,255); //start not changed
        reconfig();
        label_font=app.loadFont("Consolas-Bold-12.vlw");
    }

    void reconfig(){
        start_cell = config.getBoolean("acn-include-startcode",false) ? 0 : 1;

        // hard coded for RV
        numcols = PANELS_PER_ROW*PIXELS_PER_ROW;
        numrows = ROWS_PER_PANEL;

        num_cells = config.getInt("acn-num-slots",512);
        maxcols = config.getInt("grid-num-cols",32);
        maxcols = app.constrain(maxcols,1,num_cells);

        drawRGBcells = config.getBoolean("draw-all-colors",true);

        String cm = config.getString("cell-mode");
        if(cm!=null && cm.contains("bar"))
            setCellMode(DrawMode.BARS);
        else
            setCellMode(DrawMode.CIRCLES);

        String vm = config.getString("view-mode");

        if(vm!=null && vm.contains("pixel"))
            setViewMode(ViewMode.PIXELS);
        else
            setViewMode(ViewMode.EACH_SLOT);

        resize();
    }

    void toggleViewMode(){
        if(view_mode == ViewMode.PIXELS)
            setViewMode(ViewMode.EACH_SLOT);
        else
            setViewMode(ViewMode.PIXELS);
    }

    void setViewMode(ViewMode m){
        view_mode=m;
        System.out.println("View Mode: "+view_mode);
        resize();

    }

    void setCellMode(DrawMode m){
        cell_mode=m;
        System.out.println("Cell Mode: "+cell_mode);
    }

    void resize(){

        gw = (app.width-2*G_OFFSET_X) / numcols;
        gh = (app.height-2*G_OFFSET_Y) / numrows;
        gh_2 = gh/2;
        gw_2 = gw/2;


    }


    private boolean rgbInRange(int root, int cell,int dist){
        return root >=start_cell && root <=(num_cells+start_cell-dist) && cell>=root && cell < (root + dist);
    }

    // where to get an RGB triplet rooted at. ie 1,2,3 all use cell 1 for the R in RGB unless there is phase offset
    private int cellRoot(int cell){
        if(cell <= cellRgbPhase)
            return -1; //out of range
        int the_cell = cell-cellRgbPhase;
        return the_cell-((the_cell-1)%3)+cellRgbPhase;
    }
    // We are TRANSLATED so draw from 0,0
    private void drawCell(ACNFrame frame,int cell){

        try{
            int val = frame.get(cell);
            if( previous != null && previous.get(cell) !=val)
                change_counters[cell] = 0;
            else
                change_counters[cell]++; // timeout..

            boolean changed = change_counters[cell] < 10;

            // color the background?
            app.rectMode(PApplet.CORNER);

            // either draw ALL colors, or just the hover one
            if(drawRGBcells ){

                int color_index = cellRoot(cell);

                app.fill(cellColor(color_index).getRGB());
                app.rect(0,0,gw,gh);
                app.stroke(200);
                app.line(0,gh-1,gw-1,gh-1);//little cue line that wont get over-written by the grid

            }else if(rgbInRange(mousecell,cell,view_mode==ViewMode.PIXELS ? 1 : 3)){
                // mouse hover one triplet
                app.fill(cellColor(mousecell).getRGB());
                app.rect(0,0,gw,gh);
                app.stroke(200);
                app.line(0,gh-1,gw-1,gh-1);//little cue line that wont get over-written by the grid
            }else if(changed){
                app.fill(CELL_CHANGE_FILL);//subtle
                app.rect(0,0,gw,gh);
            }


            if(cell_mode == DrawMode.CIRCLES)
                drawCell_Circle(val);
            else if(cell_mode == DrawMode.CIRCLES)
                drawCell_Bars(val);

            app.fill(CELL_TEXT_FILL);
            app.textAlign(app.LEFT);
            if(drawAllCellNumbers)
                app.text(""+cell,LABEL_NUDGE,10); // sometimes label every cell index
            else if(drawRGBcells)
                app.text(cellRoot(cell),LABEL_NUDGE,10); // where is its color from?

            String label = ""+val;

            app.text(label,LABEL_NUDGE,gh-5);
        }catch(Exception e){
            System.err.println("Error drawing cell # "+cell);
        }
    }

    private void drawCell_Circle(int val){

        float val_size = app.map(val,0,255,8,gh/3);
        app.fill(200,val);
        app.stroke(CELL_STROKE);
        app.ellipse(gw_2,gh_2,val_size,val_size);

    }

    private void drawCell_Bars(int val){
        float val_size = app.map(val,0,255,8,gh_2);
        app.fill(200,128);
        app.rect(gw_2,gh_2,gw_2,val_size);
    }


    // General drawing helpers
    private int columnX(int col){
        return G_OFFSET_X+col*gw;
    }
    private int rowY(int row){
        return G_OFFSET_Y+row*gh;
    }


    private void rowLine(int row){
        app.stroke(GRID_STROKE);
        int y = rowY(row);
        app.line(G_OFFSET_X,y,app.width-G_OFFSET_X,y); // make a ROW

    }
    private void columnLine(int col){
        app.stroke(GRID_STROKE);
        int x = columnX(col);
        app.line(x,G_OFFSET_Y,x,app.height-G_OFFSET_Y); // make a ROW
    }

    private void rowLabel(int row){
        app.fill(CELL_TEXT_FILL);
        app.textAlign(PApplet.RIGHT);
        int y = rowY(row)+gh_2;
        app.text(""+(row*numcols+start_cell),G_OFFSET_X-LABEL_NUDGE,y); // each label is the current cell in column 0
    }
    private void columnLabel(int col){
        app.fill(CELL_TEXT_FILL);
        app.textAlign(PApplet.CENTER);
        int x = columnX(col)+gw_2;
        app.text(""+(col+start_cell),x,G_OFFSET_Y-LABEL_NUDGE);
    }

    private int gridSlotFromMouse(int mx,int my){
        if(mx < G_OFFSET_X || mx > app.width-G_OFFSET_X || my < G_OFFSET_Y || my > app.height-G_OFFSET_Y)
            return 0;
        int c =  start_cell+numcols*((my-G_OFFSET_Y)/gh)+(mx-G_OFFSET_X)/gw; // top left will be '1' if start_cell == 1
        // c is based on the full grid.
        // if a stride is set, must adjust
        if(cell_stride!=1)
            c = (c-1)*cell_stride+1;
        return c;
    }

    private void debugMouse(){
        app.fill(255,0,0);

        int col =  mousecell % numcols;
        int row = (int)(app.floor(mousecell / (float)numcols));

        int cellx = columnX(col);
        int celly = rowY(row);
        app.rectMode(PApplet.CORNER);
        app.rect(cellx,celly,gw*3,gh);
    }
    private void extractCellColors(ACNFrame frame){
        for(int i=0; i < MAX_CELLS; i++){
            if(i<frame.data.length)
                all_cells_rgb[i] = getCellRGB(frame,i);
            else
                all_cells_rgb[i] = Color.BLACK;
        }

    }
    private Color getCellRGB(ACNFrame frame,int cell){
        // if(cell < start_cell || cell > num_cells-3){
        if(!rgbInRange(cell,cell,3)){
            //   System.out.println("out of ranve "+cell);
            return Color.GRAY; // too close to edges
        }else{
            int r=frame.get(cell);
            int g=frame.get(cell+1);
            int b=frame.get(cell+2);
            return new Color(r,g,b);
        }

    }
    private String mouseStatus(int cell){
        // cell is a DMX slot so when in pixel mode it won't be a 'pixel number'. ie pixel 170 is cell 508
        Color c = cellColor(cell);
        return (String.format("%d RGB = %d,%d,%d",cell,c.getRed(),c.getGreen(),c.getBlue()));
    }

    private Color cellColor(int cell){
        if(cell >= all_cells_rgb.length)
            return Color.GRAY;

        Color c =  all_cells_rgb[cell];
        return (c==null) ? Color.GRAY : c;
    }

    private void drawSlots(ACNFrame frame){


    }

    private void drawPixels(ACNFrame frame){

    }

    public void draw(ACNFrame frame,int mouse_x,int mouse_y){
        String status="";
        cellRgbPhase=cellRgbPhase%3; // restrict
        mousecell = gridSlotFromMouse(mouse_x,mouse_y);   // respects cell_stride

        if(previous == null || frame.seq != previous.seq) // this is NEW
            extractCellColors(frame);
        int row_so_far=0;
        status+=mouseStatus(mousecell);
        status+=" RGB offset +"+cellRgbPhase;
        if((mousecell-1-cellRgbPhase) % 3 ==0)
            status+=(" Pixel # "+(1+((mousecell-cellRgbPhase)/3)));
        status+=("    raw packet size "+frame.rawSize+" bytes");
        status+=("  Start code="+frame.startCode());
        app.textFont(label_font,12);

        int x=G_OFFSET_X, y=G_OFFSET_Y;
        int cell=start_cell;

        for(int r=0; r < numrows; r++){
            rowLine(r);
            rowLabel(r);
            row_so_far=0;
            for(int c=0; c < numcols; c++){
                columnLine(c);
                columnLabel(c);
                app.pushMatrix();
                app.translate(columnX(c),rowY(r));
                drawCell(frame,cell);
                app.popMatrix();
                x+=gw;
                cell+=cell_stride;
                // row_so_far+=cell_stride;
                if(cell>num_cells ||row_so_far > numcols)
                    break;

            }// END OF COLUMN
            x=G_OFFSET_X;
            y+=gh;
            if(cell>num_cells)
                break;

        }//END OF ROW

        // close the box
        rowLine(numrows);
        columnLine(numcols);
        // debug on the bottom
        app.fill(CELL_TEXT_FILL);
        app.text(status,5,app.height-12);

        if(previous == null || frame.seq != previous.seq)
            previous=frame;
    }
}
