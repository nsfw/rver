import processing.core.PApplet;
import processing.core.PFont;
import java.awt.Color;
import java.util.*;

class Panel {
    static PApplet app;

    // emulate a single panel of RVIP lights
    Color[] pixels;
    int universe;
    int topx;
    int topy;
    int ledSize;    // how big is an 'LED' in pixels

    static void setApp(PApplet _app){
        app = _app;
    }

    Panel(int _univ, int _topx, int _topy, int _ledSize){
        System.out.println("Panel Universe: "+_univ);
        universe = _univ;
        topx = _topx;
        topy = _topy;
        ledSize = _ledSize;
    }

    void draw(int[] data, int len){

        app.pushMatrix();
        app.translate(topx*ledSize, topy*ledSize);

        int pixel = 0;
        for(int i=0; i<len; pixel++, i+=3){
            // walk our SCN data, striding by RGB
            drawLED(pixel, data[i], data[i+1], data[i+2]);
        }

        app.popMatrix();
    }

    // sACN pixel number to X,Y on panel - zig zag, starting on left
    int[][] panelMap = {
        {19,0},{18,0},{17,0},{16,0},{15,0},{14,0},{13,0},{12,0},{11,0},{10,0},
        {9,0},{8,0},{7,0},{6,0},{5,0},{4,0},{3,0},{2,0},{1,0},{0,0},
        {0,1},{1,1},{2,1},{3,1},{4,1},{5,1},{6,1},{7,1},{8,1},{9,1},
        {10,1},{11,1},{12,1},{13,1},{14,1},{15,1},{16,1},{17,1},{18,1},{19,1},
        {19,2},{18,2},{17,2},{16,2},{15,2},{14,2},{13,2},{12,2},{11,2},{10,2},
        {9,2},{8,2},{7,2},{6,2},{5,2},{4,2},{3,2},{2,2},{1,2},{0,2},
        {0,3},{1,3},{2,3},{3,3},{4,3},{5,3},{6,3},{7,3},{8,3},{9,3},
        {10,3},{11,3},{12,3},{13,3},{14,3},{15,3},{16,3},{17,3},{18,3},{19,3},
        {19,4},{18,4},{17,4},{16,4},{15,4},{14,4},{13,4},{12,4},{11,4},{10,4},
        {9,4},{8,4},{7,4},{6,4},{5,4},{4,4},{3,4},{2,4},{1,4},{0,4}
    };

    void drawLED(int pixel, int r, int g, int b){
        if(pixel>99) return;

        int px = panelMap[pixel][0];
        int py = panelMap[pixel][1];
        app.pushMatrix();
        app.translate(px*ledSize, py*ledSize);
        app.fill(r,g,b);
        app.rect(0,0,ledSize, ledSize);
        app.popMatrix();
    }
}
