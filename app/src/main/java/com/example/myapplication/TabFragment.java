package com.example.myapplication;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;


/**
 * A simple {@link Fragment} subclass.
 */
public class TabFragment extends Fragment {

    private ViewPager2 mViewPager;
    private TabLayout mTabLayout;
    private PagerAdapter mPagerAdapter;

    public TabFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPagerAdapter = new PagerAdapter(this);
        mViewPager = view.findViewById(R.id.viewPager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setUserInputEnabled(false);

        mTabLayout = view.findViewById(R.id.tabLayout);
        new TabLayoutMediator(mTabLayout, mViewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                switch (position) {
                    case 0:
                        tab.setText("Cas");
                        break;
                    case 1:
                        tab.setText("Morts");
                        break;
                    case 2:
                        tab.setText("Guéris");
                        break;
                }
            }
        }).attach();

    }

    public class PagerAdapter extends FragmentStateAdapter {


        public PagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    CasesFragment cases = new CasesFragment();
                    return cases;
                case 1:
                    DeathsFragment deaths = new DeathsFragment();
                    return deaths;
                case 2:
                    RecoveredFragment recovered = new RecoveredFragment();
                    return recovered;
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
