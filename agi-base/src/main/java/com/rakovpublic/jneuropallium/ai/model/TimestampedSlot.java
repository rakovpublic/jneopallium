package com.rakovpublic.jneuropallium.ai.model;

public class TimestampedSlot {
    private Object content;
    private int ttl;
    private double salience;
    private long createdAt;

    public TimestampedSlot() {}
    public TimestampedSlot(Object content, int ttl) {
        this.content = content; this.ttl = ttl;
        this.createdAt = System.currentTimeMillis(); this.salience = 1.0;
    }
    public TimestampedSlot(Object content, int ttl, double salience) {
        this(content, ttl); this.salience = salience;
    }

    public Object getContent() { return content; }
    public void setContent(Object content) { this.content = content; }
    public int getTtl() { return ttl; }
    public void setTtl(int ttl) { this.ttl = ttl; }
    public double getSalience() { return salience; }
    public void setSalience(double salience) { this.salience = salience; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void decrementTtl() { if (ttl > 0) ttl--; }
}
