package com.ilm.sandwich.representation;

import android.util.Log;

/**
 * The Class Matrixf4x4.
 * <p/>
 * Internal the matrix is structured as
 * <p/>
 * [ x0 , y0 , z0 , w0 ] [ x1 , y1 , z1 , w1 ] [ x2 , y2 , z2 , w2 ] [ x3 , y3 , z3 , w3 ]
 * <p/>
 * it is recommend that when setting the matrix values individually that you use the set{x,#} methods, where 'x' is
 * either x, y, z or w and # is either 0, 1, 2 or 3, setY1 for example. The reason you should use these functions is
 * because it will map directly to that part of the matrix regardless of whether or not the internal matrix is column
 * major or not. If the matrix is either or length 9 or 16 it will be able to determine if it can set the value or not.
 * If the matrix is of size 9 but you set say w2, the value will not be set and the set method will return without any
 * error.
 */
public class Matrixf4x4 {

    public static final int[] matIndCol9_3x3 = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    public static final int[] matIndCol16_3x3 = {0, 1, 2, 4, 5, 6, 8, 9, 10};
    public static final int[] matIndRow9_3x3 = {0, 3, 6, 1, 4, 7, 3, 5, 8};
    public static final int[] matIndRow16_3x3 = {0, 4, 8, 1, 5, 9, 2, 6, 10};

    public static final int[] matIndCol16_4x4 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    public static final int[] matIndRow16_4x4 = {0, 4, 8, 12, 1, 5, 9, 13, 2, 6, 10, 14, 3, 7, 11, 15};
    /**
     * The matrix.
     */
    public float[] matrix;
    private boolean colMaj = true;
    private boolean matrixValid = false;

    /**
     * Instantiates a new matrixf4x4. The Matrix is assumed to be Column major, however you can change this by using the
     * setColumnMajor function to false and it will operate like a row major matrix.
     */
    public Matrixf4x4() {
        // The matrix is defined as float[column][row]
        this.matrix = new float[16];
        Matrix.setIdentityM(this.matrix, 0);
        matrixValid = true;
    }

    /**
     * Gets the matrix.
     *
     * @return the matrix, can be null if the matrix is invalid
     */
    public float[] getMatrix() {
        return this.matrix;
    }

    /**
     * Sets the matrix from a float[16] array. If the matrix you set isn't 16 long then the matrix will be set as
     * invalid.
     *
     * @param matrix the new matrix
     */
    public void setMatrix(float[] matrix) {
        this.matrix = matrix;

        if (matrix.length == 16 || matrix.length == 9)
            this.matrixValid = true;
        else {
            this.matrixValid = false;
            Log.e("matrix", "Matrix set is invalid, size is " + matrix.length + " expected 9 or 16");
        }
    }

    public int size() {
        return matrix.length;
    }

    public void setMatrixValues(float[] otherMatrix) {
        if (this.matrix.length != otherMatrix.length) {
            Log.e("matrix", "Matrix set is invalid, size is " + otherMatrix.length + " expected 9 or 16");

        }

        for (int i = 0; i < otherMatrix.length; i++) {
            this.matrix[i] = otherMatrix[i];
        }
    }

    /**
     * Find out if the stored matrix is column major
     *
     * @return
     */
    public boolean isColumnMajor() {
        return colMaj;
    }

    /**
     * Set whether the internal data is col major by passing true, or false for a row major matrix. The matrix is column
     * major by default.
     *
     * @param colMajor
     */
    public void setColumnMajor(boolean colMajor) {
        this.colMaj = colMajor;
    }

    /**
     * Multiply the given vector by this matrix. This should only be used if the matrix is of size 16 (use the
     * matrix.size() method).
     *
     * @param vector A vector of length 4.
     */
    public void multiplyVector4fByMatrix(Vector4f vector) {

        if (matrixValid && matrix.length == 16) {
            float x = 0;
            float y = 0;
            float z = 0;
            float w = 0;

            float[] vectorArray = vector.ToArray();

            if (colMaj) {
                for (int i = 0; i < 4; i++) {

                    int k = i * 4;

                    x += this.matrix[k + 0] * vectorArray[i];
                    y += this.matrix[k + 1] * vectorArray[i];
                    z += this.matrix[k + 2] * vectorArray[i];
                    w += this.matrix[k + 3] * vectorArray[i];
                }
            } else {
                for (int i = 0; i < 4; i++) {

                    x += this.matrix[0 + i] * vectorArray[i];
                    y += this.matrix[4 + i] * vectorArray[i];
                    z += this.matrix[8 + i] * vectorArray[i];
                    w += this.matrix[12 + i] * vectorArray[i];
                }
            }

            vector.setX(x);
            vector.setY(y);
            vector.setZ(z);
            vector.setW(w);
        } else
            Log.e("matrix", "Matrix is invalid, is " + matrix.length + " long, this equation expects a 16 value matrix");
    }

