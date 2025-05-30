package com.ymp.reold.ai;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WelcomeActivity extends AppCompatActivity {
    private ViewPager viewPager;
    private MyViewPagerAdapter myViewPagerAdapter;
    private LinearLayout dotsLayout;
    private TextView[] dots;
    private int[] layouts;
    private Button btnStart;
    private int[] colors;

    private static final String PREFS_NAME = "AIPrefs";
    private static final String IS_FIRST_LAUNCH = "IsFirstLaunch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(IS_FIRST_LAUNCH, true)) {
            launchMainActivity();
            finish();
            return;
        }

        setContentView(R.layout.activity_welcome);

        viewPager = (ViewPager) findViewById(R.id.welcome_view_pager);
        dotsLayout = (LinearLayout) findViewById(R.id.dots_layout);
        btnStart = (Button) findViewById(R.id.btn_start);

        layouts = new int[]{
                R.layout.welcome_slide, //1
                R.layout.welcome_slide, //2
                R.layout.welcome_slide  //3 (фин.)
        };

        // Инициализация цветов для перехода
        colors = new int[]{
                getResources().getColor(R.color.welcome_red),
                getResources().getColor(R.color.welcome_green),
                getResources().getColor(R.color.welcome_blue)
        };

        addBottomDots(0);
        // Цвет фона (нач.)
        getWindow().getDecorView().setBackgroundColor(colors[0]);

        myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.setOnPageChangeListener(viewPagerPageChangeListener);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchMainActivity();
            }
        });
    }

    private void addBottomDots(int currentPage) {
        dots = new TextView[layouts.length];
        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText("•");
            dots[i].setTextSize(35);
            dots[i].setTextColor(Color.parseColor("#99FFFFFF"));
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0)
            dots[currentPage].setTextColor(Color.WHITE);
    }

    private void launchMainActivity() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(IS_FIRST_LAUNCH, false);
        editor.commit();
        startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
        finish();
    }

    //  ViewPager change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);

            if (position == layouts.length - 1) {
                btnStart.setText(getString(R.string.welcome_button_start));
            } else {
                btnStart.setText(getString(R.string.welcome_button_start));
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // position: текущая страница
            // positionOffset: смещение от 0.0 до 1.0 (0.0 на текущей странице, 1.0 при полном переходе на следующую)
            if (position < (colors.length - 1)) {
                // Вычисляем интерполированный цвет
                int color1 = colors[position];
                int color2 = colors[position + 1];

                int interpolatedColor = interpolateColor(color1, color2, positionOffset);
                getWindow().getDecorView().setBackgroundColor(interpolatedColor);
            } else {
                // Для последней страницы (и если ее прокручивают дальше)
                getWindow().getDecorView().setBackgroundColor(colors[colors.length - 1]);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // No-op
        }
    };

    private int interpolateColor(int color1, int color2, float fraction) {
        int r1 = Color.red(color1);
        int g1 = Color.green(color1);
        int b1 = Color.blue(color1);

        int r2 = Color.red(color2);
        int g2 = Color.green(color2);
        int b2 = Color.blue(color2);

        int r = (int) (r1 + (r2 - r1) * fraction);
        int g = (int) (g1 + (g2 - g1) * fraction);
        int b = (int) (b1 + (b2 - b1) * fraction);

        return Color.rgb(r, g, b);
    }


    /**
     * View pager adapter
     */
    public class MyViewPagerAdapter extends PagerAdapter {
        private LayoutInflater layoutInflater;

        public MyViewPagerAdapter() {
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = layoutInflater.inflate(layouts[position], container, false);

            ImageView slideIcon = (ImageView) view.findViewById(R.id.slide_icon);
            TextView slideTitle = (TextView) view.findViewById(R.id.slide_title);
            TextView slideDescription = (TextView) view.findViewById(R.id.slide_description);

            // Установка содержимого для каждой страницы
            switch (position) {
                case 0:
                    slideIcon.setImageResource(R.drawable.ic_robot);
                    slideTitle.setText(R.string.welcome_title_1);
                    slideDescription.setText(R.string.welcome_text_1);
                    break;
                case 1:
                    slideIcon.setImageResource(R.drawable.pic_old);
                    slideTitle.setText(R.string.welcome_title_2);
                    slideDescription.setText(R.string.welcome_text_2);
                    break;
                case 2:
                    slideIcon.setImageResource(R.drawable.pic_settings);
                    slideTitle.setText(R.string.welcome_title_3);
                    slideDescription.setText(R.string.welcome_text_3);
                    break;
            }

            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }
}