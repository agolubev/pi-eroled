package eroled;

import java.io.IOException;
import java.util.Arrays;

import static java.lang.Math.*;

public class OLEDWindow {
    BasicOLED oled;
    int x;
    int y;
    int width;
    int height;
    byte[] buffer;
    byte _0F= 0x0F;
    public OLEDWindow(BasicOLED oled,
            int x,
            int y,
            int width,
            int height){
        this.oled = oled;
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.buffer=new byte[(width/2)*height];

    }
    public void drawScreenBuffer() throws IOException {
        oled.setDataWindow(x,y,width,height);
        oled.writeInstruction(0x5c);
        for ( int i=0;i<buffer.length;i+=2048)
        {
            oled.writeDataBytes(Arrays.copyOfRange(buffer, i, min(i+2048,buffer.length)));
        }
    }

    public void setPixel(int x, int y, byte color){
        color &= 0x0F;
        int ofs = y * width/2;
        ofs = ofs + x/2;// 2 pixels per byte across row
        byte v;
        if (x%2 == 0) {
            v = (byte)(buffer[ofs] & 0x0F);
            v |= (color << 4);
        } else {
            v = (byte)(buffer[ofs] & 0xF0);
            v |= color;
        }
        buffer[ofs] = v;
    }

    public void drawBwImage(int x,int y,int width,int height,byte color,byte[] data,int offs){
        int pos = offs;
        for(int yy = 0; yy<height; yy++){
            int ox = x;
            for(int xx = 0; xx<width/8; xx++){
             int  mask = 0b10000000;
                for(int pp = 0; pp<8; pp++){
                    if((data[pos] & mask)>0)
                        setPixel(x,y,color);
                    else
                        setPixel(x,y,(byte)0);
                    mask = mask>>1;
                    x=x + 1;
                }
                pos++;
            }
            x=ox;
            y = y + 1;
        }
    }

    public void drawBigBwImage(int x, int y, int width, int height, byte color, byte[] data, int offs){
        int pos = offs;
        byte black = 0;
        for(int yy = 0; yy<height; yy++) {
            int ox = x;
            for (int xx = 0; xx < width / 8; xx++) {
                int mask = 128;
                for (int pp = 0; pp < 8; pp++) {
                    if((data[pos] & mask)>0) {
                        setPixel(x, y, color);
                        setPixel(x + 1, y, color);
                        setPixel(x, y + 1, color);
                        setPixel(x + 1, y + 1, color);
                    } else{
                        setPixel(x, y, black);
                        setPixel(x + 1, y, black);
                        setPixel(x, y + 1, black);
                        setPixel(x + 1, y + 1, black);
                    }
                    mask = mask>>1;
                    x=x + 2;
                }
                pos++;
            }
            x=ox;
            y = y + 1;
        }
    }
    private void swap(int a, int b)
    {
        int temp = a;
        a = b;
        b = temp;

    }

    public void drawLine(int startx, int starty,
                         int endx, int endy,
                         byte color){

        int dx = endx - startx;
        int dy = endy - starty;

        //# Determine how steep the line is
        boolean is_steep = abs(dy) > abs(dx);


        if (is_steep){
            int temp = startx;
            startx = starty;
            starty = temp;

            temp = endx;
            endx = endy;
            endy = temp;


        }


        if (startx > endx){
            int temp = startx;
            startx = endx;
            endx = temp;

            temp = starty;
            startx = starty;
            starty = temp;
        }

        dx = endx - startx;
        dy = endy - starty;


        int error = (int)(dx / 2.0);
        int ystep = (starty < endy) ? 1 : -1;

        // Iterate over bounding box generating points between start and end
        int y = starty;
        int[] points =null;
        for(int x = startx;x<endx+1;x++){
            if (is_steep) setPixel(y,x,color); else setPixel(x,y,color);
            error -= abs(dy);
            error -= abs(dy);
            if (error < 0) {
                y += ystep;
                error += dx;
            }
        }
    }

    public void drawRectangle(int x, int y, int width, int height, byte color){
        for(int xx=0;xx<width;xx++){
            setPixel(x+xx,y,color);
            setPixel(x+xx,y+height-1, color);
        }
        for(int yy=0;x<height;yy++){
            setPixel(x,y+yy,color);
            setPixel(x+width-1,y+yy,color);
        }
    }

    public void drawVLine(int x, int y, int h, byte color){
        drawLine(x, y, x, y+h-1, color);
    }
    public void drawHLine(int x, int y, int w, byte color){
        drawLine(x, y, x+w-1, y, color);
    }

    public void DrawFrame(int x,int y,int w, int h,byte color){
        drawHLine(x, y, w, color);
        drawHLine(x, y+h-1, w, color);
        drawVLine(x, y, h, color);
        drawVLine(x+w-1, y, h, color);
    }

