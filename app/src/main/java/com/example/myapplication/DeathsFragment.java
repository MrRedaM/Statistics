package com.example.myapplication;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;


/**
 * A simple {@link Fragment} subclass.
 */
public class DeathsFragment extends Fragment {

    private RequestQueue mQueue;

    private SwipeRefreshLayout mRefreshLayout;

    private BarChart mHistoryChart;
    private PieChart mPieChart;

    private TextView textTotal;
    private TextView textToday;

    private String historyUrl = "https://disease.sh/v3/covid-19/historical/algeria";
    private String dataUrl = "https://disease.sh/v3/covid-19/countries/algeria";

    private boolean connected = false;

    public DeathsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_deaths, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRefreshLayout = view.findViewById(R.id.recoveredRefreshLayout);
        mHistoryChart = view.findViewById(R.id.recoveredHistoryChart);
        mPieChart = view.findViewById(R.id.recoveredPieChart);
        textTotal = view.findViewById(R.id.textTotalDeaths);
        textToday = view.findViewById(R.id.textTodayDeaths);

        mQueue = Volley.newRequestQueue(getContext());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (connected) {
                    mPieChart.invalidate();
                    mPieChart.clear();
                    mHistoryChart.invalidate();
                    mHistoryChart.clearValues();
                    mHistoryChart.resetZoom();
                }
                updateData();
            }
        });

        //init PieChart
        mPieChart.setUsePercentValues(true);
        mPieChart.getDescription().setEnabled(false);
        mPieChart.setExtraOffsets(5, 10, 5,5);
        mPieChart.setDragDecelerationFrictionCoef(0.95f);
        mPieChart.setCenterText(generateCenterSpannableText());
        mPieChart.setDrawHoleEnabled(true);
        mPieChart.setHoleColor(Color.TRANSPARENT);
        //mPieChart.setTransparentCircleColor(Color.BLACK);
        mPieChart.setTransparentCircleRadius(0);
        //mPieChart.setTransparentCircleAlpha(110);
        //mPieChart.setHoleRadius(58f);
        mPieChart.setDrawCenterText(true);
        mPieChart.setRotationEnabled(false);
        mPieChart.setHighlightPerTapEnabled(true);
        mPieChart.getLegend().setEnabled(false);

        //half pie
        mPieChart.setMaxAngle(180f);
        mPieChart.setRotationAngle(180f);
        mPieChart.setCenterTextOffset(0, -20);

        //init bar chart
        mHistoryChart.getXAxis().setEnabled(false);
        mHistoryChart.getDescription().setEnabled(false);
        //mHistoryChart.setDrawBarShadow(true);
        mHistoryChart.setDrawValueAboveBar(true);
        mHistoryChart.setPinchZoom(true);
        mHistoryChart.setDrawGridBackground(false);

        //for theme
        mHistoryChart.getLegend().setTextColor(getColor(R.attr.numbersColors));
        mHistoryChart.getAxisLeft().setTextColor(getColor(R.attr.numbersColors));
        mHistoryChart.getAxisLeft().setAxisLineColor(getColor(R.attr.numbersColors));
        mHistoryChart.getAxisRight().setTextColor(getColor(R.attr.numbersColors));
        mHistoryChart.getAxisRight().setAxisLineColor(getColor(R.attr.numbersColors));

        //updateData();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (connected) {
            mPieChart.invalidate();
            mPieChart.clear();
            mHistoryChart.invalidate();
            mHistoryChart.clearValues();
        }
    }

    private void updateData() {

        JsonObjectRequest objectRequestPie = new JsonObjectRequest(Request.Method.GET, dataUrl,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                connected = true;

                int deaths;
                int todayDeaths;
                int recovered;

                try {
                     deaths = response.getInt("deaths");
                     todayDeaths = response.getInt("todayDeaths");
                     recovered = response.getInt("recovered");

                } catch (JSONException e) {
                    Toast.makeText(getActivity(), "JSON Exception error", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    return;
                }

                setAnimatedInt(textTotal, 0, deaths);
                setAnimatedInt(textToday, 0, todayDeaths);
                setPieChartData(recovered, deaths);

                //mRefreshLayout.setRefreshing(false);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getActivity(), "Failed to load data, please check your internet connection", Toast.LENGTH_SHORT).show();
                error.printStackTrace();
                mRefreshLayout.setRefreshing(false);
                connected = false;
            }
        });

        JsonObjectRequest objectRequestBar = new JsonObjectRequest(Request.Method.GET, historyUrl,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                connected = true;

                HashMap<String, Integer> map = new HashMap<>();

                try {
                    JSONObject deathsObject = response.getJSONObject("timeline").getJSONObject("deaths");
                    Iterator<String> dates = deathsObject.keys();

                    while (dates.hasNext()) {
                        String key = dates.next();
                        map.put(key, deathsObject.getInt(key));
                    }



                } catch (JSONException e) {
                    Toast.makeText(getActivity(), "JSON Exception error", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    mRefreshLayout.setRefreshing(false);
                    return;
                }

                setBarChartData(sortByValue(map));

                mRefreshLayout.setRefreshing(false);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getActivity(), "Failed to load data, please check your internet connection", Toast.LENGTH_SHORT).show();
                error.printStackTrace();
                mRefreshLayout.setRefreshing(false);
                connected = false;
            }
        });

        mQueue.add(objectRequestPie);
        mQueue.add(objectRequestBar);

    }

    // function to sort hashmap by values
    public static HashMap<String, Integer> sortByValue(HashMap<String, Integer> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Integer> > list =
                new LinkedList<Map.Entry<String, Integer> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Integer> >() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2)
            {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        // put data from sorted list to hashmap
        HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    private void setBarChartData(HashMap<String, Integer> map) {

        ArrayList<BarEntry> entries = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            entries.add(new BarEntry(i, entry.getValue()));
            i++;
        }

        BarDataSet barDataSet = new BarDataSet(entries, "Morts ces 30 derniers jours");
        barDataSet.setColor(Color.RED);
        //barDataSet.setBarShadowColor(Color.TRANSPARENT);
        barDataSet.setValueTextColor(getColor(R.attr.numbersColors));

        BarData data = new BarData(barDataSet);
        data.setBarWidth(0.9f);

        mHistoryChart.setData(data);
        mHistoryChart.animateY(2000, Easing.EaseInOutCubic);
    }

    private void setPieChartData(final int recovered, int deaths) {

        ArrayList<PieEntry> entries = new ArrayList<>();

        entries.add(new PieEntry(deaths, "Morts"));
        entries.add(new PieEntry(recovered, "Guéris"));

        PieDataSet dataSet = new PieDataSet(entries, "Cas Actifs");
        dataSet.setDrawIcons(false);
        //dataSet.setIconsOffset(new MPPointF(0, 40));
        dataSet.setSelectionShift(5f);
        dataSet.setSliceSpace(3f);

        ArrayList<Integer> colors = new ArrayList<>();

        colors.add(Color.RED);
        colors.add(Color.GREEN);

        dataSet.setColors(colors);

        final PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(mPieChart));
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.BLUE);


        // undo all highlights
        mPieChart.highlightValues(null);

        //mActiveChart.setCenterText(generateCenterSpannableText(actives));

        mPieChart.invalidate();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPieChart.setData(data);
                mPieChart.animateY(2000, Easing.EaseInOutCubic);
            }
        }, 1500);
    }

    private void setAnimatedInt(final TextView textView, int start, int end) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setDuration(2000);
        animator.setInterpolator(new DecelerateInterpolator(1.7f));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                textView.setText(animation.getAnimatedValue().toString());
            }
        });
        animator.start();
    }

    private SpannableString generateCenterSpannableText() {

        SpannableString s = new SpannableString("Morts/Guéris");
        s.setSpan(new RelativeSizeSpan(2.0f), 0, 12, 0);
        s.setSpan(new StyleSpan(Typeface.BOLD), 0, 12, 0);
        s.setSpan(new ForegroundColorSpan(getColor(R.attr.titlesColor)), 0, 12, 0);
        return s;
    }

    private int getColor(int id) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(id, typedValue, true);
        @ColorInt int color = typedValue.data;

        return color;
    }
}
