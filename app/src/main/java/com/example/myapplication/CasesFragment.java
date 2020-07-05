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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.gms.security.ProviderInstaller;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.net.ssl.TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.ResourceBundle;


/**
 * A simple {@link Fragment} subclass.
 */
public class CasesFragment extends Fragment implements OnChartGestureListener,
        OnChartValueSelectedListener {

    private RequestQueue mQueue;

    private SwipeRefreshLayout mRefreshLayout;

    private PieChart mActiveChart;
    private LineChart mCasesHistoryChart;

    private String dataUrl = "https://disease.sh/v3/covid-19/countries/algeria";
    private String historyUrl = "https://disease.sh/v3/covid-19/historical/algeria";

    private TextView textCasesAll;
    private TextView textCasesActive;
    private TextView textCasesToday;

    private int cases_all;
    private int cases_critical;
    private int cases_active;
    private int cases_today;

    private boolean connected = false;

    public CasesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_cases, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mActiveChart = view.findViewById(R.id.recoveredPieChart);
        mCasesHistoryChart = view.findViewById(R.id.recoveredHistoryChart);

        mRefreshLayout = view.findViewById(R.id.recoveredRefreshLayout);

        textCasesAll = view.findViewById(R.id.textTotalCases);
        textCasesToday = view.findViewById(R.id.textTodayCases);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mQueue = Volley.newRequestQueue(getContext());
        updateAndroidSecurityProvider();

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //mActiveChart.invalidate();
                //mActiveChart.clearValues();
                if (connected) {
                    mCasesHistoryChart.clearValues();
                    mCasesHistoryChart.invalidate();
                    mCasesHistoryChart.resetZoom();
                }
                updateData();
            }
        });

        //init PieChart
        //mActiveChart.setUsePercentValues(true);
        mActiveChart.getDescription().setEnabled(false);
        mActiveChart.setExtraOffsets(5, 10, 5,5);
        mActiveChart.setDragDecelerationFrictionCoef(0.95f);
        mActiveChart.setCenterText(generateCenterSpannableText(0));
        mActiveChart.setDrawHoleEnabled(true);
        mActiveChart.setHoleColor(Color.TRANSPARENT);
        mActiveChart.setTransparentCircleColor(getColor(R.attr.pieTransparentColor));
        mActiveChart.setTransparentCircleRadius(55f);
        mActiveChart.setTransparentCircleAlpha(110);
        //mActiveChart.setHoleRadius(58f);
        mActiveChart.setDrawCenterText(true);
        //mActiveChart.setRotationAngle(0);
        mActiveChart.setRotationEnabled(true);
        mActiveChart.setHighlightPerTapEnabled(true);
        mActiveChart.getLegend().setEnabled(false);
        mActiveChart.setEntryLabelColor(Color.BLACK);

        //init line chart
        mCasesHistoryChart.getDescription().setEnabled(false);
        mCasesHistoryChart.setOnChartGestureListener(this);
        mCasesHistoryChart.setOnChartValueSelectedListener(this);
        mCasesHistoryChart.getXAxis().setEnabled(false);
        mCasesHistoryChart.setDragEnabled(true);
        mCasesHistoryChart.setScaleEnabled(true);
        mCasesHistoryChart.setDrawGridBackground(false);
        mCasesHistoryChart.setPinchZoom(true);

        //colors
        mCasesHistoryChart.getLegend().setTextColor(getColor(R.attr.numbersColors));
        mCasesHistoryChart.getAxisLeft().setTextColor(getColor(R.attr.numbersColors));
        mCasesHistoryChart.getAxisLeft().setAxisLineColor(getColor(R.attr.numbersColors));
        mCasesHistoryChart.getAxisRight().setTextColor(getColor(R.attr.numbersColors));
        mCasesHistoryChart.getAxisRight().setAxisLineColor(getColor(R.attr.numbersColors));


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
        //mActiveChart.invalidate();
        //mActiveChart.clearValues();
        if (connected) {
            mCasesHistoryChart.invalidate();
            mCasesHistoryChart.clearValues();
        }
    }

    private void updateData() {

        JsonObjectRequest objectRequestPie = new JsonObjectRequest(Request.Method.GET, dataUrl,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                connected = true;

                try {
                    cases_all = response.getInt("cases");
                    cases_critical = response.getInt("critical");
                    cases_active = response.getInt("active");
                    cases_today = response.getInt("todayCases");
                } catch (JSONException e) {
                    Toast.makeText(getActivity(), "JSON Exception error", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    mRefreshLayout.setRefreshing(false);
                    return;
                }

                setAnimatedInt(textCasesAll, 0, cases_all);
                setAnimatedInt(textCasesToday, 0, cases_today);
                setPieChartData(cases_active, cases_critical);

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

        JsonObjectRequest objectRequestLine = new JsonObjectRequest(Request.Method.GET, historyUrl,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                connected = true;

                HashMap<String, Integer> mapCases = new HashMap<>();
                HashMap<String, Integer> mapDeaths = new HashMap<>();
                HashMap<String, Integer> mapRecovered = new HashMap<>();

                try {
                    JSONObject casesObject = response.getJSONObject("timeline").getJSONObject("cases");
                    Iterator<String> cases = casesObject.keys();

                    while (cases.hasNext()) {
                        String key = cases.next();
                        mapCases.put(key, casesObject.getInt(key));
                    }

                    JSONObject deathsObject = response.getJSONObject("timeline").getJSONObject("deaths");
                    Iterator<String> deaths = deathsObject.keys();

                    while (deaths.hasNext()) {
                        String key = deaths.next();
                        mapDeaths.put(key, deathsObject.getInt(key));
                    }

                    JSONObject recoveredObject = response.getJSONObject("timeline").getJSONObject("recovered");
                    Iterator<String> recovered = recoveredObject.keys();

                    while (recovered.hasNext()) {
                        String key = recovered.next();
                        mapRecovered.put(key, recoveredObject.getInt(key));
                    }


                } catch (JSONException e) {
                    Toast.makeText(getActivity(), "JSON Exception error", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    mRefreshLayout.setRefreshing(false);
                    connected = false;
                    return;
                }

                setLineChartData(sortByValue(mapCases), sortByValue(mapDeaths), sortByValue(mapRecovered));

                mRefreshLayout.setRefreshing(false);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getActivity(), "Failed to load data, please check your internet connection", Toast.LENGTH_SHORT).show();
                error.printStackTrace();
                mRefreshLayout.setRefreshing(false);
            }
        });

        objectRequestPie.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        objectRequestLine.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        mQueue.add(objectRequestPie);
        mQueue.add(objectRequestLine);

    }

    private void setPieChartData(final int actives, int critical) {

        ArrayList<PieEntry> entries = new ArrayList<>();

        entries.add(new PieEntry(critical, "Cas Critiques"));
        entries.add(new PieEntry((actives - critical), "Cas Non Critique"));

        PieDataSet dataSet = new PieDataSet(entries, "Cas Actifs");
        dataSet.setDrawIcons(false);
        //dataSet.setSliceSpace(3f);
        //dataSet.setIconsOffset(new MPPointF(0, 40));
        dataSet.setSelectionShift(5f);

        ArrayList<Integer> colors = new ArrayList<>();

        colors.add(Color.RED);
        colors.add(getColor(R.attr.pieColor1));

        dataSet.setColors(colors);

        final PieData data = new PieData(dataSet);
        //data.setValueFormatter(new PercentFormatter(mActiveChart));
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.BLACK);


        // undo all highlights
        mActiveChart.highlightValues(null);

        //mActiveChart.setCenterText(generateCenterSpannableText(actives));

        mActiveChart.invalidate();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                animateCenterText(0, actives);
                mActiveChart.setData(data);
                mActiveChart.animateY(2000, Easing.EaseInOutCubic);
            }
        }, 300);
    }

    private void setLineChartData(HashMap<String, Integer> casesMap,HashMap<String, Integer> deathsMap,
                                  HashMap<String, Integer> recoveredMap) {

        ArrayList<Entry> entriesCases = new ArrayList<>();
        ArrayList<Entry> entriesDeaths = new ArrayList<>();
        ArrayList<Entry> entriesRecovered = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Integer> entry : casesMap.entrySet()) {
            entriesCases.add(new Entry(i, entry.getValue()));
            i++;
        }

        i = 0;
        for (Map.Entry<String, Integer> entry : deathsMap.entrySet()) {
            entriesDeaths.add(new Entry(i, entry.getValue()));
            i++;
        }

        i = 0;
        for (Map.Entry<String, Integer> entry : recoveredMap.entrySet()) {
            entriesRecovered.add(new Entry(i, entry.getValue()));
            i++;
        }

        //cases
        LineDataSet dataSet = new LineDataSet(entriesCases, "Cas confirmés");
        dataSet.setFillAlpha(110);
        dataSet.setColor(Color.BLUE);
        dataSet.setLineWidth(3f);
        dataSet.setDrawValues(false);
        dataSet.setDrawValues(false);
        dataSet.setCircleColor(Color.BLUE);
        //deaths
        LineDataSet dataSet1 = new LineDataSet(entriesDeaths, "Morts");
        dataSet1.setFillAlpha(110);
        dataSet1.setColor(Color.RED);
        dataSet1.setLineWidth(3f);
        dataSet1.setDrawValues(false);
        dataSet1.setCircleColor(Color.RED);
        //recovered
        LineDataSet dataSet2 = new LineDataSet(entriesRecovered, "Guéris");
        dataSet2.setFillAlpha(110);
        dataSet2.setColor(Color.GREEN);
        dataSet2.setLineWidth(3f);
        dataSet2.setDrawValues(false);
        dataSet2.setCircleColor(Color.GREEN);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        dataSets.add(dataSet1);
        dataSets.add(dataSet2);


        final LineData lineData = new LineData(dataSets);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mCasesHistoryChart.setData(lineData);
                mCasesHistoryChart.animateX(2000, Easing.EaseInOutQuad);
            }
        }, 1000);

    }

    private SpannableString generateCenterSpannableText(int actives) {

        SpannableString s = new SpannableString("Cas Actifs\n" + actives);
        s.setSpan(new RelativeSizeSpan(2.0f), 0, 10, 0);
        s.setSpan(new StyleSpan(Typeface.BOLD), 0, 10, 0);
        s.setSpan(new ForegroundColorSpan(getColor(R.attr.titlesColor)), 0, 10, 0);
        s.setSpan(new StyleSpan(Typeface.BOLD), 10, s.length(), 0);
        s.setSpan(new ForegroundColorSpan(getColor(R.attr.numbersColors)), 10, s.length(), 0);
        s.setSpan(new RelativeSizeSpan(2.8f), 10, s.length(), 0);
        return s;
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

    private void animateCenterText(int start, final int end) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setDuration(2000);
        animator.setInterpolator(new DecelerateInterpolator(1.7f));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                String s = animation.getAnimatedValue().toString();
                mActiveChart.setCenterText(generateCenterSpannableText(Integer.parseInt(s)));
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

    private void fixNetwork() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        } };
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    private void updateAndroidSecurityProvider() {
        try {
            ProviderInstaller.installIfNeeded(getContext());
        }
        catch (Exception e) {
            e.getMessage();
        }
    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {

    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }
}
