package com.visionio.sabpay.authentication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TableLayout;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.visionio.sabpay.R;

import java.util.ArrayList;
import java.util.List;

public class AuthenticationActivity extends AppCompatActivity {

    TabLayout tabLayout;
    ViewPager viewPager;
    protected FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
        mAuth = FirebaseAuth.getInstance();

        viewPager = findViewById(R.id.viewpager);
        addTabs();

        tabLayout = findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void addTabs() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new LoginFragment(), "Login");
        adapter.addFrag(new RegisterFragment(), "Register");
        viewPager.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }



    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List mFragmentList = new ArrayList<>();
        private final List mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager){
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return (Fragment) mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return (CharSequence) mFragmentTitleList.get(position);
        }
    }
}
