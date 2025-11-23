package com.aviscribe.common.enums;
// ... (此处省略了 getter, ctor, 参照 TaskStatus)
public enum SourceType { LOCAL(1), URL(2); 
    private final int value;
    SourceType(int value) { this.value = value; }
    public int getValue() { return value; }
}