import net.librec.conf.Configuration;
import net.librec.data.model.ArffDataModel;
import net.librec.data.model.TextDataModel;
import net.librec.eval.RecommenderEvaluator;
import net.librec.eval.rating.RMSEEvaluator;
import net.librec.recommender.Recommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.cf.ItemKNNRecommender;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import modification.modifyArff;
import net.librec.conf.Configuration.*;
import oracle.jrockit.jfr.StringConstantPool;
import modification.*;
import recommender.LRFRecommender;


public class Main {
    public static void main(String[] args) throws Exception {
        //Configuration conf = new Configuration();
        //Resource resource = new Resource("experience.properties");
        //conf.addResource(resource);
        //profile Profile = new profile(conf);
        //Profile.readProfile();
        //Profile.writeData();
        // build data model
        Configuration conf = new Configuration();
        Resource resource = new Resource("recommender/lrf-test.properties");
        conf.addResource(resource);
        ArffDataModel dataModel = new ArffDataModel(conf);
        dataModel.buildDataModel();

        // build recommender context
        RecommenderContext context = new RecommenderContext(conf, dataModel);
        Recommender recommender = new LRFRecommender();
        recommender.setContext(context);
        recommender.recommend(context);







        //modi.readData();
        //modi.WriteData();

        /*


        RecommenderContext context = new RecommenderContext(conf, dataModel);

        conf.set("rec.neighbors.knn.number", "5");
        Recommender recommender = new ItemKNNRecommender();
        recommender.setContext(context);

        recommender.recommend(context);
        RecommenderEvaluator evaluator = new RMSEEvaluator();
        System.out.println("RMSE:"+ recommender.evaluate(evaluator));
        */
    }

}
