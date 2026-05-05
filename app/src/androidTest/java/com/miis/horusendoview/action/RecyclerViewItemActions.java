package com.miis.horusendoview.action;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.ViewInteraction;

import org.hamcrest.Matcher;

public class RecyclerViewItemActions {

    // 點擊 RecyclerView 內的元件
    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View childView = view.findViewById(id);
                if (childView != null && childView.isClickable()) {
                    childView.performClick();
                }
            }
        };
    }

    // 獲取 RecyclerView 的 item 數量
    public static int getRecyclerViewItemCount(final ViewInteraction view) {
        final int[] itemCount = new int[1]; // 使用陣列來存儲結果，以便在匿名類中訪問

        view.check(new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewFoundException) {
                if (noViewFoundException != null) {
                    throw noViewFoundException;
                }

                RecyclerView recyclerView = (RecyclerView) view;
                if (recyclerView.getAdapter() != null) {
                    itemCount[0] = recyclerView.getAdapter().getItemCount();
                } else {
                    itemCount[0] = 0;
                }
            }
        });

        return itemCount[0];
    }

    // 取得 RecyclerView 內元件的text
    public static ViewAction getTextFromItemWithId(final int viewId, final java.util.concurrent.atomic.AtomicReference<String> text) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                //return "getting text from view at position " + position + " and id " + viewId;
                return "Get the text of child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView textView = view.findViewById(viewId);
                text.set(textView.getText().toString());
            }
        };
    }
}
