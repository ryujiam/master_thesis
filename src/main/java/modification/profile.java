package modification;
import edu.thuir.sentires.profile.*;
import edu.thuir.sentires.program.Context;
import edu.thuir.sentires.utility.QuickFileReader;
import edu.thuir.sentires.utility.QuickFileWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import edu.thuir.sentires.program.Context;
import edu.thuir.sentires.profile.*;
import net.librec.data.model.ArffAttribute;
import org.apache.log4j.Logger;

public class profile extends modifyArff{
    private String task;
    private String docPath;
    private ArrayList<String> productInfo;
    private int lineCount;

    private String productFile;
    private String lexiconFile;
    private String indicatorFile;
    private String labeledFeatureFile;
    private String labeledOpinionFile;
    private String indexFile;
    private String rerankIndexFile;
    private String sbc2dbcFile;
    private String charsetProduct;
    private String charsetLexicon;
    private String charsetProfile;
    private String charsetIndicator;
    private String charsetLabeledFeature;
    private String charsetLabeledOpinion;
    private String charsetIndex;
    private String charsetSbc2dbc;
    /**
     * outputPath
     */
    private String profilePath;
    /**
     * Arffdata
     */


    private Lexicon lexicon;
    private Set<String> features;
    private Set<String> sortedFeatures;
    private List<Product> products;
    private List<Profile> posProfiles;
    private List<Profile> negProfiles;
    private List<Indicator> indicators;
    private Profile totalPosProfile;
    private Profile totalNegProfile;
    private Profile totalProfile;
    private List<ProductAdder> productAdders;

    private static Logger LOG = Logger.getLogger(profile.class);

    public profile(Configuration conf) {
        super(conf);
        productInfo = new ArrayList<>();
        lineCount = 0;
    }
    public  void readProfile() throws IOException{
        task= conf.get("rec.profile.task");
        //docPath = conf.get("rec.profile.docPath");
        Context context = Context.getContext(task);
        productFile = context.getString("profile.product", (String)null);
        lexiconFile = context.getString("profile.lexicon", (String)null);
        //posProfileFile = context.getString("profile.posprofile", (String)null);
        //negProfileFile = context.getString("profile.negprofile", (String)null);
        indicatorFile = context.getString("profile.indicatorfile", (String)null);
        labeledFeatureFile = context.getString("profile.feature", (String)null);
        labeledOpinionFile = context.getString("profile.opinion", (String)null);
        indexFile = context.getString("profile.index", (String)null);
        rerankIndexFile = context.getString("profile.rerank.index", (String)null);
        sbc2dbcFile = context.getString("profile.sbc2dbc", (String)null);
        charsetProduct = context.getString("profile.product.charset", "UTF8");
        charsetLexicon = context.getString("profile.lexicon.charset", "UTF8");
        charsetProfile = context.getString("profile.profile.charset", "UTF8");
        charsetIndicator = context.getString("profile.indicator.charset", "UTF8");
        charsetLabeledFeature = context.getString("profile.feature.charset", "UTF8");
        charsetLabeledOpinion = context.getString("profile.opinion.charset", "UTF8");
        charsetSbc2dbc = context.getString("profile.sbc2dbc.charset", "UTF8");
        StringManager.init(sbc2dbcFile, charsetSbc2dbc);
        lexicon = new Lexicon(lexiconFile, charsetLexicon);
        features = lexicon.getFeatureSet();
        sortedFeatures = Utility.sortByStringLength(features);
        products = new ArrayList();
        productAdders = new ArrayList();
        posProfiles = new ArrayList();
        negProfiles = new ArrayList();
        indicators = new ArrayList();
        totalPosProfile = new Profile();
        totalNegProfile = new Profile();
        totalProfile = new Profile();
        //QuickFileReader reader = QuickFileReader.create(productFile, charsetProduct);
        //String line = null;
        int productCount = 0;
        /**
         * add ProductAdder
         */
        productAdders = new ArrayList<>();

        /**
         * read DOC dataset
         */
        LOG.info("read DOC file");
        readData();
        for(int var0 = 0; var0 < readLine.size(); var0++) {
            productCount++;
            if(lineCount % 10 == 0) LOG.info("Processing line" + lineCount + "...");

            String data[] = readLine.get(var0).split(splitWord);
            String userId = data[userCol];
            String productName = data[itemCol];
            String[] sentences = data[reviewCol].split("[，,。．.？?！!；;、：:　]+");
            Profile posProfile = new Profile();
            Profile negProfile = new Profile();
            ProductAdder productAdder = new ProductAdder(productName, "no", attributes);
            System.out.println(productAdder.getName());
            productAdder.setData(data);

            for(int var1 = 0; var1 < sentences.length; var1++) {
                String sentence = sentences[var1];
                FSM.fsm(sentence, sortedFeatures, lexicon, posProfile, negProfile, totalPosProfile, totalNegProfile, totalProfile);
            }

            productAdder.addPosReviewsNum(posProfile.getReviewsNum());
            productAdder.addNegReviewsNum(negProfile.getReviewsNum());
            productAdders.add(productAdder);
            System.out.println("size"+productAdders.size());
            posProfiles.add(posProfile);
            negProfiles.add(negProfile);
        }




    }

    @Override
    public void writeData() throws IOException{
        String outPath = conf.get("rec.modiArr.outpath");
        File file = new File(outPath);
        PrintWriter pw  = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        System.out.println("size2"+productAdders.size());

        for(int var0 = 0; var0 < productAdders.size(); var0++) {
            Profile negProfile;
            ProductAdder productAdder = (ProductAdder)productAdders.get(var0);
            negProfile = (Profile)posProfiles.get(var0);
            negProfile.summarizeScore(lexicon, productAdder);
            //negProfile.summarizeScore(lexicon, product);
            negProfile = (Profile)negProfiles.get(var0);
            negProfile.summarizeScore(lexicon, productAdder);
        }

        for(int var0 = 0; var0 < productAdders.size(); var0++) {
            Profile negProfile;
            Profile posProfile = (Profile)posProfiles.get(var0);
            negProfile = (Profile)negProfiles.get(var0);
            Indicator indicator = new Indicator(posProfile, negProfile, totalProfile);
            indicators.add(indicator);
        }

        int index = 0;
        for(int var0 = 0; var0 < productAdders.size(); var0++) {
            index++;
            ProductAdder productAdder = (ProductAdder) productAdders.get(var0);
            Profile posProfile = (Profile)posProfiles.get(var0);
            Profile negProfile = (Profile)negProfiles.get(var0);
            Indicator indicator = (Indicator)indicators.get(var0);
            String productName = StringManager.toDBC(productAdder.getName());
            String userId = StringManager.toDBC(productAdder.getData(userCol));
            String feature;
            double score;

            ProductFeature[] productFeatures;
            productFeatures = posProfile.getRankedFeatures();//getRankfeaturesでないと駄目なのか?
            for(int var1 = 0; var1 < productFeatures.length; var1++) {
                ProductFeature productFeature = productFeatures[var1];
                feature = productFeature.getFeature();
                score = (double)posProfile.getScore(feature);
                pw.println(userId + writeWord + productName + writeWord + feature.toString() + writeWord + "+1");
            }

            productFeatures = negProfile.getRankedFeatures();
            for(int var1 = 0; var1 < productFeatures.length; var1++) {
                ProductFeature productFeature = productFeatures[var1];
                feature = productFeature.getFeature();
                score = (double)negProfile.getScore(feature);
                pw.println(userId + writeWord + productName + writeWord + feature.toString() + writeWord + "-1");
            }

        }
        pw.close();
    }




}
