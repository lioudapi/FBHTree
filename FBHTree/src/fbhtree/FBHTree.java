/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fbhtree;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.Random;
/**
 * The full binary hash tree proposed by Hong-Fu Chen in 2015.
 *  
 * @author scott
 */
public class FBHTree implements Serializable {
    private static final int DEFAULT_TREE_HEIGHT = 17;
    private static final boolean DEFAULT_ENABLED_LAZY_UPDATE = true;
    
    private static final char SLICE_DELIMITER = '.';
    private static final int ESTIMATED_SLICE_LENGTH = 8192;
    
    private final int height;
    private final boolean lazyUpdate;
    private final Node[] nodes;
    
    /**
     * Construct a FBHTree with initial tree height.
     * @param treeHeight the initial tree height
     * @param enableLazyUpdate specified whether the root hash re-calculates
     *         when any leaf node is updated without being read.
     * @throws IllegalArgumentException if the specified initial tree height is
     *         smaller than 1
     */
    public FBHTree(int treeHeight, boolean enableLazyUpdate) {
        if (treeHeight <= 0) {
            throw new IllegalArgumentException("The minimum value for tree height is 1.");
        }
        
        this.height = treeHeight;
        this.lazyUpdate = enableLazyUpdate;
        this.nodes = new Node[1 << height];
        for (int i = nodes.length - 1; i > 0; i--) {
            if (i >= (1 << (height - 1))) { // leaf node
                nodes[i] = new Node(i, null, null, lazyUpdate);
            }
            else { // internal node
                nodes[i] = new Node(i, nodes[i * 2], nodes[(i * 2) + 1], lazyUpdate);
            }
            
        }
    }
    
    /**
     * Construct a FBHTree with default tree height.
     */
    public FBHTree(int treeHeight) {
        this(treeHeight, DEFAULT_ENABLED_LAZY_UPDATE);
    }
    
    /**
     * Construct a FBHTree with default settings.
     */
    public FBHTree() {
        this(DEFAULT_TREE_HEIGHT, DEFAULT_ENABLED_LAZY_UPDATE);
    }
    
    /**
     * Calculate the slot index which key should be in.
     * @return slot index
     */
    private int calcLeafIndex(String key) {
        byte[] digest = HashUtils.sha256(key.getBytes());
        int index = 0;
        
        if (digest.length >= 4) {
            for (int i = 0; i < 4; i++) {
                index += digest[i] << (i * 8);
            }
        }
        return (1 << (height - 1)) + Math.abs(index) % (1 << (height - 1));
    }
    
    /**
     * Associates the specified value with the specified key in this FBHTree.
     * If the FBHTree previously contained a mapping for the key, the old
     * value is replaced.
     */
    public void put(String key, byte[] digestValue) {
        int index = calcLeafIndex(key);
        
        nodes[index].put(key, digestValue);
        for (int i = index; i > 0; i >>= 1) {
            nodes[i].setDirty(true);
        }
    }
    public void update(String key,byte[] digestValue){
            int index = calcLeafIndex(key);
            
            nodes[index].update(key, digestValue);
            for (int i = index; i > 0; i >>= 1) {
                nodes[i].setDirty(true);
            }
    }
    /**
     * Returns <tt>true</tt> if this FBHTree contains a mapping for the
     * specified key.
     */
    public boolean contains(String key) {
        
        return nodes[calcLeafIndex(key)].contains(key);
    }
    
    /**
     * Removes the mapping for the specified key from this FBHTree if present.
     * 
     * @return <tt>true</tt> if the specified key was in the FBHTree.
     */
    public boolean remove(String key) {
        int index = calcLeafIndex(key);
        
        for (int i = index; i > 0; i /= 2) {
            nodes[i].setDirty(true);
        }
        
        return nodes[index].remove(key);
    }
    
    /**
     * Returns the root hash of this FBHTree.
     * @return 
     */
    public byte[] getRootHash() {
        return nodes[1].getContentDigest();
    }

    /**
     *
     * @param key file pathname
     * @return file's collision numbber
     */
    public int MAX_collision(){
        int Leaf_min = 1<<(height-1);
        int Leaf_max = 1<<(height);
        int MAX=0;
        int nonemptycount=0;
        for (int i = Leaf_min ; i < Leaf_max; i++){
            if ( MAX < nodes[i].getContents().size()){
                MAX=nodes[i].getContents().size();
            }                      
        }        
        return MAX;
    }
    
    public int AVG_collision(){
        int Leaf_min = 1<<(height-1);
        int Leaf_max = 1<<(height);
        int avg=0;
        int nonemptycount=0;
        for (int i = Leaf_min ; i < Leaf_max;i++){
            if (!nodes[i].getContents().isEmpty()){
                nonemptycount+=1;
                avg+=nodes[i].getContents().size();
            }
        }
        avg = avg/nonemptycount;
        return avg;
    }
    

    /**
     * Extract a slice from this FBHTree by specified key.
     * 
     * @return a formatted slice string
     * @throws NoSuchElementException if the specified key does not exist in this
     *         FBHTree.
     */
    public String extractSlice(String key) throws NoSuchElementException {
        if (!contains(key)) {
//            System.out.println(key);
            throw new NoSuchElementException("The specified key does not exist in this FBHTree");
        }
        
        int index = calcLeafIndex(key);
        String leftHexStr, rightHexStr;
        StringBuilder sliceBuilder = new StringBuilder(ESTIMATED_SLICE_LENGTH);
        
        sliceBuilder.append(index).append(SLICE_DELIMITER);
        
        // internal nodes
        for (; index > 1; index /= 2) {
            leftHexStr = rightHexStr = nodes[index].getContentDigestHexString();
            
            if (index % 2 == 0) {
                rightHexStr = nodes[index + 1].getContentDigestHexString();
                
            } else {
                leftHexStr = nodes[index - 1].getContentDigestHexString();
            }
            
            sliceBuilder
                    .append(leftHexStr)
                    .append(SLICE_DELIMITER)
                    .append(rightHexStr)
                    .append(SLICE_DELIMITER);
        }
        
        sliceBuilder.append(nodes[1].getContentDigestHexString());
        
        return sliceBuilder.toString(); 
    }
    
