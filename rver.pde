final int FPS = 30;
Config config;
Grid grid;      // Grid: drawing ACN in a grid
ArrayList<ACN> acn_listeners;

PFont header_font;
boolean fakeRXmode = false;
long lastFakeRX = 0;

final int HEADER_X = Grid.G_OFFSET_X;
final int HEADER_Y = 5;
final int TITLE_Y = 40;
int aliveAlpha;
int ALIVE_FILL = 255;
int cell_stride = 1;

// RVIP
final int PANELS_PER_SECTION = 4;
final int ROWS_PER_PANEL = 5;
final int PIXELS_PER_ROW = 20;
final int SECTIONS = 2;
final int TOTAL_PANELS = PANELS_PER_SECTION * SECTIONS;

int ledSize = 0;
Panel[] Panels = new Panel[TOTAL_PANELS];

final int PANEL1_Y = 0;
final int PANEL2_Y = 10;
int[][] panelInfo = {
    // universe, upperleft x, y in pixels
    {1, 0*PIXELS_PER_ROW, PANEL1_Y}, {2, 1*PIXELS_PER_ROW, PANEL1_Y},
    {3, 2*PIXELS_PER_ROW, PANEL1_Y}, {4, 3*PIXELS_PER_ROW, PANEL1_Y},
    {5, 0*PIXELS_PER_ROW, PANEL2_Y}, {6, 1*PIXELS_PER_ROW, PANEL2_Y},
    {7, 2*PIXELS_PER_ROW, PANEL2_Y}, {8, 3*PIXELS_PER_ROW, PANEL2_Y}
};

final int MARGIN_X = 20;
final int MARGIN_Y = 20;

public void setup(){

    config = new Config(this);
    size(config.width(),config.height());
    ledSize = (int) (config.width()-2*MARGIN_X) / (PIXELS_PER_ROW * PANELS_PER_SECTION);

    frameRate(FPS);
    grid = new Grid(this,config);

    Panel.setApp(this);

    acn_listeners=new ArrayList<ACN>();

    // Configure each panel
    for(int i = 0; i < Panels.length; i++){
        int[] info = panelInfo[i];
        int universe = info[0];
        ACN acn = new ACN(universe,config);
        acn_listeners.add(acn);
        Panels[i] = new Panel(universe, info[1], info[2], ledSize, acn);
    }

    ellipseMode(CENTER);
    rectMode(CENTER);

    header_font = loadFont("Helvetica-24.vlw");
}

private void drawTopHeader(){
   fill(255);
   textAlign(LEFT);

   pushMatrix();
   translate(HEADER_X,HEADER_Y);
   // draw actve boxes
   for(ACN a : acn_listeners){
       noStroke();
       if(a.active()){
           fill(128,128,0,a.aliveCount > 0 ? 255 : 0); // yellow instead of white
       }else{
           fill(32);
       }
       rect(0,0,32,10);
       translate(40,0);
   }
   popMatrix();

   pushMatrix();
   translate(HEADER_X,TITLE_Y);
   textFont(header_font,24);
   popMatrix();
}

public void update(){

    // Faking it?
    if(fakeRXmode && (millis() - lastFakeRX) > 50){
        for(ACN acn : acn_listeners){
            acn.fakeRx();
        }
        lastFakeRX=millis();
    }

}

public void draw(){
    background(0);
    update();

    pushMatrix();
    translate(MARGIN_X,MARGIN_Y);
    // drawTopHeader();

    for(int i = 0; i < Panels.length; i++){
        Panels[i].draw();
    }
    popMatrix();
}

boolean shift_pressed=false;

public void keyPressed(){


    switch(key){
        case '_':
        case '-': grid.cellRgbPhase--; break;
        case '=':
        case '+': grid.cellRgbPhase++; break;

        case 'F': fakeRXmode = !fakeRXmode; break;
        case 'O': config.load(); break;
        case 'S': config.save(); break;
        case 'l':
        case 'L': grid.drawAllCellNumbers = !grid.drawAllCellNumbers; break;
        case 'V': grid.toggleViewMode(); break;
    };
}