    /**
     * Multiply the given vector by this matrix. This should only be used if the matrix is of size 9 (use the
     * matrix.size() method).
     *
     * @param vector A vector of length 3.
     */
    public void multiplyVector3fByMatrix(Vector3f vector) {

        if (matrixValid && matrix.length == 9) {
            float x = 0;
            float y = 0;
            float z = 0;

            float[] vectorArray = vector.toArray();

            if (!colMaj) {
                for (int i = 0; i < 3; i++) {

                    int k = i * 3;

                    x += this.matrix[k + 0] * vectorArray[i];
                    y += this.matrix[k + 1] * vectorArray[i];
                    z += this.matrix[k + 2] * vectorArray[i];
                }
            } else {
                for (int i = 0; i < 3; i++) {

                    x += this.matrix[0 + i] * vectorArray[i];
                    y += this.matrix[3 + i] * vectorArray[i];
                    z += this.matrix[6 + i] * vectorArray[i];
                }
            }

            vector.setX(x);
            vector.setY(y);
            vector.setZ(z);
        } else
            Log.e("matrix", "Matrix is invalid, is " + matrix.length
                    + " long, this function expects the internal matrix to be of size 9");
    }

    public boolean isMatrixValid() {
        return matrixValid;
    }

    /**
     * Multiply matrix4x4 by matrix.
     *
     * @param matrixf the matrixf
     */
    public void multiplyMatrix4x4ByMatrix(Matrixf4x4 matrixf) {

        // TODO implement Strassen Algorithm in place of this slower naive one.
        if (matrixValid && matrixf.isMatrixValid()) {
            float[] bufferMatrix = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            float[] matrix = matrixf.getMatrix();

            /**
             * for(int i = 0; i < 4; i++){ for(int j = 0; j < 4; j++){
             *
             * int k = i * 4; bufferMatrix[0 + j] += this.matrix[k + j] * matrix[0 * 4 + i]; bufferMatrix[1 * 4 + j] +=
             * this.matrix[k + j] * matrix[1 * 4 + i]; bufferMatrix[2 * 4 + j] += this.matrix[k + j] * matrix[2 * 4 +
             * i]; bufferMatrix[3 * 4 + j] += this.matrix[k + j] * matrix[3 * 4 + i]; } }
             */

            multiplyMatrix(matrix, 0, bufferMatrix, 0);

            matrixf.setMatrix(bufferMatrix);
        } else
            Log.e("matrix", "Matrix is invalid, internal is " + matrix.length + " long" + " , input matrix is "
                    + matrixf.getMatrix().length + " long");

    }

