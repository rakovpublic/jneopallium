/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * One syscall event sourced from eBPF (Linux) or ETW (Windows).
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class SyscallSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private int syscallNum;
    private int pid;
    private String procName;
    private long[] args;

    public SyscallSignal() { super(); this.loop = 1; this.epoch = 1L; this.timeAlive = 30; }

    public SyscallSignal(int syscallNum, int pid, String procName, long[] args) {
        this();
        this.syscallNum = syscallNum;
        this.pid = pid;
        this.procName = procName;
        this.args = args == null ? null : args.clone();
    }

    public int getSyscallNum() { return syscallNum; }
    public void setSyscallNum(int n) { this.syscallNum = n; }
    public int getPid() { return pid; }
    public void setPid(int p) { this.pid = p; }
    public String getProcName() { return procName; }
    public void setProcName(String p) { this.procName = p; }
    public long[] getArgs() { return args == null ? null : args.clone(); }
    public void setArgs(long[] a) { this.args = a == null ? null : a.clone(); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return SyscallSignal.class; }
    @Override public String getDescription() { return "SyscallSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        SyscallSignal c = new SyscallSignal(syscallNum, pid, procName, args);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
