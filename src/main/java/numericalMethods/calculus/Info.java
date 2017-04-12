package numericalMethods.calculus;
/*
 * Decompiled with CFR 0_102.
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;

public final class Info
implements Serializable {
    private boolean debug;
    private String message = "";
    private int currentIter = 0;
    private int maxIter = 0;

    public Info(boolean debug) {
        this.debug = debug;
    }

    public Info() {
        this(false);
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean getDebug() {
        return this.debug;
    }

    public String getMessage() {
        return this.message;
    }

    public int getCurrentIter() {
        return this.currentIter;
    }

    public int getMaxIter() {
        return this.maxIter;
    }

    public boolean isMaxIterationReached() {
        return this.currentIter >= this.maxIter;
    }

    void setMessage(String str) {
        this.message = str;
    }

    void addMessage(String str) {
        this.message = this.message + "\n" + str;
    }

    void setCurrentIter(int currentIter) {
        this.currentIter = currentIter;
    }

    void setMaxIter(int maxIter) {
        this.maxIter = maxIter;
    }

    public void printDebug() {
        if (this.debug) {
            System.out.println(this.toString());
        }
    }

    public void printDebug(OutputStream out) throws IOException {
        if (this.debug) {
            out.write(this.toString().getBytes());
        }
    }

    public String toString() {
        return "Max Iteration in method: " + this.maxIter + ", reached iteration: " + this.currentIter + "\nMessage: " + this.message;
    }
}

