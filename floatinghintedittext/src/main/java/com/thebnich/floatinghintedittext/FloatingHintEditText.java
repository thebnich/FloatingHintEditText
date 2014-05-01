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

    private final Paint smallHintPaint = new Paint();
    private final ColorStateList hintColors;

    private boolean wasEmpty;
    private int animationFrame;
    private Animation animation = Animation.NONE;

    public FloatingHintEditText(Context context) {
        this(context, null);
    }

    public FloatingHintEditText(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.floatingHintEditTextStyle);
    }

    public FloatingHintEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        hintColors = getHintTextColors();
        wasEmpty = TextUtils.isEmpty(getText());
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
        if (wasEmpty == isEmpty) {
            return;
        }

        wasEmpty = isEmpty;

        // Don't animate if we aren't visible.
        if (!isShown()) {
            return;
        }

        if (isEmpty) {
            animation = Animation.GROW;
            setHintTextColor(Color.TRANSPARENT);
        } else {
            animation = Animation.SHRINK;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (TextUtils.isEmpty(getHint())) {
            return;
        }

        final boolean isAnimating = animation != Animation.NONE;

        // The large hint is drawn by Android, so do nothing.
        if (!isAnimating && TextUtils.isEmpty(getText())) {
            return;
        }

        smallHintPaint.set(getPaint());
        smallHintPaint.setColor(hintColors.getColorForState(getDrawableState(), hintColors.getDefaultColor()));

        final float largeHintPosY = getBaseline();
        final float smallHintPosY = largeHintPosY + getPaint().getFontMetricsInt().top;
        final float largeHintSize = getTextSize();
        final float smallHintSize = largeHintSize * HINT_SCALE;

        // If we're not animating, we're showing the fixed small hint, so draw it and bail.
        if (!isAnimating) {
            smallHintPaint.setTextSize(smallHintSize);
            canvas.drawText(getHint().toString(), getPaddingLeft(), smallHintPosY, smallHintPaint);
            return;
        }

        if (animation == Animation.SHRINK) {
            drawAnimationFrame(canvas, largeHintSize, smallHintSize, largeHintPosY, smallHintPosY);
        } else {
            drawAnimationFrame(canvas, smallHintSize, largeHintSize, smallHintPosY, largeHintPosY);
        }

        animationFrame++;

        if (animationFrame == ANIMATION_STEPS) {
            if (animation == Animation.GROW) {
                setHintTextColor(hintColors);
            }
            animation = Animation.NONE;
            animationFrame = 0;
        }

        invalidate();
    }

    private void drawAnimationFrame(Canvas canvas, float fromSize, float toSize, float fromY, float toY) {
        final float textSize = lerp(fromSize, toSize);
        final float hintPosY = lerp(fromY, toY);
        smallHintPaint.setTextSize(textSize);
        canvas.drawText(getHint().toString(), getCompoundPaddingLeft(), hintPosY, smallHintPaint);
    }

    private float lerp(float from, float to) {
        final float alpha = (float) animationFrame / (ANIMATION_STEPS - 1);
        return from * (1 - alpha) + to * alpha;
    }
}