    public void multiplyMatrix(float[] input, int inputOffset, float[] output, int outputOffset) {
        float[] bufferMatrix = output;
        float[] matrix = input;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {

                int k = i * 4;
                bufferMatrix[outputOffset + 0 + j] += this.matrix[k + j] * matrix[inputOffset + 0 * 4 + i];
                bufferMatrix[outputOffset + 1 * 4 + j] += this.matrix[k + j] * matrix[inputOffset + 1 * 4 + i];
                bufferMatrix[outputOffset + 2 * 4 + j] += this.matrix[k + j] * matrix[inputOffset + 2 * 4 + i];
                bufferMatrix[outputOffset + 3 * 4 + j] += this.matrix[k + j] * matrix[inputOffset + 3 * 4 + i];
            }
        }
    }

    /**
     * This will rearrange the internal structure of the matrix. Be careful though as this is an expensive operation.
     */
    public void transpose() {
        if (matrixValid) {
            if (this.matrix.length == 16) {
                float[] newMatrix = new float[16];
                for (int i = 0; i < 4; i++) {

                    int k = i * 4;

                    newMatrix[k] = matrix[i];
                    newMatrix[k + 1] = matrix[4 + i];
                    newMatrix[k + 2] = matrix[8 + i];
                    newMatrix[k + 3] = matrix[12 + i];
                }
                matrix = newMatrix;

            } else {
                float[] newMatrix = new float[9];
                for (int i = 0; i < 3; i++) {

                    int k = i * 3;

                    newMatrix[k] = matrix[i];
                    newMatrix[k + 1] = matrix[3 + i];
                    newMatrix[k + 2] = matrix[6 + i];
                }
                matrix = newMatrix;
            }
        }

    }

    public void setX0(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_3x3[0]] = value;
                else
                    matrix[matIndRow16_3x3[0]] = value;
            } else {
                if (colMaj)
                    matrix[matIndCol9_3x3[0]] = value;
                else
                    matrix[matIndRow9_3x3[0]] = value;
            }
        }
    }

    public void setX1(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_3x3[1]] = value;
                else
                    matrix[matIndRow16_3x3[1]] = value;
            } else {
                if (colMaj)
                    matrix[matIndCol9_3x3[1]] = value;
                else
                    matrix[matIndRow9_3x3[1]] = value;
            }
        }
    }

    public void setX2(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_3x3[2]] = value;
                else
                    matrix[matIndRow16_3x3[2]] = value;
            } else {
                if (colMaj)
                    matrix[matIndCol9_3x3[2]] = value;
                else
                    matrix[matIndRow9_3x3[2]] = value;
            }
        }
    }

    public void setY0(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_3x3[3]] = value;
                else
                    matrix[matIndRow16_3x3[3]] = value;
            } else {
                if (colMaj)
                    matrix[matIndCol9_3x3[3]] = value;
                else
                    matrix[matIndRow9_3x3[3]] = value;
            }
        }
    }

    public void setY1(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_3x3[4]] = value;
                else
                    matrix[matIndRow16_3x3[4]] = value;
            } else {
                if (colMaj)
                    matrix[matIndCol9_3x3[4]] = value;
                else
                    matrix[matIndRow9_3x3[4]] = value;
            }
        }
    }

    public void setY2(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_3x3[5]] = value;
                else
                    matrix[matIndRow16_3x3[5]] = value;
            } else {
                if (colMaj)
                    matrix[matIndCol9_3x3[5]] = value;
                else
                    matrix[matIndRow9_3x3[5]] = value;
            }
        }
    }

    public void setZ0(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_3x3[6]] = value;
                else
                    matrix[matIndRow16_3x3[6]] = value;
            } else {
                if (colMaj)
                    matrix[matIndCol9_3x3[6]] = value;
                else
                    matrix[matIndRow9_3x3[6]] = value;
            }
        }
    }

    public void setZ1(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_3x3[7]] = value;
                else
                    matrix[matIndRow16_3x3[7]] = value;
            } else {
                if (colMaj)
                    matrix[matIndCol9_3x3[7]] = value;
                else
                    matrix[matIndRow9_3x3[7]] = value;
            }
        }
    }

    public void setZ2(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_3x3[8]] = value;
                else
                    matrix[matIndRow16_3x3[8]] = value;
            } else {
                if (colMaj)
                    matrix[matIndCol9_3x3[8]] = value;
                else
                    matrix[matIndRow9_3x3[8]] = value;
            }
        }
    }

    public void setX3(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_4x4[3]] = value;
                else
                    matrix[matIndRow16_4x4[3]] = value;
            }
        }
    }

    public void setY3(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_4x4[7]] = value;
                else
                    matrix[matIndRow16_4x4[7]] = value;
            }
        }
    }

    public void setZ3(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_4x4[11]] = value;
                else
                    matrix[matIndRow16_4x4[11]] = value;
            }
        }
    }

    public void setW0(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_4x4[12]] = value;
                else
                    matrix[matIndRow16_4x4[12]] = value;
            }
        }
    }

    public void setW1(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_4x4[13]] = value;
                else
                    matrix[matIndRow16_4x4[13]] = value;
            }
        }
    }

    public void setW2(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_4x4[14]] = value;
                else
                    matrix[matIndRow16_4x4[14]] = value;
            }
        }
    }

    public void setW3(float value) {

        if (matrixValid) {
            if (matrix.length == 16) {
                if (colMaj)
                    matrix[matIndCol16_4x4[15]] = value;
                else
                    matrix[matIndRow16_4x4[15]] = value;
            }
        }
    }

}