    /**
     * Parse and evaluate the root hash of the given slice recursively.
     * 
     * @return byte array of the root hash of the given slice
     * @throws VerifyError if any parent digest does not match to the digest of
     *         the left child and the right child.
     */
    public static byte[] evalRootHashFromSlice(String slice) {
        String[] tokens = slice.split(String.valueOf("\\" + SLICE_DELIMITER));
        int index = Integer.parseInt(tokens[0]);
        int parentIndex;
        byte[] parentDigest = null;
        
        for (int i = 1; index > 1; i += 2, index /= 2) {
            parentIndex = i + 2 + (index / 2 == 1 ? 0 : index / 2) % 2;
            parentDigest = HashUtils.sha256(
                    HashUtils.hex2byte(tokens[i]),
                    HashUtils.hex2byte(tokens[i + 1]));
            if (!HashUtils.byte2hex(parentDigest).equals(tokens[parentIndex])) {
                throw new VerifyError("Hashes of slice do not match.");
            }
        }
        
        return parentDigest;
    }
    
    /**
     * Basic node for FBHTree.
     */
    private static class Node implements Serializable {
        private final int id;
        private final boolean isLeaf;
        private boolean dirty;
        private final boolean lazyUpdate;
        private byte[] contentDigest;
        private String contentDigestHexStr;
        
        private final Node leftChild;
        private final Node rightChild;
        private LinkedHashMap<String, byte[]> contents;
        
//        private ArrayList<String> contentKeys;
//        private ArrayList<byte[]> contentValues;
        
        public Node(int id, Node leftChild, Node rightChild, boolean enableLazyUpdate) {
            this.id = id;
            this.dirty = false;
            this.lazyUpdate = enableLazyUpdate;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
            
            if (leftChild == null || rightChild == null) { // leaf node
                this.isLeaf = true;
                this.contentDigest = new byte[32];
                
                new Random().nextBytes(this.contentDigest);
            } else { // internal node
                this.isLeaf = false;
                this.contentDigest = HashUtils.sha256(
                        leftChild.getContentDigest(),
                        rightChild.getContentDigest());
            }
            
            this.contentDigestHexStr = HashUtils.byte2hex(contentDigest);
            
            this.contents = null;
            
//            this.contentKeys = null;
//            this.contentValues = null;
        }
        
        public void put(String key, byte[] bytes) {
//            if (contentKeys == null) {
//                contentKeys = new ArrayList<>(1);
//                contentValues = new ArrayList<>(1);
//            }
            if (contents == null) {
                contents = new LinkedHashMap<>();
            }
            
//            contentKeys.add(key);
//            contentValues.add(bytes);
            contents.put(key, bytes);
            setDirty(true);
        }
        public void update(String key, byte[] bytes){
            if (contents == null){
                throw new IllegalStateException("The specified key does not exist in this FBHTree");
            }
                contents.replace(key, bytes);
                setDirty(true);
        }
       
        
//        protected int indexOf(String key) {
//            if (contentKeys != null) {
//                for (int i = 0; i < contentKeys.size(); i++) {
//                    if (contentKeys.get(i).equals(key)) {
//                        return i;
//                    }
//                }
//            }
//                
//            return -1;
//        }
        
        public boolean contains(String key) {
            if (contents != null) {
//            if (contentKeys != null) {
//                return indexOf(key) >= 0;
                return contents.containsKey(key);
            } else {
                return false;
            }
        }
        
        public boolean remove(String key) {
//            int index = indexOf(key);
            if(contains(key)){
//            if (index >= 0) {
//                contentKeys.remove(index);
//                contentValues.remove(index);
                contents.remove(key);
                setDirty(true);
                
                return true;
            } else {
                return false;
            }
        }
        
        private void updateContentDigest() {
            if (isDirty()) {
                if (isLeaf) {
//                    contentDigest = HashUtils.sha256(contentValues);
                    contentDigest = HashUtils.sha256(contents.values());
//                    System.out.println(contents.values());
                } else {
                    contentDigest = HashUtils.sha256(
                            leftChild.getContentDigest(),
                            rightChild.getContentDigest());
                }
                
                contentDigestHexStr = HashUtils.byte2hex(contentDigest);
                
                setDirty(false);
            }
        }
        
        public byte[] getContentDigest() {
            updateContentDigest();
            
            return contentDigest;
        }
        
        public String getContentDigestHexString() {
            updateContentDigest();
            
            return contentDigestHexStr;
        }
        
        public Collection<byte[]> getContents() {
            if (isLeaf) {
//                System.out.println(contents.size());
//                contentKeys = new ArrayList<>(1);
//                contentValues = new ArrayList<>(1);
                if (contents == null) {
                    contents = new LinkedHashMap<>();
                }
//                return contentValues;
                return contents.values();
                
            } else {
                throw new IllegalStateException("Internal node does not have contents.");
            }
        }
        
        public void setDirty(boolean dirty) {
            this.dirty = dirty;
            
            if (!lazyUpdate && this.dirty) {
                updateContentDigest();
            }
        }
        
        public boolean isDirty() {
            return dirty;
        }        
    }
}