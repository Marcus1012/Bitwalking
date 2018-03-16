package bitwalking.bitwalking.steps.fft;

/**
 * Created by Marcus on 10/19/15.
 */
public class FFT {

    int n, m;

    // Lookup tables. Only need to recompute when size of FFT changes.
    float[] cos;
    float[] sin;

    public FFT(int n) {
        this.n = n;
        this.m = (int) (Math.log(n) / Math.log(2));

        // Make sure n is a power of 2
        if (n != (1 << m))
            throw new RuntimeException("FFT length must be power of 2");

        // precompute tables
        cos = new float[n / 2];
        sin = new float[n / 2];

        for (int i = 0; i < n / 2; i++) {
            cos[i] = (float)Math.cos(-2 * Math.PI * i / n);
            sin[i] = (float)Math.sin(-2 * Math.PI * i / n);
        }

    }

    public void fft(float[] x, float[] y, boolean outAbs) {
        int i, j, k, n1, n2, a;
        float c, s, t1, t2;

        // Bit-reverse
        j = 0;
        n2 = n / 2;
        for (i = 1; i < n - 1; i++) {
            n1 = n2;
            while (j >= n1) {
                j = j - n1;
                n1 = n1 / 2;
            }
            j = j + n1;

            if (i < j) {
                t1 = x[i];
                x[i] = x[j];
                x[j] = t1;
                t1 = y[i];
                y[i] = y[j];
                y[j] = t1;
            }
        }

        // FFT
        n1 = 0;
        n2 = 1;

        for (i = 0; i < m; i++) {
            n1 = n2;
            n2 = n2 + n2;
            a = 0;

            for (j = 0; j < n1; j++) {
                c = cos[a];
                s = sin[a];
                a += 1 << (m - i - 1);

                for (k = j; k < n; k = k + n2) {
                    t1 = c * x[k + n1] - s * y[k + n1];
                    t2 = s * x[k + n1] + c * y[k + n1];
                    x[k + n1] = x[k] - t1;
                    y[k + n1] = y[k] - t2;
                    x[k] = x[k] + t1;
                    y[k] = y[k] + t2;
                }
            }
        }

        if (outAbs) {
            for (i = 0; i < n / 2; ++i) {
                x[i] = (float)Math.sqrt(x[i] * x[i] + y[i] * y[i]);
            }
        }
    }

    // compute the inverse FFT of x[], assuming its length is a power of 2
    public void ifft(float[] x, float[] y, boolean outAbs) {
        int N = x.length;

        // take conjugate
        for (int i = 0; i < N; i++) {
            y[i] = (-1) * y[i];
        }

        // compute forward FFT
        fft(x, y, false);

        // take conjugate again
        for (int i = 0; i < N; i++) {
            y[i] = (-1) * y[i];
        }

        // divide by N
        for (int i = 0; i < N; i++) {
            x[i] = x[i] * (1.0f / N);
            y[i] = y[i] * (1.0f / N);
        }


        if (outAbs) {
            for (int i = 0; i < N; ++i) {
                x[i] = (float)Math.sqrt(x[i] * x[i] + y[i] * y[i]);
            }
        }
    }
}
