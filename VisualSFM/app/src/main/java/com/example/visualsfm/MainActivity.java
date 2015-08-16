package com.example.visualsfm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;


public class MainActivity extends Activity {
    private static final String TAG = "visualsfm";

    private static final boolean SIFT_ON = false;
    private static final boolean MATCHER_ON = false;
    private static final boolean BUNDLER_ON = false;

    // IO file and format conversion
    private static final boolean BUNDLER2PMVS_ON = false;
    private static final boolean RADIALUNDISTORT_ON = false;
    private static final boolean BUNDLER2VIS_ON = false;

    private static final boolean PMVS_CMVS_ON = true;

    static
    {
        System.loadLibrary("sift");
        System.loadLibrary("head");
        System.loadLibrary("matcher");
        System.loadLibrary("bundler");
        System.loadLibrary("cmvspmvs");
    }

    public native String jhead(String path);
    public native void sift(String path);
    public native void match(String input, String output);
    public native void bundler(String input, String option, String match, String output);
    public native void bundle2pmvs(String list, String bundle, String output);
    public native void genOption(String input);
    public native void cmvs(String input, String clusterNum, String coreNum);
    public native void pmvs2(String input, String output);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Handler mhandler = new Handler();
        mhandler.postDelayed(new WorkThread("/storage/sdcard0/kermit"), 1000);
    }

    public class WorkThread implements Runnable {
        private boolean isRunning = false;
        private String dir = null;

        public WorkThread(String dir) {
            Util.scanDir(dir, "jpg", dir + "/list_tmp.txt");
            this.dir = dir;
        }

        @Override
        public void run() {
            try {
                if(!isRunning) {
                    isRunning = true;
                    // SIFT - feature extraction
                    if(SIFT_ON) {
                        Log.d(TAG, "sift start");
                        FileInputStream fis = new FileInputStream(dir + "/list_tmp.txt");
                        BufferedReader br = new BufferedReader(new InputStreamReader(fis));

                        FileOutputStream fos = new FileOutputStream(dir + "/list.txt");
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            String jpg = line.trim();
                            String pgm = Util.jpg2pgm(jpg);
                            sift(pgm);
                            String info = jhead(jpg);
                            String focal = Util.extractInfo(info);
                            bw.write(jpg + " 0 " + focal + "\n");
                        }
                        br.close();
                        bw.close();
                        fis.close();
                        fos.close();

                        Util.scanDir(dir, "key", dir + "/list_keys.txt");
                        fis = new FileInputStream(dir + "/list_keys.txt");
                        br = new BufferedReader(new InputStreamReader(fis));
                        line = null;
                        while ((line = br.readLine()) != null) {
                            String key = line.trim();
                            Util.gzipFile(key, key + ".gz");
                        }
                        br.close();
                        fis.close();
                        Log.d(TAG, "sift end");
                    }

                    // Matcher - feature matching
                    if(MATCHER_ON) {
                        Log.d(TAG, "match start");
                        match(dir + "/list_keys.txt", dir + "/matches.init.txt");
                        Log.d(TAG, "match end");
                    }

                    // Bundler - bundle adjustment
                    // bug : exit abruptly (memory limit?)
                    if (BUNDLER_ON) {
                        Log.d(TAG, "bundler start");
                        Util.createOption(dir + "/options.txt");
                        Util.mkdir(dir + "/bundle");
                        Util.mkdir(dir + "/prepare");

                        bundler(dir + "/list.txt", dir + "/options.txt", dir, dir + "/bundle");
                        Log.d(TAG, "bundler end");
                    }

                    // Bundler2PMVS
                    if(BUNDLER2PMVS_ON) {
                        Log.d(TAG, "bundle2pmvs start");
                        bundle2pmvs(dir + "/list.txt", dir + "/bundle/bundle.out", dir + "/pmvs");
                        Log.d(TAG, "bundle2pmvs start");
                    }

                    // Radiaundistort
                    // bug : memory limit
                    if(RADIALUNDISTORT_ON) {
                        Log.d(TAG, "radiaundistort start");
                        Util.radiaundistort(dir + "/list.txt", dir + "/bundle/bundle.out", dir + "/pmvs");
                        Log.d(TAG, "radiaundistort end");
                    }

                    // Bundler2vis
                    if(BUNDLER2VIS_ON) {
                        Log.d(TAG, "bundle2vis start");
                        Util.mkdir(dir + "/pmvs/txt");
                        Util.mkdir(dir + "/pmvs/visualize");
                        Util.mkdir(dir + "/pmvs/models");

                        File directory = new File(dir + "/pmvs");
                        File[] files = directory.listFiles();
                        for (File file : files) {
                            if (file.getName().contains(".rd.jpg")) {
                                String name = file.getName().substring(file.getName().length() - 10, file.getName().length() - 7);
                                Util.mv(file.getAbsolutePath(), dir + "/pmvs/visualize/" + String.format("%08d.jpg", Integer.valueOf(name)));
                            }
                        }
                        for (File file : files) {
                            if (file.getName().length() == 12 && file.getName().contains(".txt")) {
                                Util.mv(file.getAbsolutePath(), dir + "/pmvs/txt/" + file.getName());
                            }
                        }

                        // bug : memory limit
                        Util.bundle2vis(dir + "/pmvs/bundle.rd.out", dir + "/pmvs/vis.dat");
                        Log.d(TAG, "bundle2vis end");
                    }

                    // CMVS-PMVS - Cluster-Patch Multiview Stereo
                    if(PMVS_CMVS_ON) {
                        Log.d(TAG, "pmvs-cmvs start");
                        cmvs(dir + "/pmvs/", "100", "8");
                        genOption(dir + "/pmvs/");
                        for (int i = 0; i < 1; i++) {
                            String name = String.format("option-%04d", i);
                            String path = dir + "/pmvs/" + name;
                            pmvs2(dir + "/pmvs/", name);
                        }
                        Log.d(TAG, "pmvs-cmvs end");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
