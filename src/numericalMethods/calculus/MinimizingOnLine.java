package numericalMethods.calculus;
/*
 * Decompiled with CFR 0_102.
 */


import java.io.Serializable;

public abstract class MinimizingOnLine
implements Serializable {
    private static final long serialVersionUID = 1;
    final Line line;
    final RealFunctionOnLine g;
    final double[] abc = new double[3];
    final double[] valuesAtABC = new double[3];
    final double[] result = new double[2];

    public MinimizingOnLine(Line line, RealFunctionOfSeveralVariables f) {
        this.line = line;
        this.g = new RealFunctionOnLine(line, f);
    }

    public MinimizingOnLine(double[] point, double[] direction, RealFunctionOfSeveralVariables f) {
        this(new Line(point, direction), f);
    }

    public abstract double search(double var1);
}

