package com.thebnich.floatinghintedittext;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;

public class FloatingHintEditText extends EditText {
    private static enum Animation { NONE, SHRINK, GROW };

    private final static float HINT_SCALE = 0.6f;
    private final static int ANIMATION_STEPS = 6;

    private final Paint mSmallHintPaint = new Paint();
    private final ColorStateList mHintColors;

    private boolean mWasEmpty;
    private int mAnimationFrame;
    private Animation mAnimation = Animation.NONE;

    public FloatingHintEditText(Context context) {
        this(context, null);
    }

    public FloatingHintEditText(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.floatingHintEditTextStyle);
    }

    public FloatingHintEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mHintColors = getHintTextColors();
        mWasEmpty = TextUtils.isEmpty(getText());
    }

    @Override
    public int getCompoundPaddingTop() {
        final FontMetricsInt metrics = getPaint().getFontMetricsInt();
        final int smallHintHeight = (int) ((metrics.bottom - metrics.top) * HINT_SCALE);
        return super.getCompoundPaddingTop() + smallHintHeight;
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);

        final boolean isEmpty = TextUtils.isEmpty(getText());

        // The empty state hasn't changed, so the hint stays the same.
        if (mWasEmpty == isEmpty) {
            return;
        }

        mWasEmpty = isEmpty;

        // Don't animate if we aren't visible.
        if (!isShown()) {
            return;
        }

        if (isEmpty) {
            mAnimation = Animation.GROW;
            setHintTextColor(Color.TRANSPARENT);
        } else {
            mAnimation = Animation.SHRINK;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (TextUtils.isEmpty(getHint())) {
            return;
        }

        final boolean isAnimating = mAnimation != Animation.NONE;

        // The large hint is drawn by Android, so do nothing.
        if (!isAnimating && TextUtils.isEmpty(getText())) {
            return;
        }

        mSmallHintPaint.set(getPaint());
        mSmallHintPaint.setColor(mHintColors.getColorForState(getDrawableState(), mHintColors.getDefaultColor()));

        final float largeHintPosY = getBaseline();
        final float smallHintPosY = largeHintPosY + getPaint().getFontMetricsInt().top;
        final float largeHintSize = getTextSize();
        final float smallHintSize = largeHintSize * HINT_SCALE;

        // If we're not animating, we're showing the fixed small hint, so draw it and bail.
        if (!isAnimating) {
            mSmallHintPaint.setTextSize(smallHintSize);
            canvas.drawText(getHint().toString(), getPaddingLeft(), smallHintPosY, mSmallHintPaint);
            return;
        }

        if (mAnimation == Animation.SHRINK) {
            drawAnimationFrame(canvas, largeHintSize, smallHintSize, largeHintPosY, smallHintPosY);
        } else {
            drawAnimationFrame(canvas, smallHintSize, largeHintSize, smallHintPosY, largeHintPosY);
        }

        mAnimationFrame++;

        if (mAnimationFrame == ANIMATION_STEPS) {
            if (mAnimation == Animation.GROW) {
                setHintTextColor(mHintColors);
            }
            mAnimation = Animation.NONE;
            mAnimationFrame = 0;
        }

        invalidate();
    }

    private void drawAnimationFrame(Canvas canvas, float fromSize, float toSize, float fromY, float toY) {
        final float textSize = lerp(fromSize, toSize);
        final float hintPosY = lerp(fromY, toY);
        mSmallHintPaint.setTextSize(textSize);
        canvas.drawText(getHint().toString(), getCompoundPaddingLeft(), hintPosY, mSmallHintPaint);
    }

    private float lerp(float from, float to) {
        final float alpha = (float) mAnimationFrame / (ANIMATION_STEPS - 1);
        return from * (1 - alpha) + to * alpha;
    }
}
