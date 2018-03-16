package bitwalking.bitwalking.mvi.welcome;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by User on 15.08.2017.
 */

public class AdapterText extends FragmentPagerAdapter {

     AdapterText(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:return PageFragment.newInstance("Generate money \n" +
                    "by walking","A step is worth the same value\n" +
                    "for everyon - no matter who you are,\n" +
                    "or where you are.");

            case 1:return PageFragment.newInstance("A new\n" +
                    "global currency","Bitwalking is a new way to participate \n" +
                    "in the world. A technology that walks \n" +
                    "with us, that recognizes our human value. \n" +
                    "A new global currency generated \n" +
                    "by each of us, for all of us.");

            case 2:return PageFragment.newInstance("Discover and\n" +
                    "make a difference","Find local businesses and services\n" +
                    "near you. Buy local and help\n" +
                    "your community grow.");

            case 3:return PageFragment.newInstance("Money won’t create \n" +
                    "success, the freedom \n" +
                    "to make it will.","— Nelson Mandela");

        }


        return PageFragment.newInstance("Discover and make a difference","Find local businesses and services near you. Buy local and help your community grow.");
    }

    @Override
    public int getCount() {
        return 4;
    }

}
