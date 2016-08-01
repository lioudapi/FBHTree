package fbhtree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;



public class Main {
    public static List<String> getAllFilePaths(File folder) {
        List<String> filePaths = new ArrayList<>();
//        int i = 0 ;
        if (folder.listFiles() == null) {
            return filePaths;            
        }
        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                filePaths.add(file.getAbsolutePath());
            } else if (file.isDirectory()) {
                filePaths.addAll(getAllFilePaths(file));
//                i++;
                
            }
        }
//        System.out.println(i);
        return filePaths;
    }
    
    public static String TestDataPath = "/System/Library";
    public static String TestDataPath2 = "/Usr/lib";
    public static String PathFormat = "/Users/lioudapi/Desktop/FBHTreeSize/%d.FBHTree";
    public static String HashMapPath = "/Users/Lioudapi/Desktop/HashMap";
    
    public static void digestAllFiles() {
        long time;
        
        System.out.println("getting file paths...");
        time = System.currentTimeMillis();
        List<String> files = getAllFilePaths(new File(TestDataPath));
        files.addAll(getAllFilePaths(new File(TestDataPath2)));
        time = System.currentTimeMillis() - time;
        System.out.println("done! get " + files.size() + " files cost " + time + " ms");
        
        System.out.println("start digesting files...");
        time = System.currentTimeMillis();
        Map<String, byte[]> digests = new HashMap<>();

//        int count = 0;

            for (String path : files) {
                
                String digestStr = HashUtils.sha256(new File(path));
                
                if (digestStr != null) {     
                    digests.put(path, HashUtils.hex2byte(digestStr));
                }
  
//                count++;
//                if (count==500000){
//                    break;
//                }
            }

        
        time = System.currentTimeMillis() - time;
        System.out.println("digesting files cost : " + time );
        
        time = System.currentTimeMillis() - time;
        
        
        try (FileOutputStream fos = new FileOutputStream(new File(HashMapPath));
             ObjectOutputStream os = new ObjectOutputStream(fos)) {                
            os.writeObject(digests);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
    public static Map<String, byte[]> deserializeAllFilePaths() {
        try (FileInputStream fis = new FileInputStream(new File(HashMapPath));
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            
            return (Map<String , byte[]>) ois.readObject();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
    
    public static void constructFBHTrees(int minTreeHeight, int maxTreeHeight, int step) {
        long time;
        
        // deserialize hash map
        Map<String , byte[]> digests = deserializeAllFilePaths();

        
        for (int treeHeight = minTreeHeight; treeHeight <= maxTreeHeight; treeHeight += step) {
            FBHTree tree = new FBHTree(treeHeight);

            time = System.currentTimeMillis();       
                for (Entry<String, byte[]> entry : digests.entrySet()) {
                    
                    tree.put(entry.getKey(), entry.getValue());                    
                }   

            
            time = System.currentTimeMillis() - time;
            System.out.println("construct " + treeHeight + "-level FBHTree costs " + time + " ms");
            
            String treeFileName = String.format(PathFormat, treeHeight);

            try (FileOutputStream fos = new FileOutputStream(new File(treeFileName));
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                time = System.currentTimeMillis();
                oos.writeObject(tree);
                time = System.currentTimeMillis() - time;
                System.out.println("serialize " + treeHeight + "-level FBHTree costs " + time + " ms");
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static FBHTree deserializeFBHTree(int treeHeight) {
        String path = String.format(PathFormat, treeHeight);
        
            try (FileInputStream fis = new FileInputStream(new File(path));
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                long time = System.currentTimeMillis();
                FBHTree tree = (FBHTree) ois.readObject();
                time = System.currentTimeMillis() - time;
//                System.out.println("deserialize " + treeHeight + "-level FBHTree costs " + time + " ms");

                return tree;
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        return null;
    }
    
    public static void Random_Audit(){
        
        Random r = new Random();
        Map<String , byte[]> digests = deserializeAllFilePaths();
        Set<String> pathSet = digests.keySet();
        String[] paths = pathSet.toArray(new String[pathSet.size()]);
        digests.clear();
        
        System.out.println("done getting paths, length: " + paths.length);
  
        int times=500;
        long audit_elapse=0;        
        float AVG_audit_time;
        int[] heights = new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
        
        for (int treeHeight : heights) {
            FBHTree tree = deserializeFBHTree(treeHeight);
            byte[] Roothash = tree.getRootHash();
            
            for(int i =0;i<times;i++){
                long audit_time = System.currentTimeMillis();
                String random_slice = tree.extractSlice(paths[r.nextInt(paths.length)]);               
                boolean random_equal = Arrays.equals(FBHTree.evalRootHashFromSlice(random_slice), Roothash);               
                audit_time = System.currentTimeMillis()-audit_time;
                audit_elapse += audit_time;              
            }
            System.out.println("FBHTree level-" +treeHeight+ " audit cost time : " + (float) audit_elapse / times);
        }
        
    }

    public static void Random_update(){
        Random r = new Random();
        Map<String , byte[]> digests = deserializeAllFilePaths();
        Set<String> pathSet = digests.keySet();
        String[] paths = pathSet.toArray(new String[pathSet.size()]);
        digests.clear();
        
        System.out.println("done getting paths, length: " + paths.length);
  
        int times=500;
        long update_elapse=0;        
        float AVG_update_time;
        int[] heights = new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
        
        for (int treeHeight : heights) {
            FBHTree tree = deserializeFBHTree(treeHeight);
                        
            for(int i =0;i<times;i++){
                long update_time = System.currentTimeMillis();
                tree.update(paths[r.nextInt(paths.length)], HashUtils.hex2byte(HashUtils.sha256(new File(paths[r.nextInt(paths.length)]))));
                update_time = System.currentTimeMillis()-update_time;
                update_elapse += update_time;              
            }
            System.out.println("FBHTree level-" +treeHeight+ " update cost time : " + (float) update_elapse / times);
        
        }
    }
    
    public static void Add_file(){
        int[] heights = new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
        for (int treeHeight : heights) {
            FBHTree tree = deserializeFBHTree(treeHeight); 
            
             
            String add_file_path = "/Users/lioudapi/Desktop/addfile.txt";
            byte[] add_file_digest = HashUtils.hex2byte(HashUtils.sha256(new File(add_file_path)));
            
            tree.put(add_file_path, add_file_digest);
                        
        }
            
    }
    //Application Photo in mac
    public static void Application_Photo_test(){
        
        for (int treeHeight = 0;treeHeight <= 20 ; treeHeight++) {
            FBHTree tree = deserializeFBHTree(treeHeight); 
            
            int times = 500;
            long extract_elapse = 0;
            long evalue_elapse = 0;
            long audit_elapse = 0;
            
                
            String[] datapath = new String[63];
            datapath[0] = "/System/Library/PrivateFrameworks/PhotosPlayer.framework/Versions/A/PhotosPlayer";
            datapath[1] = "/System/Library/Frameworks/SystemConfiguration.framework/Versions/A/SystemConfiguration";
            datapath[2] = "/System/Library/Frameworks/Photos.framework/Versions/A/Photos";
            datapath[3] = "/System/Library/Frameworks/PhotosUI.framework/Versions/A/PhotosUI";
            datapath[4] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/PhotoLibraryServices.framework/Versions/A/PhotoLibraryServices";
            datapath[5] = "/System/Library/PrivateFrameworks/GeoKit.framework/Versions/A/GeoKit";
            datapath[6] = "/System/Library/Frameworks/OSAKit.framework/Versions/A/OSAKit";
            datapath[7] = "/System/Library/PrivateFrameworks/CloudPhotoServices.framework/Versions/A/Frameworks/CloudPhotoServicesConfiguration.framework/Versions/A/CloudPhotoServicesConfiguration";
            datapath[8] = "/System/Library/PrivateFrameworks/AOSAccounts.framework/Versions/A/AOSAccounts";
            datapath[9] = "/System/Library/PrivateFrameworks/AOSUI.framework/Versions/A/AOSUI";
            datapath[10] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/PhotosImagingFoundation.framework/Versions/A/PhotosImagingFoundation";
            datapath[11] = "/System/Library/PrivateFrameworks/PhotoLibrary.framework/Versions/A/Frameworks/PhotosFormats.framework/Versions/A/PhotosFormats";
            datapath[12] = "/usr/lib/libDiagnosticMessagesClient.dylib";
            datapath[13] = "/System/Library/PrivateFrameworks/PlugInKit.framework/Versions/A/PlugInKit";
            datapath[14] = "/System/Library/Frameworks/IOKit.framework/Versions/A/IOKit";                     
            datapath[15] = "/System/Library/Frameworks/AddressBook.framework/Versions/A/AddressBook";
            datapath[16] = "/System/Library/Frameworks/AudioToolbox.framework/Versions/A/AudioToolbox";
            datapath[17] = "/System/Library/PrivateFrameworks/Slideshows.framework/Versions/A/Slideshows";
            datapath[18] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/PhotoLibraryPrivate";
            datapath[19] = "/System/Library/Frameworks/AVFoundation.framework/Versions/A/AVFoundation";
            datapath[20] = "/System/Library/Frameworks/Carbon.framework/Versions/A/Carbon";
            datapath[21] = "/System/Library/Frameworks/CoreLocation.framework/Versions/A/CoreLocation";
            datapath[22] = "/System/Library/Frameworks/MapKit.framework/Versions/A/MapKit";
            datapath[23] = "/System/Library/Frameworks/CoreMedia.framework/Versions/A/CoreMedia";
            datapath[24] = "/System/Library/Frameworks/QTKit.framework/Versions/A/QTKit";
            datapath[25] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/ShareServicesCore.framework/Versions/A/ShareServicesCore";
            datapath[26] = "/usr/lib/libicucore.A.dylib";
            datapath[27] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/UXKit.framework/Versions/A/UXKit";
            datapath[28] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/PMR.framework/Versions/A/PMR";
            datapath[29] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/Mondrian.framework/Versions/A/Mondrian";
            datapath[30] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/PhotoFoundation.framework/Versions/A/PhotoFoundation";
            datapath[31] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/Geode.framework/Versions/A/Geode";
            datapath[32] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/PAImaging.framework/Versions/A/PAImaging";
            datapath[33] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/PAImagingCore.framework/Versions/A/PAImagingCore";
            datapath[34] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/iLifeKit.framework/Versions/A/iLifeKit";
            datapath[35] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/RedRock.framework/Versions/A/RedRock";
            datapath[36] = "/System/Library/Frameworks/IOSurface.framework/Versions/A/IOSurface";
            datapath[37] = "/System/Library/Frameworks/OpenCL.framework/Versions/A/OpenCL";                      
            datapath[38] = "/usr/lib/libc++.1.dylib";
            datapath[39] = "/System/Library/PrivateFrameworks/PerformanceAnalysis.framework/Versions/A/PerformanceAnalysis";
            datapath[40] = "/System/Library/Frameworks/QuartzCore.framework/Versions/A/QuartzCore";
            datapath[41] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/PhotoPrintProduct.framework/Versions/A/PhotoPrintProduct";
            datapath[42] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/PhotoPrintProductStore.framework/Versions/A/PhotoPrintProductStore";
            datapath[43] = "/System/Library/PrivateFrameworks/CloudPhotoServices.framework/Versions/A/CloudPhotoServices";
            datapath[44] = "/System/Library/Frameworks/OpenGL.framework/Versions/A/OpenGL";
            datapath[45] = "/System/Library/Frameworks/Cocoa.framework/Versions/A/Cocoa";
            datapath[46] = "/System/Library/PrivateFrameworks/PhotoLibraryPrivate.framework/Versions/A/Frameworks/MediaConversionService.framework/Versions/A/MediaConversionService";
            datapath[47] = "/System/Library/PrivateFrameworks/ViewBridge.framework/Versions/A/ViewBridge";
            datapath[48] = "/System/Library/PrivateFrameworks/SocialUI.framework/Versions/A/SocialUI";
            datapath[49] = "/System/Library/PrivateFrameworks/IMCore.framework/Versions/A/IMCore";
            datapath[50] = "/System/Library/PrivateFrameworks/SocialAppsCore.framework/Versions/A/SocialAppsCore";
            datapath[51] = "/System/Library/Frameworks/ImageIO.framework/Versions/A/ImageIO";
            datapath[52] = "/System/Library/Frameworks/Foundation.framework/Versions/C/Foundation";
            datapath[53] = "/usr/lib/libobjc.A.dylib";
            datapath[54] = "/usr/lib/libSystem.B.dylib";
            datapath[55] = "/System/Library/Frameworks/AppKit.framework/Versions/C/AppKit";
            datapath[56] = "/System/Library/Frameworks/ApplicationServices.framework/Versions/A/ApplicationServices";
            datapath[57] = "/System/Library/Frameworks/CoreFoundation.framework/Versions/A/CoreFoundation";
            datapath[58] = "/System/Library/Frameworks/CoreGraphics.framework/Versions/A/CoreGraphics";
            datapath[59] = "/System/Library/Frameworks/CoreImage.framework/Versions/A/CoreImage";
            datapath[60] = "/System/Library/Frameworks/CoreServices.framework/Versions/A/CoreServices";
            datapath[61] = "/System/Library/Frameworks/CoreText.framework/Versions/A/CoreText";
        
            String[] slice = new String[datapath.length];
            boolean[] equal = new boolean[datapath.length];
            byte[] rootHash = tree.getRootHash();
            for (int i = 0 ; i < times ; i ++){
                for (int datanumber = 0;datanumber < datapath.length;datanumber++){
                    slice[datanumber] = tree.extractSlice(datapath[datanumber]);
                    equal[datanumber] = !Arrays.equals(FBHTree.evalRootHashFromSlice(slice[datanumber]), rootHash);
                }
            }
        }    
    }

    public static void main(String[] args) {
        
    }
}