    /*public void DrawRFrame(int x,int y,int w, int h, int r, int color){
        drawHLine(x+r, y, w-2*r, color);
        drawHLine(x+r, y+h-1, w-2*r, color)  ;
        drawVLine(x, y+r, h-2*r, color)   ;
        drawVLine(x+w-1, y+r, h-2*r, color)  ;
        //# draw four corners
        DrawCircleHelper(x+r, y+r, r, 1, color);
        DrawCircleHelper(x+w-r-1, y+r, r, 2, color);
        DrawCircleHelper(x+w-r-1, y+h-r-1, r, 4, color);
        DrawCircleHelper(x+r, y+h-r-1, r, 8, color);
    }*/
/*

    def DrawFrame(self, x, y, w, h, color):         # Draw_rectangle alternative
        self.drawHLine(x, y, w, color)
        self.drawHLine(x, y+h-1, w, color)
        self.drawVLine(x, y, h, color)
        self.drawVLine(x+w-1, y, h, color)

    def DrawRFrame(self, x, y, w, h, r, color):     # Draw Rounded Corners Rectangle
        self.drawHLine(x+r, y, w-2*r, color)        # Top
        self.drawHLine(x+r, y+h-1, w-2*r, color)    # Bottom
        self.drawVLine(x, y+r, h-2*r, color)        # Left
        self.drawVLine(x+w-1, y+r, h-2*r, color)    # Right
        # draw four corners
        self.DrawCircleHelper(x+r, y+r, r, 1, color)
        self.DrawCircleHelper(x+w-r-1, y+r, r, 2, color)
        self.DrawCircleHelper(x+w-r-1, y+h-r-1, r, 4, color)
        self.DrawCircleHelper(x+r, y+h-r-1, r, 8, color)

    def DrawBox(self, x, y, w, h, color):           # Draw Filled Rectangle
        for t in range(x, x+w):
            self.drawVLine(t, y, h, color)

    def DrawRBox(self, x, y, w, h, r, color):       # Draw Rounded Corners Filled Rectangle
        self.DrawBox(x+r, y, w-2*r, h, color)
        # draw four corners
        self.FillCircleHelper(x+w-r-1, y+r, r, 1, h-2*r-1, color)
        self.FillCircleHelper(x+r    , y+r, r, 2, h-2*r-1, color)

    def DrawCircle(self, x0, y0, r, color):         # Draw Circle
        f = 1 - r
        ddF_x = 1
        ddF_y = -2 * r
        x = 0
        y = r

        self.setPixel(x0, y0+r, color)
        self.setPixel(x0, y0-r, color)
        self.setPixel(x0+r, y0, color)
        self.setPixel(x0-r, y0, color)

        while x < y:
            if f >= 0:
                y -= 1
                ddF_y += 2
                f += ddF_y

            x += 1
            ddF_x += 2
            f += ddF_x

            self.setPixel(x0+x, y0+y, color)
            self.setPixel(x0-x, y0+y, color)
            self.setPixel(x0+x, y0-y, color)
            self.setPixel(x0-x, y0-y, color)
            self.setPixel(x0+y, y0+x, color)
            self.setPixel(x0-y, y0+x, color)
            self.setPixel(x0+y, y0-x, color)
            self.setPixel(x0-y, y0-x, color)

    def DrawDisc(self, x0, y0, r, color):               # Draw Filled Circle
        self.drawVLine(x0, y0-r, 2*r+1, color)
        self.FillCircleHelper(x0, y0, r, 3, 0, color)

    def DrawTriangle(self, x0, y0, x1, y1, x2, y2,color):   # Draw a Triangle
        self.drawLine((x0, y0), (x1, y1), color)
        self.drawLine((x1, y1), (x2, y2), color)
        self.drawLine((x2, y2), (x0, y0), color)

    def DrawCircleHelper(self, x0, y0, r, cornername, color):
        f = 1 - r
        ddF_x = 1
        ddF_y = -2 * r
        x = 0
        y = r

        while x < y:
            if f >= 0:
                y -= 1
                ddF_y += 2
                f += ddF_y

            x += 1
            ddF_x += 2
            f += ddF_x

            if (cornername & 0x4):
                self.setPixel(x0 + x, y0 + y, color)
                self.setPixel(x0 + y, y0 + x, color)

            if (cornername & 0x2):
                self.setPixel(x0 + x, y0 - y, color)
                self.setPixel(x0 + y, y0 - x, color)

            if (cornername & 0x8):
                self.setPixel(x0 - y, y0 + x, color)
                self.setPixel(x0 - x, y0 + y, color)

            if (cornername & 0x1):
                self.setPixel(x0 - y, y0 - x, color)
                self.setPixel(x0 - x, y0 - y, color)

    def FillCircleHelper(self, x0, y0, r, cornername, delta, color):
        f = 1 - r
        ddF_x = 1
        ddF_y = -2 * r
        x = 0
        y = r

        while x < y:
            if (f >= 0):
                y -= 1
                ddF_y += 2
                f += ddF_y

            x += 1
            ddF_x += 2
            f += ddF_x

            if (cornername & 0x1):
                self.drawVLine(x0+x, y0-y, 2*y+1+delta, color)
                self.drawVLine(x0+y, y0-x, 2*x+1+delta, color)

            if (cornername & 0x2):
                self.drawVLine(x0-x, y0-y, 2*y+1+delta, color)
                self.drawVLine(x0-y, y0-x, 2*x+1+delta, color)
    def draw_text(self,x,y,mes,color):
        for c in mes:
            self.draw_bw_image(x, y, 8, 8, color, FONT_5x7, ord(c)*8)
            x=x+8
    def draw_big_text(self,x,y,mes,color):
        for c in mes:
            self.drawBigBwImage(x, y, 8, 8, color, FONT_5x7, ord(c)*8)
            x=x+16
 */
}
