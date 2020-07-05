package com.example.myapplication;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

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


/**
 * A simple {@link Fragment} subclass.
 */
public class RecoveredFragment extends Fragment {

    private RequestQueue mQueue;

    private SwipeRefreshLayout mRefreshLayout;

    private HorizontalBarChart mHistoryChart;

    private TextView textTotal;
    private TextView textToday;

    private String historyUrl = "https://disease.sh/v3/covid-19/historical/algeria";
    private String dataUrl = "https://disease.sh/v3/covid-19/countries/algeria";

    private boolean connected = false;

    public RecoveredFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recovered, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mQueue = Volley.newRequestQueue(getContext());

        mRefreshLayout = view.findViewById(R.id.recoveredRefreshLayout);

        mHistoryChart = view.findViewById(R.id.recoveredHistoryChart);

        textTotal = view.findViewById(R.id.textTotalRecovered);
        textToday = view.findViewById(R.id.textTodayRecovered);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (connected) {
                    mHistoryChart.invalidate();
                    mHistoryChart.clearValues();
                }
                updateData();
            }
        });

        //init bar char
        mHistoryChart.getXAxis().setEnabled(false);
        mHistoryChart.getDescription().setEnabled(false);
        mHistoryChart.setDrawBarShadow(false);
        mHistoryChart.setDrawValueAboveBar(true);
        mHistoryChart.setPinchZoom(true);
        mHistoryChart.setDrawGridBackground(false);

        XAxis xl = mHistoryChart.getXAxis();
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        //xl.setTypeface(tfLight);
        xl.setDrawAxisLine(true);
        xl.setDrawGridLines(false);
        xl.setGranularity(10f);

        YAxis yl = mHistoryChart.getAxisLeft();
        //yl.setTypeface(tfLight);
        yl.setDrawAxisLine(true);
        yl.setDrawGridLines(true);
        yl.setAxisMinimum(0f); // this replaces setStartAtZero(true)
        //yl.setInverted(true);

        YAxis yr = mHistoryChart.getAxisRight();
        //yr.setTypeface(tfLight);
        yr.setDrawAxisLine(true);
        yr.setDrawGridLines(false);
        yr.setAxisMinimum(0f); // this replaces setStartAtZero(true)
        //yr.setInverted(true);

        mHistoryChart.setFitBars(true);

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
            mHistoryChart.invalidate();
            mHistoryChart.resetZoom();
            mHistoryChart.clearValues();
        }
    }

    private void updateData() {


        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, dataUrl,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                connected = true;

                int todayRecovered;
                int recovered;

                try {
                    todayRecovered = response.getInt("todayRecovered");
                    recovered = response.getInt("recovered");

                } catch (JSONException e) {
                    Toast.makeText(getActivity(), "JSON Exception error", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    return;
                }

                setAnimatedInt(textTotal, 0, recovered);
                setAnimatedInt(textToday, 0, todayRecovered);

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
                    JSONObject deathsObject = response.getJSONObject("timeline").getJSONObject("recovered");
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

        mQueue.add(objectRequestBar);
        mQueue.add(objectRequest);

    }

    private void setBarChartData(HashMap<String, Integer> map) {

        ArrayList<BarEntry> entries = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            entries.add(new BarEntry(i, entry.getValue()));
            i++;
        }

        BarDataSet barDataSet = new BarDataSet(entries, "Guéris ces 30 derniers jours");
        barDataSet.setColor(Color.GREEN);
        barDataSet.setValueTextColor(getColor(R.attr.numbersColors));

        BarData data = new BarData(barDataSet);
        data.setBarWidth(0.8f);

        mHistoryChart.setData(data);
        mHistoryChart.animateY(2000, Easing.EaseInOutCubic);
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

    private int getColor(int id) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(id, typedValue, true);
        @ColorInt int color = typedValue.data;

        return color;
    }
}
