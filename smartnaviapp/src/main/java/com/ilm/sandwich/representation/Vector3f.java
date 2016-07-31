package com.ilm.sandwich.representation;

/**
 * 3-dimensional vector with conventient getters and setters. Additionally this class is serializable and
 */
public class Vector3f extends Renderable {

    /**
     * ID for serialisation
     */
    private static final long serialVersionUID = -4565578579900616220L;

    /**
     * A float array was chosen instead of individual variables due to performance concerns. Converting the points into
     * an array at run time can cause slowness so instead we use one array and extract the individual variables with get
     * methods.
     */
    protected float[] points = new float[3];

    /**
     * Initialises the vector with the given values
     *
     * @param x the x-component
     * @param y the y-component
     * @param z the z-component
     */
    public Vector3f(float x, float y, float z) {
        this.points[0] = x;
        this.points[1] = y;
        this.points[2] = z;
    }

    /**
     * Initialises all components of this vector with the given same value.
     *
     * @param value Initialisation value for all components
     */
    public Vector3f(float value) {
        this.points[0] = value;
        this.points[1] = value;
        this.points[2] = value;
    }

    /**
     * Instantiates a new vector3f.
     */
    public Vector3f() {
    }

    /**
     * Copy constructor
     */
    public Vector3f(Vector3f vector) {
        this.points[0] = vector.points[0];
        this.points[1] = vector.points[1];
        this.points[2] = vector.points[2];
    }

    /**
     * Initialises this vector from a 4-dimensional vector. If the fourth component is not zero, a normalisation of all
     * components will be performed.
     *
     * @param vector The 4-dimensional vector that should be used for initialisation
     */
    public Vector3f(Vector4f vector) {
        if (vector.w() != 0) {
            this.points[0] = vector.x() / vector.w();
            this.points[1] = vector.y() / vector.w();
            this.points[2] = vector.z() / vector.w();
        } else {
            this.points[0] = vector.x();
            this.points[1] = vector.y();
            this.points[2] = vector.z();
        }
    }

    /**
     * Returns this vector as float-array.
     *
     * @return the float[]
     */
    public float[] toArray() {
        return this.points;
    }

    /**
     * Adds a vector to this vector
     *
     * @param summand the vector that should be added component-wise
     */
    public void add(Vector3f summand) {
        this.points[0] += summand.points[0];
        this.points[1] += summand.points[1];
        this.points[2] += summand.points[2];
    }

    /**
     * Adds the value to all components of this vector
     *
     * @param summand The value that should be added to all components
     */
    public void add(float summand) {
        this.points[0] += summand;
        this.points[1] += summand;
        this.points[2] += summand;
    }

    /**
     * @param subtrahend
     */
    public void subtract(Vector3f subtrahend) {
        this.points[0] -= subtrahend.points[0];
        this.points[1] -= subtrahend.points[1];
        this.points[2] -= subtrahend.points[2];
    }

    /**
     * Multiply by scalar.
     *
     * @param scalar the scalar
     */
    public void multiplyByScalar(float scalar) {
        this.points[0] *= scalar;
        this.points[1] *= scalar;
        this.points[2] *= scalar;
    }

    /**
     * Normalize.
     */
    public void normalize() {

        double a = Math.sqrt(points[0] * points[0] + points[1] * points[1] + points[2] * points[2]);
        this.points[0] = (float) (this.points[0] / a);
        this.points[1] = (float) (this.points[1] / a);
        this.points[2] = (float) (this.points[2] / a);

    }

    /**
     * Gets the x.
     *
     * @return the x
     */
    public float getX() {
        return points[0];
    }

    /**
     * Sets the x.
     *
     * @param x the new x
     */
    public void setX(float x) {
        this.points[0] = x;
    }

    /**
     * Gets the y.
     *
     * @return the y
     */
    public float getY() {
        return points[1];
    }

    /**
     * Sets the y.
     *
     * @param y the new y
     */
    public void setY(float y) {
        this.points[1] = y;
    }

    /**
     * Gets the z.
     *
     * @return the z
     */
    public float getZ() {
        return points[2];
    }

    /**
     * Sets the z.
     *
     * @param z the new z
     */
    public void setZ(float z) {
        this.points[2] = z;
    }

    /**
     * Functions for convenience
     */

    public float x() {
        return this.points[0];
    }

    public float y() {
        return this.points[1];
    }

    public float z() {
        return this.points[2];
    }

    public void x(float x) {
        this.points[0] = x;
    }

    public void y(float y) {
        this.points[1] = y;
    }

    public void z(float z) {
        this.points[2] = z;
    }

    public void setXYZ(float x, float y, float z) {
        this.points[0] = x;
        this.points[1] = y;
        this.points[2] = z;
    }

    /**
     * Return the dot product of this vector with the input vector
     *
     * @param inputVec The vector you want to do the dot product with against this vector.
     * @return Float value representing the scalar of the dot product operation
     */
    public float dotProduct(Vector3f inputVec) {
        return points[0] * inputVec.points[0] + points[1] * inputVec.points[1] + points[2] * inputVec.points[2];

    }

    /**
     * Get the cross product of this vector and another vector. The result will be stored in the output vector.
     *
     * @param inputVec  The vector you want to get the dot product of against this vector.
     * @param outputVec The vector to store the result in.
     */
    public void crossProduct(Vector3f inputVec, Vector3f outputVec) {
        outputVec.setX(points[1] * inputVec.points[2] - points[2] * inputVec.points[1]);
        outputVec.setY(points[2] * inputVec.points[0] - points[0] * inputVec.points[2]);
        outputVec.setZ(points[0] * inputVec.points[1] - points[1] * inputVec.points[0]);
    }

    public Vector3f crossProduct(Vector3f in) {
        Vector3f out = new Vector3f();
        crossProduct(in, out);
        return out;
    }

    /**
     * If you need to get the length of a vector then use this function.
     *
     * @return The length of the vector
     */
    public float getLength() {
        return (float) Math.sqrt(points[0] * points[0] + points[1] * points[1] + points[2] * points[2]);
    }

    @Override
    public String toString() {
        return "X:" + points[0] + " Y:" + points[1] + " Z:" + points[2];
    }

    /**
     * Clone the input vector so that this vector has the same values.
     *
     * @param source The vector you want to clone.
     */
    public void clone(Vector3f source) {
        // this.points[0] = source.points[0];
        // this.points[1] = source.points[1];
        // this.points[2] = source.points[2];
        System.arraycopy(source.points, 0, points, 0, 3);
    }

    /**
     * Clone the input vector so that this vector has the same values.
     *
     * @param source The vector you want to clone.
     */
    public void clone(float[] source) {
        // this.points[0] = source[0];
        // this.points[1] = source[1];
        // this.points[2] = source[2];
        System.arraycopy(source, 0, points, 0, 3);
    }
}
