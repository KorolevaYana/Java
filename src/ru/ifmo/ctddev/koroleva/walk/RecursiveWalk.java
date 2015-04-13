package ru.ifmo.ctddev.koroleva.walk;

import java.io.*;

/**
 * Created by Яна on 15.02.2015.
 */
public class RecursiveWalk {

    public static void main(String[] args) {
        if (args == null) {
            System.out.println("Null arguments");
            return;
        }
        if (args.length < 2) {
            System.out.println("Found only " + args.length + " arguments from 2.");
            return;
        }
        if (args[0] == null || args[1] == null) {
            System.out.println("Null arguments");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"))) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8"))) {
                try {
                    String s;
                    while ((s = reader.readLine()) != null) {
                        File file = new File(s);
                        walk(writer, file);
                    }
                } catch (IOException e) {
                    System.out.println("Problems with reading input file.");
                }
            } catch (FileNotFoundException e) {
                System.out.println("Output file not found.");
            } catch (UnsupportedEncodingException e) {
                System.out.println("UTF-8 is unsupported encoding.");
            }
        } catch (FileNotFoundException e) {
            System.out.println("Input file not found.");
        } catch (UnsupportedEncodingException e) {
            System.out.println("UTF-8 is unsupported encoding.");
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    private static void walk(BufferedWriter writer, File file) {
        if (file.isDirectory()) {
            File[] list = file.listFiles();

            if (list == null) {
                System.out.println("Wrong directory descriptor " + file.getPath() + " or an I/O error occurs.");
                printFileInfo(writer, 0, file);
            } else {
                for (File f : list) {
                    walk(writer, f);
                }
            }
        } else if (file.isFile()) {
            printFileInfo(writer, fnv(file), file);
        } else {
            System.out.print("Something wrong with file " + file.getPath() + "\n");
            printFileInfo(writer, 0, file);
        }
    }

    private static int fnv(File file) {
        int h = 0x811c9dc5;

        try {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte b[] = new byte[100];
                int r;
                while ((r = fileInputStream.read(b)) >= 0) {
                    for (int i = 0; i < r; i++) {
                        h = (h * 0x01000193) ^ (b[i] & 0xff);
                    }
                }
                return h;
            }
        } catch (FileNotFoundException e) {
            System.out.println(file.getPath() + " not found.");
            return 0;
        } catch (IOException e) {
            System.out.println("Problems with reading file " + file.getPath() + ".");
            return 0;
        }
    }

    private static void printFileInfo(BufferedWriter writer, int h, File file) {
        try {
            writer.write(String.format("%08x %s \n", h, file.getPath()));
        } catch (IOException e) {
            System.out.println("Problems with writing to output file.");
        }
    }

}
