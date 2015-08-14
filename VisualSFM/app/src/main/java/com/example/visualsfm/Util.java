package com.example.visualsfm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

public final class Util {
    private static final String TAG = "Util";

    public static String extractInfo(String buffer) {
        String[] lines = buffer.toString().split("\n");
        String maker = null;
        String model = null;
        String resolution = null;
        String focallength = null;
        String ccdwidth = null;
        int width = 0;
        int height = 0;
        float focal = 0.0f;
        float ccd = 0.0f;
        String result = null;
        for(String line : lines) {
            if(line != null) {
                line = line.trim();
                if(line.contains("maker")) {
                    maker = line.split("=")[1];
                } else if(line.contains("model")) {
                    model = line.split("=")[1];
                } else if(line.contains("resolution")) {
                    resolution = line.split("=")[1];
                    width = Integer.valueOf(resolution.trim().split("x")[0]);
                    height = Integer.valueOf(resolution.trim().split("x")[1]);
                    if(focallength != null) {
                        focal = Float.valueOf(focallength);
                    } else {
                        focal = 5.4f;
                    }
                } else if(line.contains("focallength")) {
                    focallength = line.split("=")[1];
                } else if(line.contains("ccdwidth")) {
                    ccdwidth = line.split("=")[1];
                    if(ccdwidth != null) {
                        ccd = Float.valueOf(ccdwidth);
                    } else {
                        ccd = 5.23f; // TODO
                    }
                }
            }
            boolean hasFocal = true;
            if(focal == 0.0f || ccd == 0.0f || width == 0) {
                hasFocal = false;
            }
            if(hasFocal) {
                float focalPixel = Math.max(width, height) * (focal / ccd);
                result = 1.0 * focalPixel + "";
            }
        }
        return result;
    }

    public static void createOption(String filename) {
        try {
            FileWriter fw = new FileWriter(filename);
            fw.write("--match_table matches.init.txt\n");
            fw.write("--output bundle.out\n");
            fw.write("--output_all bundle_\n");
            fw.write("--output_dir bundle\n");
            fw.write("--variable_focal_length\n");
            fw.write("--use_focal_estimate\n");
            fw.write("--constrain_focal\n");
            fw.write("--constrain_focal_weight 0.0001\n");
            fw.write("--estimate_distortion\n");
            fw.write("--run_bundle\n");
            fw.close();
        } catch (IOException e) {
        }
    }

    public static List<String> scanDir(String dirname, String ext, String ofilename) {
        try {
            List<String> result = new ArrayList<String>();
            FileWriter fw = new FileWriter(ofilename);
            File dir = new File(dirname);
            File[] files = dir.listFiles();
            for(File file:files){
                if(file.getName().contains("." + ext)) {
                    fw.write(file.getAbsolutePath() + "\n");
                    result.add(file.getAbsolutePath());
                }
            }
            fw.close();
            return result;
        } catch (IOException e) {
        }
        return null;
    }

