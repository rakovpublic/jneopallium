package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class ActivityMeasurementSignal extends BaseSignal {
    private String regionId;
    private double meanFiringRate;
    private double variance;
    private double trend;
    private long tickWindow;

    public ActivityMeasurementSignal() { super(); this.loop = 1; this.epoch = 2L; }
    public ActivityMeasurementSignal(String regionId, double meanFiringRate, double variance, double trend, long tickWindow) {
        this(); this.regionId = regionId; this.meanFiringRate = meanFiringRate;
        this.variance = variance; this.trend = trend; this.tickWindow = tickWindow;
    }

    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }
    public double getMeanFiringRate() { return meanFiringRate; }
    public void setMeanFiringRate(double meanFiringRate) { this.meanFiringRate = meanFiringRate; }
    public double getVariance() { return variance; }
    public void setVariance(double variance) { this.variance = variance; }
    public double getTrend() { return trend; }
    public void setTrend(double trend) { this.trend = trend; }
    public long getTickWindow() { return tickWindow; }
    public void setTickWindow(long tickWindow) { this.tickWindow = tickWindow; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ActivityMeasurementSignal.class; }
    @Override public String getDescription() { return "ActivityMeasurementSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        ActivityMeasurementSignal c = new ActivityMeasurementSignal(regionId, meanFiringRate, variance, trend, tickWindow);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
