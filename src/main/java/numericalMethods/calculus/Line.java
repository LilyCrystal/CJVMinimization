package numericalMethods.calculus;
/*
 * Decompiled with CFR 0_102.
 */
import java.io.Serializable;

public class Line
implements Serializable {
    private static final long serialVersionUID = 1;
    final double[] point;
    final double[] direction;
    final double[] otherPoint;
    public final int n;

    public Line(double[] point, double[] direction) {
        this.n = point.length;
        if (this.n != direction.length) {
            throw new IllegalArgumentException(" dimension of direction and point do not coincide ");
        }
        this.point = point;
        this.direction = direction;
        this.otherPoint = new double[this.n];
    }

    public final void getPoint(double t, double[] pointAtT) {
        if (this.n != pointAtT.length) {
            throw new IllegalArgumentException(" dimension of pointAtT does not coincide ");
        }
        for (int i = 0; i < this.n; ++i) {
            pointAtT[i] = this.point[i] + t * this.direction[i];
        }
    }
}

