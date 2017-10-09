package structure;

import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;

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


    public DenseTensor(double[][][] array, int numRows, int numColumns, int numTubes) {
        this.numRows = numRows;
        this.numColumns = numColumns;
        this.numTubes = numTubes;

        this.data = array;
    }

    public DenseTensor(DenseTensor ten) { this(ten.data);}

    public DenseTensor clone() { return new DenseTensor(this);}

    public static DenseTensor eye(int dim) {
        DenseTensor ten = new DenseTensor(dim, dim, dim);
        for(int i = 0; i < ten.numRows; i++)
            ten.set(i, i, i, 1.0);

        return ten;
    }

    public void init(double mean, double sigma) {
        for(int i = 0; i < numRows; i++) {
            for(int j = 0; j < numColumns; j++) {
                for(int k = 0; k < numTubes; k++) {
                    data[i][j][k] = Randoms.gaussian(mean, sigma);
                }
            }
        }
    }

    public void init(double range) {
        for(int i = 0; i < numRows; i++) {
            for(int j = 0; j < numColumns; j++) {
                for(int k = 0; k < numTubes; k++) {
                    data[i][j][k] = Randoms.uniform(0, range);
                }
            }
        }
    }

    public void init() { init(1.0);}

    public int numRows() { return numRows; }

    public int numColumns() { return  numColumns; }

    public int numTubes() { return  numTubes; }

    /**Dense Matrix*/

    public DenseMatrix row(int rowId) { return new DenseMatrix(data[rowId]); }

    public DenseMatrix column(int column) {
        DenseMatrix mat = new DenseMatrix(numRows, numTubes);

        for(int i = 0; i < numRows; i++)
            for(int j = 0; j < numTubes; j++)
                mat.set(i, j, data[i][column][j]);

        return mat;
    }

    public DenseMatrix tube(int tube) {
        DenseMatrix mat = new DenseMatrix(numRows, numColumns);

        for(int i = 0; i < numRows; i++)
            for (int j = 0; j < numColumns; j++)
                mat.set(i, j, data[i][j][tube]);

        return mat;
    }

    public DenseVector rowcolumn(int rowId, int columnId) { return rowcolumn(rowId, columnId, true); }

    public DenseVector rowcolumn(int rowId, int columnId, boolean deep) { return new DenseVector(data[rowId][columnId], deep); }

    public DenseVector rowtube(int row, int tube) {
        DenseVector vec = new DenseVector(numColumns);

        for (int i = 0; i < numColumns; i++)
            vec.set(i, data[row][i][tube]);

        return vec;
    }

    public DenseVector columntube(int column, int tube) {
        DenseVector vec = new DenseVector(numRows);

        for (int i = 0; i < numRows; i++)
            vec.set(i, data[i][column][tube]);

        return vec;
    }

    public double norm() {
        double res = 0;

        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numColumns; j++)
                for (int k = 0; k < numTubes; k++)
                    res += data[i][j][k] * data[i][j][k];

        return Math.sqrt(res);
    }


    /** befor add tensor product */

    public double get(int row, int column, int tube) { return data[row][column][tube]; }


    public void set(int row, int column, int tube, double val) {
        if(topN < 0) {

        } else {
            data[row][column][tube] = val;
        }
    }
}
