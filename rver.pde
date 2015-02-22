final int FPS = 60;
Config config;

PFont header_font;
boolean fakeRXmode = false;
long lastFakeRX = 0;

ACN_UNI acn;

// RVIP
final int PANELS_PER_SECTION = 4;
final int ROWS_PER_PANEL = 5;
final int PIXELS_PER_ROW = 20;
final int SECTIONS = 2;
final int TOTAL_PANELS = PANELS_PER_SECTION * SECTIONS;

int ledSize = 0;
Panel[] Panels = new Panel[TOTAL_PANELS];

final int PANEL1_Y = 0;
final int PANEL2_Y = 13;
int[][] panelInfo = {
    // universe, upperleft x, y in pixels
    {1, 0*PIXELS_PER_ROW, PANEL1_Y}, {2, 1*PIXELS_PER_ROW, PANEL1_Y},
    {3, 2*PIXELS_PER_ROW, PANEL1_Y}, {4, 3*PIXELS_PER_ROW, PANEL1_Y},
    {5, 0*PIXELS_PER_ROW, PANEL2_Y}, {6, 1*PIXELS_PER_ROW, PANEL2_Y},
    {7, 2*PIXELS_PER_ROW, PANEL2_Y}, {8, 3*PIXELS_PER_ROW, PANEL2_Y}
};

int lx = 0;
int ly = 0;

PImage backgroundImg;

public void setup(){

    config = new Config(this);

    // load background image and scale to configured width
    backgroundImg = loadImage("data/RV_Outline_Light_Side_2015.png");
    backgroundImg.resize(config.width(),0);
    size(config.width(),backgroundImg.height);

    // compute panel position and ledSize relative to background image size
    lx = (int) (0.03 * backgroundImg.width);
    ly = (int) (0.08 * backgroundImg.height);
    int lwidth = (int) ((0.83 - 0.03) * backgroundImg.width);

    ledSize = (int) (lwidth / (PIXELS_PER_ROW * PANELS_PER_SECTION));

    frameRate(FPS);

    Panel.setApp(this);

    acn = new ACN_UNI(1,8,ACNFrame.ACN_PORT);    // accept sACN universes 1-8 on this port

    // Configure each panel
    for(int i = 0; i < Panels.length; i++){
        int[] info = panelInfo[i];
        int universe = info[0];
        Panels[i] = new Panel(universe, info[1], info[2], ledSize);
    }

    header_font = loadFont("Helvetica-24.vlw");
}

public void update(){
    // Faking it?
    if(fakeRXmode && (millis() - lastFakeRX) > 50){
        acn.fakeRx();
        lastFakeRX=millis();
    }
}

public void draw(){
    background(backgroundImg);
    update();
    pushMatrix();
    translate(lx, ly);
    for(int i = 0; i < Panels.length; i++){
        Panels[i].draw(acn.data.get(i).data, acn.data.get(i).dataLen);
    }
    popMatrix();

    drawHeader();
}

public void drawHeader(){
    fill(0,200,200);
    pushMatrix();
    translate(10,backgroundImg.height-10);
    textFont(header_font,24);
    String rxCount = String.format("rx: %d", acn.rxCount);
    text(rxCount, 0,0);
    popMatrix();
}

public void keyPressed(){
    switch(key){
    case 'f':
    case 'F': fakeRXmode = !fakeRXmode; break;
    case 'O': config.load(); break;
    case 'S': config.save(); break;
    };
}
