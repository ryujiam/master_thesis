package recommender;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.eval.Measure;
import net.librec.eval.RecommenderEvaluator;
import net.librec.math.structure.*;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.TensorRecommender;
import net.librec.recommender.item.GenericRecommendedItem;
import net.librec.recommender.item.RecommendedItem;
import net.librec.recommender.item.UserItemRatingEntry;
import net.librec.util.ReflectionUtil;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class LRFRecommender extends TensorRecommender{
    public BiMap<Integer, String> featureSentimentPairsMappingData;
    protected int numberOfFeatures;
    protected BiMap<String, Integer> featureDict;

    protected DenseMatrix userFactors;
    protected DenseMatrix itemFactors;
    protected DenseMatrix itemFeatureFactors;
    protected DenseMatrix userFeatureFactors;


    //確認
    @Override
    public void recommend(RecommenderContext context) throws LibrecException {
        setup();
    }

    @Override
    protected void setup() throws LibrecException{
        super.setup();
        featureSentimentPairsMappingData = allFeaturesMappingData.get(2).inverse();
        featureDict = HashBiMap.create();
        Table<Integer, Integer, String> userItemFeatureCube = HashBasedTable.create();

        for(TensorEntry te: trainTensor) {
            int[] entryKeys = te.keys();
            int userIndex = entryKeys[0];
            int itemIndex = entryKeys[1];
            int featureSentimentPairsIndex = entryKeys[2];
            String featureSentimentPairsString = featureSentimentPairsMappingData.get(featureSentimentPairsIndex);
            String[] fSPList = featureSentimentPairsString.split(" ");

            for(String p : fSPList) {
                String k = p.split(":")[0];
                if(!featureDict.containsKey(k) && !StringUtils.isEmpty(k)) {
                    featureDict.put(k, numberOfFeatures);
                    numberOfFeatures++;
                }
                if(userItemFeatureCube.contains(userIndex, itemIndex)) {
                    userItemFeatureCube.put(userIndex, itemIndex, userItemFeatureCube.get(userIndex, itemIndex) + " "+ p);
                } else {
                    userItemFeatureCube.put(userIndex, itemIndex, p);
                }
            }

            userFactors = new DenseMatrix(numUsers, numFactors);
            userFactors.init(1);
            itemFactors = new DenseMatrix(numItems, numFactors);
            itemFactors.init(1);
            userFeatureFactors = new DenseMatrix(numberOfFeatures, numFactors);
            userFeatureFactors.init(1);
            itemFeatureFactors = new DenseMatrix(numberOfFeatures, numFactors);
            itemFeatureFactors.init(1);




        }

    }


    @Override
    protected void trainModel() throws LibrecException {

    }
    @Override
    protected double predict(int[] indices) { return predict(indices[0], indices[1]); }
    protected double predict(int u, int j) {
        return u;
    }

}


