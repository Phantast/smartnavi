package com.ilm.sandwich.representation;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Leigh Beattie
 *         <p/>
 *         At the moment this is a place holder for objects that can be put in the scene graph. There may be some
 *         requirements later specified.
 */
public class Renderable implements Serializable {

    /**
     * ID for serialisation
     */
    private static final long serialVersionUID = 6701586807666461858L;

    //Used in data managemenst and synchronisation. If you make a renderable then you should change this boolean to true.
    protected boolean dirty = true;
    protected ReentrantLock lock = new ReentrantLock();

    public boolean dirty() {
        return dirty;
    }

    public void setClean() {
        this.dirty = false;
    }

    public void setDirty() {
        this.dirty = true;
    }

    public ReentrantLock getLock() {
        return this.lock;
    }

}
