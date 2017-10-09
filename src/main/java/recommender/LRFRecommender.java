package recommender;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.eval.Measure;
import net.librec.eval.RecommenderEvaluator;
import net.librec.math.algorithm.Maths;
import net.librec.math.structure.*;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.TensorRecommender;
import net.librec.recommender.item.GenericRecommendedItem;
import net.librec.recommender.item.RecommendedItem;
import net.librec.recommender.item.UserItemRatingEntry;
import net.librec.util.ReflectionUtil;
import org.apache.commons.lang.StringUtils;
import structure.DenseTensor;

import java.util.*;
import java.util.Random;

public class LRFRecommender extends TensorRecommender{
    public BiMap<Integer, String> featureSentimentPairsMappingData;

    protected int numberOfFeatures;
    protected int featurerNum;

    protected BiMap<String, Integer> featureDict;
    protected Table<Integer, Integer, String> userItemFeatureCube;
    protected Table<Integer, Integer, HashSet<String>> posUserItemFeatureSet;
    protected Table<Integer, Integer, HashSet<String>> negUserItemFeatureSet;

    protected DenseMatrix userFactors;
    protected DenseMatrix itemFactors;
    protected DenseMatrix itemFeatureFactors;
    protected DenseMatrix userFeatureFactors;
    protected SparseMatrix trainMatrix;
    /**
     * param
     */
    protected double lambda;
    protected double lambdaU;
    protected double lambdaI;
    protected double lambdaUF;
    protected double lambdaIF;
    protected double learnRate;
    boolean doExplain;


    //確認
    @Override
    public void recommend(RecommenderContext context) throws LibrecException {
        setup();
        trainModel();
    }

    @Override
    protected void setup() throws LibrecException{
        super.setup();
        lambda = conf.getDouble("rec.regularization.lambda", 0.001);
        lambdaU = conf.getDouble("rec.regularization.lambdaU", 0.001);
        lambdaI = conf.getDouble("rec.regularization.lambdaI", 0.001);
        lambdaUF = conf.getDouble("rec.regularization.lambdaUF", 0.001);
        lambdaIF = conf.getDouble("rec.regularization.lambdaIF", 0.001);
        learnRate = conf.getDouble("rec.regularization.learnRate", 0.001);

        featureSentimentPairsMappingData = allFeaturesMappingData.get(2).inverse();
        featureDict = HashBiMap.create();
        userItemFeatureCube = HashBasedTable.create();
        posUserItemFeatureSet = HashBasedTable.create();
        negUserItemFeatureSet = HashBasedTable.create();

        for(TensorEntry te: trainTensor) {
            int[] entryKeys = te.keys();
            int userIndex = entryKeys[0];
            int itemIndex = entryKeys[1];
            int featureSentimentPairsIndex = entryKeys[2];
            String featureSentimentPairsString = featureSentimentPairsMappingData.get(featureSentimentPairsIndex);
            String[] fSPList = featureSentimentPairsString.split(" ");
            posUserItemFeatureSet.put(userIndex, itemIndex, new HashSet<String>());
            negUserItemFeatureSet.put(userIndex, itemIndex, new HashSet<String>());

            for(String p : fSPList) {
                String k = p.split(":")[0];
                if(!featureDict.containsKey(k) && !StringUtils.isEmpty(k)) {
                    featureDict.put(k, numberOfFeatures);
                    numberOfFeatures++;
                }
                if(userItemFeatureCube.contains(userIndex, itemIndex)) {
                    userItemFeatureCube.put(userIndex, itemIndex, userItemFeatureCube.get(userIndex, itemIndex) + " " + p);
                } else {
                    userItemFeatureCube.put(userIndex, itemIndex, p);
                }
                if(!posUserItemFeatureSet.get(userIndex, itemIndex).contains(k)) {
                    HashSet<String> posFeatureIndex = posUserItemFeatureSet.get(userIndex, itemIndex);
                    posFeatureIndex.add(k);
                    posUserItemFeatureSet.put(userIndex, itemIndex, posFeatureIndex);
                }
            }
        }

        for(Table.Cell<Integer, Integer, HashSet<String>> cell: posUserItemFeatureSet.cellSet()) {
            int userIndex = cell.getRowKey();
            int itemIndex = cell.getColumnKey();
            HashSet<String> posFeatureIndexSet= cell.getValue();

            for(Map.Entry entry : featureDict.entrySet()) {
                String featureIndex = (String) entry.getKey();
                if(!posFeatureIndexSet.contains(featureIndex) && !StringUtils.isEmpty(featureIndex)) {
                    HashSet<String> negFeatureIndex = negUserItemFeatureSet.get(userIndex, itemIndex);
                    negFeatureIndex.add(featureIndex);
                    negUserItemFeatureSet.put(userIndex, itemIndex, negFeatureIndex);
                }
            }
        }

        userFactors = new DenseMatrix(numUsers, numFactors);
        setMatrix(userFactors);
        //userFactors.init(1);
        itemFactors = new DenseMatrix(numItems, numFactors);
        setMatrix(itemFactors);
        //itemFactors.init(1);
        userFeatureFactors = new DenseMatrix(numberOfFeatures, numFactors);
        setMatrix(userFeatureFactors);
        //userFeatureFactors.init(1);
        itemFeatureFactors = new DenseMatrix(numberOfFeatures, numFactors);
        setMatrix(itemFeatureFactors);
        //itemFeatureFactors.init(1);

        printMatrix(userFactors);
        printMatrix(itemFactors);
        printMatrix(userFeatureFactors);
        printMatrix(itemFeatureFactors);

        doExplain = conf.getBoolean("rec.explain.flag");
        LOG.info("numUsers:" + numUsers);
        LOG.info("numItems:" + numItems);
        LOG.info("numFeatures:" + numberOfFeatures);

    }


