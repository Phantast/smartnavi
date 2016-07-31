package com.ilm.sandwich.representation;

import java.io.Serializable;

/**
 * Representation of a four-dimensional float-vector
 */
public class Vector4f extends Renderable implements Serializable {

    /**
     * ID for Serialisation
     */
    private static final long serialVersionUID = 1L;
    /**
     * The points.
     */
    protected float points[] = {0, 0, 0, 0};

    /**
     * Instantiates a new vector4f.
     *
     * @param x the x
     * @param y the y
     * @param z the z
     * @param w the w
     */
    public Vector4f(float x, float y, float z, float w) {
        this.points[0] = x;
        this.points[1] = y;
        this.points[2] = z;
        this.points[3] = w;
    }

    /**
     * Instantiates a new vector4f.
     */
    public Vector4f() {
        this.points[0] = 0;
        this.points[1] = 0;
        this.points[2] = 0;
        this.points[3] = 0;
    }

    public Vector4f(Vector3f vector3f, float w) {
        this.points[0] = vector3f.x();
        this.points[1] = vector3f.y();
        this.points[2] = vector3f.z();
        this.points[3] = w;
    }

    /**
     * To array.
     *
     * @return the float[]
     */
    public float[] ToArray() {
        return points;
    }

    public void copyVec4(Vector4f vec) {
        this.points[0] = vec.points[0];
        this.points[1] = vec.points[1];
        this.points[2] = vec.points[2];
        this.points[3] = vec.points[3];
    }

    /**
     * Adds the.
     *
     * @param vector the vector
     */
    public void add(Vector4f vector) {
        this.points[0] += vector.points[0];
        this.points[1] += vector.points[1];
        this.points[2] += vector.points[2];
        this.points[3] += vector.points[3];
    }

    public void add(Vector3f vector, float w) {
        this.points[0] += vector.x();
        this.points[1] += vector.y();
        this.points[2] += vector.z();
        this.points[3] += w;
    }

    public void subtract(Vector4f vector) {
        this.points[0] -= vector.points[0];
        this.points[1] -= vector.points[1];
        this.points[2] -= vector.points[2];
        this.points[3] -= vector.points[3];
    }

    public void subtract(Vector4f vector, Vector4f output) {
        output.setXYZW(this.points[0] - vector.points[0], this.points[1] - vector.points[1], this.points[2]
                - vector.points[2], this.points[3] - vector.points[3]);
    }

    public void subdivide(Vector4f vector) {
        this.points[0] /= vector.points[0];
        this.points[1] /= vector.points[1];
        this.points[2] /= vector.points[2];
        this.points[3] /= vector.points[3];
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
        this.points[3] *= scalar;
    }

    public float dotProduct(Vector4f input) {
        return this.points[0] * input.points[0] + this.points[1] * input.points[1] + this.points[2] * input.points[2]
                + this.points[3] * input.points[3];
    }

    /**
     * Linear interpolation between two vectors storing the result in the output variable.
     *
     * @param input
     * @param output
     * @param t
     */
    public void lerp(Vector4f input, Vector4f output, float t) {
        output.points[0] = (points[0] * (1.0f * t) + input.points[0] * t);
        output.points[1] = (points[1] * (1.0f * t) + input.points[1] * t);
        output.points[2] = (points[2] * (1.0f * t) + input.points[2] * t);
        output.points[3] = (points[3] * (1.0f * t) + input.points[3] * t);

    }

    /**
     * Normalize.
     */
    public void normalize() {
        if (points[3] == 0)
            return;

        points[0] /= points[3];
        points[1] /= points[3];
        points[2] /= points[3];

        double a = Math.sqrt(this.points[0] * this.points[0] + this.points[1] * this.points[1] + this.points[2]
                * this.points[2]);
        points[0] = (float) (this.points[0] / a);
        points[1] = (float) (this.points[1] / a);
        points[2] = (float) (this.points[2] / a);
    }

    /**
     * Gets the x.
     *
     * @return the x
     */
    public float getX() {
        return this.points[0];
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
        return this.points[1];
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
        return this.points[2];
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
     * Gets the w.
     *
     * @return the w
     */
    public float getW() {
        return this.points[3];
    }

    /**
     * Sets the w.
     *
     * @param w the new w
     */
    public void setW(float w) {
        this.points[3] = w;
    }

    public float x() {
        return this.points[0];
    }

    public float y() {
        return this.points[1];
    }

    public float z() {
        return this.points[2];
    }

    public float w() {
        return this.points[3];
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

    public void w(float w) {
        this.points[3] = w;
    }

    public void setXYZW(float x, float y, float z, float w) {
        this.points[0] = x;
        this.points[1] = y;
        this.points[2] = z;
        this.points[3] = w;
    }

    /**
     * Compare this vector4f to the supplied one
     *
     * @param rhs True if they match, false other wise.
     * @return
     */
    public boolean compareTo(Vector4f rhs) {
        boolean ret = false;
        if (this.points[0] == rhs.points[0] && this.points[1] == rhs.points[1] && this.points[2] == rhs.points[2]
                && this.points[3] == rhs.points[3])
            ret = true;
        return ret;
    }

    /**
     * Copies the data from the supplied vec3 into this vec4 plus the supplied w.
     *
     * @param input The x y z values to copy in.
     * @param w     The extra w element to copy in
     */
    public void copyFromV3f(Vector3f input, float w) {
        points[0] = (input.x());
        points[1] = (input.y());
        points[2] = (input.z());
        points[3] = (w);
    }

    @Override
    public String toString() {
        return "X:" + points[0] + " Y:" + points[1] + " Z:" + points[2] + " W:" + points[3];
    }

}