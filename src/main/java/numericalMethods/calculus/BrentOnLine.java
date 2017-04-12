package numericalMethods.calculus;
/*
 * Decompiled with CFR 0_102.
 */
import java.io.Serializable;

public class BrentOnLine
extends MinimizingOnLine
implements Serializable {
    private static final long serialVersionUID = 1;

    public BrentOnLine(Line line, RealFunctionOfSeveralVariables f) {
        super(line, f);
    }

    public BrentOnLine(double[] point, double[] direction, RealFunctionOfSeveralVariables f) {
        this(new Line(point, direction), f);
    }

    @Override
    public final double search(double tol) {
        this.abc[0] = -0.5;
        this.abc[1] = 0.0;
        this.abc[2] = 0.5;
        Braket.search(this.abc, this.valuesAtABC, this.g);
        Brent.search(this.abc, this.result, this.g, tol);
        double xmin = this.result[0];
        int j = 0;
        while (j < this.line.n) {
            double[] arrd = this.line.point;
            int n = j;
            double[] arrd2 = this.line.direction;
            int n2 = j++;
            double d = arrd2[n2] * xmin;
            arrd2[n2] = d;
            arrd[n] = arrd[n] + d;
        }
        return this.result[1];
    }

    public final double search(int id, double boundguess, double unit_tolerance, double tol, double min, double max) {
        double margin = Math.pow(10.0, -4.0);
        double xt = this.line.point[id] + boundguess * unit_tolerance;
        while ((xt < min - margin || xt > max + margin) && boundguess > 1.0) {
            xt = this.line.point[id] + (boundguess-=1.0) * unit_tolerance;
        }
        this.abc[0] = boundguess * unit_tolerance;
        this.abc[1] = 0.0;
        this.abc[2] = boundguess * unit_tolerance;
        Braket.search(this.abc, this.valuesAtABC, min, max, this.line.point[id], unit_tolerance, this.g, null);
        if (this.abc[0] != this.abc[1]) {
            Brent.search(this.abc, this.result, this.g, unit_tolerance);
        } else if (this.valuesAtABC[0] < this.valuesAtABC[2]) {
            this.result[0] = this.abc[0];
            this.result[1] = this.valuesAtABC[0];
        } else {
            this.result[0] = this.abc[2];
            this.result[1] = this.valuesAtABC[2];
        }
        double xmin = this.result[0];
        int j = 0;
        while (j < this.line.n) {
            double[] arrd = this.line.point;
            int n = j;
            double[] arrd2 = this.line.direction;
            int n2 = j++;
            double d = arrd2[n2] * xmin;
            arrd2[n2] = d;
            arrd[n] = arrd[n] + d;
        }
        return this.result[1];
    }
}

