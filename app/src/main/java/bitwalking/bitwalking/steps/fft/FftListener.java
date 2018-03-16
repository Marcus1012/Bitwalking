package bitwalking.bitwalking.steps.fft;

/**
 * Created by Marcus on 10/22/15.
 */
public interface FftListener {
    void onFFT(float[] fft, float[] ifft, float binWidth);
}
