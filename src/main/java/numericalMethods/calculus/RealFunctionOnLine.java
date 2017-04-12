package numericalMethods.calculus;
/*
 * Decompiled with CFR 0_102.
 */
import java.io.Serializable;

class RealFunctionOnLine
implements RealFunctionOfOneVariable,
Serializable {
    private static final long serialVersionUID = 1;
    final RealFunctionOfSeveralVariables f;
    final Line line;

    RealFunctionOnLine(Line line, RealFunctionOfSeveralVariables f) {
        this.line = line;
        this.f = f;
    }

    RealFunctionOnLine(double[] point, double[] direction, RealFunctionOfSeveralVariables f) {
        this(new Line(point, direction), f);
    }

    @Override
    public final double eval(double x) {
        this.line.getPoint(x, this.line.otherPoint);
        return this.f.eval(this.line.otherPoint);
    }

    final double brent(double tol) {
        double[] abc = new double[3];
        double[] valuesAtABC = new double[3];
        double[] result = new double[2];
        abc[0] = -1.0;
        abc[1] = 0.0;
        abc[2] = 1.0;
        Braket.search(abc, valuesAtABC, this);
        Brent.search(abc, result, this, tol);
        double xmin = result[0];
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
        return result[1];
    }
}

