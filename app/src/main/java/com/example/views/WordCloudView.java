package com.example.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.mcommercemobile04.R;
import com.example.models.WordFrequency;

import java.util.ArrayList;
import java.util.List;

public final class WordCloudView extends View {

    private static final int MAX_PLACEMENT_ATTEMPTS =
            1800;

    private final Paint textPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<WordFrequency> words =
            new ArrayList<>();

    private final List<PlacedWord> placedWords =
            new ArrayList<>();

    private final int[] wordColors;

    private final float minimumTextSize;
    private final float maximumTextSize;
    private final float cloudPadding;
    private final float collisionPadding;

    public WordCloudView(
            Context context
    ) {
        this(context, null);
    }

    public WordCloudView(
            Context context,
            @Nullable AttributeSet attrs
    ) {
        this(context, attrs, 0);
    }

    public WordCloudView(
            Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(
                context,
                attrs,
                defStyleAttr
        );

        minimumTextSize =
                getResources().getDimension(
                        R.dimen.mc_word_cloud_text_min
                );

        maximumTextSize =
                getResources().getDimension(
                        R.dimen.mc_word_cloud_text_max
                );

        cloudPadding =
                getResources().getDimension(
                        R.dimen.mc_word_cloud_padding
                );

        collisionPadding =
                getResources().getDimension(
                        R.dimen
                                .mc_word_cloud_collision_padding
                );

        wordColors = new int[]{
                ContextCompat.getColor(
                        context,
                        R.color.mc_word_cloud_blue
                ),
                ContextCompat.getColor(
                        context,
                        R.color.mc_word_cloud_indigo
                ),
                ContextCompat.getColor(
                        context,
                        R.color.mc_word_cloud_violet
                ),
                ContextCompat.getColor(
                        context,
                        R.color.mc_word_cloud_green
                ),
                ContextCompat.getColor(
                        context,
                        R.color.mc_word_cloud_orange
                ),
                ContextCompat.getColor(
                        context,
                        R.color.mc_word_cloud_pink
                )
        };

        textPaint.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );
    }

    /**
     * Cập nhật dữ liệu WordCloud.
     */
    public void setWords(
            List<WordFrequency> newWords
    ) {

        words.clear();

        if (newWords != null) {
            words.addAll(newWords);
        }

        rebuildLayout();
        invalidate();
    }

    @Override
    protected void onSizeChanged(
            int width,
            int height,
            int oldWidth,
            int oldHeight
    ) {
        super.onSizeChanged(
                width,
                height,
                oldWidth,
                oldHeight
        );

        rebuildLayout();
    }

    /**
     * Tính vị trí các từ trước khi onDraw chạy.
     */
    private void rebuildLayout() {

        placedWords.clear();

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        if (viewWidth <= 0
                || viewHeight <= 0
                || words.isEmpty()) {

            return;
        }

        int maximumFrequency =
                words.get(0).getFrequency();

        int minimumFrequency =
                words.get(
                        words.size() - 1
                ).getFrequency();

        float centerX =
                viewWidth / 2f;

        float centerY =
                viewHeight / 2f;

        for (
                int index = 0;
                index < words.size();
                index++
        ) {

            WordFrequency wordFrequency =
                    words.get(index);

            float textSize =
                    calculateTextSize(
                            wordFrequency.getFrequency(),
                            minimumFrequency,
                            maximumFrequency
                    );

            textPaint.setTextSize(textSize);

            Paint.FontMetrics fontMetrics =
                    textPaint.getFontMetrics();

            float textWidth =
                    textPaint.measureText(
                            wordFrequency.getWord()
                    );

            float textHeight =
                    fontMetrics.bottom
                            - fontMetrics.top;

            RectF selectedBounds = null;

            for (
                    int attempt = 0;
                    attempt < MAX_PLACEMENT_ATTEMPTS;
                    attempt++
            ) {

                double angle =
                        attempt * 0.36;

                double radius =
                        2.25 * angle;

                float candidateCenterX =
                        centerX
                                + (float) (
                                Math.cos(angle)
                                        * radius
                        );

                float candidateCenterY =
                        centerY
                                + (float) (
                                Math.sin(angle)
                                        * radius
                                        * 0.72
                        );

                float left =
                        candidateCenterX
                                - textWidth / 2f;

                float top =
                        candidateCenterY
                                - textHeight / 2f;

                RectF candidateBounds =
                        new RectF(
                                left,
                                top,
                                left + textWidth,
                                top + textHeight
                        );

                if (!isInsideView(
                        candidateBounds,
                        viewWidth,
                        viewHeight
                )) {
                    continue;
                }

                if (intersectsExistingWord(
                        candidateBounds
                )) {
                    continue;
                }

                selectedBounds =
                        candidateBounds;

                break;
            }

            if (selectedBounds == null) {
                continue;
            }

            float baseline =
                    selectedBounds.top
                            - fontMetrics.top;

            placedWords.add(
                    new PlacedWord(
                            wordFrequency.getWord(),
                            textSize,
                            wordColors[
                                    index
                                            % wordColors.length
                                    ],
                            selectedBounds.left,
                            baseline,
                            selectedBounds
                    )
            );
        }
    }

    private float calculateTextSize(
            int frequency,
            int minimumFrequency,
            int maximumFrequency
    ) {

        if (maximumFrequency
                == minimumFrequency) {

            return (
                    minimumTextSize
                            + maximumTextSize
            ) / 2f;
        }

        float ratio =
                (frequency - minimumFrequency)
                        / (float) (
                        maximumFrequency
                                - minimumFrequency
                );

        return minimumTextSize
                + ratio
                * (
                maximumTextSize
                        - minimumTextSize
        );
    }

    private boolean isInsideView(
            RectF bounds,
            int viewWidth,
            int viewHeight
    ) {

        float leftBoundary =
                getPaddingLeft()
                        + cloudPadding;

        float topBoundary =
                getPaddingTop()
                        + cloudPadding;

        float rightBoundary =
                viewWidth
                        - getPaddingRight()
                        - cloudPadding;

        float bottomBoundary =
                viewHeight
                        - getPaddingBottom()
                        - cloudPadding;

        return bounds.left >= leftBoundary
                && bounds.top >= topBoundary
                && bounds.right <= rightBoundary
                && bounds.bottom <= bottomBoundary;
    }

    private boolean intersectsExistingWord(
            RectF candidateBounds
    ) {

        RectF expandedCandidate =
                new RectF(
                        candidateBounds.left
                                - collisionPadding,
                        candidateBounds.top
                                - collisionPadding,
                        candidateBounds.right
                                + collisionPadding,
                        candidateBounds.bottom
                                + collisionPadding
                );

        for (
                PlacedWord placedWord
                : placedWords
        ) {

            if (
                    RectF.intersects(
                            expandedCandidate,
                            placedWord.bounds
                    )
            ) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onDraw(
            Canvas canvas
    ) {
        super.onDraw(canvas);

        for (
                PlacedWord placedWord
                : placedWords
        ) {

            textPaint.setTextSize(
                    placedWord.textSize
            );

            textPaint.setColor(
                    placedWord.color
            );

            canvas.drawText(
                    placedWord.text,
                    placedWord.x,
                    placedWord.baseline,
                    textPaint
            );
        }
    }

    private static final class PlacedWord {

        private final String text;
        private final float textSize;
        private final int color;

        private final float x;
        private final float baseline;

        private final RectF bounds;

        private PlacedWord(
                String text,
                float textSize,
                int color,
                float x,
                float baseline,
                RectF bounds
        ) {
            this.text = text;
            this.textSize = textSize;
            this.color = color;
            this.x = x;
            this.baseline = baseline;
            this.bounds = bounds;
        }
    }
}