package numericalMethods.calculus;
/*
 * Decompiled with CFR 0_102.
 */
import java.io.Serializable;

public final class Brent
implements Serializable {
    private static final long serialVersionUID = 1;
    static final double CGOLD = 0.381966;
    static final double ZEPS = 1.0E-17;
    static int ITMAX = 100;

    public static double getITMAX() {
        return ITMAX;
    }

    public static void setITMAX(int v) {
        ITMAX = v;
    }

    private static final double sign(double a, double b) {
        return b > 0.0 ? Math.abs(a) : - Math.abs(a);
    }

    public static final void search(double[] t, double[] X, RealFunctionOfOneVariable f, double tol) {
        Brent.search(t[0], t[1], t[2], X, f, tol);
    }

    public static final void search(double ax, double bx, double cx, double[] X, RealFunctionOfOneVariable f, double tol) {
        Brent.search(ax, bx, cx, X, f, tol, null);
    }

    public static final void search(double ax, double bx, double cx, double[] X, RealFunctionOfOneVariable f, double tol, Info info) {
        double fx;
        double v;
        double fw;
        double d = 0.0;
        double e = 0.0;
        double a = ax < cx ? ax : cx;
        double b = ax > cx ? ax : cx;
        double w = v = bx;
        double x = v;
        double fv = fx = f.eval(x);
        double inValue = fw = fx;
        if (info != null) {
            info.setMaxIter(ITMAX);
        }
        for (int iter = 1; iter <= ITMAX; ++iter) {
            double u;
            double xm = 0.5 * (a + b);
            double tol1 = tol * Math.abs(x) + 1.0E-17;
            double tol2 = 2.0 * tol1;
            if (Math.abs(x - xm) <= tol2 - 0.5 * (b - a)) {
                X[0] = x;
                X[1] = fx;
                if (info != null) {
                    info.setCurrentIter(iter);
                }
                return;
            }
            if (Math.abs(e) > tol1) {
                double r = (x - w) * (fx - fv);
                double q = (x - v) * (fx - fw);
                double p = (x - v) * q - (x - w) * r;
                if ((q = 2.0 * (q - r)) > 0.0) {
                    p = - p;
                }
                q = Math.abs(q);
                double etmp = e;
                e = d;
                if (Math.abs(p) >= Math.abs(0.5 * q * etmp) || p < q * (a - x) || p >= q * (b - x)) {
                    e = x >= xm ? a - x : b - x;
                    d = 0.381966 * e;
                } else {
                    d = p / q;
                    u = x + d;
                    if (u - a < tol2 || b - u < tol2) {
                        d = Brent.sign(tol1, xm - x);
                    }
                }
            } else {
                e = x >= xm ? a - x : b - x;
                d = 0.381966 * e;
            }
            u = Math.abs(d) >= tol1 ? x + d : x + Brent.sign(tol1, d);
            double fu = f.eval(u);
            if (fu <= fx) {
                if (u >= x) {
                    a = x;
                } else {
                    b = x;
                }
                v = w;
                w = x;
                x = u;
                fv = fw;
                fw = fx;
                fx = fu;
                continue;
            }
            if (u < x) {
                a = u;
            } else {
                b = u;
            }
            if (fu <= fw || w == x) {
                v = w;
                w = u;
                fv = fw;
                fw = fu;
                continue;
            }
            if (fu > fv && v != x && v != w) continue;
            v = u;
            fv = fu;
        }
        X[0] = x;
        X[1] = fx;
        if (info != null) {
            String str = "Too many iteration in BRENT\n";
            if (fx > inValue) {
                str = str + " proc Brent failed to decrease center value! " + ax + " " + bx + " " + cx;
            }
            info.setMessage(str);
            info.printDebug();
        }
    }
}

