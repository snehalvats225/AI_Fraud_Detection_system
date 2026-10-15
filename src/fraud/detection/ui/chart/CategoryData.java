package fraud.detection.ui.chart;

import java.util.Collections;
import java.util.List;

/** Data for bar and line charts: category labels + one or more series. */
public final class CategoryData {

    private final List<String> labels;
    private final List<ChartSeries> series;

    public CategoryData(List<String> labels, List<ChartSeries> series) {
        this.labels = Collections.unmodifiableList(labels);
        this.series = Collections.unmodifiableList(series);
    }

    public List<String> getLabels()      { return labels; }
    public List<ChartSeries> getSeries() { return series; }
}
