package bitwalking.bitwalking.mvi.profile;

import com.hannesdorfmann.mosby3.mvp.MvpView;

/**
 * Created by alexey on 20.08.17.
 */

public interface ViewProfile extends MvpView {
   void render(StateProfile state);
}
