package com.eveningoutpost.dexdrip.carblookup;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import androidx.appcompat.widget.AppCompatButton;

public class CenteredIconButton extends AppCompatButton {
    private boolean originalPaddingCaptured;
    private int originalPaddingStart;
    private int originalPaddingTop;
    private int originalPaddingEnd;
    private int originalPaddingBottom;
    private boolean adjustingPadding;

    public CenteredIconButton(Context context) {
        super(context);
    }

    public CenteredIconButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CenteredIconButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? getWidth() : MeasureSpec.getSize(widthMeasureSpec);
        centerIconAndText(width);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        centerIconAndText(getWidth());
    }

    private void centerIconAndText(int width) {
        if (adjustingPadding || width <= 0) {
            return;
        }
        captureOriginalPadding();
        Drawable startDrawable = getCompoundDrawablesRelative()[0];
        if (startDrawable == null) {
            return;
        }

        int drawableWidth = startDrawable.getBounds().width();
        if (drawableWidth <= 0) {
            drawableWidth = startDrawable.getIntrinsicWidth();
        }
        if (drawableWidth <= 0) {
            return;
        }

        CharSequence text = transformedText();
        float textWidth = text == null ? 0f : getPaint().measureText(text.toString());
        int contentWidth = (int) Math.ceil(textWidth + drawableWidth + getCompoundDrawablePadding());
        int centeredPaddingStart = Math.max(originalPaddingStart, (width - contentWidth) / 2);

        adjustingPadding = true;
        setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        setPaddingRelative(centeredPaddingStart, originalPaddingTop, originalPaddingEnd, originalPaddingBottom);
        adjustingPadding = false;
    }

    private void captureOriginalPadding() {
        if (originalPaddingCaptured) {
            return;
        }
        originalPaddingStart = getPaddingStart();
        originalPaddingTop = getPaddingTop();
        originalPaddingEnd = getPaddingEnd();
        originalPaddingBottom = getPaddingBottom();
        originalPaddingCaptured = true;
    }

    private CharSequence transformedText() {
        CharSequence text = getText();
        TransformationMethod transformationMethod = getTransformationMethod();
        if (transformationMethod == null) {
            return text;
        }
        CharSequence transformed = transformationMethod.getTransformation(text, this);
        return transformed != null ? transformed : text;
    }
}