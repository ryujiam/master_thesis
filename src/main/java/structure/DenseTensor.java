package structure;

import java.io.Serializable;

public class DenseTensor implements Serializable{
    /**dimension */
    public int numRows, numColumns, numTubes, topN;

    public double[][][] data;

    public DenseTensor(int numRows, int numColumns, int numTubes) {
        this.numRows = numRows;
        this.numColumns = numColumns;
        this.numTubes = numTubes;

        data = new double[numRows][numColumns][numTubes];
    }

    public DenseTensor(double[][][] array) {
        this(array.length, array[0].length, array[1].length);

        for(int i = 0; i < numRows; i++) {
            for(int j = 0; j < numColumns; j++) {
                for(int k = 0; k < numTubes; k++) {
                    data[i][j][k] = array[i][j][k];
                }
            }
        }
    }

    public DenseTensor(DenseTensor ten) { this(ten.data);}
}
