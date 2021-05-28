package android.example.surfacepad.util;

public class FFT {
    private FFT() { }

    public static Complex[] fft(Complex[] x) {
        int n = x.length;

        if (n == 1) {
            return new Complex[] { x[0] };
        }

        Complex[] even = new Complex[n/2];
        for (int k = 0; k < n/2; k++) {
            even[k] = x[2*k];
        }
        Complex[] q = fft(even);

        for (int k = 0; k < n/2; k++) {
            even[k] = x[2*k + 1];
        }
        Complex[] r = fft(even);

        Complex[] y = new Complex[n];
        for (int k = 0; k < n/2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k]       = q[k].plus(wk.times(r[k]));
            y[k + n/2] = q[k].minus(wk.times(r[k]));
        }
        return y;
    }

    public static Complex[] ifft(Complex[] x) {
        int n = x.length;
        Complex[] y = new Complex[n];

        for (int i = 0; i < n; i++) {
            y[i] = x[i].conjugate();
        }

        y = fft(y);

        for (int i = 0; i < n; i++) {
            y[i] = y[i].conjugate();
        }

        for (int i = 0; i < n; i++) {
            y[i] = y[i].scale(1.0 / n);
        }

        return y;
    }
}
