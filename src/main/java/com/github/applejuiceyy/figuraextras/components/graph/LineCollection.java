package com.github.applejuiceyy.figuraextras.components.graph;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import com.github.applejuiceyy.figuraextras.util.MathUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class LineCollection extends DataCollection {
    private final LineGraphRenderer graphRenderer;
    ArrayList<DataPoint> dataPoints = new ArrayList<>();
    @Nullable
    private Bounds dataBounds = null;

    public LineCollection(LineGraphRenderer graphRenderer) {
        this.graphRenderer = graphRenderer;
    }

    public static LineCollection of(int color) {
        return of(color, color);
    }

    public static LineCollection of(int lineColor, int markerColor) {
        return of(defaultLineRenderer(lineColor), defaultMarkerRenderer(markerColor));
    }

    public static LineCollection of(int lineColor, Consumer<GuiGraphics> markerRenderer) {
        return of(defaultLineRenderer(lineColor), markerRenderer);
    }

    public static LineCollection of(LineRenderer lineRenderer, int markerColor) {
        return of(defaultLineGraphRenderer(lineRenderer, defaultMarkerRenderer(markerColor)));
    }

    public static LineCollection of(LineRenderer lineRenderer, Consumer<GuiGraphics> markerRenderer) {
        return of(defaultLineGraphRenderer(lineRenderer, markerRenderer));
    }

    public static LineCollection of(LineGraphRenderer graphRenderer) {
        return new LineCollection(graphRenderer);
    }

    public static Consumer<GuiGraphics> defaultMarkerRenderer(int color) {
        return graphics -> graphics.fill(-1, -1, 1, 1, color);
    }

    public static LineGraphRenderer defaultLineGraphRenderer(LineRenderer lineRenderer, Consumer<GuiGraphics> markerRenderer) {
        return (graphics, dataPoints, context) -> {
            Bounds bounds = context.renderingBounds();

            int index = Collections.binarySearch(dataPoints, null, (a, b) -> {
                assert b == null;
                return Double.compare(a.getX(), bounds.xMin());
            });

            if(index < 0) {
                index = -(index + 1);
            }

            int dotIndex = index;
            if(index == 0) {
                index = 1;
            }
            PoseStack pose = graphics.pose();
            int viewportHeight = context.viewportHeight();
            int viewportWidth = context.viewportWidth();
            for(; index < dataPoints.size() && bounds.xMax() > dataPoints.get(index).getX(); index++) {
                DataPoint current = dataPoints.get(index);
                DataPoint previous = dataPoints.get(index - 1);

                pose.pushPose();

                double x2 = MathUtil.map(current.getX(), bounds.xMin(), bounds.xMax(), 0, viewportWidth);
                double y2 = MathUtil.map(current.getY(), bounds.yMin(), bounds.yMax(), viewportHeight, 0);
                double x1 = MathUtil.map(previous.getX(), bounds.xMin(), bounds.xMax(), 0, viewportWidth);
                double y1 = MathUtil.map(previous.getY(), bounds.yMin(), bounds.yMax(), viewportHeight, 0);
                pose.translate(x1, y1, 0);
                pose.rotateAround(Axis.ZP.rotation((float) MathUtil.angleTo(x1, y1, x2, y2)), 0, 0, 0);

                lineRenderer.render(graphics, context, (float) MathUtil.length(x1, y1, x2, y2));

                pose.popPose();
            }

            for(; dotIndex < dataPoints.size() && bounds.xMax() > dataPoints.get(dotIndex).getX(); dotIndex++) {
                DataPoint current = dataPoints.get(dotIndex);
                double x = MathUtil.map(current.getX(), bounds.xMin(), bounds.xMax(), 0, viewportWidth);
                double y = MathUtil.map(current.getY(), bounds.yMin(), bounds.yMax(), viewportHeight, 0);
                pose.pushPose();
                pose.translate(x, y, 0);

                markerRenderer.accept(graphics);

                pose.popPose();
            }
        };
    }

    public static LineRenderer defaultLineRenderer(int color) {
        return (graphics, context, size) -> {
            graphics.fill(0, 1, (int) size, -1, color);
        };
    }


    @Override
    public Bounds getDataBounds(boolean hintWantsMinX, boolean hintWantsMinY, boolean hintWantsMaxX, boolean hintWantsMaxY) {
        if(dataPoints.isEmpty()) return null;
        if(dataBounds != null) return dataBounds;

        double ax, ay, bx, by;
        ax = ay = bx = by = 0;

        if(hintWantsMinX || hintWantsMaxX) {
            DataPoint first = dataPoints.get(0);
            DataPoint last = dataPoints.get(dataPoints.size() - 1);
            ax = first.getX();
            bx = last.getX();
        }

        if(hintWantsMinY || hintWantsMaxY) {
            ay = Float.POSITIVE_INFINITY;
            by = Float.NEGATIVE_INFINITY;

            for (DataPoint dataPoint : dataPoints) {
                double image = dataPoint.getY();
                by = Math.max(by, image);
                ay = Math.min(ay, image);
            }
        }

        dataBounds = new Bounds(ax, ay, bx, by);
        return dataBounds;
    }

    @Override
    public InGraphRenderer getBaked(BakingContext bakingContext) {
        return new InGraphRenderer() {
            @Override
            public void render(GuiGraphics context) {
                graphRenderer.render(context, dataPoints, bakingContext);
            }

            @Override
            public boolean doTooltip(DefaultCancellableEvent.ToolTipEvent toolTipEvent, double x, double y) {
                Bounds bounds = bakingContext.renderingBounds();

                int index = Collections.binarySearch(dataPoints, null, (a, b) -> {
                    assert b == null;
                    return Double.compare(a.getX(), bounds.xMin());
                });

                if(index < 0) {
                    index = -(index + 1);
                }

                for(; index < dataPoints.size() && bounds.xMax() > dataPoints.get(index).getX(); index++) {
                    DataPoint current = dataPoints.get(index);
                    double px = MathUtil.map(current.getX(), bounds.xMin(), bounds.xMax(), 0, bakingContext.viewportWidth());
                    double py = MathUtil.map(current.getY(), bounds.yMin(), bounds.yMax(), bakingContext.viewportHeight(), 0);

                    if(MathUtil.length(px, py, (float) x, (float) y) < 2) {
                        current.onHover(toolTipEvent);
                        return true;
                    }
                }

                return false;
            }

            @Override
            public boolean handleMouseDown(double x, double y, int button) {
                Bounds bounds = bakingContext.renderingBounds();

                int index = Collections.binarySearch(dataPoints, null, (a, b) -> {
                    assert b == null;
                    return Double.compare(a.getX(), bounds.xMin());
                });

                if(index < 0) {
                    index = -(index + 1);
                }

                for(; index < dataPoints.size() && bounds.xMax() > dataPoints.get(index).getX(); index++) {
                    DataPoint current = dataPoints.get(index);
                    double px = MathUtil.map(current.getX(), bounds.xMin(), bounds.xMax(), 0, bakingContext.viewportWidth());
                    double py = MathUtil.map(current.getY(), bounds.yMin(), bounds.yMax(), bakingContext.viewportHeight(), 0);

                    if(MathUtil.length(px, py, (float) x, (float) y) < 2) {
                        current.onClick(x, y);
                        return true;
                    }
                }

                return false;
            }
        };
    }

    public void add(DataPoint dataPoint) {
        invalidate();

        dataBounds = null;

        if(dataPoints.isEmpty() || dataPoints.get(dataPoints.size() - 1).getX() < dataPoint.getX()) {
            dataPoints.add(dataPoint);
            return;
        }
        int index = Collections.binarySearch(dataPoints, dataPoint, Comparator.comparingDouble(DataPoint::getX));

        if(index < 0) {
            index = -(index + 1);
        }

        dataPoints.add(index, dataPoint);
    }

    public void add(float x, float y) {
        add(new DataPoint() {
            @Override
            public double getY() {
                return y;
            }

            @Override
            public double getX() {
                return x;
            }

            @Override
            public String toString() {
                return "DataPoint[%s, %s]".formatted(x, y);
            }
        });
    }

    public DataPoint get(int idx) {
        return dataPoints.get(idx);
    }

    public DataPoint remove(int idx) {
        invalidate();
        dataBounds = null;
        return dataPoints.remove(idx);
    }

    public int size() {
        return dataPoints.size();
    }

    public interface DataPoint {
        @Contract(pure = true)
        double getY();
        @Contract(pure = true)
        double getX();

        default void onHover(DefaultCancellableEvent.ToolTipEvent event) {
            event.add(Component.literal("(%s, %s)".formatted(getX(), getY())));
        }

        default void onClick(double x, double y) {

        };
    }

    public interface LineGraphRenderer {
        void render(GuiGraphics graphics, List<DataPoint> dataPoints, BakingContext context);
    }

    public interface LineRenderer {
        void render(GuiGraphics graphics, BakingContext context, float size);
    }
}
