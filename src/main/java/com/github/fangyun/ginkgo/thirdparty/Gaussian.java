package com.github.fangyun.ginkgo.thirdparty;

/*************************************************************************
 *
 *  From Sedgewick and Wayne, Introduction to Programming in Java
 *  http://introcs.cs.princeton.edu/java/code/
 *  Used with permission
 *
 *  Compilation:  javac Gaussian.java
 *  Execution:    java Gaussian x mu sigma
 *
 * 计算高斯概率密度函数和高斯累积密度函数
 *
 *  % java Gaussian 820 1019 209
 *  0.17050966869132111
 *
 *  % java Gaussian 1500 1019 209
 *  0.9893164837383883
 *
 *  % java Gaussian 1500 1025 231
 *  0.9801220907365489
 *
 *  这个近似精确到绝对误差小于等于 8 * 10^(-16).
 *  Reference: Evaluating the Normal Distribution by George Marsaglia.
 *  http://www.jstatsoft.org/v11/a04/paper
 *
 *************************************************************************/

public class Gaussian {

    // return phi(x) = standard Gaussian pdf
    public static double phi(double x) {
        return Math.exp(-x*x / 2) / Math.sqrt(2 * Math.PI);
    }

    // return phi(x, mu, signma) = Gaussian pdf with mean mu and stddev sigma
    public static double phi(double x, double mu, double sigma) {
        return phi((x - mu) / sigma) / sigma;
    }

    // return Phi(z) = standard Gaussian cdf using Taylor approximation
    public static double Phi(double z) {
        if (z < -8.0) return 0.0;
        if (z >  8.0) return 1.0;
        double sum = 0.0, term = z;
        for (int i = 3; sum + term != sum; i += 2) {
            sum  = sum + term;
            term = term * z * z / i;
        }
        return 0.5 + sum * phi(z);
    }

    // return Phi(z, mu, sigma) = Gaussian cdf with mean mu and stddev sigma
    public static double Phi(double z, double mu, double sigma) {
        return Phi((z - mu) / sigma);
    } 

    // Compute z such that Phi(z) = y via bisection search
    public static double PhiInverse(double y) {
        return PhiInverse(y, .00000001, -8, 8);
    } 

    // bisection search
    private static double PhiInverse(double y, double delta, double lo, double hi) {
        double mid = lo + (hi - lo) / 2;
        if (hi - lo < delta) return mid;
        if (Phi(mid) > y) return PhiInverse(y, delta, lo, mid);
        else              return PhiInverse(y, delta, mid, hi);
    }

    // test client
    public static void main(String[] args) {
        double z     = Double.parseDouble(args[0]);
        double mu    = Double.parseDouble(args[1]);
        double sigma = Double.parseDouble(args[2]);
        System.out.println(Phi(z, mu, sigma));
        double y = Phi(z);
        System.out.println(PhiInverse(y));
    }
}
