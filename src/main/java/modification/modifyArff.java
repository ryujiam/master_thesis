package modification;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.thuir.sentires.utility.Debug;
import edu.thuir.sentires.utility.QuickFileReader;
import edu.thuir.sentires.utility.QuickFileWriter;

import net.librec.data.model.ArffAttribute;
import net.librec.data.model.ArffInstance;
import net.librec.data.convertor.ArffDataConvertor;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

public class modifyArff  extends Configured{


    private static final Log LOG = LogFactory.getLog(modifyArff.class);
    /** The size of the Buffer */
    private static final int BUF_SIZE = 1024 * 1024;
    /**The path of the input data file*/
    private String inPath;
    private String outPath;
    private String relationName;

    /**
     * conf
     */
    protected String splitWord;
    protected String writeWord;


    /**
     * attr data col index
     *
     */
    protected int userCol;
    protected int itemCol;
    protected int ratingCol;
    protected int reviewCol;
    protected int cateCol;//category used in
    protected int helpCol;//helpful of review
    /**
     * attr data list
     */
    protected   ArrayList<ArffInstance> instances;
    protected   ArrayList<ArffAttribute> attributes;
    protected   ArrayList<String> attrTypes;
    protected   ArrayList<BiMap<String, Integer>> columnIds;
    protected   ArrayList<String> readLine;//for write <DOC></DOC>


    /**
     * Initialize
     */

    public modifyArff(Configuration conf) {
        this.conf = conf;

        instances = new ArrayList<>();
        columnIds = new ArrayList<>();
        attributes = new ArrayList<>();
        attrTypes = new ArrayList<>();
        readLine = new ArrayList<>();

        userCol = -1;
        itemCol = -1;
        ratingCol = -1;
        cateCol = -1;
        helpCol = -1;
    }





    public void readData() throws IOException {
            //QuickFileWriter bws = Debug.createWriter("debug.supected.spam");
            //String lineSeperator = System.getProperty("line.separater");
            //QuickFileReader br = QuickFileReader.create(rawData, rawCharset);

            inPath = conf.get("rec.modiArr.inpath");
            splitWord = conf.get("rec.modiArr.splitWord", "::::");
            writeWord = conf.get("rec.modiArr.writeWord", ",");
            HashSet<String> set = new HashSet();
            StringBuffer sb = null;
            boolean flag = false;
            int c = 0;

            final List<File> files = new ArrayList<>();
            final ArrayList<Long> fileSizeList = new ArrayList<>();
            SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
                /**
                 * ファイルにアクセスするときに呼び出される
                 * @param file
                 * @param attrs
                 * @return
                 * @throws IOException
                 */
              @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                  fileSizeList.add(file.toFile().length());
                  files.add(file.toFile());
                  return super.visitFile(file, attrs);
              }
            };

            for(String path : inPath.trim().split(" ")) {
                Files.walkFileTree(Paths.get(path), finder);
            }


        /**
         * read file
         */
        for (File dataFile : files) {
            LOG.info("Now loading dataset file" + dataFile.toString().substring(dataFile.toString().lastIndexOf(File.separator) + 1, dataFile.toString().lastIndexOf(".")));

            BufferedReader br = new BufferedReader(new FileReader(dataFile));

            boolean dataFlag = false;
            int attridx = 0;
            String attrName = null;
            String attrType = null;
            String line = null;




            while(true) {//need to add code for second file
                line = br.readLine();
                if(line == null) break;
                if(line.isEmpty() || line.startsWith("%")) continue;

                if(dataFlag) {//finish read @DATA and read data value
                    readLine.add(line.toString());
                    continue;
                }




                String data[] = line.split("[ \t]");

                if(data[0].toUpperCase().equals("@RELATION")) {
                    relationName = data[1];
                } else if(data[0].toUpperCase().equals("@ATTRIBUTE")) {
                    attrName = data[1];
                    attrType = data[2];
                    if(attrName.equals("user")) userCol = attridx;
                    if(attrName.equals("item")) itemCol = attridx;
                    if(attrName.equals("rating")) ratingCol = attridx;
                    if(attrName.equals("review")) reviewCol = attridx;
                    if(attrName.equals("category")) cateCol = attridx;
                    if(attrName.equals("helpful")) helpCol = attridx;
                    //add nominal data
                    BiMap<String, Integer> colId = HashBiMap.create();

                    System.out.println("columnid");
                    System.out.println(colId.get(0));
                    columnIds.add(colId);
                    attributes.add(new ArffAttribute(attrName, attrType.toUpperCase(), attridx++));
                } else if(data[0].toUpperCase().equals("@DATA")) {
                    dataFlag = true;
                }
            }
            br.close();
        }
    }


    public void writeData() throws IOException{
        LOG.info("start write <DOC></DOC> file");
        outPath = conf.get("rec.modiArr.outpath");
        File file = new File(outPath);
        PrintWriter pw  = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        for(int i = 0; i < readLine.size(); i++) {
            String data[] = readLine.get(i).split(splitWord);
            pw.printf("<DOC>" + ",");
            pw.printf(data[userCol] + ",");
            pw.printf(data[itemCol] + ",");
            pw.printf(data[ratingCol] + ",");
            pw.printf(data[cateCol] + ",");
            pw.printf(data[helpCol] + "\n");
            pw.println(data[reviewCol]);
            pw.println("</DOC>");
        }
        pw.close();
    }

    public ArrayList<ArffAttribute> getAttributes() {
        return attributes;
    }
}