    @Override
    protected void trainModel() throws LibrecException {
        DenseTensor posTen = new DenseTensor(numUsers, numItems, numberOfFeatures);
        DenseTensor negTen = new DenseTensor(numUsers, numItems, numberOfFeatures);
        for (int iter = 1; iter <= conf.getInt("rec.iterator.maximum"); iter++) {
            loss = 0.0;
            for (TensorEntry te : trainTensor) {
                int[] entryKeys = te.keys();
                int userIndex = entryKeys[0];
                int itemIndex = entryKeys[1];
                HashSet<String> posPreSet = new HashSet<String>(posUserItemFeatureSet.get(userIndex, itemIndex));
                HashSet<String> negPreSet = new HashSet<String>(negUserItemFeatureSet.get(userIndex, itemIndex));
                DenseVector preferenceVec = new DenseVector(numberOfFeatures);


                double realRating = te.get();
                System.out.println("userIdx" + userIndex + " " + "itemIdx" + itemIndex + " "+ "realRating" + " " + realRating);
                double predictRating = predict(userIndex, itemIndex);
                System.out.println(predictRating);
                double error = realRating - predictRating;
                System.out.println("error" + error);
                loss += error * error;

                for (int featureIdx = 0; featureIdx < numberOfFeatures; featureIdx++) {
                    preferenceVec.set(featureIdx, DenseMatrix.rowMult(userFactors, userIndex, itemFactors, itemIndex) +
                            DenseMatrix.rowMult(itemFactors, itemIndex, itemFeatureFactors, featureIdx) +
                            DenseMatrix.rowMult(userFactors, userIndex, userFeatureFactors, featureIdx) );
                }
                printVector(preferenceVec, numberOfFeatures);

                System.out.println("posSize" + " " + posPreSet.size() +  " " + "negSize " + negPreSet.size());
                double decayingParameter = 1.0 / ((double) posPreSet.size() * (double) negPreSet.size());
                System.out.println("decayingParameter" + " " + decayingParameter);
                for (String posString : posPreSet) {
                    for (String negString : negPreSet) {
                        int posFeatureIdx = Integer.parseInt(posString);
                        int negFeatureIdx = Integer.parseInt(negString);
                        System.out.println("posIdx" + " " + posFeatureIdx + " " + "negIdx" + " " + negFeatureIdx);
                        double preference = preferenceVec.get(posFeatureIdx) - preferenceVec.get(negFeatureIdx);
                        System.out.println("preference" + " " + preference);
                        double deltaRanking = 1.0 - Maths.logistic(preference);
                        System.out.println("preference" + " " + Maths.logistic(preference));
                        System.out.println("deltaRanking" + " " + deltaRanking);
                        double deltaRating = 2.0 * decayingParameter * te.get() - DenseMatrix.rowMult(userFactors, userIndex, itemFactors, itemIndex);
                        System.out.println("deltaRating" + " " + deltaRating+ "\n");
                        double lossPreference = Math.log(Maths.logistic(preference));
                        loss += -lambda * lossPreference;
                        System.out.println("loss preference " + lossPreference);
                        System.out.println("loss " + -lambda * lossPreference);
                        System.out.println("loss ans " + loss);

                        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                            double userFactorValue = userFactors.get(userIndex, factorIdx);
                            double itemFactorValue = itemFactors.get(itemIndex, factorIdx);
                            double posUserFeatureFactorValue = userFeatureFactors.get(posFeatureIdx, factorIdx);
                            double negUserFeatureFactorValue = userFeatureFactors.get(negFeatureIdx, factorIdx);
                            double posItemFeatureFactorValue = itemFeatureFactors.get(posFeatureIdx, factorIdx);
                            double negItemFeatureFactorValue = itemFeatureFactors.get(negFeatureIdx, factorIdx);

                            userFactors.add(userIndex, factorIdx, learnRate * (deltaRating * itemFactorValue +
                                    lambda * deltaRanking * (posUserFeatureFactorValue - negUserFeatureFactorValue) - lambdaU * userFactorValue));

                            itemFactors.add(itemIndex, factorIdx, learnRate * (deltaRating* userFactorValue +
                                    lambda * deltaRanking * (posItemFeatureFactorValue - negItemFeatureFactorValue) - lambdaI * itemFactorValue));

                            userFeatureFactors.add(posFeatureIdx, factorIdx, learnRate * (lambda * deltaRanking * userFactorValue - lambdaUF * posUserFeatureFactorValue));

                            userFeatureFactors.add(negFeatureIdx, factorIdx, learnRate * (lambda * deltaRanking * (-userFactorValue) - lambdaUF * negUserFeatureFactorValue));

                            itemFeatureFactors.add(posFeatureIdx, factorIdx, learnRate * (lambda * deltaRanking * itemFactorValue - lambdaIF * posItemFeatureFactorValue));

                            itemFeatureFactors.add(negFeatureIdx, factorIdx, learnRate * (lambda * deltaRanking * (-itemFactorValue) - lambdaIF * negItemFeatureFactorValue));
                            System.out.println("userIdx " + userIndex + " factorIdx " + factorIdx + " userfactors " + userFactors.get(userIndex, factorIdx));
                            System.out.println("itemIdx " + itemIndex + " factorIdx " + factorIdx + " itemfactors " + itemFactors.get(itemIndex, factorIdx));
                            System.out.println("posFeatureIdx " + posFeatureIdx + " factorIdx " + factorIdx + " userFeatureFactors " + userFeatureFactors.get(posFeatureIdx, factorIdx));
                            System.out.println("negFeatureIdx " + negFeatureIdx + " factorIdx " + factorIdx + " userFeaturefactors " + userFeatureFactors.get(negFeatureIdx, factorIdx));
                            System.out.println("posFeatureIdx " + posFeatureIdx + " factorIdx " + factorIdx + " itemFeaturefactors " + itemFeatureFactors.get(posFeatureIdx, factorIdx));
                            System.out.println("negFeaturerIdx " + negFeatureIdx + " factorIdx " + factorIdx + " itemFeaturefactors " + itemFeatureFactors.get(negFeatureIdx, factorIdx));
                        }
                    }
                }
            }
            loss += lambdaU * (Math.pow(userFactors.norm(), 2));
            loss += lambdaI * (Math.pow(itemFactors.norm(), 2));
            loss += lambdaUF * (Math.pow(userFeatureFactors.norm(), 2));
            loss += lambdaIF * (Math.pow(itemFeatureFactors.norm(), 2));

            LOG.info("iter:" + iter + ", loss" + loss);
        }
    }
    @Override
    protected double predict(int[] indices) { return predict(indices[0], indices[1]); }
    protected double predict(int userIdx, int itemIdx) {
        return DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
    }

    protected void printMatrix(DenseMatrix mat) {
        System.out.println("Matrix");
        for (int i = 0; i < mat.numRows; i++) {
            for (int j = 0; j < mat.numColumns; j++)  {
                System.out.print(mat.get(i, j) + " ");
            }
            System.out.println("");
        }
    }

    protected void printVector(DenseVector vec, int size) {
        System.out.println("Vector");
        for (int i = 0; i < size; i++) System.out.println(vec.get(i) + " ");
        System.out.println("");
    }

    protected DenseMatrix setMatrix(int numRows, int numColumns, DenseMatrix mat) {
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                Random rand = new Random();
                mat.set(i, j, rand.nextInt(5) + 1);
            }
        }
        return mat;
    }

    protected void setMatrix(DenseMatrix mat) { setMatrix(mat.numRows, mat.numColumns, mat); }

    protected DenseMatrix setMatrix(int numRows, int numColumns) { return setMatrix(numRows, numColumns, new DenseMatrix(numRows, numColumns)); }


    protected double normalize(double rating) { return (rating - minRate) / (maxRate - minRate); }

    protected double predictRating(int userIdx, int itemIdx) {
        return DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
    }

}


