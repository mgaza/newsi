package com.firexweb.newsi;

import android.database.Cursor;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.firexweb.newsi.components.NewsAdapter;
import com.firexweb.newsi.components.NewsList;
import com.firexweb.newsi.components.NewsMenu;
import com.firexweb.newsi.components.NewsProgressBar;
import com.firexweb.newsi.components.NewsSwipeRefreshLayout;
import com.firexweb.newsi.data.NewsProvider;
import com.firexweb.newsi.settings.CachingSettings;
import com.firexweb.newsi.settings.WatanyiaSettings;
import com.firexweb.newsi.sync.Auth;
import com.firexweb.newsi.sync.NewsiOperations;


/**
 * Created by root on 6/10/16.
 */
public class MainSystem {
    private static final String TAG = "MainSystem";
    // settings
    public static WatanyiaSettings watanyiaSettings;
    public static CachingSettings cachingSettings;
    public static NewsProvider newsProvider;
    public static NewsAdapter newsAdapter;
    // trackers
    public static MenuItem ITEM_MENU_CHECKED;
    public static int LIST_CAT = 1;
    public static int LIST_PAGE = 1;

    // data
    public static int LIST_LIMIT = 9;
    public static DetailActivity ARTICLE_DISPLAYED;
    public static boolean LIST_CHANGED = false;
    public static boolean FETCH_ARTICLE_FOR_FUTURE_USE = false;
    public static boolean WE_ARE_FETCHING_NEW_NEWS = false;
    private static NewsActivity context;
    // components
    private static NewsList newsList;
    private static NewsProgressBar newsProgressBar;
    private static NewsMenu newsMenu;
    private static NewsSwipeRefreshLayout newsSwipeRefreshLayout;
    private static Cursor cursor;

    public static void run(NewsActivity context) {
        MainSystem.context = context;

        newsList = new NewsList(((ListView) context.findViewById(R.id.news_list)));
        newsProgressBar = new NewsProgressBar(((LinearLayout) context.findViewById(R.id.news_list_full_progress_bar)));
        newsSwipeRefreshLayout = new NewsSwipeRefreshLayout(((SwipeRefreshLayout) context.findViewById(R.id.swiperefresh)));


        newsProvider = new NewsProvider(context);
        cursor = newsProvider.fetchNewsForAdapter(LIST_CAT, LIST_LIMIT, LIST_PAGE);
        newsAdapter = new NewsAdapter(context, cursor);

        // build components ( attach events to them )
        newsList.build();
        newsProgressBar.build();
        newsSwipeRefreshLayout.build();

        // build settings
        watanyiaSettings = new WatanyiaSettings(context);
        cachingSettings = new CachingSettings(context);

        // run the app
        Auth.checkAuth();
        newsList.getList().setAdapter(newsAdapter);
        WE_ARE_FETCHING_NEW_NEWS = true;

        MainSystem.fetchNewsOperation();

    }

    /*
        Operations
     */
    public static void refreshNewsAdapter() {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainSystem.getNewsProgressBar().hideFullProgressBar();
                cursor = newsProvider.fetchNewsForAdapter(LIST_CAT, LIST_LIMIT, LIST_PAGE);
                Log.d("CursorData", Integer.toString(cursor.getCount()));
                newsAdapter.swapCursor(cursor);
                newsAdapter.notifyDataSetChanged();
                MainSystem.getNewsSwipeRefreshLayout().getSwipeRefreshLayout().setRefreshing(false);
                if (LIST_CHANGED) {
                    newsList.getList().smoothScrollToPositionFromTop(0, 0);
                    LIST_CHANGED = false;
                }
            }
        });
    }

    public static void refreshNewsAdapterWithoutRequests() {
        cursor = newsProvider.fetchNewsForAdapter(LIST_CAT, LIST_LIMIT, LIST_PAGE);
        Log.d("CursorData", Integer.toString(cursor.getCount()));
        newsAdapter.swapCursor(cursor);
        newsAdapter.notifyDataSetChanged();
        MainSystem.getNewsSwipeRefreshLayout().getSwipeRefreshLayout().setRefreshing(false);
        if (LIST_CHANGED) {
            newsList.getList().smoothScrollToPositionFromTop(0, 0);
            LIST_CHANGED = false;
        }
    }

    public static void refreshArticle() {
        ARTICLE_DISPLAYED.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ARTICLE_DISPLAYED.updateActivity();
            }
        });
    }


    public static void fetchNewsOperation() {

        Log.e(TAG, "fetchNewsOperation: fetchNewsOperation");
        FETCH_ARTICLE_FOR_FUTURE_USE = false;

//        FETCH_ARTICLE_FOR_FUTURE_USE = true;
//        NewsiOperations.fetch_news_by_web(context,newsProvider);

        int mode = Auth.getMode();

        if (mode == Auth.MODE_WIFI || mode == Auth.MODE_DATA) {
            FETCH_ARTICLE_FOR_FUTURE_USE = true;
            NewsiOperations.fetch_news_by_web(context, newsProvider);
        } else if (mode == Auth.MODE_GSM && watanyiaSettings.getWatanyiaConnectMode()) {
            NewsiOperations.fetch_news_by_sms(context, newsProvider);
        } else if (mode == Auth.MODE_AIRPLANE || mode == Auth.MODE_OFFLINE) {
            displayToastMessage(R.string.error_no_data);
            refreshNewsAdapter();
        } else {
            displayToastMessage(R.string.error_provider_not_reached);
        }

    }

    public static void fetchArticleOperation(int id) {
        Log.e(TAG, "fetchArticleOperation: fetchNewsOperation");

        if (Auth.getMode() == Auth.MODE_WIFI || Auth.getMode() == Auth.MODE_DATA) {
            NewsiOperations.fetch_article_by_web(ARTICLE_DISPLAYED, newsProvider, id);
        } else if (Auth.getMode() == Auth.MODE_GSM && watanyiaSettings.getWatanyiaConnectMode()) {
            NewsiOperations.fetch_article_by_sms(ARTICLE_DISPLAYED, newsProvider, id, 1);
        } else {
            refreshArticle();
        }

    }

    public static Cursor fetchArticle(int id) {
        Cursor c = newsProvider.fetchArticleForActivity(id);
        c.moveToFirst();
        return c;
    }

    public static void displayToastMessage(final int string) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, context.getResources().getString(string), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void checkRequestError(int errorCode)
    {
        switch (errorCode)
        {
            case 63:displayToastMessage(R.string.error_provider_not_reached);break;
            case 64:displayToastMessage(R.string.error_wrong_news_id);break;
            default:displayToastMessage(R.string.request_failed);
        }
    }
    public static void wipeNewsCache(int beforeTimeStamp) {
        newsProvider.wipeCache(beforeTimeStamp);
    }

    public static void trackNewNewsComing() {
        if (WE_ARE_FETCHING_NEW_NEWS) {
            LIST_PAGE = 1;
            WE_ARE_FETCHING_NEW_NEWS = false;
            newsList.getList().smoothScrollToPositionFromTop(0, 0);
        }
    }

    /*
        static setters and getter
    */
    public static void setMenu(Menu menu) {
        newsMenu = new NewsMenu(menu);
        newsMenu.build();
    }

    public static NewsProgressBar getNewsProgressBar() {
        return newsProgressBar;
    }

    public static NewsSwipeRefreshLayout getNewsSwipeRefreshLayout() {
        return newsSwipeRefreshLayout;
    }

    public static NewsActivity getNewsActivity() {
        return context;
    }


}
