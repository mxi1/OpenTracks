package de.dennisguse.opentracks.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;

public class IntervalStatisticsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<IntervalStatistics.Interval> intervalList;
    private final Context context;
    private final StackMode stackMode;
    private boolean metricUnits;
    private String category;

    public IntervalStatisticsAdapter(Context context, String category, StackMode stackMode) {
        metricUnits = PreferencesUtils.isMetricUnits(PreferencesUtils.getSharedPreferences(context), context);
        this.context = context;
        this.category = category;
        this.stackMode = stackMode;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.interval_stats_list_item, parent, false);
        return new IntervalStatisticsAdapter.ViewHolder(view);
    }

    //TODO Check preference handling! Should not be accessed in getView()
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int actualPosition = stackMode == StackMode.STACK_FROM_TOP ? position : getItemCount() - 1 - position;
        IntervalStatisticsAdapter.ViewHolder viewHolder = (IntervalStatisticsAdapter.ViewHolder) holder;
        IntervalStatistics.Interval interval = intervalList.get(actualPosition);
        viewHolder.itemView.setTag(actualPosition);

        float sumDistance_m;
        if (actualPosition + 1 == getItemCount() && actualPosition > 0) {
            sumDistance_m = actualPosition * intervalList.get(actualPosition - 1).getDistance_m() + interval.getDistance_m();
        } else {
            sumDistance_m = (actualPosition + 1) * interval.getDistance_m();
        }
        viewHolder.distance.setText(StringUtils.formatDistance(context, sumDistance_m, metricUnits));

        if (PreferencesUtils.isReportSpeed(PreferencesUtils.getSharedPreferences(context), context, category)) {
            viewHolder.rate.setText(StringUtils.formatSpeed(context, interval.getSpeed_ms(), metricUnits, true));
        } else {
            viewHolder.rate.setText(StringUtils.formatSpeed(context, interval.getSpeed_ms(), metricUnits, false));
        }

        viewHolder.gain.setText(StringUtils.formatDistance(context, interval.getGain_m(), metricUnits));
        viewHolder.loss.setText(StringUtils.formatDistance(context, interval.getLoss_m(), metricUnits));
    }

    @Override
    public int getItemCount() {
        if (intervalList == null) {
            return 0;
        } else {
            return intervalList.size();
        }
    }

    public List<IntervalStatistics.Interval> swapData(List<IntervalStatistics.Interval> data, String category, boolean metricUnits) {
        this.category = category;
        this.metricUnits = metricUnits;

        if (intervalList == data) {
            return null;
        }

        intervalList = data;

        if (data != null) {
            this.notifyDataSetChanged();
        }

        return data;
    }

    /**
     * Defines the two modes of list items stacking: from top or from bottom.
     */
    public enum StackMode {
        STACK_FROM_BOTTOM,
        STACK_FROM_TOP
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        TextView distance;
        TextView rate;
        TextView gain;
        TextView loss;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            distance = itemView.findViewById(R.id.interval_item_distance);
            rate = itemView.findViewById(R.id.interval_item_rate);
            gain = itemView.findViewById(R.id.interval_item_gain);
            loss = itemView.findViewById(R.id.interval_item_loss);
        }
    }
}