    public static void gzipFile(String src, String dest) {
        byte[] buffer = new byte[1024];
        try {
            FileOutputStream fileOutputStream =new FileOutputStream(dest);
            GZIPOutputStream gzipOuputStream = new GZIPOutputStream(fileOutputStream);
            FileInputStream fileInput = new FileInputStream(src);
            int bytes_read;

            while ((bytes_read = fileInput.read(buffer)) > 0) {
                gzipOuputStream.write(buffer, 0, bytes_read);
            }

            fileInput.close();
            gzipOuputStream.finish();
            gzipOuputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void mkdir(String dir) {
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    public static void mv(String src, String dest) {
        File file = new File(src);
        file.renameTo(new File(dest));
    }

    public static String jpg2pgm(String jpg) {
        String pgm = jpg.replace(".jpg", ".pgm");
        try {
            OutputStream fileOutput =
                    new FileOutputStream(pgm);
            DataOutputStream output =
                    new DataOutputStream(fileOutput);

            Bitmap bMap = BitmapFactory.decodeFile(jpg);
            boolean isGray = true;
            int w = bMap.getWidth(), h = bMap.getHeight();
            output.writeBytes((isGray ? "P5" : "P6")
                    + "\n# Written by ImageJ PNM Writer\n"
                    + w + " " + h + "\n255\n");
            byte[] pixels = null;
            if(isGray) {
                pixels = new byte[w * h];
                for (int j = 0; j < h; j++) {
                    for (int i = 0; i < w; i++) {
                        int c = bMap.getPixel(i, j);
                        pixels[i + w * j] = (byte) (0.2126 * (byte)((c & 0xff0000) >> 16) + 0.7152 * (byte)((c & 0xff00) >> 8) + 0.0722 * (byte)(c & 0xff));
                    }
                }
            } else {
                pixels = new byte[w * h * 3];
                for (int j = 0; j < h; j++) {
                    for (int i = 0; i < w; i++) {
                        int c = bMap.getPixel(i, j);
                        pixels[3 * (i + w * j) + 0] =
                                (byte)((c & 0xff0000) >> 16);
                        pixels[3 * (i + w * j) + 1] =
                                (byte)((c & 0xff00) >> 8);
                        pixels[3 * (i + w * j) + 2] =
                                (byte)(c & 0xff);
                    }
                }
            }
            output.write(pixels, 0, pixels.length);
            output.flush();
            output.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return pgm;
    }

    private static boolean isGray(int pixel) {
        int alpha = (pixel & 0xFF000000) >> 24;
        int red   = (pixel & 0x00FF0000) >> 16;
        int green = (pixel & 0x0000FF00) >> 8;
        int blue  = (pixel & 0x000000FF);

        if( 0 == alpha && red == green && green == blue ) return true;
        else return false;
    }

    // Bundler -> PMVS/CMVS
    private static class Color_t {
        double r;
        double g;
        double b;
        public Color_t() {
        }
        public Color_t(double r, double g, double b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private static class View
    {
        public int image;
        public int key;
        public double x;
        public double y;
        public View() {
        }
    };

    private static class Point
    {
        public double[] pos = new double[3];
        public double[] color = new double[3];
        public List<View> views;
        public Point() {
            pos = new double[3];
            color = new double[3];
            views = new ArrayList<View>();
        }
    };

    private static class Camera {
        public double[] R;     /* Rotation */
        public double[] t;     /* Translation */
        public double f;        /* Focal length */
        public double[] k;     /* Undistortion parameters */
        public double k_inv[]; /* Inverse undistortion parameters */
        public char[] constrained;
        public double[] constraints;  /* Constraints (if used) */
        public double[] weights;      /* Weights on the constraints */
        public double[] K_known;  /* Intrinsics (if known) */
        public double[] k_known;  /* Distortion params (if known) */

        public char fisheye;            /* Is this a fisheye image? */
        public char known_intrinsics;   /* Are the intrinsics known? */
        public double f_cx;
        public double f_cy;       /* Fisheye center */
        public double f_rad;
        public double f_angle;   /* Other fisheye parameters */
        public double f_focal;          /* Fisheye focal length */

        public double f_scale;
        public double k_scale; /* Scale on focal length, distortion params */
        public Camera() {
            R = new double[9];
            t = new double[3];
            f = 0.0;
            k = new double[2];
            k_inv = new double[6];
            constrained = new char[9];
            constraints = new double[9];
            weights = new double[9];
            K_known = new double[9];
            k_known = new double[5];
        }
    };

    private static void ReadBundleFile(String bundle_file,
                                       List<Camera> cameras,
                                       List<Point> points)
    {
        File f = new File(bundle_file);
        int num_images, num_points;
        double bundle_version;
        boolean coalesced = false;
        Scanner scanner = null;
        try {
            scanner = new Scanner(f);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        String first_line = scanner.nextLine();
        if (first_line.charAt(0) == '#') {
            bundle_version = Double.valueOf(first_line.split("v")[1]);

            num_images = scanner.nextInt();
            num_points = scanner.nextInt();
            if (bundle_version >= 0.4) {
                int coalesced_int = scanner.nextInt();
                coalesced = (coalesced_int != 0);
            }

        } else if(first_line.charAt(0) == 'v') {
            bundle_version = Double.valueOf(first_line.split("v")[1]);
            Log.d(TAG, "[ReadBundleFile2] Bundle version: " + String.format("%0.3f", bundle_version));

            num_images = scanner.nextInt();
            num_points = scanner.nextInt();
        } else {
            bundle_version = 0.1;
            num_images = Integer.valueOf(first_line.split(" ")[0]);
            num_points = Integer.valueOf(first_line.split(" ")[1]);
        }

        Log.d(TAG, "[ReadBundleFile4] Reading " + num_images + " images and " + num_points + " points...");

	    /* Read cameras */
        for (int i = 0; i < num_images; i++) {
            double focal_length = scanner.nextDouble();
            double k0 = scanner.nextDouble();
            double k1 = scanner.nextDouble();

            double[] R = new double[9];
            double[] t = new double[3];

            for(int j = 0; j < 9; j++) {
                R[j] = scanner.nextDouble();
            }
            for(int j = 0; j < 3; j++) {
                t[j] = scanner.nextDouble();
            }

            Camera cam = new Camera();
            cam.f = focal_length;
            cam.k[0] = k0;
            cam.k[1] = k1;
            cam.R = R;
            cam.t = t;

	        /* Flip the scene if needed */
            if (bundle_version < 0.3) {
                R[2] = -R[2];
                R[5] = -R[5];
                R[6] = -R[6];
                R[7] = -R[7];
                t[2] = -t[2];
            }

            cameras.add(cam);
        }

	    /* Read points */
        for (int i = 0; i < num_points; i++) {
            if (bundle_version >= 0.4) {
                int player_id = scanner.nextInt();
            }

            Point pt = new Point();
            for(int j = 0; j < 3; j++) {
                pt.pos[j] = scanner.nextDouble();
            }
            for(int j = 0; j < 3; j++) {
                pt.color[j] = scanner.nextDouble();
            }

            if (bundle_version >= 0.4 && coalesced) {
                float[] desc = new float[128];
                for (int j = 0; j < 16; j++) {
                    for (int k = 0; k < 8; k++) {
                        desc[j * 8 + k] = scanner.nextFloat();
                    }
                }
            }

            int num_visible = scanner.nextInt();

            for(int j = 0; j < num_visible; j++) {
                int image = scanner.nextInt();
                int key = scanner.nextInt();

                double x = 0, y = 0;
                if (bundle_version >= 0.3) {
                    x = scanner.nextDouble();
                    y = scanner.nextDouble();
                }

                View view = new View();
                view.image = image;
                view.key = key;
                view.x = x;
                view.y = y;

                pt.views.add(view);
            }

            if (bundle_version < 0.3)
                pt.pos[2] = -pt.pos[2];

            if (num_visible > 0) {
                points.add(pt);
            }
        }

        scanner.close();
    }

    private static List<String> ReadListFile(String list_file)
    {
        List<String> files = new ArrayList<String>();
        File f = new File(list_file);

        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(f));
            String line = null;
            while ((line = br.readLine()) != null) {
                files.add(line.trim());
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }

    private static void WriteVisFile(String vis_file, List<Camera> cameras, List<Point> points)
    {
        int nCameras = (int) cameras.size();
        int nPoints = (int) points.size();

        try {
            File f = new File(vis_file);
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));

			/* Fill in the matches matrix */
            int[][] matches = new int[nCameras][nCameras];
            for (int i = 0; i < nCameras; i++) {
                for (int j = 0; j < nCameras; j++) {
                    matches[i][j] = 0;
                }
            }

            for (int i = 0; i < nPoints; i++) {
                int nViews = points.get(i).views.size();
                for (int j = 0; j < nViews; j++) {
                    int i1 = points.get(i).views.get(j).image;
                    for (int k = j+1; k < nViews; k++) {
                        if (j == k) continue;
                        int i2 = points.get(i).views.get(k).image;

                        matches[i1][i2]++;
                        matches[i2][i1]++;
                    }
                }
            }

            bw.write("VISDATA");
            bw.newLine();
            bw.write(nCameras + "");
            bw.newLine();

            // write camera rows
            int MATCH_THRESHOLD = 32;
            for (int i = 0; i < nCameras; i++) {
                List<Integer> vis = new ArrayList<Integer>();
                for (int j = 0; j < nCameras; j++) {
                    if (matches[i][j] >= MATCH_THRESHOLD)
                        vis.add(j);
                }

                int nVis = vis.size();
                bw.write(i + " " + nVis);

                for (int j = 0; j < nVis; j++) {
                    bw.write(" " + vis.get(j));
                }
                bw.newLine();
            }

            bw.close();
        } catch(IOException e) {
        }
    }

    private static void WriteBundleFile(String bundle_file, List<Camera> cameras, List<Point> points)
    {
        try {
            File f = new File(bundle_file);
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));

            int num_images = cameras.size();
            int num_points = points.size();

		    /* Count the number of good images */
            int num_good_images = 0;
            int[] map = new int[num_images];
            for (int i = 0; i < num_images; i++) {
                if (cameras.get(i).f == 0) {
                    map[i] = -1;
                    continue;
                }

                map[i] = num_good_images;
                num_good_images++;
            }

            Log.d(TAG, "[WriteBundleFile] Writing "+num_good_images+" images and "+num_points+" points...");

            bw.write("# Bundle file v0.3");
            bw.newLine();
            bw.write(num_good_images + " " + num_points);
            bw.newLine();

            Log.d(TAG, "write # Bundle file v0.3");
            Log.d(TAG, "write " + num_good_images + " " + num_points);

		    /* Write cameras */
            for (int i = 0; i < num_images; i++) {
                if (cameras.get(i).f == 0)
                    continue;

		        /* Focal length */
                bw.write(String.format("%.6f", cameras.get(i).f) + " 0.0 0.0");
                bw.newLine();
                Log.d(TAG, "write " + cameras.get(i).f +" 0.0 0.0\n");

				/* Rotation */
                double[] R = cameras.get(i).R;
                for(int j = 0; j < 9; j++) {
                    if(j != 0) {
                        bw.write(" ");
                    }
                    bw.write(String.format("%.6f", (float) R[j]));
                }
                bw.newLine();

		        /* Translation */
                double[] t = cameras.get(i).t;
                for(int j = 0; j < 3; j++) {
                    if(j != 0) {
                        bw.write(" ");
                    }
                    bw.write(String.format("%.6f", (float) t[j]));
                }
                bw.newLine();
            }

		    /* Write points */
            for (int i = 0; i < num_points; i++) {
		    	/* Position */
                double[] pos = points.get(i).pos;
                for(int j = 0; j < 3; j++) {
                    if(j != 0) {
                        bw.write(" ");
                    }
                    bw.write(String.format("%.6f", (float) pos[j]));
                }
                bw.newLine();

		        /* Color */
                double[] color = points.get(i).color;
                for(int j = 0; j < 3; j++) {
                    if(j != 0) {
                        bw.write(" ");
                    }
                    bw.write(String.format("%d", (int) Math.round(color[j])));
                }
                bw.newLine();

                int num_visible = points.get(i).views.size();
                bw.write(num_visible + "");

                for (int j = 0; j < num_visible; j++) {
                    int view = map[points.get(i).views.get(j).image];
                    assert(view >= 0 && view < num_good_images);
                    int key = points.get(i).views.get(j).key;
                    float x = (float)points.get(i).views.get(j).x;
                    float y = (float)points.get(i).views.get(j).y;

                    bw.write(String.format(" %d %d %.2f %.2f", view, key, x, y));
                }
                bw.newLine();
            }

            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void UndistortImage(String in, Camera camera, String out)
    {
        Log.d(TAG, "Undistorting image " + in);

        Bitmap inMap = BitmapFactory.decodeFile(in);
        int w = inMap.getWidth();
        int h = inMap.getHeight();

        Bitmap outMap = inMap.copy(inMap.getConfig(), true);

        double f2_inv = 1.0 / (camera.f * camera.f);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double x_c = x - 0.5 * w;
                double y_c = y - 0.5 * h;

                double r2 = (x_c * x_c + y_c * y_c) * f2_inv;
                double factor = 1.0 + camera.k[0] * r2 + camera.k[1] * r2 * r2;

                x_c *= factor;
                y_c *= factor;

                x_c += 0.5 * w;
                y_c += 0.5 * h;

                Color_t c = null;
                if (x_c >= 0 && x_c < w - 1 && y_c >= 0 && y_c < h - 1) {
                    c = pixel_lerp(inMap, x_c, y_c);
                } else {
                    c = new Color_t(0.0, 0.0, 0.0);
                }

                int blue = (int)(Math.round(c.b));
                int green = (int)(Math.round(c.g));
                int red = (int)(Math.round(c.r));

                outMap.setPixel(x, y, Color.rgb(red, green, blue));
            }
        }

        File f = new File(out);
        try {
            outMap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(f));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private static void UndistortImages(String output_path,
                                        List<String> files,
                                        List<Camera> cameras)
    {
        int num_files = files.size();
        assert(files.size() == cameras.size());

        for (int i = 0; i < num_files; i++) {
            if (cameras.get(i).f == 0.0)
                continue;

            String in = files.get(i).split(" ")[0];

            int last_slash = in.lastIndexOf('/');
            int last_dot = in.lastIndexOf('.');
            assert(last_slash < last_dot);
            String basename =
                    in.substring(last_slash + 1, last_dot);

            String out = output_path + "/" + basename + ".rd.jpg";
            UndistortImage(in, cameras.get(i), out);
        }
    }

    private static void WriteNewFiles(String output_path,
                                      List<String> files,
                                      List<Camera> cameras,
                                      List<Point> points)
    {
        try {
            String buf = output_path + "/list.rd.txt";
            File f = new File(buf);
            BufferedWriter bw;

            bw = new BufferedWriter(new FileWriter(f));

            int num_files = files.size();
            for (int i = 0; i < num_files; i++) {
                if (cameras.get(i).f == 0.0)
                    continue;
                bw.write(files.get(i));
            }
            bw.close();

            buf = output_path + "/bundle.rd.out";
            WriteBundleFile(buf, cameras, points);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void radiaundistort(String list_file, String bundle_file, String pmvs_dir)    {
        List<Camera> cameras = new ArrayList<Camera>();
        List<Point> points = new ArrayList<Point>();
        List<String> files = ReadListFile(list_file);
        ReadBundleFile(bundle_file, cameras, points);

        UndistortImages(pmvs_dir, files, cameras);
        WriteNewFiles(pmvs_dir, files, cameras, points);
    }

    public static void bundle2vis(String bundle_file, String vis_file)
    {
        List<Camera> cameras = new ArrayList<Camera>();
        List<Point> points = new ArrayList<Point>();
        ReadBundleFile(bundle_file, cameras, points);
        WriteVisFile(vis_file, cameras, points);
    }


    private static Color_t pixel_lerp(Bitmap img, double x, double y) {
        Color_t col = new Color_t();

        int xf = (int) Math.floor(x), yf = (int) Math.floor(y);
        double xp = x - xf, yp = y - yf;

        int[] fr = new int[4];
        int[] fg = new int[4];
        int[] fb = new int[4];
        double rd, gd, bd;

        int[] pixels = img_get_pixel_square(img, xf, yf);

        if (pixels[0] != 0) {
            fr[0] = Color.red(pixels[0]);
            fg[0] = Color.green(pixels[0]);
            fb[0] = Color.blue(pixels[0]);
        }

        if (pixels[1] != 0) {
            fr[1] = Color.red(pixels[1]);
            fg[1] = Color.green(pixels[1]);
            fb[1] = Color.blue(pixels[1]);
        }

        if (pixels[2] != 0) {
            fr[2] = Color.red(pixels[2]);
            fg[2] = Color.green(pixels[2]);
            fb[2] = Color.blue(pixels[2]);
        }

        if (pixels[3] != 0) {
            fr[3] = Color.red(pixels[3]);
            fg[3] = Color.green(pixels[3]);
            fb[3] = Color.blue(pixels[3]);
        }

	    /* Lerp */
        if (xp >= yp) {
            rd = ((double) fr[0]) + ((double) (fr[1] - fr[0])) * xp + ((double) (fr[3] - fr[1])) * yp;
            gd = ((double) fg[0]) + ((double) (fg[1] - fg[0])) * xp + ((double) (fg[3] - fg[1])) * yp;
            bd = ((double) fb[0]) + ((double) (fb[1] - fb[0])) * xp + ((double) (fb[3] - fb[1])) * yp;
        } else {
            rd = ((double) fr[0]) + ((double) (fr[2] - fr[0])) * yp + ((double) (fr[3] - fr[2])) * xp;
            gd = ((double) fg[0]) + ((double) (fg[2] - fg[0])) * yp + ((double) (fg[3] - fg[2])) * xp;
            bd = ((double) fb[0]) + ((double) (fb[2] - fb[0])) * yp + ((double) (fb[3] - fb[2])) * xp;
        }

        col.r = (float) rd;
        col.g = (float) gd;
        col.b = (float) bd;

        return col;
    }

    private static int[] img_get_pixel_square(Bitmap img, int x, int y) {
        int[] sq = new int[4];
        sq[0] = img.getPixel(x, y);

        if (x < img.getWidth() - 1)
            sq[1] = img.getPixel(x + 1, y);
        else
            sq[1] = 0;

        if (y < img.getHeight() - 1)
            sq[2] = img.getPixel(x, y + 1);
        else
            sq[2] = 0;

        if (x < img.getWidth() - 1 && y < img.getHeight() - 1)
            sq[3] = img.getPixel(x + 1, y + 1);
        else
            sq[3] = 0;

        return sq;
    }
}
