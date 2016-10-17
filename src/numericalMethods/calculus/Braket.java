package numericalMethods.calculus;

/*
 * Decompiled with CFR 0_102.
 */
import java.io.Serializable;

public final class Braket
implements Serializable {
    private static final long serialVersionUID = 1;
    static final double GOLD = 1.618034;
    static final double GLIMIT = 100.0;
    static final double TINY = 1.0E-20;

    private static final double sign(double a, double b) {
        return b > 0.0 ? Math.abs(a) : - Math.abs(a);
    }

    public static void search(double[] t, double[] fOfT, RealFunctionOfOneVariable f) {
        Braket.search(t, fOfT, f, null);
    }

    public static void search(double[] t, double[] fOfT, RealFunctionOfOneVariable f, Info info) {
        double fb;
        double dum = 0.0;
        double ax = t[0];
        double bx = t[1];
        double fa = f.eval(ax);
        double inValue = fb = f.eval(bx);
        if (fb > fa) {
            dum = ax;
            ax = bx;
            bx = dum;
            dum = fa;
            fa = fb;
            fb = dum;
        }
        double cx = bx + 1.618034 * (bx - ax);
        double fc = f.eval(cx);
        while (fb > fc) {
            double fu;
            double r = (bx - ax) * (fb - fc);
            double q = (bx - cx) * (fb - fa);
            double u = bx - ((bx - cx) * q - (bx - ax) * r) / (2.0 * Braket.sign(Math.max(Math.abs(q - r), 1.0E-20), q - r));
            double ulim = bx + 100.0 * (cx - bx);
            if ((bx - u) * (u - cx) > 0.0) {
                fu = f.eval(u);
                if (fu < fc) {
                    ax = bx;
                    bx = u;
                    fa = fb;
                    fb = fu;
                    break;
                }
                if (fu > fb) {
                    cx = u;
                    fc = fu;
                    break;
                }
                u = cx + 1.618034 * (cx - bx);
                fu = f.eval(u);
            } else if ((cx - u) * (u - ulim) > 0.0) {
                fu = f.eval(u);
                if (fu < fc) {
                    bx = cx;
                    cx = u;
                    u = cx + 1.618034 * (cx - bx);
                    fb = fc;
                    fc = fu;
                    fu = f.eval(u);
                }
            } else if ((u - ulim) * (ulim - cx) >= 0.0) {
                u = ulim;
                fu = f.eval(u);
            } else {
                u = cx + 1.618034 * (cx - bx);
                fu = f.eval(u);
            }
            ax = bx;
            bx = cx;
            cx = u;
            fa = fb;
            fb = fc;
            fc = fu;
        }
        t[0] = ax;
        fOfT[0] = fa;
        t[1] = bx;
        fOfT[1] = fb;
        t[2] = cx;
        fOfT[2] = fc;
        if (fb > inValue && info != null) {
            info.setMessage("proc Braket failed to decrease center value! ");
            info.printDebug();
        }
    }

    public static void search(double[] t, double[] fOfT, double maxLimit1D, double minLimit1D, double val, double uTol, RealFunctionOfOneVariable f, Info info) {
        double ax;
        double fa;
        double fc;
        double cx;
        double bx;
        double inValue;
        double fb;
        double dum = 0.0;
        ax = t[0];
        bx = t[1];
        fa = f.eval(ax);
        inValue = fb = f.eval(bx);
        if (fb > fa) {
            dum = ax;
            ax = bx;
            bx = dum;
            dum = fa;
            fa = fb;
            fb = dum;
        }
        double maxC = 0.0;
        maxC = bx - ax > 0.0 ? maxLimit1D - val : minLimit1D - val;
        cx = bx + 1.618034 * (bx - ax);
        double fraction = 1.0;
        while (cx > maxC && 1.618034 * fraction > uTol) {
            cx = bx + 1.618034 * fraction * (bx - ax);
            fraction*=0.95;
        }
        fc = f.eval(cx);
        if (1.618034 * fraction > uTol) {
            while (fb > fc) {
                double fu;
                double r = (bx - ax) * (fb - fc);
                double q = (bx - cx) * (fb - fa);
                double u = bx - ((bx - cx) * q - (bx - ax) * r) / (2.0 * Braket.sign(Math.max(Math.abs(q - r), 1.0E-20), q - r));
                double ulim = bx + 100.0 * (cx - bx);
                if ((bx - u) * (u - cx) > 0.0) {
                    fu = f.eval(u);
                    if (fu < fc) {
                        ax = bx;
                        bx = u;
                        fa = fb;
                        fb = fu;
                        break;
                    }
                    if (fu > fb) {
                        cx = u;
                        fc = fu;
                        break;
                    }
                    u = cx + 1.618034 * (cx - bx);
                    fu = f.eval(u);
                } else if ((cx - u) * (u - ulim) > 0.0) {
                    fu = f.eval(u);
                    if (fu < fc) {
                        bx = cx;
                        cx = u;
                        u = cx + 1.618034 * (cx - bx);
                        fb = fc;
                        fc = fu;
                        fu = f.eval(u);
                    }
                } else if ((u - ulim) * (ulim - cx) >= 0.0) {
                    u = ulim;
                    fu = f.eval(u);
                } else {
                    u = cx + 1.618034 * (cx - bx);
                    fu = f.eval(u);
                }
                ax = bx;
                bx = cx;
                cx = u;
                fa = fb;
                fb = fc;
                fc = fu;
            }
        } else {
            ax = bx;
        }
        t[0] = ax;
        fOfT[0] = fa;
        t[1] = bx;
        fOfT[1] = fb;
        t[2] = cx;
        fOfT[2] = fc;
        if (fb > inValue && info != null) {
            info.setMessage("proc Braket failed to decrease center value! ");
            info.printDebug();
        }
    }
}

