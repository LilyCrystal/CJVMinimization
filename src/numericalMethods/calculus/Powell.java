package numericalMethods.calculus;
/*
 * Decompiled with CFR 0_102.
 */
import java.io.Serializable;

public final class Powell
implements Serializable {
    private static final long serialVersionUID = 1;
    static int ITMAX = 200;
    private static final double TINY = 1.0 * Math.pow(10.0, -20.0);

    public static int getITMAX() {
        return ITMAX;
    }

    public static void setITMAX(int v) {
        ITMAX = v;
    }

    static final double sqr(double a) {
        return a * a;
    }

    public static double[][] getStandardBasis(int dim) {
        double[][] basis = new double[dim][dim];
        for (int i = 0; i < dim; ++i) {
            basis[i][i] = 1.0;
        }
        return basis;
    }

    public static double getBasisTolerance(int dof, double[] xit, double[] aftol) {
        int i;
        double tol = 0.0;
        double sum = 0.0;
        for (i = 0; i < dof; ++i) {
            sum+=xit[i] * xit[i];
        }
        sum = Math.sqrt(sum);
        for (i = 0; i < dof; ++i) {
            if (aftol[i] <= TINY) continue;
            tol+=Math.abs(xit[i] / (sum * aftol[i]));
        }
        double unit_tolerance = Math.abs(1.0 / tol);
        return unit_tolerance;
    }

    public static final double search(double[] p, double ftol, RealFunctionOfSeveralVariables f) {
        return Powell.search(p, Powell.getStandardBasis(p.length), ftol, f, ITMAX, null);
    }

    public static final double search(double[] p, double ftol, int maxIteration, RealFunctionOfSeveralVariables f) {
        return Powell.search(p, Powell.getStandardBasis(p.length), ftol, f, maxIteration, null);
    }

    public static final double search(double[] p, double ftol, int maxIteration, RealFunctionOfSeveralVariables f, Info info) {
        return Powell.search(p, Powell.getStandardBasis(p.length), ftol, f, maxIteration, info);
    }

    public static double search(double[] p, double[][] xi, double ftol, RealFunctionOfSeveralVariables f, int itMax, Info info) {
        double[] aTuple = new double[2];
        int n = p.length;
        double[] pt = new double[n];
        double[] ptt = new double[n];
        double[] xit = new double[n];
        BrentOnLine brentOnLine = new BrentOnLine(p, xit, f);
        double fret = f.eval(p);
        if (info != null) {
            String s = new String(" f(p) = " + fret + " , p = ");
            for (int i = 0; i < n; ++i) {
                s = s + p[i] + " ";
            }
            info.setMessage(s);
            info.setMaxIter(itMax);
        }
        for (int j = 0; j < n; ++j) {
            pt[j] = p[j];
        }
        int iter = 1;
        do {
            double fptt;
            double t;
            double fp = fret;
            int ibig = 0;
            double del = 0.0;
            for (int i = 0; i < n; ++i) {
                for (int j2 = 0; j2 < n; ++j2) {
                    xit[j2] = xi[j2][i];
                }
                fptt = fret;
                fret = brentOnLine.search(2.0E-8);
                if (Math.abs(fptt - fret) <= del) continue;
                del = Math.abs(fptt - fret);
                ibig = i;
            }
            if (2.0 * Math.abs(fp - fret) <= ftol * (Math.abs(fp) + Math.abs(fret))) {
                if (info != null) {
                    String s = new String("iter = " + iter + ", fret = " + fret + ", fp = " + fp + ", p = ");
                    for (int i2 = 0; i2 < n; ++i2) {
                        s = s + p[i2] + " ";
                    }
                    info.addMessage(s);
                    info.setCurrentIter(iter);
                    info.printDebug();
                }
                return fret;
            }
            if (info != null && iter == itMax) {
                info.setCurrentIter(iter);
                info.setMessage("Too many iterations in routine POWELL");
                info.printDebug();
                return fret;
            }
            for (int j3 = 0; j3 < n; ++j3) {
                ptt[j3] = 2.0 * p[j3] - pt[j3];
                xit[j3] = p[j3] - pt[j3];
                pt[j3] = p[j3];
            }
            fptt = f.eval(ptt);
            if (fptt < fp && (t = 2.0 * (fp - 2.0 * fret + fptt) * Powell.sqr(fp - fret - del) - del * Powell.sqr(fp - fptt)) < 0.0) {
                fret = brentOnLine.search(2.0E-8);
                for (int j4 = 0; j4 < n; ++j4) {
                    xi[j4][ibig] = xit[j4];
                }
            }
            ++iter;
        } while (true);
    }

    public static double search(double[] p, double[][] xi, double[] aftol, RealFunctionOfSeveralVariables f, int itMax, Info info) {
        double[] aTuple = new double[2];
        int n = p.length;
        double[] pt = new double[n];
        double[] ptt = new double[n];
        double[] xit = new double[n];
        BrentOnLine brentOnLine = new BrentOnLine(p, xit, f);
        double fret = f.eval(p);
        if (info != null) {
            String s = new String(" f(p) = " + fret + " , p = ");
            for (int i = 0; i < n; ++i) {
                s = s + p[i] + " ";
            }
            info.setMessage(s);
            info.setMaxIter(itMax);
        }
        for (int j = 0; j < n; ++j) {
            pt[j] = p[j];
        }
        int iter = 1;
        do {
            double t;
            double fptt;
            double fp = fret;
            int ibig = 0;
            double del = 0.0;
            double big_tolerance = 0.0;
            for (int i = 0; i < n; ++i) {
                for (int j2 = 0; j2 < n; ++j2) {
                    xit[j2] = xi[j2][i];
                }
                double unit_tolerance = Powell.getBasisTolerance(n, xit, aftol);
                fptt = fret;
                fret = brentOnLine.search(unit_tolerance);
                if (Math.abs(fptt - fret) <= del) continue;
                del = Math.abs(fptt - fret);
                ibig = i;
                big_tolerance = unit_tolerance;
            }
            if (2.0 * Math.abs(fp - fret) <= big_tolerance * (Math.abs(fp) + Math.abs(fret))) {
                if (info != null) {
                    String s = new String("iter = " + iter + ", fret = " + fret + ", fp = " + fp + ", p = ");
                    for (int i2 = 0; i2 < n; ++i2) {
                        s = s + p[i2] + " ";
                    }
                    info.addMessage(s);
                    info.setCurrentIter(iter);
                    info.printDebug();
                }
                return fret;
            }
            if (info != null && iter == itMax) {
                info.setCurrentIter(iter);
                info.setMessage("Too many iterations in routine POWELL");
                info.printDebug();
                return fret;
            }
            for (int j3 = 0; j3 < n; ++j3) {
                ptt[j3] = 2.0 * p[j3] - pt[j3];
                xit[j3] = p[j3] - pt[j3];
                pt[j3] = p[j3];
            }
            fptt = f.eval(ptt);
            if (fptt < fp && (t = 2.0 * (fp - 2.0 * fret + fptt) * Powell.sqr(fp - fret - del) - del * Powell.sqr(fp - fptt)) < 0.0) {
                fret = brentOnLine.search(Powell.getBasisTolerance(n, xit, aftol));
                for (int j4 = 0; j4 < n; ++j4) {
                    xi[j4][ibig] = xit[j4];
                }
            }
            ++iter;
        } while (true);
    }

    public static double search(double[] p, double[][] xi, double[] aftol, double boundguess, float[][] steps, RealFunctionOfSeveralVariables f, int itMax, Info info) {
        int n = p.length;
        double[] pt = new double[n];
        double[] ptt = new double[n];
        double[] xit = new double[n];
        BrentOnLine brentOnLine = new BrentOnLine(p, xit, f);
        double fret = f.eval(p);
        if (info != null) {
            String s = new String(" f(p) = " + fret + " , p = ");
            for (int i = 0; i < n; ++i) {
                s = s + p[i] + " ";
            }
            info.setMessage(s);
            info.setMaxIter(itMax);
        }
        for (int j = 0; j < n; ++j) {
            pt[j] = p[j];
        }
        int iter = 1;
        do {
            double fptt;
            double t;
            double fp = fret;
            int ibig = 0;
            double del = 0.0;
            double big_tolerance = 0.0;
            for (int i = 0; i < n; ++i) {
                for (int j2 = 0; j2 < n; ++j2) {
                    xit[j2] = xi[j2][i];
                }
                double unit_tolerance = Powell.getBasisTolerance(n, xit, aftol);
                fptt = fret;
                fret = brentOnLine.search(i, boundguess, unit_tolerance, aftol[i], steps[0][i], steps[1][i]);
                if (Math.abs(fptt - fret) <= del) continue;
                del = Math.abs(fptt - fret);
                ibig = i;
                big_tolerance = unit_tolerance;
            }
            if (2.0 * Math.abs(fp - fret) <= big_tolerance * (Math.abs(fp) + Math.abs(fret))) {
                if (info != null) {
                    String s = new String("iter = " + iter + ", fret = " + fret + ", fp = " + fp + ", p = ");
                    for (int i2 = 0; i2 < n; ++i2) {
                        s = s + p[i2] + " ";
                    }
                    info.addMessage(s);
                    info.setCurrentIter(iter);
                    info.printDebug();
                }
                return fret;
            }
            if (info != null && iter == itMax) {
                info.setCurrentIter(iter);
                info.setMessage("Too many iterations in routine POWELL");
                info.printDebug();
                return fret;
            }
            for (int j3 = 0; j3 < n; ++j3) {
                ptt[j3] = 2.0 * p[j3] - pt[j3];
                xit[j3] = p[j3] - pt[j3];
                pt[j3] = p[j3];
            }
            fptt = f.eval(ptt);
            if (fptt < fp && (t = 2.0 * (fp - 2.0 * fret + fptt) * Powell.sqr(fp - fret - del) - del * Powell.sqr(fp - fptt)) < 0.0) {
                double unit_tolerance = Powell.getBasisTolerance(n, xit, aftol);
                fret = brentOnLine.search(ibig, boundguess, unit_tolerance, aftol[ibig], steps[0][ibig], steps[1][ibig]);
                for (int j4 = 0; j4 < n; ++j4) {
                    xi[j4][ibig] = xit[j4];
                }
            }
            ++iter;
        } while (true);
    }
}

