package com.tonic.util;

import lombok.Getter;

@Getter
public class Pair<K, V> {
    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public static <K, V> Pair<K, V> of(K key, V value) {
        return new Pair<>(key, value);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            Pair<?, ?> pair = (Pair<?,?>)o;
            return this.key.equals(pair.key) && this.value.equals(pair.value);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return 31 * this.key.hashCode() + this.value.hashCode();
    }

    public String toString() {
        return "Pair{key=" + this.key + ", value=" + this.value + "}";
    }
}