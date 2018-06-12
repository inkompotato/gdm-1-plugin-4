import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;


public class GRDM_U4_s0563016 implements PlugInFilter {

    protected ImagePlus imp;
    final static String[] choices = {"Wischen", "Weiche Blende", "Overlay", "Schieben", "Chroma Key", "Extra"};

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_RGB + STACK_REQUIRED;
    }

    public static void main(String args[]) {
        ImageJ ij = new ImageJ(); // neue ImageJ Instanz starten und anzeigen
        ij.exitWhenQuitting(true);


        IJ.open("C:\\Users\\frajs\\Creative Cloud Files\\Eclipse\\workspace-gdm1\\gdm-1-plugin-4\\src\\StackB.zip");

        GRDM_U4_s0563016 sd = new GRDM_U4_s0563016();
        sd.imp = IJ.getImage();
        ImageProcessor B_ip = sd.imp.getProcessor();
        sd.run(B_ip);
    }

    public void run(ImageProcessor B_ip) {
        // Film B wird uebergeben
        ImageStack stack_B = imp.getStack();

        int length = stack_B.getSize();
        int width = B_ip.getWidth();
        int height = B_ip.getHeight();

        // ermoeglicht das Laden eines Bildes / Films
        Opener o = new Opener();
        //OpenDialog od_A = new OpenDialog("AuswÃ¤hlen des 2. Filmes ...", "");

        // Film A wird dazugeladen
        //String dateiA = od_A.getFileName();
        //if (dateiA == null) return; // Abbruch
        //String pfadA = od_A.getDirectory();
        ImagePlus A = o.openImage("C:\\Users\\frajs\\Creative Cloud Files\\Eclipse\\workspace-gdm1\\gdm-1-plugin-4\\src", "StackA.zip");
        if (A == null) return; // Abbruch

        ImageProcessor A_ip = A.getProcessor();
        ImageStack stack_A = A.getStack();

        if (A_ip.getWidth() != width || A_ip.getHeight() != height) {
            IJ.showMessage("Fehler", "BildgrÃ¶ÃŸen passen nicht zusammen");
            return;
        }

        // Neuen Film (Stack) "Erg" mit der kleineren Laenge von beiden erzeugen
        length = Math.min(length, stack_A.getSize());

        ImagePlus Erg = NewImage.createRGBImage("Ergebnis", width, height, length, NewImage.FILL_BLACK);
        ImageStack stack_Erg = Erg.getStack();

        // Dialog fuer Auswahl des Ueberlagerungsmodus
        GenericDialog gd = new GenericDialog("Ãœberlagerung");
        gd.addChoice("Methode", choices, "");
        gd.showDialog();

        int methode = 0;
        int m = 0; //modus
        String s = gd.getNextChoice();
        if (s.equals("Wischen")){
            String[] modes = {"Vertikal", "Horizontal"};
            GenericDialog gm = new GenericDialog("Modus");
            gm.addChoice("Modus", modes, "");
            gm.showDialog();
            String mode = gm.getNextChoice();
            if (mode.equals("Vertikal")) m = 0;
            if (mode.equals("Horizontal")) m = 1;
            methode = 1;
        }
        if (s.equals("Weiche Blende")) methode = 2;
        if (s.equals("Overlay")) {
            String[] modes = {"Overlay (A,B)", "Overlay (B,A)"};
            GenericDialog gm = new GenericDialog("Modus");
            gm.addChoice("Modus", modes, "");
            gm.showDialog();
            String mode = gm.getNextChoice();
            if (mode.equals("Overlay (A,B)")) m = 1;
            if (mode.equals("Overlay (B,A)")) m = 0;

            methode = 3;
        }
        if (s.equals(("Schieben"))) {
            String[] modes = {"B über A, x-Richtung", "A über B, x-Richtung", "B über A, y-Richtung", "A über B, y-Richtung"};
            GenericDialog gm = new GenericDialog("Modus");
            gm.addChoice("Modus", modes, "");
            gm.showDialog();
            String mode = gm.getNextChoice();
            if (mode.equals("B über A, x-Richtung")) m = 0;
            if (mode.equals("A über B, x-Richtung")) m = 1;
            if (mode.equals("A über B, y-Richtung")) m = 2;
            if (mode.equals("B über A, y-Richtung")) m = 3;

            methode = 4;
        }
        if (s.equals("Chroma Key")) methode = 5;
        if (s.equals("Extra")) methode = 6;

        // Arrays fuer die einzelnen Bilder
        int[] pixels_B;
        int[] pixels_A;
        int[] pixels_Erg;

        // Schleife ueber alle Bilder
        for (int z = 1; z <= length; z++) {
            pixels_B = (int[]) stack_B.getPixels(z);
            pixels_A = (int[]) stack_A.getPixels(z);
            pixels_Erg = (int[]) stack_Erg.getPixels(z);

            int pos = 0;
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++, pos++) {
                    int cA = pixels_A[pos];
                    int rA = (cA & 0xff0000) >> 16;
                    int gA = (cA & 0x00ff00) >> 8;
                    int bA = (cA & 0x0000ff);

                    int cB = pixels_B[pos];
                    int rB = (cB & 0xff0000) >> 16;
                    int gB = (cB & 0x00ff00) >> 8;
                    int bB = (cB & 0x0000ff);

                    //wischen
                    if (methode == 1) {
                        //vertikal
                        if (m == 0) {
                            if (y + 1 > (z - 1) * (double) width / (length - 1))
                                pixels_Erg[pos] = pixels_B[pos];
                            else
                                pixels_Erg[pos] = pixels_A[pos];
                        }
                        //horizontal
                        if (m == 1) {
                            if (x + 1 > (z - 1) * (double) width / (length - 1))
                                pixels_Erg[pos] = pixels_B[pos];
                            else
                                pixels_Erg[pos] = pixels_A[pos];
                        }

                    }


                    //weiche blende - additive blende
                    if (methode == 2) {

                        float i = z / (length - 0F);

                        int r = (int) (rA * i + rB * (1 - i));
                        int g = (int) (gA * i + gB * (1 - i));
                        int b = (int) (bA * i + bB * (1 - i));

                        r = preventOverflow(r);
                        g = preventOverflow(g);
                        b = preventOverflow(b);


                        pixels_Erg[pos] = 0xFF000000 + ((r & 0xff) << 16) + ((g & 0xff) << 8) + (b & 0xff);
                    }


                    //overlay
                    if (methode == 3) {

                        int r = preventOverflow(overlayColor(rA, rB, m));
                        int g = preventOverflow(overlayColor(gA, gB, m));
                        int b = preventOverflow(overlayColor(bA, bB, m));

                        pixels_Erg[pos] = 0xFF000000 + ((r & 0xff) << 16) + ((g & 0xff) << 8) + (b & 0xff);
                    }

                    //schieben
                    if (methode == 4) {
                        //B über A in x-Richtung
                        if (m == 0) {
                            int xlimit = (int) ((float) z * (float) width / (float) length);


                            if (x < xlimit) {
                                pixels_Erg[pos] = pixels_B[y * width + (width - (xlimit - x))];
                            } else {
                                pixels_Erg[pos] = pixels_A[y * width + x - xlimit];
                            }
                        }
                        //A über B in x-Richtung
                        if (m == 1) {
                            int xlimit = (int) ((float) z * (float) width / (float) length);


                            if (x < xlimit) {
                                pixels_Erg[pos] = pixels_A[y * width + (width - (xlimit - x))];
                            } else {
                                pixels_Erg[pos] = pixels_B[y * width + x - xlimit];
                            }
                        }

                        //A über B in y-Richtung
                        if (m == 2) {
                            int ylimit = (int) ((float) z * (float) height / (float) length);

                            if (y < ylimit) {
                                pixels_Erg[pos] = pixels_A[((height - (ylimit - y)) * width + x)];
                            } else {
                                pixels_Erg[pos] = pixels_B[(y - ylimit) * width + x];
                            }
                        }

                        //B über A in y-Richtung
                        if (m == 3) {
                            int ylimit = (int) ((float) z * (float) height / (float) length);

                            if (y < ylimit) {
                                pixels_Erg[pos] = pixels_B[((height - (ylimit - y)) * width + x)];
                            } else {
                                pixels_Erg[pos] = pixels_A[(y - ylimit) * width + x];
                            }
                        }


                    }


                    //Chroma Key
                    if (methode == 5) {
                        if (rA > 160 && bA < 150) {
                            pixels_Erg[pos] = pixels_B[pos];
                        } else {
                            pixels_Erg[pos] = pixels_A[pos];
                        }
                    }

                    //Extra
                    if (methode == 6) {
                        int gridwidth = 32;
                        int gridheight = 18;
                        //int direction = 0;
                        int gridxpos = x/(width/gridwidth); //current x- position in the grid
                        int gridypos = y/(height/gridheight); //current y- position in the grid
                        int direction = (gridypos%2==0)? 1 : -1; //x-y- position in the grid
                        int gridpos;

                        if (direction == -1) {
                             gridpos = gridwidth * gridypos + gridwidth + gridxpos * direction;
                        } else {
                             gridpos = gridwidth * gridypos + gridxpos;
                        }

                        float timepos = (((float)gridwidth*(float)gridheight)/(float)length)*z;

                        if (gridpos <= timepos){
                            pixels_Erg[pos] = pixels_B[pos];
                        }
                        else {
                            pixels_Erg[pos] = pixels_A[pos];
                        }




                    }
                }

        }

        // neues Bild anzeigen
        Erg.show();
        Erg.updateAndDraw();
    }


    private int overlayColor(int cA, int cB, int m) {
        int c;
        float cAF = 0;
        float cBF = 0;
        if (m == 0) {
            cAF = cA / 255F;
            cBF = cB / 255F;
        }
        if (m == 1) {
            cAF = cB / 255F;
            cBF = cA / 255F;
        }
        if (cAF < 0.5) c = (int) ((2F * cAF * cBF) * 255F);
        else c = (int) ((1F - 2F * (1F - cAF) * (1F - cBF)) * 255F);
        return c;
    }

    private int preventOverflow(int c) {
        if (c > 255) c = 255;
        if (c < 0) c = 0;
        return c;
    }

}
