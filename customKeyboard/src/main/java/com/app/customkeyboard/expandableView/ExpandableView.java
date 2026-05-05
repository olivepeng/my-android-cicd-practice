package com.app.customkeyboard.expandableView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.app.customkeyboard.ResizableRelativeLayout;

import java.util.ArrayList;

/**
 * Created by Don.Brody on 7/18/18.
 */
public abstract class ExpandableView extends ResizableRelativeLayout {

    @Nullable
    private ExpandableState state = null;

    @NonNull
    private final ArrayList<ExpandableStateListener> stateListeners = new ArrayList<>();

//    @Nullable
//    private ViewPropertyAnimator animate;

    public ExpandableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        state = ExpandableState.EXPANDED; // view is expanded when initially created
        setVisibility(View.INVISIBLE);
    }

    public boolean isExpanded() {
        return state == ExpandableState.EXPANDED;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        translateLayout(); // collapse view after initial inflation
    }

    public void registerListener(@NonNull ExpandableStateListener listener) {
        stateListeners.add(listener);
    }

    public void translateLayout() {
        // Ignore calls that occur during animation (prevents issues from wood-pecker'ing)
//        if (state != ExpandableState.EXPANDING && state != ExpandableState.COLLAPSING) {
        float pixels = toDp(500); // Adjust this value as needed
        long millis = (long) pixels; // translates layout 1px per millisecond
        float deltaY;
        if (state == null) {
            return;
        }
        switch (state) {
            case EXPANDING:
            case EXPANDED:
                updateState(ExpandableState.COLLAPSING);
                deltaY = ((float) pixels); // pushes layout down 500 device pixels
                /*
                if (animate != null) {
                    animate.cancel();
                }
                animate = animate();
                animate.translationY(deltaY);
                animate.setDuration(millis);
                animate.withEndAction(() -> {
                    updateState(ExpandableState.COLLAPSED);
                    setVisibility(View.INVISIBLE);
                });
                animate.start();
                */
                updateState(ExpandableState.COLLAPSED);
                setVisibility(View.INVISIBLE);
                break;
            case COLLAPSING:
            case COLLAPSED:
                updateState(ExpandableState.EXPANDING);
                setVisibility(View.VISIBLE);
                deltaY = 0.0f; // pulls layout back to its original position
                /*
                if (animate != null) {
                    animate.cancel();
                }
                animate = animate();
                animate.translationY(deltaY);
                animate.setDuration(millis);
                animate.withEndAction(() -> updateState(ExpandableState.EXPANDED));
                animate.start();
                */
                updateState(ExpandableState.EXPANDED);
                break;
            default:
                return;
        }
    }

    private void updateState(@NonNull ExpandableState nextState) {
        state = nextState;
        for (ExpandableStateListener listener : stateListeners) {
            listener.onStateChange(nextState);
        }
    }

    protected abstract void configureSelf();
}
